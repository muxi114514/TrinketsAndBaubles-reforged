package xzeroair.trinkets.traits.abilities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.ManaCost;
import xzeroair.trinkets.traits.abilities.interfaces.IInteractionAbility;
import xzeroair.trinkets.traits.abilities.interfaces.IJumpAbility;
import xzeroair.trinkets.traits.abilities.interfaces.IMovementAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.util.config.abilities.ElytraFlightAbilityConfig;
import xzeroair.trinkets.util.handlers.Counter;
import xzeroair.trinkets.util.helpers.NBTHelper;

/**
 * 鞘翅飞行（无需鞘翅的滑翔）：下落中按跳跃开始滑翔，滑翔中再按跳跃获得升力，手持烟花右键加速，撞墙受伤。
 * 滑翔物理照搬原版鞘翅。对应 1.12 traits/abilities/AbilityElytraFlight。
 *
 * 两端分工：开始滑翔 / 升力 / 扣魔 / 撞墙伤害只在服务端判定；滑翔状态与烟花加速剩余时长经能力数据缓存下发，
 * 客户端据此在本地同步施加滑翔物理，服务端每 tick 标记速度变更以校正。
 * 正在使用原版鞘翅或创造飞行时让位，并在退出后延迟 7 tick 才允许重新开始（避免切换瞬间误触发）。
 *
 * 移植说明：
 * - isElytraFlying → isFallFlying；motion 字段 + velocityChanged → setDeltaMovement + hurtMarked；
 *   DamageSource.FLY_INTO_WALL → damageSources().flyIntoWall()。
 * - 1.12 连同消耗、升力等配置值一并经数据缓存下发客户端；1.20.1 的 SERVER 配置自动同步，只保留运行时状态。
 */
public class AbilityElytraFlight extends Ability implements ITickableAbility, IMovementAbility, IJumpAbility, IInteractionAbility {

    private static final String GLIDING_TAG = "GLIDING";
    private static final String FIREWORK_BOOST_TICKS_TAG = "FIREWORK_BOOST_TICKS";
    private static final String COST_TIMER = "elytra_flight_cost";
    private static final int VANILLA_FLIGHT_EXIT_DELAY = 7;

    private final ElytraFlightAbilityConfig config;
    private boolean gliding;
    private boolean liftRequested;
    private boolean vanillaFlightActive;
    private int vanillaFlightExitTicks;
    private int fireworkBoostTicks;
    private double glideHorizontalSpeed;

    public AbilityElytraFlight(@Nonnull ElytraFlightAbilityConfig config) {
        super(AbilityNames.ELYTRA_FLIGHT);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    public boolean isGliding() {
        return this.gliding;
    }

    // ── 输入 ──

    /** 只关心「跳跃刚按下」，其余移动输入无需发包 */
    @Override
    public boolean onMovement(Entity entity, int primaryState, boolean primaryDown, boolean auxiliaryDown,
            int left, int right, int forward, int back, int jump, int sneak, @Nullable CompoundTag payload) {
        return jump == KEY_PRESS && this.jump(entity, jump, primaryState, primaryDown, auxiliaryDown, payload);
    }

    @Override
    public boolean jump(Entity entity, int state, int primaryState, boolean primaryDown, boolean auxiliaryDown, @Nullable CompoundTag payload) {
        if (state != KEY_PRESS || !(entity instanceof LivingEntity living) || living.level().isClientSide) {
            return true;
        }
        if (this.gliding) {
            this.liftRequested = true;
        } else if (!this.vanillaFlightActive && this.vanillaFlightExitTicks == 0 && this.canStartFlying(living)) {
            final float cost = this.config.cost.get().floatValue();
            final MagicStats magic = MagicStats.get(living);
            if (cost <= 0F || (magic != null && magic.canSpendMana(cost))) {
                this.startFlight(living);
            }
        }
        return true;
    }

    /** 滑翔中手持烟花右键：按烟花飞行时长加速并消耗烟花 */
    @Override
    public void rightClickWithItem(LivingEntity entity, Level level, ItemStack stack, InteractionHand hand,
            @Nullable Direction face, BlockPos pos) {
        if (level.isClientSide || !this.gliding || !stack.is(Items.FIREWORK_ROCKET) || this.isUsingVanillaFlight(entity)) {
            return;
        }
        final CompoundTag fireworks = stack.getTagElement("Fireworks");
        final int flight = fireworks == null ? 0 : fireworks.getByte("Flight");
        this.fireworkBoostTicks = Math.max(this.fireworkBoostTicks,
                10 * (1 + flight) + entity.getRandom().nextInt(6) + entity.getRandom().nextInt(7));
        this.setChanged(true);
        level.addFreshEntity(new FireworkRocketEntity(level, stack.copy(), entity));
        if (!(entity instanceof Player player) || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    // ── tick ──

    @Override
    public void tickAbilityPre(LivingEntity entity) {
        if (!this.gliding) {
            return;
        }
        if (entity.getDeltaMovement().y > -0.5D) {
            entity.fallDistance = 1F;
        }
        if (this.config.collisionDamage.get() && !entity.level().isClientSide) {
            this.glideHorizontalSpeed = entity.getDeltaMovement().horizontalDistance();
        }
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        if (this.gliding && entity.hasEffect(MobEffects.LEVITATION)) {
            this.stopFlight();
            this.liftRequested = false;
            return;
        }
        if (this.isUsingVanillaFlight(entity)) {
            this.vanillaFlightActive = true;
            this.vanillaFlightExitTicks = 0;
            this.liftRequested = false;
            this.stopFlight();
            return;
        }
        if (this.vanillaFlightActive) {
            this.vanillaFlightActive = false;
            this.vanillaFlightExitTicks = VANILLA_FLIGHT_EXIT_DELAY;
        } else if (this.vanillaFlightExitTicks > 0) {
            this.vanillaFlightExitTicks--;
        }
        if (!this.gliding) {
            return;
        }
        if (this.config.collisionDamage.get() && !entity.level().isClientSide && entity.horizontalCollision) {
            final double horizontalSpeed = entity.getDeltaMovement().horizontalDistance();
            final float crashDamage = (float) ((this.glideHorizontalSpeed - horizontalSpeed) * 10.0D - 3.0D);
            if (crashDamage > 0F) {
                entity.hurt(entity.damageSources().flyIntoWall(), crashDamage);
            }
        }
        if (!canKeepFlying(entity)) {
            this.stopFlight();
            this.liftRequested = false;
            return;
        }
        final float cost = this.config.cost.get().floatValue();
        if (!entity.level().isClientSide && cost > 0F) {
            final MagicStats magic = MagicStats.get(entity);
            final Counter timer = this.tickHandler.getCounter(COST_TIMER, 20, true, true, true, true);
            if (magic == null || (timer != null && timer.Tick() && !magic.spendMana(cost))) {
                this.stopFlight();
                this.liftRequested = false;
                return;
            }
        }
        applyElytraMotion(entity);
        this.applyFireworkBoost(entity);
        if (this.liftRequested) {
            this.liftRequested = false;
            this.applyLift(entity);
        }
    }

    @Override
    public void onAbilityRemoved(LivingEntity entity) {
        this.stopFlight();
        this.liftRequested = false;
        this.vanillaFlightActive = false;
        this.vanillaFlightExitTicks = 0;
        this.tickHandler.removeCounter(COST_TIMER);
    }

    private void applyLift(LivingEntity entity) {
        if (!this.config.liftEnabled.get()) {
            return;
        }
        final float liftCost = this.config.liftCost.get().floatValue();
        final MagicStats magic = MagicStats.get(entity);
        if (liftCost <= 0F || (magic != null && magic.spendMana(liftCost))) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, this.config.liftStrength.get(), 0));
            entity.fallDistance = 0F;
            entity.hurtMarked = true;
        }
    }

    private void applyFireworkBoost(LivingEntity entity) {
        if (this.fireworkBoostTicks <= 0) {
            return;
        }
        this.fireworkBoostTicks--;
        final Vec3 look = entity.getLookAngle();
        final Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.add(
                look.x * 0.1D + (look.x * 1.5D - motion.x) * 0.5D,
                look.y * 0.1D + (look.y * 1.5D - motion.y) * 0.5D,
                look.z * 0.1D + (look.z * 1.5D - motion.z) * 0.5D));
        entity.hurtMarked = true;
    }

    private void startFlight(LivingEntity entity) {
        if (!this.gliding) {
            this.gliding = true;
            this.setChanged(true);
        }
        entity.fallDistance = 0F;
    }

    private void stopFlight() {
        if (this.gliding) {
            this.gliding = false;
            this.glideHorizontalSpeed = 0D;
            this.setChanged(true);
        }
    }

    private boolean canStartFlying(LivingEntity entity) {
        return !entity.hasEffect(MobEffects.LEVITATION)
                && !this.isUsingVanillaFlight(entity)
                && canKeepFlying(entity)
                && !this.gliding
                && entity.getDeltaMovement().y < 0D;
    }

    private static boolean canKeepFlying(LivingEntity entity) {
        return !entity.onGround() && !entity.isPassenger() && !entity.isInWater() && !entity.isInLava();
    }

    private boolean isUsingVanillaFlight(LivingEntity entity) {
        return entity.isFallFlying() || this.isCreativeFlying(entity);
    }

    /** 原版鞘翅的滑翔物理：先抵消原版每 tick 的空气阻力与重力，再按视线俯仰重新计算 */
    private static void applyElytraMotion(LivingEntity entity) {
        final Vec3 initial = entity.getDeltaMovement();
        double mx = initial.x / 0.91D;
        double my = initial.y / 0.98D + 0.08D;
        double mz = initial.z / 0.91D;
        final Vec3 look = entity.getLookAngle();
        final float pitch = entity.getXRot() * Mth.DEG_TO_RAD;
        final double horizontalSpeed = Math.sqrt(mx * mx + mz * mz);
        final double lookHorizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        final float pitchCos = Mth.cos(pitch);
        final float lift = (float) (pitchCos * pitchCos * Math.min(1.0D, look.length() / 0.4D));
        my += -0.08D + lift * 0.06D;
        if (my < 0.0D && lookHorizontal > 0.0D) {
            final double glidePull = my * -0.1D * lift;
            my += glidePull;
            mx += look.x * glidePull / lookHorizontal;
            mz += look.z * glidePull / lookHorizontal;
        }
        if (pitch < 0.0F && lookHorizontal > 0.0D) {
            final double climbPull = horizontalSpeed * -Mth.sin(pitch) * 0.04D;
            my += climbPull * 3.2D;
            mx -= look.x * climbPull / lookHorizontal;
            mz -= look.z * climbPull / lookHorizontal;
        }
        if (lookHorizontal > 0.0D) {
            mx += (look.x / lookHorizontal * horizontalSpeed - mx) * 0.1D;
            mz += (look.z / lookHorizontal * horizontalSpeed - mz) * 0.1D;
        }
        entity.setDeltaMovement(mx * 0.99D, my * 0.98D, mz * 0.99D);
        entity.hurtMarked = true;
    }

    // ── 同步：滑翔状态与烟花加速剩余时长下发客户端 ──

    @Nullable
    @Override
    public CompoundTag sendAbilityData() {
        final CompoundTag tag = new CompoundTag();
        tag.putBoolean(GLIDING_TAG, this.gliding);
        tag.putInt(FIREWORK_BOOST_TICKS_TAG, this.fireworkBoostTicks);
        return tag;
    }

    @Override
    public void loadDataCache(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        NBTHelper.hasBoolean(tag, GLIDING_TAG, value -> this.gliding = value);
        NBTHelper.hasInteger(tag, FIREWORK_BOOST_TICKS_TAG, value -> this.fireworkBoostTicks = Math.max(0, value));
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final double cost = this.config.cost.get();
        final boolean lift = this.config.liftEnabled.get();
        final double liftCost = this.config.liftCost.get();
        variables.number("cost", cost > 0, cost)
                .number("lift", lift, this.config.liftStrength.get())
                .number("liftcost", lift && liftCost > 0, liftCost)
                .flag("liftfree", liftCost <= 0)
                .flag("collision", this.config.collisionDamage.get());
    }

    @Override
    public ManaCost getManaCost() {
        final ManaCost upkeep = ManaCost.of(this.config.cost.get(), ManaCost.Unit.SECOND);
        return upkeep != null || !this.config.liftEnabled.get() ? upkeep : ManaCost.of(this.config.liftCost.get(), ManaCost.Unit.USE);
    }

    @Override
    public boolean isActiveAbility() {
        return true;
    }
}
