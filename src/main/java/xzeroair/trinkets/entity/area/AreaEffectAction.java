package xzeroair.trinkets.entity.area;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 区域效果实体每次脉冲时执行的一个动作（给药水、修物品、催熟、放火、结冰……）。
 * 对应 1.12 AreaEffectEntity.AreaEffectAction。
 *
 * 扩展方式：实现本接口并在 {@link AreaEffectActions} 登记类型名与读档工厂，附属模组即可追加新动作。
 * 方块回调里的 state 由调用方经区块安全读取传入，实现不应再自行读取可能跨区块的方块。
 */
public interface AreaEffectAction {

    /** 存档用的类型名，须与 AreaEffectActions 中登记的一致 */
    String type();

    /** 写入本动作的参数与状态（类型名由调用方写入） */
    default void save(CompoundTag tag) {
    }

    default boolean canAffectEntity(AreaEffectEntity area, Entity entity) {
        return false;
    }

    default void affectEntity(AreaEffectEntity area, Entity entity) {
    }

    default boolean canAffectBlock(AreaEffectEntity area, BlockPos pos, BlockState state) {
        return false;
    }

    default void affectBlock(AreaEffectEntity area, BlockPos pos, BlockState state) {
    }

    /** 为 true 时不参与逐格遍历，改由 {@link #processBlocks} 自行挑选方块 */
    default boolean handlesOwnBlockProcessing() {
        return false;
    }

    default void processBlocks(AreaEffectEntity area, float radius) {
    }
}
