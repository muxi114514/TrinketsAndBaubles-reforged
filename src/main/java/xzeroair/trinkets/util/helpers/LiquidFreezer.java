package xzeroair.trinkets.util.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import xzeroair.trinkets.entity.area.action.FreezeLiquidAreaAction;

/**
 * 冻结液体源方块：水 → 霜冰，岩浆 → 冷却岩浆（均会随时间融回）。对应 1.12 BlockHelperUtil.freezeWater / freezeLava。
 * 冰霜吐息与霜行者共用；动手前确认覆盖区块均已加载，避免同步加载区块卡住服务端主线程。
 */
public final class LiquidFreezer {

    /** 以 center 为圆心、水平半径 radius、上下 1 格内，上方为空气的液体源 */
    public static void freeze(Level level, Vec3 center, int radius, boolean water) {
        final BlockPos origin = BlockPos.containing(center);
        if (!ChunkSafety.isAreaLoaded(level, origin.getX(), origin.getZ(), radius + 1)) {
            return;
        }
        final BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -1, -radius), origin.offset(radius, 1, radius))) {
            if (pos.distToCenterSqr(center) > radius * radius) {
                continue;
            }
            above.set(pos.getX(), pos.getY() + 1, pos.getZ());
            if (!level.getBlockState(above).isAir()) {
                continue;
            }
            final BlockState state = level.getBlockState(pos);
            final Block frozen = FreezeLiquidAreaAction.frozenForm(state);
            if (frozen != null && state.getFluidState().is(water ? FluidTags.WATER : FluidTags.LAVA)
                    && level.isUnobstructed(frozen.defaultBlockState(), pos, CollisionContext.empty())) {
                level.setBlockAndUpdate(pos, frozen.defaultBlockState());
                level.scheduleTick(pos.immutable(), frozen, Mth.nextInt(level.random, 60, 120));
            }
        }
    }

    private LiquidFreezer() {
    }
}
