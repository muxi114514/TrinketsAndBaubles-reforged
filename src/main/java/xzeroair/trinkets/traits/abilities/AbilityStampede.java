package xzeroair.trinkets.traits.abilities;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.ManaCost;
import xzeroair.trinkets.traits.abilities.base.AbilityRaceSpecific;
import xzeroair.trinkets.util.KeyNames;
import xzeroair.trinkets.util.config.abilities.StampedeAbilityConfig;
import xzeroair.trinkets.util.handlers.Counter;

/**
 * 践踏冲锋（牛头人主动技）：按住种族技能键原地蓄力（魔力越多可蓄越久），松开后沿水平视线冲刺，
 * 撞到的实体受到随蓄力平方增长的伤害与击退。蓄力不足 10% 时松开无效。
 *
 * 移植说明：
 * - 蓄力期间的原地锁定只在本地客户端做（玩家移动由客户端权威），与 1.12 一致。
 * - 撞击判定与伤害只在服务端做：1.12 两端都跑，客户端那份只会让其他实体的本地位置抖动。
 * - 1.12 的 velocityChanged 对应 1.20.1 的 hurtMarked（触发向本人下发速度包）。
 */
public class AbilityStampede extends AbilityRaceSpecific {

    private static final String COUNTER = "heldCounter";
    private static final double BASE_KNOCKBACK = 0.6D;
    private static final double BASE_Y_KNOCKBACK = 0.3D;
    private static final float MIN_DAMAGE = 1F;
    /** 松开时蓄力不足该比例则取消 */
    private static final double MIN_CHARGE = 0.10D;
    /** 空中冲刺的速度倍率 */
    private static final double AIR_VELOCITY_MULTIPLIER = 0.25D;

    private final StampedeAbilityConfig config;
    private final Set<Entity> hitEntities = new HashSet<>();
    private double startX;
    private double startZ;

    public AbilityStampede(@Nonnull StampedeAbilityConfig config) {
        super(AbilityNames.STAMPEDE);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public boolean onKeyPress(Entity entity, boolean aux) {
        if (aux || !(entity instanceof LivingEntity living)) {
            return false;
        }
        final MagicStats magic = MagicStats.get(living);
        if (magic == null) {
            return true;
        }
        final int maxCharge = this.config.chargeTime.get();
        final double pct = Math.min(Mth.inverseLerp(magic.getMana(), 0, this.cost()), 1D);
        final int length = Math.min((int) (maxCharge * pct), maxCharge);
        final Counter counter = this.counter(length);
        counter.resetTick();
        counter.setLength(length);
        this.startX = living.getX();
        this.startZ = living.getZ();
        return pct >= MIN_CHARGE;
    }

    @Override
    public boolean onKeyDown(Entity entity, boolean aux) {
        if (aux || !(entity instanceof LivingEntity living)) {
            return false;
        }
        final MagicStats magic = MagicStats.get(living);
        if (magic == null) {
            return true;
        }
        final Counter counter = this.counter(this.config.chargeTime.get());
        final int tick = counter.getTick();
        final float progress = (float) Mth.inverseLerp(tick, 0, Math.max(counter.getLength(), 1));
        final float realCost = (float) (this.cost() * Mth.inverseLerp(tick, 0, this.config.chargeTime.get()));
        final boolean local = living instanceof Player && living.level().isClientSide;
        if (!counter.Tick()) {
            if (local) {
                if (living.tickCount % 4 == 0) {
                    living.level().playSound((Player) living, living.getX(), living.getY(), living.getZ(),
                            SoundEvents.POLAR_BEAR_STEP, SoundSource.PLAYERS, 0.3F, Math.min(0.4F + 0.6F * progress, 1F));
                }
                // 蓄力时锁住水平位置
                living.setDeltaMovement(0, living.getDeltaMovement().y, 0);
                living.setPos(this.startX, living.getY(), this.startZ);
            }
            magic.syncManaCostToHud(realCost);
            return realCost != magic.getMana();
        }
        if (local) {
            living.level().playSound((Player) living, living.getX(), living.getY(), living.getZ(),
                    SoundEvents.ZOMBIFIED_PIGLIN_ANGRY, SoundSource.PLAYERS, 0.4F, 0.2F);
        }
        magic.syncManaCostToHud(realCost);
        return false;
    }

    @Override
    public boolean onKeyRelease(Entity entity, boolean aux) {
        if (aux || !(entity instanceof LivingEntity living)) {
            return false;
        }
        final MagicStats magic = MagicStats.get(living);
        if (magic == null) {
            return true;
        }
        final Counter counter = this.counter(this.config.chargeTime.get());
        final int tick = counter.getTick();
        final int maxCharge = this.config.chargeTime.get();
        final float realCost = (float) (this.cost() * Mth.inverseLerp(tick, 0, maxCharge));
        if (tick > counter.getLength() * MIN_CHARGE && magic.spendMana(realCost)) {
            final double groundMultiplier = living.onGround() ? 1D : AIR_VELOCITY_MULTIPLIER;
            this.dash(living, tick, maxCharge, this.config.velocityMin.get() * groundMultiplier,
                    this.config.velocityMax.get() * groundMultiplier);
        }
        this.reset(living);
        return true;
    }

    private void dash(LivingEntity entity, int chargeTicks, int maxChargeTicks, double minVelocity, double maxVelocity) {
        double curve = (double) Math.min(chargeTicks, maxChargeTicks) / maxChargeTicks;
        curve *= curve;
        final double velocity = minVelocity + (maxVelocity - minVelocity) * curve;
        final Vec3 look = entity.getLookAngle();
        final Vec3 horizontal = new Vec3(look.x, 0, look.z).normalize();
        entity.setDeltaMovement(horizontal.x * velocity, 0, horizontal.z * velocity);
        entity.hurtMarked = true;
        if (!entity.level().isClientSide) {
            this.hitAlongPath(entity, curve);
        }
    }

    private void hitAlongPath(LivingEntity entity, double curve) {
        final AABB box = entity.getBoundingBox();
        final AABB swept = box.minmax(box.move(entity.getDeltaMovement())).inflate(0.5D);
        final boolean pvp = entity instanceof ServerPlayer player && player.server.isPvpAllowed();
        final float maxDamage = this.config.attackDamage.get().floatValue();
        for (Entity target : entity.level().getEntities(entity, swept)) {
            if (!this.canHit(entity, target) || !this.hitEntities.add(target)) {
                continue;
            }
            if (target instanceof Player && !pvp) {
                continue;
            }
            final float damage = (float) (MIN_DAMAGE + (maxDamage - MIN_DAMAGE) * curve);
            final DamageSource source = entity instanceof Player player
                    ? entity.damageSources().playerAttack(player)
                    : entity.damageSources().flyIntoWall();
            target.hurt(source, damage);
            final Vec3 look = entity.getLookAngle().normalize();
            final double strength = BASE_KNOCKBACK * curve;
            target.setDeltaMovement(target.getDeltaMovement().add(look.x * strength, BASE_Y_KNOCKBACK * curve, look.z * strength));
            target.hurtMarked = true;
        }
    }

    private boolean canHit(LivingEntity entity, Entity target) {
        if (!target.isAlive() || target.level() != entity.level() || target instanceof HangingEntity) {
            return false;
        }
        final Vec3 start = entity.getEyePosition();
        final Vec3 end = target.getBoundingBox().getCenter();
        return entity.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity))
                .getType() == HitResult.Type.MISS;
    }

    private void reset(LivingEntity entity) {
        final Counter counter = this.tickHandler.getCounter(COUNTER);
        if (counter != null) {
            counter.resetTick();
            counter.setLength(this.config.chargeTime.get());
        }
        this.hitEntities.clear();
        final MagicStats magic = MagicStats.get(entity);
        if (magic != null) {
            magic.syncManaCostToHud(0);
        }
    }

    private Counter counter(int length) {
        return this.tickHandler.getCounter(COUNTER, length, false, true, false, true, false);
    }

    private double cost() {
        return this.config.attackCost.get();
    }

    @Override
    public void describe(DescriptionVariables variables) {
        variables.keybind("key", KeyNames.RACE_ABILITY)
                .seconds("charge", true, this.config.chargeTime.get())
                .percent("mincharge", true, MIN_CHARGE)
                .number("vmin", this.config.velocityMin.get())
                .number("vmax", this.config.velocityMax.get())
                .number("air", AIR_VELOCITY_MULTIPLIER)
                .number("mindamage", MIN_DAMAGE)
                .number("damage", this.config.attackDamage.get())
                .number("knockback", BASE_KNOCKBACK)
                .number("lift", BASE_Y_KNOCKBACK)
                .number("cost", this.config.attackCost.get());
    }

    @Override
    public ManaCost getManaCost() {
        return ManaCost.of(this.config.attackCost.get(), ManaCost.Unit.FULL_CHARGE);
    }
}
