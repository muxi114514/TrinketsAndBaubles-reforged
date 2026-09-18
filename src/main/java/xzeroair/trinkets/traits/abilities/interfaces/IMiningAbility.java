package xzeroair.trinkets.traits.abilities.interfaces;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import xzeroair.trinkets.loot.BlockDropContext;

/** 挖掘相关：速度、能否采集、破坏掉落经验、掉落物。 */
public interface IMiningAbility extends IAbilityInterface {

    default float breakingBlock(LivingEntity entity, BlockState state, BlockPos pos, float originalSpeed, float newSpeed) {
        return newSpeed;
    }

    /** 能否采集该方块（决定掉落与「工具不对时挖掘极慢」的惩罚）；对应 1.12 被注释掉的 HarvestCheck 钩子 */
    default boolean canHarvest(LivingEntity entity, BlockState state, boolean canHarvest) {
        return canHarvest;
    }

    default int brokeBlock(LivingEntity entity, Level level, BlockState state, BlockPos pos, int expToDrop) {
        return expToDrop;
    }

    /**
     * 方块掉落（仅服务端，经全局战利品修改器调用），可直接增删 drops。
     *
     * 移植说明：1.12 签名为（实体, 世界, 方块, 位置, 掉落, 几率, 精准采集, 时运），1.20.1 将这些信息与「换工具重掷」合并为 {@link BlockDropContext}。
     *
     * @return 新的掉落几率，≤ 0 表示不掉落
     */
    default float blockDrops(LivingEntity entity, BlockDropContext context, List<ItemStack> drops, float dropChance) {
        return dropChance;
    }
}
