package xzeroair.trinkets.traits.abilities;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.client.particles.ClientEffects;
import xzeroair.trinkets.enums.ActivationMethod;
import xzeroair.trinkets.network.EffectsRenderPacket;
import xzeroair.trinkets.network.StatusMessagePacket;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.ManaCost;
import xzeroair.trinkets.traits.abilities.interfaces.IKeyBindInterface;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.traits.abilities.interfaces.IToggleAbility;
import xzeroair.trinkets.traits.abilities.treasure.TreasureTarget;
import xzeroair.trinkets.util.KeyNames;
import xzeroair.trinkets.util.config.abilities.GreedyEyesAbilityConfig;
import xzeroair.trinkets.util.handlers.Counter;
import xzeroair.trinkets.util.helpers.ChunkSafety;

/**
 * 贪婪之眼（寻宝）：按龙之眼目标键循环切换要寻找的宝藏（辅助键 + 目标键反向切换），
 * 服务端定期扫描周围方块/实体，把结果同步给本人客户端，由客户端在目标处闪烁光点，潜行时伴随龙吼提示距离。
 *
 * 移植说明：
 * - 扫描逐格读方块前用 ChunkSafety 只取已加载区块，未加载的格直接跳过（1.12 直接 getBlockState 可能拉起区块）。
 * - 扫描结果会随能力同步包发给追踪者，光点与龙吼只在本地玩家自己的客户端上显示。
 */
public class AbilityGreedyEyes extends Ability implements ITickableAbility, IToggleAbility, IKeyBindInterface {

    private static final String COUNTER = "refresh_rate";
    private static final String MESSAGE_PREFIX = "xat.ability.greedy_eyes.treasurefinder";
    private static final int OFF = -1;
    /** 龙吼音量随距离线性衰减，超过该距离无声 */
    private static final double GROWL_RANGE = 10.0D;

    private final GreedyEyesAbilityConfig config;
    /** 按距离排序的发现点 */
    private final TreeMap<Double, Vec3> found = new TreeMap<>();
    private List<? extends String> targetSource;
    private List<TreasureTarget> targets = List.of();
    private int targetIndex = OFF;
    private String targetEntry = "";
    private int targetColor;
    private boolean wasEmptyLastSync = true;

    public AbilityGreedyEyes(@Nonnull GreedyEyesAbilityConfig config) {
        super(AbilityNames.GREEDY_EYES);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    // ── 切换 ──

    @Override
    public int getToggleMode() {
        return this.targetIndex;
    }

    @Override
    public boolean isAbilityToggled() {
        return this.targetIndex > OFF;
    }

    @Override
    public IToggleAbility toggleAbility(boolean forward) {
        return this.toggleAbility(this.targetIndex + (forward ? 1 : -1));
    }

    /** 越过末尾回到关闭，低于关闭则绕到最后一个 */
    @Override
    public IToggleAbility toggleAbility(int value) {
        final int size = this.targets().size();
        this.targetIndex = value;
        if (this.targetIndex >= size) {
            this.targetIndex = OFF;
        } else if (this.targetIndex < OFF) {
            this.targetIndex = size - 1;
        }
        return this;
    }

    public String getTargetEntry() {
        return this.targetEntry;
    }

    // ── 按键 ──

    @Override
    public String getKey() {
        return KeyNames.DRAGONS_EYE_TARGET;
    }

    @Override
    public String getAuxKey() {
        return KeyNames.AUX_KEY;
    }

    @Override
    public boolean onKeyPress(Entity entity, boolean aux) {
        final int frequency = this.config.frequency.get();
        this.counter().setTick(Math.max(frequency / 5, 30));
        if (entity.level().isClientSide) {
            if (!this.found.isEmpty()) {
                this.found.clear();
                this.setChanged(true);
            }
            return true;
        }
        this.toggleAbility(!aux);
        if (this.isAbilityToggled()) {
            final TreasureTarget target = this.targets().get(this.targetIndex);
            this.targetEntry = target.entry();
            this.targetColor = target.color();
            final String name = target.displayName();
            final StatusMessagePacket message = new StatusMessagePacket(MESSAGE_PREFIX + (name.isEmpty() ? ".notfound" : ".on"), true)
                    .withKey("looking", "xat.tooltip.on");
            if (name.isEmpty()) {
                message.with("target", target.entry());
            } else if (target.isDisplayNameTranslatable()) {
                message.withKey("target", name);
            } else {
                message.with("target", name);
            }
            message.send(entity);
        } else {
            this.turnOff(entity);
        }
        return true;
    }

    private void turnOff(Entity entity) {
        this.targetEntry = "";
        new StatusMessagePacket(MESSAGE_PREFIX + ".off", true).withKey("looking", "xat.tooltip.off").send(entity);
    }

    // ── 扫描 ──

    @Override
    public void tickAbility(LivingEntity entity) {
        final Level level = entity.level();
        if (!level.isClientSide && !this.isAbilityToggled()) {
            return;
        }
        final int vertical = this.config.rangeVertical.get();
        final int horizontal = this.config.rangeHorizontal.get();
        if (vertical <= 0 || horizontal <= 0) {
            return;
        }
        if (!this.isFirstUpdate() && !this.counter().Tick()) {
            return;
        }
        if (level.isClientSide) {
            this.showFound(entity);
            return;
        }
        final float cost = this.config.cost.get().floatValue();
        final MagicStats magic = MagicStats.get(entity);
        if (cost > 0F && magic != null && !magic.spendMana(cost)) {
            this.toggleAbility(OFF);
            this.turnOff(entity);
            return;
        }
        this.scan(entity, entity.getBoundingBox().inflate(horizontal, vertical, horizontal));
    }

    private void scan(LivingEntity entity, AABB area) {
        this.found.clear();
        final TreasureTarget target = this.isAbilityToggled() && this.targetIndex < this.targets().size()
                ? this.targets().get(this.targetIndex) : null;
        if (target == null) {
            return;
        }
        final Vec3 origin = entity.position();
        if (target.isEntity()) {
            for (Entity candidate : entity.level().getEntities(target.entityType(), area, e -> true)) {
                this.found.put(candidate.position().distanceTo(origin), candidate.position());
            }
        } else {
            final ChunkSafety.ChunkCache chunks = new ChunkSafety.ChunkCache(entity.level());
            final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = Mth.floor(area.minX); x <= Mth.floor(area.maxX); x++) {
                for (int y = Mth.floor(area.minY); y <= Mth.floor(area.maxY); y++) {
                    for (int z = Mth.floor(area.minZ); z <= Mth.floor(area.maxZ); z++) {
                        pos.set(x, y, z);
                        final BlockState state = chunks.getBlockState(pos);
                        if (state != null && !state.isAir() && target.matches(state)) {
                            final Vec3 point = new Vec3(x, y, z);
                            this.found.put(point.distanceTo(origin), point);
                        }
                    }
                }
            }
        }
        final boolean empty = this.found.isEmpty();
        if (!(this.wasEmptyLastSync && empty)) {
            this.wasEmptyLastSync = empty;
            this.setChanged(true);
        }
    }

    /** 客户端：在发现点闪烁光点，距离较远时按配置播放龙吼 */
    private void showFound(LivingEntity entity) {
        if (this.found.isEmpty() || !(entity instanceof Player player) || !player.isLocalPlayer()) {
            return;
        }
        final Map.Entry<Double, Vec3> closest = this.found.firstEntry();
        if (closest.getKey() > 1.8D) {
            this.growl(player, closest.getValue(), closest.getKey());
        }
        if (this.config.closest.get()) {
            this.spawnParticles(closest.getValue(), 3);
            return;
        }
        int count = 0;
        final int limit = this.config.particles.get();
        for (Vec3 point : this.found.values()) {
            if (count >= limit) {
                break;
            }
            this.spawnParticles(point, 3);
            count += 3;
        }
    }

    private void spawnParticles(Vec3 point, int amount) {
        final int color = this.targetColor;
        for (int i = 0; i < amount; i++) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEffects.play(EffectsRenderPacket.GREED,
                    point.x, point.y, point.z, 0, 0, 0, color, 1.0F, 1.0F));
        }
    }

    private void growl(Player player, Vec3 point, double distance) {
        if (!this.config.growlActivation.get().isActive(player.isShiftKeyDown())) {
            return;
        }
        final float volume = this.config.volume.get() / 100.0F;
        final float scaled = Mth.clamp((float) (1.0D - distance / GROWL_RANGE) * volume, 0.0F, 1.0F);
        player.level().playSound(player, point.x, point.y, point.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, scaled, 1.0F);
    }

    // ── 同步 ──

    @Nullable
    @Override
    public CompoundTag sendAbilityData() {
        final CompoundTag tag = new CompoundTag();
        tag.putBoolean("Empty", this.wasEmptyLastSync);
        if (!this.found.isEmpty()) {
            tag.putString("Target", this.targetEntry);
            tag.putInt("Color", this.targetColor);
            int index = 0;
            for (Map.Entry<Double, Vec3> entry : this.found.entrySet()) {
                final CompoundTag point = new CompoundTag();
                point.putDouble("Distance", entry.getKey());
                point.putDouble("x", entry.getValue().x);
                point.putDouble("y", entry.getValue().y);
                point.putDouble("z", entry.getValue().z);
                tag.put(String.valueOf(index++), point);
            }
        }
        return tag;
    }

    @Override
    public void loadDataCache(CompoundTag tag) {
        this.found.clear();
        if (tag == null || tag.isEmpty()) {
            return;
        }
        this.wasEmptyLastSync = tag.getBoolean("Empty");
        this.targetEntry = tag.getString("Target");
        this.targetColor = tag.getInt("Color");
        if (this.targetEntry.isEmpty()) {
            return;
        }
        for (String key : tag.getAllKeys()) {
            final CompoundTag point = tag.getCompound(key);
            if (point.contains("Distance") && point.contains("x")) {
                this.found.put(point.getDouble("Distance"), new Vec3(point.getDouble("x"), point.getDouble("y"), point.getDouble("z")));
            }
        }
    }

    private Counter counter() {
        return this.tickHandler.getCounter(COUNTER, this.config.frequency.get(), true, true, true, false);
    }

    /** 配置重载后列表对象会更换，发现即重新解析 */
    private List<TreasureTarget> targets() {
        final List<? extends String> source = this.config.targets.get();
        if (source != this.targetSource) {
            this.targetSource = source;
            this.targets = TreasureTarget.parseAll(source);
        }
        return this.targets;
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final boolean closest = this.config.closest.get();
        final double cost = this.config.cost.get();
        final ActivationMethod growl = this.config.growlActivation.get();
        variables.keybind("key", KeyNames.DRAGONS_EYE_TARGET)
                .keybind("aux", KeyNames.AUX_KEY)
                .number("count", this.targets().size())
                .seconds("interval", true, this.config.frequency.get())
                .number("horizontal", this.config.rangeHorizontal.get())
                .number("vertical", this.config.rangeVertical.get())
                .flag("closest", closest)
                .number("points", !closest, (this.config.particles.get() + 2) / 3)
                .number("cost", cost > 0, cost)
                .activation("growl", growl != ActivationMethod.NEVER, growl)
                .number("growlrange", GROWL_RANGE);
    }

    /** 当前目标：方块 / 实体名按语言翻译，标签显示可读名 */
    @Override
    public void describeStatus(DescriptionVariables variables, ItemStack source) {
        final TreasureTarget target = this.targetEntry.isEmpty() ? null
                : this.targets().stream().filter(candidate -> candidate.entry().equals(this.targetEntry)).findFirst().orElse(null);
        final String name = target == null ? this.targetEntry : target.displayName();
        if (target != null && target.isDisplayNameTranslatable()) {
            variables.translated("target", true, name);
        } else {
            variables.option("target", !name.isEmpty(), name);
        }
        variables.flag("none", this.targetEntry.isEmpty());
    }

    @Override
    public ManaCost getManaCost() {
        return ManaCost.of(this.config.cost.get(), ManaCost.Unit.USE);
    }
}
