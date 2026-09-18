package xzeroair.trinkets.blocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FrostedIceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 冷却岩浆：冰霜吐息冻住岩浆时临时生成，随时间逐级融化回岩浆，破坏也会变回岩浆。
 * 对应 1.12 blocks/TempBlock。
 *
 * 移植说明：1.12 手写了一整套「按光照与邻居数逐级融化」逻辑，与原版霜冰完全一致，只是融化产物是岩浆；
 * 1.20.1 直接继承 FrostedIceBlock，只改写两处产出水的地方（融化与被挖掉）。
 */
public class CooledMagmaBlock extends FrostedIceBlock {

    public CooledMagmaBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void melt(BlockState state, Level level, BlockPos pos) {
        if (level.dimensionType().ultraWarm()) {
            level.removeBlock(pos, false);
            return;
        }
        level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
        level.neighborChanged(pos, Blocks.LAVA, pos);
    }

    /** 不掉落任何物品；非精准采集时，下方是实心或液体则变回岩浆（与 1.12 一致） */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0) {
            return;
        }
        if (level.dimensionType().ultraWarm()) {
            level.removeBlock(pos, false);
            return;
        }
        final BlockState below = level.getBlockState(pos.below());
        if (below.blocksMotion() || below.liquid()) {
            level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
        }
    }
}
