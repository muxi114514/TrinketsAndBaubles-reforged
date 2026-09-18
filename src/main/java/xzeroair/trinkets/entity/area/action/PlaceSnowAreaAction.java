package xzeroair.trinkets.entity.area.action;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

import xzeroair.trinkets.entity.area.AreaEffectAction;
import xzeroair.trinkets.entity.area.AreaEffectEntity;

/** 在范围内铺雪，已有雪层则加厚一层（最多 8 层）。需开启生物破坏规则。 */
public class PlaceSnowAreaAction implements AreaEffectAction {

    public static final String TYPE = "place_snow";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public boolean canAffectBlock(AreaEffectEntity area, BlockPos pos, BlockState state) {
        if (!area.mobGriefing() || !area.canOwnerModifyBlock(pos)) {
            return false;
        }
        if (state.is(Blocks.SNOW)) {
            return state.getValue(SnowLayerBlock.LAYERS) < SnowLayerBlock.MAX_HEIGHT;
        }
        return state.canBeReplaced() && Blocks.SNOW.defaultBlockState().canSurvive(area.level(), pos);
    }

    @Override
    public void affectBlock(AreaEffectEntity area, BlockPos pos, BlockState state) {
        if (state.is(Blocks.SNOW)) {
            final int layers = state.getValue(SnowLayerBlock.LAYERS);
            if (layers < SnowLayerBlock.MAX_HEIGHT) {
                area.level().setBlockAndUpdate(pos, state.setValue(SnowLayerBlock.LAYERS, layers + 1));
            }
            return;
        }
        area.level().setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState());
    }
}
