package xzeroair.trinkets.entity.area.action;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

import xzeroair.trinkets.entity.area.AreaEffectAction;
import xzeroair.trinkets.entity.area.AreaEffectEntity;

/** 对范围内可施骨粉的方块催熟，每次脉冲最多处理若干格。 */
public class GrowBlockAreaAction implements AreaEffectAction {

    public static final String TYPE = "grow_block";

    private final int maxBlocksPerPulse;
    private int grownThisPulse;
    private int lastPulseTick = -1;

    public GrowBlockAreaAction(int maxBlocksPerPulse) {
        this.maxBlocksPerPulse = Math.max(0, maxBlocksPerPulse);
    }

    public static GrowBlockAreaAction load(CompoundTag tag) {
        return new GrowBlockAreaAction(tag.getInt("MaxBlocksPerPulse"));
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void save(CompoundTag tag) {
        tag.putInt("MaxBlocksPerPulse", this.maxBlocksPerPulse);
    }

    @Override
    public boolean canAffectBlock(AreaEffectEntity area, BlockPos pos, BlockState state) {
        this.resetPulse(area);
        return this.grownThisPulse < this.maxBlocksPerPulse && area.canOwnerModifyBlock(pos)
                && state.getBlock() instanceof BonemealableBlock growable
                && growable.isValidBonemealTarget(area.level(), pos, state, false);
    }

    @Override
    public void affectBlock(AreaEffectEntity area, BlockPos pos, BlockState state) {
        this.resetPulse(area);
        if (this.grownThisPulse >= this.maxBlocksPerPulse || !(area.level() instanceof ServerLevel level)
                || !(state.getBlock() instanceof BonemealableBlock growable)) {
            return;
        }
        if (growable.isBonemealSuccess(level, level.random, pos, state)) {
            growable.performBonemeal(level, level.random, pos, state);
            this.grownThisPulse++;
            area.spawnBonemealParticles(pos, 18, 0.65D, 0.45D);
        }
    }

    private void resetPulse(AreaEffectEntity area) {
        if (this.lastPulseTick != area.tickCount) {
            this.lastPulseTick = area.tickCount;
            this.grownThisPulse = 0;
        }
    }
}
