package xzeroair.trinkets.traits.abilities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.init.ModEffects;
import xzeroair.trinkets.network.EffectsRenderPacket;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.ManaCost;
import xzeroair.trinkets.traits.abilities.interfaces.IMovementAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.util.KeyNames;
import xzeroair.trinkets.util.compat.DodgeCompat;
import xzeroair.trinkets.util.compat.ModCompat;
import xzeroair.trinkets.util.config.abilities.DodgeAbilityConfig;
import xzeroair.trinkets.util.handlers.Counter;

/**
 * 闪避：地面上双击方向键（或按住闪避键 + 方向键）朝该方向瞬移一小段，可麻痹身边的生物。
 *
 * 流程：客户端按方向键时生成载荷（方向 + 本次计数），服务端在 3 tick 窗口内累计到 2 次即触发；
 * 位移只在服务端计算，经 hurtMarked 下发给客户端。客户端侧只做魔力是否足够的预判。
 *
 * 移植说明：与 1.12 相同，闪避由其它模组接管时（Elenai Dodge 2，或 Talents 侧步技能已开启）让出双击闪避，
 * 改由其闪避触发本能力的麻痹效果并扣魔力（见 util/compat/DodgeCompat）。
 */
public class AbilityDodge extends Ability implements ITickableAbility, IMovementAbility {

    private static final int DIRECTION_BACK = 0;
    private static final int DIRECTION_LEFT = 1;
    private static final int DIRECTION_FORWARD = 2;
    private static final int DIRECTION_RIGHT = 3;
    private static final String PRESSES_TAG = "Presses";
    private static final String DIRECTION_TAG = "Direction";
    private static final String COUNTER = "lastKeyPress";
    private static final double DODGE_SPEED = 1.25D;
    private static final int STUN_DURATION = 3 * 20;
    private static final int EFFECT_COLOR = 12648447;
    /** 与 KeyHandler 状态值一致 */
    private static final int STATE_PRESS = 0;
    private static final int STATE_DOWN = 1;

    private final DodgeAbilityConfig config;
    private int keyPresses;
    private int direction = -1;
    private int lastDodgeTick = -1;

    public AbilityDodge(@Nonnull DodgeAbilityConfig config) {
        super(AbilityNames.DODGING);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        final Counter counter = this.tickHandler.getCounter(COUNTER);
        if (counter != null && counter.Tick()) {
            this.resetPresses();
        }
    }

    @Override
    public String getKey() {
        return this.config.keybindMovement.get() ? KeyNames.ARCING_ORB_DODGE : "";
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public CompoundTag createMovementPayload(Entity entity, int primaryState, boolean primaryDown, boolean auxiliaryDown,
            int left, int right, int forward, int back, int jump, int sneak) {
        final int selected = selectDirection(primaryState, left, right, forward, back);
        if (selected < 0) {
            return null;
        }
        final CompoundTag payload = new CompoundTag();
        // 按键模式下一次按下即算作双击
        payload.putInt(PRESSES_TAG, this.config.keybindMovement.get() ? (primaryDown ? 2 : 0) : 1);
        payload.putInt(DIRECTION_TAG, selected);
        return payload;
    }

    @Override
    public boolean onMovement(Entity entity, int primaryState, boolean primaryDown, boolean auxiliaryDown,
            int left, int right, int forward, int back, int jump, int sneak, @Nullable CompoundTag payload) {
        final int selected = readDirection(payload);
        final int state = directionState(selected, left, right, forward, back);
        final int presses = readPresses(payload, selected);
        if ((state != STATE_PRESS && primaryState != STATE_PRESS) || presses <= 0 || !this.recordPress(selected, presses)) {
            return true;
        }
        return this.tryDodge(entity, selected);
    }

    private boolean tryDodge(Entity entity, int selected) {
        if (!entity.onGround() || entity.isShiftKeyDown() || !(entity instanceof LivingEntity living)
                || (entity instanceof Player player && DodgeCompat.isHandledExternally(player))) {
            return false;
        }
        final MagicStats magic = MagicStats.get(living);
        final float cost = this.config.cost.get().floatValue();
        if (entity.level().isClientSide) {
            return magic == null || magic.canSpendMana(cost);
        }
        final int cooldown = this.config.cooldown.get();
        if (cooldown > 0 && this.lastDodgeTick >= 0 && entity.tickCount - this.lastDodgeTick < cooldown) {
            return false;
        }
        final Vec3 motion = dodgeMotion(entity, selected);
        if (motion == null || (magic != null && !magic.spendMana(cost))) {
            return false;
        }
        this.lastDodgeTick = entity.tickCount;
        entity.setDeltaMovement(motion.x, 0.3D, motion.z);
        entity.hurtMarked = true;
        this.applyDodgeEffects(living);
        return true;
    }

    /** 由其它模组的闪避触发：扣魔力成功则施加闪避效果（1.12 ElenaiDodgeCompat#DodgeEvent） */
    public void onExternalDodge(LivingEntity entity) {
        final MagicStats magic = MagicStats.get(entity);
        if (magic == null || magic.spendMana(this.config.cost.get().floatValue())) {
            this.applyDodgeEffects(entity);
        }
    }

    private void applyDodgeEffects(LivingEntity entity) {
        new EffectsRenderPacket(EffectsRenderPacket.LIGHTNING_ORB, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5F,
                entity.getZ(), entity.getX(), entity.getY(), entity.getZ(), EFFECT_COLOR, 0.8F, 1.0F).sendNear(entity);
        if (!this.config.stuns.get()) {
            return;
        }
        final double radius = this.config.stunRadius.get();
        final MobEffect paralysis = ModEffects.PARALYSIS.get();
        for (LivingEntity target : entity.level().getEntitiesOfClass(LivingEntity.class,
                entity.getBoundingBox().inflate(radius, 1.0D, radius), target -> target != entity)) {
            final MobEffectInstance active = target.getEffect(paralysis);
            if (active == null || active.getDuration() < STUN_DURATION) {
                target.addEffect(new MobEffectInstance(paralysis, STUN_DURATION, 0, false, false));
            }
        }
    }

    @Nullable
    private static Vec3 dodgeMotion(Entity entity, int selected) {
        final Vec3 look = entity.getLookAngle();
        final Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-6D) {
            return null;
        }
        final Vec3 dir = flat.normalize();
        return switch (selected) {
            case DIRECTION_LEFT -> new Vec3(dir.z, 0, -dir.x).scale(DODGE_SPEED);
            case DIRECTION_RIGHT -> new Vec3(-dir.z, 0, dir.x).scale(DODGE_SPEED);
            case DIRECTION_FORWARD -> dir.scale(DODGE_SPEED);
            case DIRECTION_BACK -> dir.scale(-DODGE_SPEED);
            default -> null;
        };
    }

    /** 新按下的方向键优先；按键模式下按住闪避键时，持续按住的方向键也算 */
    private static int selectDirection(int primaryState, int left, int right, int forward, int back) {
        if (isSelected(primaryState, left)) {
            return DIRECTION_LEFT;
        }
        if (isSelected(primaryState, right)) {
            return DIRECTION_RIGHT;
        }
        if (isSelected(primaryState, forward)) {
            return DIRECTION_FORWARD;
        }
        if (isSelected(primaryState, back)) {
            return DIRECTION_BACK;
        }
        return -1;
    }

    private static boolean isSelected(int primaryState, int state) {
        return state == STATE_PRESS || (primaryState == STATE_PRESS && state == STATE_DOWN);
    }

    private static int readDirection(@Nullable CompoundTag payload) {
        if (payload == null || !payload.contains(DIRECTION_TAG, Tag.TAG_ANY_NUMERIC)) {
            return -1;
        }
        final int value = payload.getInt(DIRECTION_TAG);
        return value >= DIRECTION_BACK && value <= DIRECTION_RIGHT ? value : -1;
    }

    private static int directionState(int selected, int left, int right, int forward, int back) {
        return switch (selected) {
            case DIRECTION_LEFT -> left;
            case DIRECTION_RIGHT -> right;
            case DIRECTION_FORWARD -> forward;
            case DIRECTION_BACK -> back;
            default -> -1;
        };
    }

    private static int readPresses(@Nullable CompoundTag payload, int selected) {
        if (payload == null || !payload.contains(PRESSES_TAG, Tag.TAG_ANY_NUMERIC) || readDirection(payload) != selected) {
            return 0;
        }
        final int presses = payload.getInt(PRESSES_TAG);
        return presses >= 0 && presses <= 2 ? presses : 0;
    }

    private void resetPresses() {
        this.keyPresses = 0;
        this.direction = -1;
        this.tickHandler.removeCounter(COUNTER);
    }

    /** 同方向在 3 tick 窗口内累计到 2 次按压返回 true */
    private boolean recordPress(int selected, int presses) {
        if (this.direction != selected) {
            this.resetPresses();
        }
        if (this.tickHandler.getCounter(COUNTER) == null) {
            this.tickHandler.getCounter(COUNTER, 3, true, true, true, false);
            this.direction = selected;
            this.keyPresses = presses;
        } else {
            this.keyPresses += presses;
        }
        if (this.keyPresses < 2) {
            return false;
        }
        this.resetPresses();
        return true;
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final boolean keybind = this.config.keybindMovement.get();
        final boolean stuns = this.config.stuns.get();
        variables.flag("doubletap", !keybind)
                .flag("keymode", keybind)
                .keybind("key", KeyNames.ARCING_ORB_DODGE)
                .number("speed", DODGE_SPEED)
                .number("cost", this.config.cost.get())
                .seconds("cooldown", true, this.config.cooldown.get())
                .number("radius", stuns, this.config.stunRadius.get())
                .seconds("stun", stuns, STUN_DURATION)
                .translated("paralysis", stuns, ModEffects.PARALYSIS.get().getDescriptionId())
                .flag("external", ModCompat.elenaiDodge() || ModCompat.talents());
    }

    @Override
    public ManaCost getManaCost() {
        return ManaCost.of(this.config.cost.get(), ManaCost.Unit.USE);
    }

    @Override
    public boolean isActiveAbility() {
        return true;
    }
}
