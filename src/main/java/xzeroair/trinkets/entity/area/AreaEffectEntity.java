package xzeroair.trinkets.entity.area;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.util.helpers.ChunkSafety;

/**
 * 通用区域效果实体：在半径范围内按固定间隔对实体与方块执行一组 {@link AreaEffectAction}。
 * 治愈之云（妖精）与吐息命中后的残留区域都用它。对应 1.12 entity/AreaEffectEntity。
 *
 * 移植说明：
 * - 1.12 的 1465 行单文件拆为状态层 AbstractAreaEffect + 本类 + 动作接口 + 各动作类，读写档改为登记表（见 AreaEffectActions）。
 * - 1.12 残留的「原版药水云」路径（setPotion/addEffect、radiusOnUse/durationOnUse）全源码无调用方，不移植。
 * - 逐格处理方块前先确认半径覆盖的区块均已加载，否则本次脉冲跳过方块动作（放火/结冰等副作用会拉起区块）。
 * - 未加载区块不再触发同步加载：方块读取一律走 ChunkSafety。
 */
public class AreaEffectEntity extends AbstractAreaEffect {

    private final Map<Entity, Integer> entityDelays = new HashMap<>();
    private final Map<Long, Integer> blockDelays = new HashMap<>();

    public AreaEffectEntity(EntityType<? extends AreaEffectEntity> type, Level level) {
        super(type, level);
    }

    public AreaEffectEntity(EntityType<? extends AreaEffectEntity> type, Level level, double x, double y, double z) {
        this(type, level);
        this.setPos(x, y, z);
    }

    // ── tick ──

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && !this.handleLiquidBehavior()) {
            return;
        }
        this.updateGravityMotion();
        if (this.level().isClientSide) {
            this.spawnClientParticles();
        } else {
            this.updateServerEffect();
        }
    }

    private boolean handleLiquidBehavior() {
        // 实体自身所在方块必在已加载区块内
        final FluidState fluid = this.level().getFluidState(this.blockPosition());
        final LiquidBehavior behavior = fluid.is(FluidTags.WATER) ? this.waterBehavior
                : fluid.is(FluidTags.LAVA) ? this.lavaBehavior : LiquidBehavior.STAY;
        switch (behavior) {
            case EXPIRE -> {
                this.discard();
                return false;
            }
            case FLOAT -> this.move(MoverType.SELF, new Vec3(0.0D, LIQUID_MOVEMENT_PER_TICK, 0.0D));
            case SINK -> this.move(MoverType.SELF, new Vec3(0.0D, -LIQUID_MOVEMENT_PER_TICK, 0.0D));
            default -> {
            }
        }
        return true;
    }

    private void updateGravityMotion() {
        if (!this.affectedByGravity) {
            return;
        }
        this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -this.gravityPerTick, 0.0D));
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
    }

    private void spawnClientParticles() {
        final ParticleOptions particle = this.getParticle();
        final boolean waiting = this.isWaiting();
        if (waiting && !this.random.nextBoolean()) {
            return;
        }
        final float radius = waiting ? 0.2F : this.getRadius();
        final int count = waiting ? 2 : Mth.ceil(Math.min(24.0F, Mth.PI * radius * radius * 0.5F));
        for (int i = 0; i < count; i++) {
            final float angle = this.random.nextFloat() * Mth.TWO_PI;
            final float distance = Mth.sqrt(this.random.nextFloat()) * radius;
            final double x = this.getX() + Mth.cos(angle) * distance;
            final double z = this.getZ() + Mth.sin(angle) * distance;
            if (particle.getType() == ParticleTypes.ENTITY_EFFECT) {
                final int color = waiting && this.random.nextBoolean() ? 0xFFFFFF : this.getColor();
                this.level().addAlwaysVisibleParticle(particle, x, this.getY(), z,
                        (color >> 16 & 255) / 255.0D, (color >> 8 & 255) / 255.0D, (color & 255) / 255.0D);
            } else if (waiting) {
                this.level().addAlwaysVisibleParticle(particle, x, this.getY(), z, 0.0D, 0.0D, 0.0D);
            } else {
                this.level().addAlwaysVisibleParticle(particle, x, this.getY(), z,
                        (0.5D - this.random.nextDouble()) * 0.15D, 0.01D, (0.5D - this.random.nextDouble()) * 0.15D);
            }
        }
    }

    private void updateServerEffect() {
        if (this.tickCount >= this.waitTime + this.duration) {
            this.discard();
            return;
        }
        final boolean waiting = this.tickCount < this.waitTime;
        if (this.isWaiting() != waiting) {
            this.entityData.set(DATA_WAITING, waiting);
        }
        if (waiting) {
            return;
        }
        float radius = this.getRadius();
        if (this.radiusPerTick != 0.0F) {
            radius += this.radiusPerTick;
            if (radius < MIN_RADIUS) {
                this.discard();
                return;
            }
            this.setRadius(radius);
        }
        if (this.tickCount % this.pulseInterval != 0 || this.actions.isEmpty()) {
            return;
        }
        this.clearExpired(this.entityDelays);
        this.clearExpired(this.blockDelays);
        this.processEntities(radius);
        this.processBlocks(radius);
    }

    private <K> void clearExpired(Map<K, Integer> delays) {
        final Iterator<Map.Entry<K, Integer>> iterator = delays.entrySet().iterator();
        while (iterator.hasNext()) {
            if (this.tickCount >= iterator.next().getValue()) {
                iterator.remove();
            }
        }
    }

    private void processEntities(float radius) {
        for (Entity entity : this.level().getEntities(this, this.areaBox(radius))) {
            if (!this.isEntityTargetAllowed(entity) || !this.isInArea(entity, radius) || this.entityDelays.containsKey(entity)) {
                continue;
            }
            boolean affected = false;
            for (AreaEffectAction action : this.actions) {
                if (action.canAffectEntity(this, entity)) {
                    action.affectEntity(this, entity);
                    affected = true;
                }
            }
            if (affected) {
                this.entityDelays.put(entity, this.tickCount + this.reapplicationDelay);
            }
        }
    }

    private void processBlocks(float radius) {
        final int reach = Mth.ceil(radius) + 1;
        if (!ChunkSafety.isAreaLoaded(this.level(), this.getBlockX(), this.getBlockZ(), reach)) {
            return;
        }
        for (AreaEffectAction action : this.actions) {
            if (action.handlesOwnBlockProcessing()) {
                action.processBlocks(this, radius);
            }
        }
        final AABB box = this.areaBox(radius);
        final ChunkSafety.ChunkCache chunks = new ChunkSafety.ChunkCache(this.level());
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = Mth.floor(box.minX); x <= Mth.floor(box.maxX); x++) {
            for (int y = Mth.floor(box.minY); y <= Mth.floor(box.maxY); y++) {
                for (int z = Mth.floor(box.minZ); z <= Mth.floor(box.maxZ); z++) {
                    cursor.set(x, y, z);
                    final BlockState state = chunks.getBlockState(cursor);
                    if (state != null) {
                        this.processBlock(cursor.immutable(), state, radius);
                    }
                }
            }
        }
    }

    private void processBlock(BlockPos pos, BlockState state, float radius) {
        final long key = pos.asLong();
        if (!this.isBlockTargetAllowed(pos, state) || !this.isBlockInArea(pos, radius) || this.blockDelays.containsKey(key)) {
            return;
        }
        boolean affected = false;
        for (AreaEffectAction action : this.actions) {
            if (!action.handlesOwnBlockProcessing() && action.canAffectBlock(this, pos, state)) {
                action.affectBlock(this, pos, state);
                affected = true;
            }
        }
        if (affected) {
            this.blockDelays.put(key, this.tickCount + this.reapplicationDelay);
        }
    }

    private boolean isEntityTargetAllowed(Entity entity) {
        if (entity == this || !this.entityTargetPredicate.test(entity)) {
            return false;
        }
        final ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id == null || !this.entityBlacklist.contains(id.toString());
    }

    private boolean isBlockTargetAllowed(BlockPos pos, BlockState state) {
        if (!this.blockTargetPredicate.test(pos)) {
            return false;
        }
        final ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id == null || !this.blockBlacklist.contains(id.toString());
    }

    public AABB areaBox(float radius) {
        final float vertical = this.getVerticalRadius();
        return new AABB(this.getX() - radius, this.getY() - vertical, this.getZ() - radius,
                this.getX() + radius, this.getY() + vertical, this.getZ() + radius);
    }

    private boolean isInArea(Entity entity, float radius) {
        final double dx = entity.getX() - this.getX();
        final double dz = entity.getZ() - this.getZ();
        final double dy = Math.abs(entity.getY() + entity.getBbHeight() * 0.5D - this.getY());
        return dx * dx + dz * dz <= radius * radius && dy <= this.getVerticalRadius();
    }

    public boolean isBlockInArea(BlockPos pos, float radius) {
        final double dx = pos.getX() + 0.5D - this.getX();
        final double dz = pos.getZ() + 0.5D - this.getZ();
        final double dy = Math.abs(pos.getY() + 0.5D - this.getY());
        return dx * dx + dz * dz <= radius * radius && dy <= this.getVerticalRadius();
    }

}
