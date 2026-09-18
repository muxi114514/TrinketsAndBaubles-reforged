package xzeroair.trinkets.util.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;

/**
 * 区域挖掘：按玩家瞄准的面展开一块立方区域，并用指定工具逐格破坏。
 * 对应 1.12 BlockHelperUtil.getBlockList / canBreakBlock / breakBlock（Tinkers 风格的范围挖掘算法）。
 */
public final class AreaMiningHelper {

    /**
     * 以 origin 为基准、按被瞄准的面展开 width × height × depth 的区域（不含 origin 本身）。
     * 瞄准面垂直方向为深度，平面方向为宽高；偶数边长时按命中点偏向补一格（与 1.12 一致）。
     * 本方法只计算坐标、不读方块，调用方在读/写前须自行做区块守卫。
     */
    public static List<BlockPos> collectArea(Player player, BlockPos origin, int width, int height, int depth,
            @Nullable Predicate<BlockPos> filter) {
        final Aim aim = aimAt(player, origin);
        int x;
        int y;
        int z;
        BlockPos start = origin;
        switch (aim.face()) {
            case DOWN, UP -> {
                final Vec3i facing = player.getDirection().getNormal();
                x = (facing.getX() * height) + (facing.getZ() * width);
                y = aim.face().getAxisDirection().getStep() * -depth;
                z = (facing.getX() * width) + (facing.getZ() * height);
                start = start.offset(-x / 2, 0, -z / 2);
                if (x % 2 == 0) {
                    final double dx = aim.hit().x - origin.getX();
                    if (x > 0 && dx > 0.5D) {
                        start = start.offset(1, 0, 0);
                    } else if (x < 0 && dx < 0.5D) {
                        start = start.offset(-1, 0, 0);
                    }
                }
                if (z % 2 == 0) {
                    final double dz = aim.hit().z - origin.getZ();
                    if (z > 0 && dz > 0.5D) {
                        start = start.offset(0, 0, 1);
                    } else if (z < 0 && dz < 0.5D) {
                        start = start.offset(0, 0, -1);
                    }
                }
            }
            case NORTH, SOUTH -> {
                x = width;
                y = height;
                z = aim.face().getAxisDirection().getStep() * -depth;
                start = start.offset(-x / 2, -y / 2, 0);
                if (x % 2 == 0 && aim.hit().x - origin.getX() > 0.5D) {
                    start = start.offset(1, 0, 0);
                }
                if (y % 2 == 0 && aim.hit().y - origin.getY() > 0.5D) {
                    start = start.offset(0, 1, 0);
                }
            }
            default -> {
                x = aim.face().getAxisDirection().getStep() * -depth;
                y = height;
                z = width;
                start = start.offset(0, -y / 2, -z / 2);
                if (y % 2 == 0 && aim.hit().y - origin.getY() > 0.5D) {
                    start = start.offset(0, 1, 0);
                }
                if (z % 2 == 0 && aim.hit().z - origin.getZ() > 0.5D) {
                    start = start.offset(0, 0, 1);
                }
            }
        }

        final List<BlockPos> result = new ArrayList<>();
        if (x == 0 || y == 0 || z == 0) {
            return result;
        }
        final int stepX = Integer.signum(x);
        final int stepY = Integer.signum(y);
        final int stepZ = Integer.signum(z);
        for (int xp = start.getX(); xp != start.getX() + x; xp += stepX) {
            for (int yp = start.getY(); yp != start.getY() + y; yp += stepY) {
                for (int zp = start.getZ(); zp != start.getZ() + z; zp += stepZ) {
                    final BlockPos pos = new BlockPos(xp, yp, zp);
                    if (pos.equals(origin) || (filter != null && !filter.test(pos))) {
                        continue;
                    }
                    result.add(pos);
                }
            }
        }
        return result;
    }

    /**
     * 用指定工具破坏方块：照 ServerPlayerGameMode#destroyBlock 的流程走一遍
     * （触发破坏事件以尊重领地保护、移除方块、掉落、经验、统计），但掉落按传入的工具计算。
     * 调用方须保证 pos 所在区块已加载。
     */
    public static boolean breakWithTool(ServerPlayer player, ItemStack tool, BlockPos pos) {
        final ServerLevel level = player.serverLevel();
        final BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        final int exp = ForgeHooks.onBlockBreakEvent(level, player.gameMode.getGameModeForPlayer(), player, pos);
        if (exp == -1) {
            return false;
        }
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        final boolean canHarvest = !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
        if (!state.onDestroyedByPlayer(level, pos, player, canHarvest, level.getFluidState(pos))) {
            return false;
        }
        state.getBlock().destroy(level, pos, state);
        if (player.isCreative()) {
            return true;
        }
        if (canHarvest) {
            state.getBlock().playerDestroy(level, player, pos, state, blockEntity, tool);
        }
        if (exp > 0) {
            state.getBlock().popExperience(level, pos, exp);
        }
        return true;
    }

    /** 被瞄准的面与命中点；射线没命中该方块时按视角推断（对应 1.12 getFallbackTargetPoint） */
    private static Aim aimAt(Player player, BlockPos origin) {
        final HitResult hit = player.pick(player.getBlockReach(), 1.0F, false);
        if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK
                && blockHit.getBlockPos().equals(origin)) {
            return new Aim(blockHit.getDirection(), blockHit.getLocation());
        }
        final Direction face;
        if (player.getXRot() > 45.0F) {
            face = Direction.UP;
        } else if (player.getXRot() < -45.0F) {
            face = Direction.DOWN;
        } else {
            face = player.getDirection().getOpposite();
        }
        return new Aim(face, Vec3.atCenterOf(origin));
    }

    private record Aim(Direction face, Vec3 hit) {
    }

    private AreaMiningHelper() {
    }
}
