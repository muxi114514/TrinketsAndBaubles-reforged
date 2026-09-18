package xzeroair.trinkets.util.helpers;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * 区块访问安全守卫（服务端主线程）。
 *
 * 为什么需要：在 tick / 事件里对「可能跨区块边界」的坐标直接调 Level#getBlockState，
 * 若目标区块未加载，ServerChunkCache 会同步加载甚至生成区块，直接卡死主线程。
 * getChunkNow 只查内存、不调度加载也不阻塞，未加载时返回 null。
 *
 * 用法约定：
 * 1. 扫描前逐格取 {@link #chunkAt}，拿到 null 就跳过该格；循环内按 (cx, cz) 复用 {@link ChunkCache}。
 * 2. 执行副作用（setBlock / destroyBlock / 爆炸 / 生成实体）之前，再用 {@link #isAreaLoaded}
 *    确认副作用作用半径覆盖的区块全部已加载，否则本 tick 直接放弃、留待下次重试——
 *    只守卫扫描不够，副作用自身也会拉区块。
 */
public final class ChunkSafety {

    /** 只查内存的区块获取；客户端世界直接走普通接口（客户端不会触发区块生成） */
    @Nullable
    public static LevelChunk chunkAt(Level level, int chunkX, int chunkZ) {
        return level.getChunkSource().getChunkNow(chunkX, chunkZ);
    }

    /** 以 (x, z) 为中心、半径 radius 格覆盖到的所有区块是否均已加载 */
    public static boolean isAreaLoaded(Level level, int x, int z, int radius) {
        final int minCx = SectionPos.blockToSectionCoord(x - radius);
        final int maxCx = SectionPos.blockToSectionCoord(x + radius);
        final int minCz = SectionPos.blockToSectionCoord(z - radius);
        final int maxCz = SectionPos.blockToSectionCoord(z + radius);
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (chunkAt(level, cx, cz) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 方块坐标矩形 [minX, maxX] × [minZ, maxZ] 覆盖到的所有区块是否均已加载 */
    public static boolean isRegionLoaded(Level level, int minX, int minZ, int maxX, int maxZ) {
        final int minCx = SectionPos.blockToSectionCoord(minX);
        final int maxCx = SectionPos.blockToSectionCoord(maxX);
        final int minCz = SectionPos.blockToSectionCoord(minZ);
        final int maxCz = SectionPos.blockToSectionCoord(maxZ);
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (chunkAt(level, cx, cz) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 单个坐标所在区块是否已加载 */
    public static boolean isLoaded(Level level, BlockPos pos) {
        return chunkAt(level, SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())) != null;
    }

    /**
     * 扫描循环内的区块缓存：连续取同一区块的方块时不重复查表。
     * 仅限单次扫描内使用，不可跨 tick 持有（区块可能在两 tick 之间被卸载）。
     */
    public static final class ChunkCache {

        private final Level level;
        private int cachedX = Integer.MIN_VALUE;
        private int cachedZ = Integer.MIN_VALUE;
        @Nullable
        private LevelChunk cached;

        public ChunkCache(Level level) {
            this.level = level;
        }

        /** 目标区块未加载时返回 null，调用方应跳过该格 */
        @Nullable
        public BlockState getBlockState(BlockPos pos) {
            final int cx = SectionPos.blockToSectionCoord(pos.getX());
            final int cz = SectionPos.blockToSectionCoord(pos.getZ());
            if (cx != this.cachedX || cz != this.cachedZ) {
                this.cached = chunkAt(this.level, cx, cz);
                this.cachedX = cx;
                this.cachedZ = cz;
            }
            return this.cached == null ? null : this.cached.getBlockState(pos);
        }
    }

    private ChunkSafety() {
    }
}
