package xzeroair.trinkets.entity.area.action;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

import xzeroair.trinkets.entity.area.AreaEffectAction;
import xzeroair.trinkets.entity.area.AreaEffectEntity;
import xzeroair.trinkets.init.ModBlocks;

/** 把范围内上方可替换的静止水源冻成霜冰、岩浆源冻成冷却岩浆，每次脉冲最多处理若干格。 */
public class FreezeLiquidAreaAction implements AreaEffectAction {

    public static final String TYPE = "freeze_liquid";

    private final int maxBlocksPerPulse;
    private int frozenThisPulse;
    private int lastPulseTick = -1;

    public FreezeLiquidAreaAction(int maxBlocksPerPulse) {
        this.maxBlocksPerPulse = Math.max(0, maxBlocksPerPulse);
    }

    public static FreezeLiquidAreaAction load(CompoundTag tag) {
        return new FreezeLiquidAreaAction(tag.getInt("MaxBlocksPerPulse"));
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
        if (this.frozenThisPulse >= this.maxBlocksPerPulse || !area.mobGriefing() || !area.canOwnerModifyBlock(pos)) {
            return false;
        }
        final Block frozen = frozenForm(state);
        // 区域实体在处理方块前已确认半径 +1 格覆盖的区块均已加载
        return frozen != null && area.level().getBlockState(pos.above()).canBeReplaced()
                && area.level().isUnobstructed(frozen.defaultBlockState(), pos, CollisionContext.empty());
    }

    @Override
    public void affectBlock(AreaEffectEntity area, BlockPos pos, BlockState state) {
        this.resetPulse(area);
        final Block frozen = frozenForm(state);
        if (frozen == null || this.frozenThisPulse >= this.maxBlocksPerPulse) {
            return;
        }
        freeze(area.level(), pos, frozen);
        this.frozenThisPulse++;
    }

    /** 冻结一格并安排融化计时（霜冰与冷却岩浆都会随时间融回液体） */
    public static void freeze(Level level, BlockPos pos, Block frozen) {
        level.setBlockAndUpdate(pos, frozen.defaultBlockState());
        level.scheduleTick(pos, frozen, Mth.nextInt(level.random, 60, 120));
    }

    /** 静止水源 → 霜冰，静止岩浆源 → 冷却岩浆，其余返回 null */
    @Nullable
    public static Block frozenForm(BlockState state) {
        if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
            return Blocks.FROSTED_ICE;
        }
        if (state.is(Blocks.LAVA) && state.getFluidState().isSource()) {
            return ModBlocks.COOLED_MAGMA.get();
        }
        return null;
    }

    private void resetPulse(AreaEffectEntity area) {
        if (this.lastPulseTick != area.tickCount) {
            this.lastPulseTick = area.tickCount;
            this.frozenThisPulse = 0;
        }
    }
}
