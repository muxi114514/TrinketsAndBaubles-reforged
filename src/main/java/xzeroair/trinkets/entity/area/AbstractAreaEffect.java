package xzeroair.trinkets.entity.area;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 区域效果实体的状态层：同步数据、可调参数、主人与存读档。脉冲处理逻辑见 {@link AreaEffectEntity}。
 * 拆分原因：两部分合在一起超过 500 行，按「状态」与「行为」分层。
 */
public abstract class AbstractAreaEffect extends Entity {

    protected static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(AbstractAreaEffect.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_VERTICAL_RADIUS = SynchedEntityData.defineId(AbstractAreaEffect.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(AbstractAreaEffect.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Boolean> DATA_WAITING = SynchedEntityData.defineId(AbstractAreaEffect.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> DATA_RENDER_CIRCLE = SynchedEntityData.defineId(AbstractAreaEffect.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<ParticleOptions> DATA_PARTICLE = SynchedEntityData.defineId(AbstractAreaEffect.class, EntityDataSerializers.PARTICLE);

    protected static final double LIQUID_MOVEMENT_PER_TICK = 0.1D;
    protected static final float MIN_RADIUS = 0.5F;

    public enum LiquidBehavior {
        EXPIRE, STAY, FLOAT, SINK
    }

    protected final List<AreaEffectAction> actions = new ArrayList<>();
    protected final Set<String> entityBlacklist = new LinkedHashSet<>();
    protected final Set<String> blockBlacklist = new LinkedHashSet<>();
    protected int duration = 600;
    protected int waitTime = 20;
    protected int reapplicationDelay = 20;
    protected int pulseInterval = 20;
    protected float radiusPerTick;
    protected boolean colorSet;
    protected boolean breathImpact;
    protected boolean affectedByGravity;
    protected double gravityPerTick = 0.03D;
    protected LiquidBehavior waterBehavior = LiquidBehavior.STAY;
    protected LiquidBehavior lavaBehavior = LiquidBehavior.STAY;
    protected Predicate<Entity> entityTargetPredicate = entity -> true;
    protected Predicate<BlockPos> blockTargetPredicate = pos -> true;
    @Nullable
    protected LivingEntity owner;
    @Nullable
    protected UUID ownerUUID;

    protected AbstractAreaEffect(EntityType<? extends AbstractAreaEffect> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setRadius(3.0F);
        this.setVerticalRadius(1.5F);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_COLOR, 0);
        this.entityData.define(DATA_RADIUS, MIN_RADIUS);
        this.entityData.define(DATA_VERTICAL_RADIUS, 1.5F);
        this.entityData.define(DATA_WAITING, false);
        this.entityData.define(DATA_RENDER_CIRCLE, true);
        this.entityData.define(DATA_PARTICLE, ParticleTypes.ENTITY_EFFECT);
    }

    // ── 参数 ──

    public void setRadius(float radius) {
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_RADIUS, Mth.clamp(radius, 0.0F, 32.0F));
        }
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    public void setVerticalRadius(float radius) {
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_VERTICAL_RADIUS, Mth.clamp(radius, 0.0F, 32.0F));
        }
    }

    public float getVerticalRadius() {
        return this.entityData.get(DATA_VERTICAL_RADIUS);
    }

    public void setDuration(int duration) {
        this.duration = Math.max(0, duration);
    }

    public int getDuration() {
        return this.duration;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = Math.max(0, waitTime);
    }

    public void setReapplicationDelay(int delay) {
        this.reapplicationDelay = Math.max(1, delay);
    }

    public void setPulseInterval(int interval) {
        this.pulseInterval = Math.max(1, interval);
    }

    public void setRadiusPerTick(float radiusPerTick) {
        this.radiusPerTick = radiusPerTick;
    }

    public void setAffectedByGravity(boolean affected) {
        this.affectedByGravity = affected;
        this.noPhysics = !affected;
    }

    public void setGravityPerTick(double gravity) {
        this.gravityPerTick = Math.max(0.0D, gravity);
    }

    public AbstractAreaEffect setWaterBehavior(@Nullable LiquidBehavior behavior) {
        this.waterBehavior = behavior == null ? LiquidBehavior.STAY : behavior;
        return this;
    }

    public AbstractAreaEffect setLavaBehavior(@Nullable LiquidBehavior behavior) {
        this.lavaBehavior = behavior == null ? LiquidBehavior.STAY : behavior;
        return this;
    }

    public AbstractAreaEffect addAction(@Nullable AreaEffectAction action) {
        if (action != null) {
            this.actions.add(action);
        }
        return this;
    }

    public List<AreaEffectAction> getActions() {
        return this.actions;
    }

    public AbstractAreaEffect setEntityTargetPredicate(@Nullable Predicate<Entity> predicate) {
        this.entityTargetPredicate = predicate == null ? entity -> true : predicate;
        return this;
    }

    public AbstractAreaEffect setBlockTargetPredicate(@Nullable Predicate<BlockPos> predicate) {
        this.blockTargetPredicate = predicate == null ? pos -> true : predicate;
        return this;
    }

    public AbstractAreaEffect blacklistEntityRegistry(String... ids) {
        addIds(this.entityBlacklist, ids);
        return this;
    }

    public AbstractAreaEffect blacklistBlockRegistry(String... ids) {
        addIds(this.blockBlacklist, ids);
        return this;
    }

    private static void addIds(Set<String> target, String... ids) {
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                target.add(id.trim().toLowerCase(Locale.ROOT));
            }
        }
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    public void setColor(int color) {
        this.colorSet = true;
        this.entityData.set(DATA_COLOR, color);
    }

    public ParticleOptions getParticle() {
        return this.entityData.get(DATA_PARTICLE);
    }

    public void setParticle(@Nullable ParticleOptions particle) {
        this.entityData.set(DATA_PARTICLE, particle == null ? ParticleTypes.ENTITY_EFFECT : particle);
    }

    public boolean isWaiting() {
        return this.entityData.get(DATA_WAITING);
    }

    public AbstractAreaEffect setRenderCircle(boolean render) {
        this.entityData.set(DATA_RENDER_CIRCLE, render);
        return this;
    }

    public boolean shouldRenderCircle() {
        return this.entityData.get(DATA_RENDER_CIRCLE);
    }

    public AbstractAreaEffect setBreathImpact(boolean breathImpact) {
        this.breathImpact = breathImpact;
        return this;
    }

    public boolean isBreathImpact() {
        return this.breathImpact;
    }

    // ── 主人 ──

    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner;
        this.ownerUUID = owner == null ? null : owner.getUUID();
    }

    @Nullable
    public LivingEntity getOwner() {
        if (this.owner == null && this.ownerUUID != null && this.level() instanceof ServerLevel server
                && server.getEntity(this.ownerUUID) instanceof LivingEntity living) {
            this.owner = living;
        }
        return this.owner;
    }

    public boolean isOwnedBy(@Nullable UUID id) {
        return id != null && id.equals(this.ownerUUID);
    }

    /** 主人是玩家时须有权修改该方块（冒险模式、出生点保护等） */
    public boolean canOwnerModifyBlock(BlockPos pos) {
        return !(this.getOwner() instanceof Player player) || player.mayUseItemAt(pos, Direction.UP, ItemStack.EMPTY);
    }

    public boolean mobGriefing() {
        return this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    /** 骨粉粒子与快乐村民粒子（修复物品、催熟作物时的反馈） */
    public void spawnBonemealParticles(BlockPos pos, int count, double horizontalSpread, double verticalSpread) {
        if (this.level() instanceof ServerLevel server) {
            server.levelEvent(1505, pos, 0);
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    Math.max(1, count), horizontalSpread, verticalSpread, horizontalSpread, 0.02D);
        }
    }

    // ── 尺寸与杂项 ──

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (DATA_RADIUS.equals(key) || DATA_VERTICAL_RADIUS.equals(key)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(this.getRadius() * 2.0F, Math.max(MIN_RADIUS, this.getVerticalRadius() * 2.0F));
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    // ── 存档 ──

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.actions.clear();
        this.entityBlacklist.clear();
        this.blockBlacklist.clear();
        this.owner = null;
        this.tickCount = tag.getInt("Age");
        this.setDuration(tag.getInt("Duration"));
        this.setWaitTime(tag.getInt("WaitTime"));
        this.setReapplicationDelay(tag.getInt("ReapplicationDelay"));
        this.setPulseInterval(tag.contains("PulseInterval") ? tag.getInt("PulseInterval") : this.reapplicationDelay);
        this.radiusPerTick = tag.getFloat("RadiusPerTick");
        this.setAffectedByGravity(tag.getBoolean("AffectedByGravity"));
        this.breathImpact = tag.getBoolean("BreathImpact");
        this.gravityPerTick = tag.contains("GravityPerTick") ? tag.getDouble("GravityPerTick") : 0.03D;
        this.setRenderCircle(!tag.contains("RenderCircle") || tag.getBoolean("RenderCircle"));
        this.waterBehavior = readBehavior(tag, "WaterBehavior");
        this.lavaBehavior = readBehavior(tag, "LavaBehavior");
        this.setRadius(tag.getFloat("Radius"));
        this.setVerticalRadius(tag.contains("VerticalRadius") ? tag.getFloat("VerticalRadius") : 1.5F);
        this.ownerUUID = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        if (tag.contains("Particle", Tag.TAG_STRING)) {
            final ResourceLocation id = ResourceLocation.tryParse(tag.getString("Particle"));
            if (id != null && ForgeRegistries.PARTICLE_TYPES.getValue(id) instanceof SimpleParticleType simple) {
                this.setParticle(simple);
            }
        }
        if (tag.contains("Color", Tag.TAG_ANY_NUMERIC)) {
            this.setColor(tag.getInt("Color"));
        }
        final ListTag entityList = tag.getList("EntityBlacklist", Tag.TAG_STRING);
        for (int i = 0; i < entityList.size(); i++) {
            this.blacklistEntityRegistry(entityList.getString(i));
        }
        final ListTag blockList = tag.getList("BlockBlacklist", Tag.TAG_STRING);
        for (int i = 0; i < blockList.size(); i++) {
            this.blacklistBlockRegistry(blockList.getString(i));
        }
        final ListTag actionList = tag.getList("Actions", Tag.TAG_COMPOUND);
        for (int i = 0; i < actionList.size(); i++) {
            this.addAction(AreaEffectActions.read(actionList.getCompound(i)));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.tickCount);
        tag.putInt("Duration", this.duration);
        tag.putInt("WaitTime", this.waitTime);
        tag.putInt("ReapplicationDelay", this.reapplicationDelay);
        tag.putInt("PulseInterval", this.pulseInterval);
        tag.putFloat("RadiusPerTick", this.radiusPerTick);
        tag.putFloat("Radius", this.getRadius());
        tag.putFloat("VerticalRadius", this.getVerticalRadius());
        tag.putBoolean("AffectedByGravity", this.affectedByGravity);
        tag.putBoolean("BreathImpact", this.breathImpact);
        tag.putDouble("GravityPerTick", this.gravityPerTick);
        tag.putBoolean("RenderCircle", this.shouldRenderCircle());
        tag.putString("WaterBehavior", this.waterBehavior.name());
        tag.putString("LavaBehavior", this.lavaBehavior.name());
        final ResourceLocation particle = ForgeRegistries.PARTICLE_TYPES.getKey(this.getParticle().getType());
        if (particle != null) {
            tag.putString("Particle", particle.toString());
        }
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
        if (this.colorSet) {
            tag.putInt("Color", this.getColor());
        }
        tag.put("EntityBlacklist", toStringList(this.entityBlacklist));
        tag.put("BlockBlacklist", toStringList(this.blockBlacklist));
        final ListTag actionList = new ListTag();
        for (AreaEffectAction action : this.actions) {
            actionList.add(AreaEffectActions.write(action));
        }
        tag.put("Actions", actionList);
    }

    private static ListTag toStringList(Set<String> values) {
        final ListTag list = new ListTag();
        values.forEach(value -> list.add(StringTag.valueOf(value)));
        return list;
    }

    private static LiquidBehavior readBehavior(CompoundTag tag, String key) {
        try {
            return tag.contains(key, Tag.TAG_STRING) ? LiquidBehavior.valueOf(tag.getString(key)) : LiquidBehavior.STAY;
        } catch (IllegalArgumentException e) {
            return LiquidBehavior.STAY;
        }
    }
}
