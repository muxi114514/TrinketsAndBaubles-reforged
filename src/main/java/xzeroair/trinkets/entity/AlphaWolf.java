package xzeroair.trinkets.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import xzeroair.trinkets.init.ModDamageTypes;
import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 狼王：哥布林「骑狼」时把自己驯服的狼临时替换成的坐骑。被骑乘者操控移动，按技能键发动冲撞撕咬，
 * 骑手下来（或死亡、被移除）时还原成原来那只狼。对应 1.12 entity/AlphaWolf。
 *
 * 移植说明：
 * - 1.12 覆写 travel 自行处理骑乘操控；1.20.1 原版已为可骑乘生物提供 tickRidden / getRiddenInput / getRiddenSpeed 钩子。
 * - 1.12 在 setDead 里还原原狼；1.20.1 改在 remove 中只对「真正销毁」（被杀/丢弃）还原，
 *   区块卸载、随玩家下线保存时不还原，避免复制出两只狼。
 * - 1.12 的跳跃力来自自定义跳跃属性（基础值 0.7），这里直接覆写 getJumpPower。
 */
public class AlphaWolf extends Wolf {

    private static final String STORED_WOLF_TAG = "xat.wolf.stored";
    private static final String SUMMONED_TAG = "xat:summoned";
    private static final UUID ATTACK_BONUS_UUID = UUID.fromString("76c436ad-d830-48ff-8b3c-fa3bcc1891c2");
    private static final int RETALIATION_COOLDOWN_TICKS = 40;
    private static final double RETALIATION_RANGE_SQUARED = 25.0D;
    private static final float RETALIATION_DAMAGE = 9.0F;
    private static final float JUMP_POWER = 0.7F;

    @Nullable
    private CompoundTag storedWolf;
    private int retaliationCooldown;

    public AlphaWolf(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
        final AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null && attack.getModifier(ATTACK_BONUS_UUID) == null) {
            attack.addPermanentModifier(new AttributeModifier(ATTACK_BONUS_UUID, "xat.alpha_wolf.attack", 4.0D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    /** 无 AI：完全由骑手操控 */
    @Override
    protected void registerGoals() {
    }

    // ── tick ──

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && !TrinketsConfig.SERVER.races.goblin.abilities.wolfRider.isEnabled()) {
            this.discard();
            return;
        }
        if (!this.isAlive() || this.level().isClientSide) {
            return;
        }
        if (this.retaliationCooldown > 0) {
            this.retaliationCooldown--;
        }
        final LivingEntity driver = this.getControllingPassenger();
        if (driver != null) {
            this.shareBuffs(driver);
        }
        if (this.tickCount > 1 && !this.getPassengers().contains(this.getOwner())) {
            this.discard();
        }
    }

    private void shareBuffs(LivingEntity driver) {
        if (!driver.hasEffect(MobEffects.DAMAGE_BOOST)) {
            driver.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0, false, false));
        }
        if (!driver.hasEffect(MobEffects.REGENERATION)) {
            driver.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false));
        }
        if (!this.hasEffect(MobEffects.REGENERATION)) {
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, false));
        }
        final MobEffectInstance fireResistance = driver.getEffect(MobEffects.FIRE_RESISTANCE);
        if (fireResistance != null) {
            this.addEffect(new MobEffectInstance(fireResistance));
        }
        final MobEffectInstance invisibility = driver.getEffect(MobEffects.INVISIBILITY);
        if (invisibility != null) {
            this.addEffect(new MobEffectInstance(invisibility));
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            this.restoreStoredWolf();
        }
        super.remove(reason);
    }

    private void restoreStoredWolf() {
        if (!this.hasStoredWolf() || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        EntityType.create(this.storedWolf, level).ifPresent(wolf -> {
            wolf.moveTo(this.getX(), this.getY() + 1.1D, this.getZ(), this.getYRot(), 0.0F);
            if (level.addFreshEntity(wolf)) {
                this.storedWolf = null;
            }
        });
    }

    // ── 骑乘 ──

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        this.setRot(player.getYRot(), player.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
        this.setSprinting(player.isSprinting());
        if (this.onGround()) {
            this.setJumping(false);
        }
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 input) {
        final float forward = player.zza <= 0.0F ? player.zza * 0.25F : player.zza;
        return new Vec3(player.xxa * 0.5F, 0.0D, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    @Override
    public double getPassengersRidingOffset() {
        return this.getBbHeight();
    }

    @Override
    public boolean dismountsUnderwater() {
        return false;
    }

    @Override
    protected float getJumpPower() {
        return JUMP_POWER * this.getBlockJumpFactor() + this.getJumpBoostPower();
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    // ── 战斗 ──

    /** 骑手视线方向上的一串目标：第一个全伤、第二个半伤、之后 20%，并带着狼向前扑 */
    public void mountedAttack(Player player, double maxDistance) {
        final Vec3 start = this.getEyePosition();
        final Vec3 look = player.getLookAngle();
        final Vec3 end = start.add(look.scale(maxDistance));
        final AABB search = new AABB(start, end).inflate(1.0D);
        final List<LivingEntity> targets = new ArrayList<>();
        for (Entity entity : this.level().getEntities(this, search, e -> e instanceof LivingEntity && e != player)) {
            if (entity.getBoundingBox().inflate(0.3D).clip(start, end).isPresent()
                    && this.wantsToAttack((LivingEntity) entity, player)) {
                targets.add((LivingEntity) entity);
            }
        }
        targets.sort(Comparator.comparingDouble(target -> target.position().subtract(start).dot(look)));
        final float baseDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean hit = false;
        for (int i = 0; i < targets.size(); i++) {
            final float multiplier = i == 0 ? 1.0F : i == 1 ? 0.5F : 0.2F;
            hit = this.attack(targets.get(i), ModDamageTypes.source(this.level(), DamageTypes.MOB_ATTACK, this, player), baseDamage * multiplier) || hit;
        }
        final double dx = end.x - this.getX();
        final double dz = end.z - this.getZ();
        final double distance = Math.sqrt(dx * dx + dz * dz);
        Vec3 motion = this.getDeltaMovement();
        if (distance >= 1.0E-4D) {
            motion = motion.add(dx / distance * 4.0D * 0.4D + motion.x * 0.6D, 0.0D, dz / distance * 4.0D * 0.4D + motion.z * 0.6D);
        }
        if (hit) {
            motion = new Vec3(motion.x, 0.42D, motion.z);
        }
        this.setDeltaMovement(motion);
        this.hurtMarked = true;
        this.swing(InteractionHand.MAIN_HAND);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        final Entity attacker = source.getEntity();
        final boolean fromRider = this.isVehicle() && attacker != null && this.hasPassenger(attacker);
        final boolean damaged = !fromRider && super.hurt(source, amount);
        if (damaged && !this.level().isClientSide) {
            this.retaliate(attacker);
        }
        return damaged;
    }

    private void retaliate(@Nullable Entity attacker) {
        if (this.retaliationCooldown > 0 || !(attacker instanceof LivingEntity target)) {
            return;
        }
        final LivingEntity owner = this.getOwner();
        if (owner == null || !this.wantsToAttack(target, owner) || !this.hasLineOfSight(target)
                || this.distanceToSqr(target) > RETALIATION_RANGE_SQUARED) {
            return;
        }
        this.retaliationCooldown = RETALIATION_COOLDOWN_TICKS;
        final double dx = target.getX() - this.getX();
        final double dz = target.getZ() - this.getZ();
        final double distance = Math.sqrt(dx * dx + dz * dz);
        Vec3 motion = this.getDeltaMovement();
        if (distance >= 1.0E-4D) {
            motion = motion.add(dx / distance, 0.0D, dz / distance);
        }
        if (this.onGround()) {
            motion = new Vec3(motion.x, 0.32D, motion.z);
        }
        this.setDeltaMovement(motion);
        this.attack(target, this.damageSources().mobAttack(this), RETALIATION_DAMAGE);
    }

    private boolean attack(Entity target, DamageSource source, float damage) {
        final boolean hit = target.hurt(source, damage);
        if (hit) {
            this.doEnchantDamageEffects(this, target);
        }
        return hit;
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof Creeper || target instanceof Ghast) {
            return false;
        }
        if (target instanceof Wolf wolf && wolf.isTame() && wolf.getOwner() == owner) {
            return false;
        }
        if (target instanceof Player targetPlayer && owner instanceof Player ownerPlayer && !ownerPlayer.canHarmPlayer(targetPlayer)) {
            return false;
        }
        return !(target instanceof AbstractHorse horse) || !horse.isTamed();
    }

    // ── 杂项 ──

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Nullable
    @Override
    public Wolf getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    // ── 存档 ──

    /** 保存被替换掉的原狼（完整 NBT 含实体 id），还原时据此重建 */
    public void storeOldWolf(Wolf wolf) {
        final CompoundTag tag = new CompoundTag();
        if (wolf.saveAsPassenger(tag)) {
            this.storedWolf = tag;
        }
    }

    public boolean hasStoredWolf() {
        return this.storedWolf != null && this.storedWolf.contains("id", Tag.TAG_STRING);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.hasStoredWolf()) {
            tag.put(STORED_WOLF_TAG, this.storedWolf);
        }
        tag.putBoolean(SUMMONED_TAG, true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.storedWolf = tag.contains(STORED_WOLF_TAG, Tag.TAG_COMPOUND) ? tag.getCompound(STORED_WOLF_TAG) : null;
    }
}
