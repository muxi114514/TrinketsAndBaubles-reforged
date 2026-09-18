package xzeroair.trinkets.loot;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 一次方块掉落的上下文（取自战利品上下文），供 IMiningAbility#blockDrops 使用。对应 1.12 HarvestDropsEvent 携带的信息。
 *
 * @param tool 破坏所用工具（战利品上下文里的 TOOL，无则为空栈）
 */
public record BlockDropContext(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool,
        @Nullable BlockEntity blockEntity, LivingEntity breaker) {

    /** 同一线程上正在「换工具重掷」时，内层战利品生成不再触发本模组的修改器，避免递归 */
    private static final ThreadLocal<Boolean> REROLLING = ThreadLocal.withInitial(() -> false);

    public boolean isSilkTouching() {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, this.tool) > 0;
    }

    public int fortuneLevel() {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, this.tool);
    }

    /** 用另一把工具按同一方块、位置、方块实体重新掷一次战利品表 */
    public List<ItemStack> dropsWithTool(ItemStack otherTool) {
        REROLLING.set(true);
        try {
            return Block.getDrops(this.state, this.level, this.pos, this.blockEntity, this.breaker, otherTool);
        } finally {
            REROLLING.set(false);
        }
    }

    static boolean isRerolling() {
        return REROLLING.get();
    }
}
