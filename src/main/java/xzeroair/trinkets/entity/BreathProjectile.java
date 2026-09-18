package xzeroair.trinkets.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.DistExecutor;

import xzeroair.trinkets.client.particles.ClientEffects;
import xzeroair.trinkets.entity.area.AreaEffectEntity;
import xzeroair.trinkets.init.ModDamageTypes;
import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.init.ModEntities;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.util.helpers.PotionHelper;

/**
 * 龙息投射物：沿直线飞行（受阻力与重力），命中实体造成元素伤害并波及周围，命中方块时留下残留区域。
 * 对应 1.12 entity/EntityRangedAttack。
 *
 * 移植说明：
 * - 改继承 Projectile：主人 UUID 缓存、离开发射者前不误伤主人、命中事件都由原版/Forge 提供。
 * - 伤害源由 1.12 的「dragonBreath/lightningBolt + 穿甲 + 魔法 (+火)」改为数据化伤害类型
 *   xat:dragon_fire / dragon_ice / dragon_lightning（标签里带 bypasses_armor、is_fire、is_lightning）。
 * - 1.12 中定义但从未调用的 applyIce/FireTerrainEffect 等地形方法不移植；地形交互全部经残留区域动作完成。
 * - 残留区域与冰霜吐息入水冻结的方块改动前都确认区块已加载。
 */
public class BreathProjectile extends Projectile {

    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(BreathProjectile.class, EntityDataSerializers.INT);

    private static final float IMPACT_AREA_RADIUS = 3.0F;
    private static final float IMPACT_AREA_VERTICAL_RADIUS = 1.5F;
    private static final int IMPACT_AREA_DURATION = 80;
    private static final float IMPACT_AREA_MIN_RADIUS = 0.5F;
    private static final int MAX_ACTIVE_IMPACT_AREAS_PER_PLAYER = 3;
    private static final int DEFAULT_COLOR = 12582912;

    /** 命中后点燃目标的秒数（寒冰吐息不点燃） */
    public static final int FIRE_BURN_SECONDS = 5;
    public static final int LIGHTNING_BURN_SECONDS = 1;

    private Element element = ModElements.NEUTRAL.get();
    private float damage = 1.0F;
    private boolean ignoreBlocks;
    private boolean interactWithTerrain;
    private boolean renderImpactAreaCircle = true;
    private float airDrag = 0.99F;
    private float waterDrag = 0.8F;
    private float lavaDrag = 0.8F;
    private float gravityPerTick = 0.03F;
    private int lifetimeTicks = 30;
    private int maxLifetimeTicks = 2400;
    private boolean expireInWater = true;
    private boolean expireInLava = true;
    private List<? extends String> effects = List.of();

    public BreathProjectile(EntityType<? extends BreathProjectile> type, Level level) {
        super(type, level);
    }

    public BreathProjectile(Level level, LivingEntity shooter, int color) {
        this(ModEntities.DRAGON_BREATH.get(), level);
        this.setOwner(shooter);
        this.moveTo(shooter.getX(), shooter.getY(), shooter.getZ(), shooter.getYRot(), shooter.getXRot());
        this.setColor(color);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_COLOR, DEFAULT_COLOR);
    }

    // ── 参数 ──

    public BreathProjectile setColor(int color) {
        this.entityData.set(DATA_COLOR, color);
        return this;
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    public BreathProjectile setElement(@Nullable Element element) {
        this.element = element == null ? ModElements.NEUTRAL.get() : element;
        return this;
    }

    public BreathProjectile setDamage(float damage) {
        this.damage = damage;
        return this;
    }

    public BreathProjectile setEffects(List<? extends String> effects) {
        this.effects = effects == null ? List.of() : List.copyOf(effects);
        return this;
    }

    public BreathProjectile setAllowTerrainInteraction(boolean allow) {
        this.interactWithTerrain = allow;
        return this;
    }

    public BreathProjectile setRenderImpactAreaCircle(boolean render) {
        this.renderImpactAreaCircle = render;
        return this;
    }

    public BreathProjectile setIgnoreBlocks(boolean ignoreBlocks) {
        this.ignoreBlocks = ignoreBlocks;
        return this;
    }

    // ── 飞行 ──

    @Override
    public void tick() {
        final Entity owner = this.getOwner();
        if (!this.level().isClientSide && owner != null && !owner.isAlive()) {
            this.discard();
            return;
        }
        super.tick();
        if (!this.level().isClientSide && this.interactWithTerrain && this.isIce() && this.freezeLiquidAtPosition()) {
            return;
        }
        final HitResult hit = this.traceImpact();
        if (hit.getType() != HitResult.Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hit)) {
            this.onHit(hit);
            if (this.isRemoved()) {
                return;
            }
        }
        final Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
        this.updateRotation();
        this.applyDrag();
        if (this.level().isClientSide) {
            // 拖尾：发射 3 tick 后每 tick 在身后留一团淡出的火焰
            if (this.tickCount >= 3) {
                final int color = this.getColor();
                final double x = this.getX();
                final double y = this.getY() + 0.5D;
                final double z = this.getZ();
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEffects.breathWisp(x, y, z, color));
            }
            return;
        }
        if (this.tickCount >= this.lifetimeTicks || this.tickCount >= this.maxLifetimeTicks
                || (this.expireInWater && this.isInWater()) || (this.expireInLava && this.isInLava())) {
            this.discard();
        }
    }

    private HitResult traceImpact() {
        final Vec3 start = this.position();
        final Vec3 end = start.add(this.getDeltaMovement());
        HitResult result = this.ignoreBlocks
                ? BlockHitResult.miss(end, Direction.UP, BlockPos.containing(end))
                : this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        final Vec3 entityEnd = result.getType() != HitResult.Type.MISS ? result.getLocation() : end;
        final EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(this.level(), this, start, entityEnd,
                this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0D), this::canHitEntity);
        return entityHit != null ? entityHit : result;
    }

    private void applyDrag() {
        float drag = this.airDrag;
        if (this.isInWater()) {
            final Vec3 motion = this.getDeltaMovement();
            for (int i = 0; i < 4; i++) {
                this.level().addParticle(ParticleTypes.BUBBLE, this.getX() - motion.x * 0.25D, this.getY() - motion.y * 0.25D,
                        this.getZ() - motion.z * 0.25D, motion.x, motion.y, motion.z);
            }
            drag = this.waterDrag;
        } else if (this.isInLava()) {
            drag = this.lavaDrag;
        }
        Vec3 motion = this.getDeltaMovement().scale(drag);
        if (!this.isNoGravity()) {
            motion = motion.add(0.0D, -this.gravityPerTick, 0.0D);
        }
        this.setDeltaMovement(motion);
    }

    /** 冰霜吐息进入水/岩浆：冻结附近液体并留下残留区域后消失 */
    private boolean freezeLiquidAtPosition() {
        final BlockPos pos = this.blockPosition();
        final boolean water = this.level().getFluidState(pos).is(FluidTags.WATER);
        final boolean lava = this.level().getFluidState(pos).is(FluidTags.LAVA);
        if ((!water && !lava) || !this.canModifyTerrain(pos)) {
            return false;
        }
        BreathTerrain.freezeAround(this.level(), this.position(), water);
        this.spawnImpactArea(this.position());
        this.discard();
        return true;
    }

    // ── 命中 ──

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target != this.getOwner() && !(target instanceof BreathProjectile);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.level().isClientSide) {
            this.spawnImpactArea(result.getLocation());
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide) {
            return;
        }
        final Entity direct = result.getEntity();
        final boolean pvp = this.isPvpEnabled();
        this.applyHitToTarget(direct, pvp);
        for (Entity target : this.level().getEntities(this, this.getBoundingBox().inflate(1.0D),
                e -> !e.isSpectator() && e != direct && e != this.getOwner() && !(e instanceof BreathProjectile) && !e.fireImmune())) {
            this.applyHitToTarget(target, pvp);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (result instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof BreathProjectile) {
            return;
        }
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }

    private void applyHitToTarget(Entity target, boolean pvp) {
        if (!this.canAffectTarget(target, pvp)) {
            return;
        }
        final LivingEntity shooter = this.getOwner() instanceof LivingEntity living ? living : null;
        if (shooter != null && target instanceof LivingEntity livingTarget) {
            EnchantmentHelper.doPostHurtEffects(livingTarget, shooter);
            EnchantmentHelper.doPostDamageEffects(shooter, livingTarget);
        }
        final boolean lightning = this.element == ModElements.LIGHTNING.get();
        target.hurt(this.damageSource(shooter), this.damage);
        if (lightning) {
            target.setSecondsOnFire(LIGHTNING_BURN_SECONDS);
        } else if (!this.isIce()) {
            target.setSecondsOnFire(FIRE_BURN_SECONDS);
        }
        if (target instanceof LivingEntity living) {
            PotionHelper.applyAllFromConfig(living, this.effects);
        }
    }

    private DamageSource damageSource(@Nullable LivingEntity shooter) {
        final ResourceKey<DamageType> type = this.element == ModElements.LIGHTNING.get() ? ModDamageTypes.DRAGON_LIGHTNING
                : this.isIce() ? ModDamageTypes.DRAGON_ICE : ModDamageTypes.DRAGON_FIRE;
        return ModDamageTypes.source(this.level(), type, this, shooter != null ? shooter : this);
    }

    private boolean canAffectTarget(Entity target, boolean pvp) {
        if (!target.isPickable() || target == this.getOwner() || target instanceof BreathProjectile) {
            return false;
        }
        return !(target instanceof Player) || pvp;
    }

    private boolean isPvpEnabled() {
        return this.level() instanceof ServerLevel server && server.getServer().isPvpAllowed();
    }

    private boolean isIce() {
        return this.element == ModElements.ICE.get();
    }

    private boolean canModifyTerrain(BlockPos pos) {
        return !(this.getOwner() instanceof Player player) || player.mayUseItemAt(pos, Direction.UP, ItemStack.EMPTY);
    }

    // ── 残留区域 ──

    private void spawnImpactArea(Vec3 impact) {
        final AreaEffectEntity area = new AreaEffectEntity(ModEntities.AREA_EFFECT.get(), this.level(), impact.x, impact.y, impact.z);
        area.setOwner(this.getOwner() instanceof LivingEntity living ? living : null);
        area.setBreathImpact(true);
        area.setRadius(IMPACT_AREA_RADIUS);
        area.setVerticalRadius(IMPACT_AREA_VERTICAL_RADIUS);
        area.setWaitTime(0);
        area.setDuration(IMPACT_AREA_DURATION);
        area.setRadiusPerTick(-(IMPACT_AREA_RADIUS - IMPACT_AREA_MIN_RADIUS) / IMPACT_AREA_DURATION);
        area.setPulseInterval(10);
        area.setReapplicationDelay(20);
        area.setColor(this.getColor());
        area.setRenderCircle(this.renderImpactAreaCircle);
        final boolean pvp = this.isPvpEnabled();
        area.setEntityTargetPredicate(target -> this.canAffectTarget(target, pvp));
        BreathTerrain.configureImpactArea(area, this.element, this.damage, this.effects, this.interactWithTerrain);
        if (!area.getActions().isEmpty()) {
            this.enforceImpactAreaLimit(area);
            this.level().addFreshEntity(area);
        }
    }

    /** 每个玩家同时最多保留 3 个吐息残留区域，超出时移除存在最久的 */
    private void enforceImpactAreaLimit(AreaEffectEntity newArea) {
        if (!(newArea.getOwner() instanceof Player owner) || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        final UUID ownerId = owner.getUUID();
        final List<AreaEffectEntity> active = new ArrayList<>();
        for (Entity entity : server.getAllEntities()) {
            if (entity instanceof AreaEffectEntity area && area.isAlive() && area.isBreathImpact() && area.isOwnedBy(ownerId)) {
                active.add(area);
            }
        }
        while (active.size() >= MAX_ACTIVE_IMPACT_AREAS_PER_PLAYER) {
            AreaEffectEntity oldest = active.get(0);
            for (AreaEffectEntity area : active) {
                if (area.tickCount > oldest.tickCount) {
                    oldest = area;
                }
            }
            oldest.discard();
            active.remove(oldest);
        }
    }

    // ── 存档 ──

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BreathColor", this.getColor());
        tag.putFloat("Damage", this.damage);
        final ResourceLocation elementId = ModElements.registry().getKey(this.element);
        if (elementId != null) {
            tag.putString("Element", elementId.toString());
        }
        tag.putBoolean("IgnoreBlocks", this.ignoreBlocks);
        tag.putBoolean("TerrainInteraction", this.interactWithTerrain);
        tag.putFloat("AirDrag", this.airDrag);
        tag.putFloat("WaterDrag", this.waterDrag);
        tag.putFloat("LavaDrag", this.lavaDrag);
        tag.putFloat("GravityPerTick", this.gravityPerTick);
        tag.putInt("LifetimeTicks", this.lifetimeTicks);
        tag.putInt("MaxLifetimeTicks", this.maxLifetimeTicks);
        tag.putBoolean("ExpireInWater", this.expireInWater);
        tag.putBoolean("ExpireInLava", this.expireInLava);
        final ListTag effectList = new ListTag();
        this.effects.forEach(effect -> effectList.add(StringTag.valueOf(effect)));
        tag.put("Effects", effectList);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BreathColor")) {
            this.setColor(tag.getInt("BreathColor"));
        }
        this.damage = tag.getFloat("Damage");
        final ResourceLocation elementId = ResourceLocation.tryParse(tag.getString("Element"));
        this.setElement(elementId == null ? null : ModElements.registry().getValue(elementId));
        this.ignoreBlocks = tag.getBoolean("IgnoreBlocks");
        this.interactWithTerrain = tag.getBoolean("TerrainInteraction");
        this.airDrag = tag.contains("AirDrag") ? tag.getFloat("AirDrag") : this.airDrag;
        this.waterDrag = tag.contains("WaterDrag") ? tag.getFloat("WaterDrag") : this.waterDrag;
        this.lavaDrag = tag.contains("LavaDrag") ? tag.getFloat("LavaDrag") : this.lavaDrag;
        this.gravityPerTick = tag.contains("GravityPerTick") ? tag.getFloat("GravityPerTick") : this.gravityPerTick;
        this.lifetimeTicks = tag.contains("LifetimeTicks") ? Math.max(1, tag.getInt("LifetimeTicks")) : this.lifetimeTicks;
        this.maxLifetimeTicks = tag.contains("MaxLifetimeTicks") ? Math.max(1, tag.getInt("MaxLifetimeTicks")) : this.maxLifetimeTicks;
        this.expireInWater = !tag.contains("ExpireInWater") || tag.getBoolean("ExpireInWater");
        this.expireInLava = !tag.contains("ExpireInLava") || tag.getBoolean("ExpireInLava");
        final ListTag effectList = tag.getList("Effects", Tag.TAG_STRING);
        final List<String> loaded = new ArrayList<>();
        for (int i = 0; i < effectList.size(); i++) {
            loaded.add(effectList.getString(i));
        }
        this.effects = List.copyOf(loaded);
    }
}
