package xzeroair.trinkets.traits.abilities.other;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.util.config.abilities.HeavyAbilityConfig;
import xzeroair.trinkets.util.helpers.ChunkSafety;

/**
 * 沉重：踩坏耕地与脚边植物（可配置关闭、潜行时不踩）；在水中下沉、所乘的船也被压沉。
 * 身怀「熟练泳者」能力时不下沉。
 *
 * 移植说明（区块访问安全）：
 * 1.12 的 trample 把包围盒外扩 1 格后逐格 world.getBlockState，再 destroyBlock——
 * 包围盒一旦跨区块边界，读和写都会同步加载区块、卡住服务端主线程。
 * 现改为：先确认整个踩踏范围覆盖的区块均已加载（副作用守卫），不满足则本 tick 放弃；
 * 扫描时再经 ChunkCache 逐格只读内存。
 */
public class AbilityHeavy extends Ability implements ITickableAbility {

    /** 每 tick 附加的下沉速度 */
    private static final double PLAYER_SINK_SPEED = 0.2D;
    private static final double MOB_SINK_SPEED = 0.1D;
    /** 头部未入水时，脚下这么深内有地面就不下沉 */
    private static final double GROUND_PROBE_DEPTH = 3.0D;

    private final HeavyAbilityConfig config;

    public AbilityHeavy(@Nonnull HeavyAbilityConfig config) {
        super(AbilityNames.HEAVY);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        if (this.isCreativePlayer(entity)) {
            return;
        }
        this.trample(entity);
        if (hasSkilledSwimmer(entity)) {
            return;
        }
        if (entity.isPassenger() && entity.getVehicle() instanceof Boat boat) {
            boat.setDeltaMovement(boat.getDeltaMovement().add(0, -0.02D, 0));
        } else if (entity.isInWater() && this.shouldSink(entity)) {
            final double sink = entity instanceof Player ? -PLAYER_SINK_SPEED : -MOB_SINK_SPEED;
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, sink, 0));
        }
    }

    /** 完全没入水中，或脚下 3 格内触不到底时下沉 */
    private boolean shouldSink(LivingEntity entity) {
        if (entity.isEyeInFluid(FluidTags.WATER)) {
            return true;
        }
        // 只沿当前 x/z 列向下探 3 格，不跨区块边界
        final Vec3 start = entity.position();
        final HitResult hit = entity.level().clip(new ClipContext(start, start.add(0, -GROUND_PROBE_DEPTH, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        return hit.getType() != HitResult.Type.BLOCK;
    }

    private void trample(LivingEntity entity) {
        final Level level = entity.level();
        if (level.isClientSide || !this.config.trample.get() || entity.isShiftKeyDown()) {
            return;
        }
        final AABB box = entity.getBoundingBox().inflate(1, 0, 1);
        final int minX = Mth.floor(box.minX);
        final int maxX = Mth.floor(box.maxX + 1.0D);
        final int minZ = Mth.floor(box.minZ);
        final int maxZ = Mth.floor(box.maxZ + 1.0D);
        if (!ChunkSafety.isRegionLoaded(level, minX, minZ, maxX, maxZ)) {
            return;
        }
        final int groundY = entity.blockPosition().getY() - 1;
        final ChunkSafety.ChunkCache chunks = new ChunkSafety.ChunkCache(level);
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                final BlockPos ground = new BlockPos(x, groundY, z);
                final BlockState groundState = chunks.getBlockState(ground);
                if (groundState == null) {
                    continue;
                }
                if (groundState.is(Blocks.FARMLAND) && canModifyTerrain(entity, ground)) {
                    level.setBlockAndUpdate(ground, Blocks.DIRT.defaultBlockState());
                }
                final BlockPos plantPos = ground.above();
                final BlockState plant = chunks.getBlockState(plantPos);
                if (plant != null && plant.getBlock() instanceof BushBlock) {
                    // 双高植物破坏上半格，下半格随之掉落
                    final BlockPos breakPos = plant.getBlock() instanceof DoublePlantBlock ? ground.above(2) : plantPos;
                    if (canModifyTerrain(entity, breakPos)) {
                        level.destroyBlock(breakPos, true);
                    }
                }
            }
        }
    }

    /** 玩家须有该位置的编辑权限且破坏事件未被其他模组（领地等）取消 */
    private static boolean canModifyTerrain(LivingEntity entity, BlockPos pos) {
        if (!(entity instanceof Player player)) {
            return true;
        }
        if (!player.mayUseItemAt(pos, Direction.UP, ItemStack.EMPTY)) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            return ForgeHooks.onBlockBreakEvent(entity.level(), serverPlayer.gameMode.getGameModeForPlayer(),
                    serverPlayer, pos) != -1;
        }
        return true;
    }

    private static boolean hasSkilledSwimmer(LivingEntity entity) {
        final EntityProperties properties = EntityProperties.get(entity);
        return properties != null
                && properties.getAbilityHandler().getAbility(AbilityNames.key(AbilityNames.SKILLED_SWIMMER)) != null;
    }

    @Override
    public void describe(DescriptionVariables variables) {
        variables.number("sink", PLAYER_SINK_SPEED)
                .number("depth", GROUND_PROBE_DEPTH)
                .flag("trample", this.config.trample.get());
    }
}
