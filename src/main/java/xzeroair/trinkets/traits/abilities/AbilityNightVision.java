package xzeroair.trinkets.traits.abilities;

import javax.annotation.Nonnull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.init.ModItems;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.ManaCost;
import xzeroair.trinkets.traits.abilities.interfaces.IKeyBindInterface;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.traits.abilities.interfaces.IToggleAbility;
import xzeroair.trinkets.util.KeyNames;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.abilities.NightVisionAbilityConfig;
import xzeroair.trinkets.util.helpers.TrinketHelper;

/**
 * 夜视：按键开关；开启时给予夜视效果，失明时暂停。有魔力消耗时每秒扣一次，付不起则自动关闭。
 * 开关状态随能力存储持久化。对应 1.12 traits/abilities/AbilityNightVision。
 *
 * 移植说明：
 * - 效果保持 1.12 的 800 tick 有限时长并按期续上，**不改用 1.20.1 的无限时长**：
 *   「效果是否由本能力添加」只是运行时字段、不存盘，若施加无限时长，玩家下线重进后该标记丢失，
 *   效果会被当作外来药水永久保留，变成白嫖的永久夜视。
 *   1.12 在客户端用 setPotionDurationMax 把显示伪装成无限，纯属外观；续期间隔（免费每 10 秒 / 付费每秒）
 *   让剩余时长始终高于原版 200 tick 的闪烁阈值，故不做该伪装也不会闪烁。
 * - 其它来源（如种族）的夜视在佩戴发光戒指时取二者中较低的消耗（1.12 经 TrinketHelper 检查饰品）。
 */
public class AbilityNightVision extends Ability implements ITickableAbility, IToggleAbility, IKeyBindInterface {

    private static final int EFFECT_DURATION = 800;
    private static final int FREE_REFRESH_TICKS = 20 * 10;
    private static final int PAID_REFRESH_TICKS = 20;

    private final NightVisionAbilityConfig config;
    private boolean toggled = true;
    private boolean selfAdded = false;
    private int mode = -1;

    public AbilityNightVision(@Nonnull NightVisionAbilityConfig config) {
        super(AbilityNames.NIGHT_VISION);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public void tickAbility(@Nonnull LivingEntity entity) {
        if (entity.level().isClientSide) {
            return;
        }
        final boolean active = entity.hasEffect(MobEffects.NIGHT_VISION);
        if (!active) {
            this.selfAdded = false;
        }
        if (!this.isAbilityToggled() || entity.hasEffect(MobEffects.BLINDNESS)) {
            if (active && this.selfAdded) {
                entity.removeEffect(MobEffects.NIGHT_VISION);
                this.selfAdded = false;
            }
            return;
        }
        final float cost = this.effectiveCost(entity);
        if (cost <= 0F) {
            if (!active || (this.selfAdded && entity.tickCount % FREE_REFRESH_TICKS == 0)) {
                this.grant(entity);
            }
        } else if (!active) {
            this.payAndGrant(entity, cost);
        } else if (this.selfAdded && entity.tickCount % PAID_REFRESH_TICKS == 0) {
            this.payAndGrant(entity, cost);
        }
    }

    private float effectiveCost(LivingEntity entity) {
        final float cost = this.config.cost.get().floatValue();
        final var glowRing = TrinketsConfig.SERVER.items.glowRing.abilities;
        if (glowRing == null || this.config == glowRing.nightVision
                || !TrinketHelper.isEquipped(entity, stack -> stack.is(ModItems.GLOW_RING.get()))) {
            return cost;
        }
        return Math.min(cost, glowRing.nightVision.cost.get().floatValue());
    }

    private void payAndGrant(LivingEntity entity, float cost) {
        final MagicStats magic = MagicStats.get(entity);
        if (magic != null && magic.spendMana(cost)) {
            this.grant(entity);
        } else {
            this.toggleAbility(false);
        }
    }

    private void grant(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_DURATION, 0, false, false));
        this.selfAdded = true;
    }

    @Override
    public void onAbilityRemoved(@Nonnull LivingEntity entity) {
        if (this.selfAdded && entity.hasEffect(MobEffects.NIGHT_VISION)) {
            entity.removeEffect(MobEffects.NIGHT_VISION);
        }
        this.selfAdded = false;
    }

    // ── 开关 ──

    @Override
    public boolean isAbilityToggled() {
        return this.toggled;
    }

    @Override
    public int getToggleMode() {
        return this.mode;
    }

    @Override
    public IToggleAbility toggleAbility(boolean enabled) {
        if (this.toggled != enabled) {
            this.toggled = enabled;
            this.setChanged(true);
        }
        return this;
    }

    @Override
    public IToggleAbility toggleAbility(int value) {
        if (this.mode != value) {
            this.mode = value;
            this.setChanged(true);
        }
        return this.toggleAbility(value > 0);
    }

    // ── 按键 ──

    @Override
    public String getKey() {
        return KeyNames.DRAGONS_EYE_ABILITY;
    }

    @Override
    public String getAuxKey() {
        return KeyNames.AUX_KEY;
    }

    /** 按下即切换；只在服务端生效，客户端的预测调用不改状态，等存储同步回来 */
    @Override
    public boolean onKeyPress(@Nonnull Entity entity, boolean aux) {
        if (!entity.level().isClientSide) {
            this.toggleAbility(!this.isAbilityToggled());
        }
        return true;
    }

    // ── 存取 ──

    @Override
    public CompoundTag saveStorage(@Nonnull CompoundTag compound) {
        compound.putBoolean("isEnabled", this.isAbilityToggled());
        return compound;
    }

    @Override
    public void loadStorage(@Nonnull CompoundTag compound) {
        if (compound.contains("isEnabled")) {
            this.toggled = compound.getBoolean("isEnabled");
        }
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final double cost = this.config.cost.get();
        variables.keybind("key", KeyNames.DRAGONS_EYE_ABILITY)
                .translated("nightvision", true, MobEffects.NIGHT_VISION.getDescriptionId())
                .translated("blindness", true, MobEffects.BLINDNESS.getDescriptionId())
                .number("cost", cost > 0, cost)
                .flag("free", cost <= 0);
    }

    @Override
    public void describeStatus(DescriptionVariables variables, ItemStack source) {
        variables.toggle("state", true, this.isAbilityToggled());
    }

    @Override
    public ManaCost getManaCost() {
        return ManaCost.of(this.config.cost.get(), ManaCost.Unit.SECOND);
    }
}
