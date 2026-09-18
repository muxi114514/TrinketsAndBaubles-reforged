package xzeroair.trinkets.entity.area.action;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;

import xzeroair.trinkets.entity.area.AreaEffectAction;
import xzeroair.trinkets.entity.area.AreaEffectEntity;
import xzeroair.trinkets.util.helpers.ChunkSafety;

/**
 * 在范围内随机点火：首次脉冲按递减概率连续点若干处，此后每隔一段时间以一定概率再点一处。
 * 需开启生物破坏规则。
 */
public class PlaceFireAreaAction implements AreaEffectAction {

    public static final String TYPE = "place_fire";

    private static final int RANDOM_PLACEMENT_TRIES = 24;

    private final int initialMaxBlocks;
    private final int delayedAttemptInterval;
    private final float delayedAttemptChance;
    private int initialFiresPlaced;
    private int initialPulseTick = -1;
    private int nextDelayedAttemptTick = -1;
    private int lastPulseTick = -1;
    private boolean initialPlacementFinished;
    private boolean delayedAttemptSucceeded;
    private boolean placedThisPulse;

    public PlaceFireAreaAction(int initialMaxBlocks, int delayedAttemptInterval, float delayedAttemptChance) {
        this.initialMaxBlocks = Math.max(0, initialMaxBlocks);
        this.delayedAttemptInterval = Math.max(1, delayedAttemptInterval);
        this.delayedAttemptChance = Mth.clamp(delayedAttemptChance, 0.0F, 1.0F);
    }

    public static PlaceFireAreaAction load(CompoundTag tag) {
        final PlaceFireAreaAction action = new PlaceFireAreaAction(
                tag.contains("InitialMaxBlocks") ? tag.getInt("InitialMaxBlocks") : 6,
                tag.contains("DelayedAttemptInterval") ? tag.getInt("DelayedAttemptInterval") : 20,
                tag.contains("DelayedAttemptChance") ? tag.getFloat("DelayedAttemptChance") : 0.5F);
        if (tag.contains("InitialPulseTick")) {
            action.initialFiresPlaced = tag.getInt("InitialFiresPlaced");
            action.initialPulseTick = tag.getInt("InitialPulseTick");
            action.nextDelayedAttemptTick = tag.getInt("NextDelayedAttemptTick");
            action.initialPlacementFinished = tag.getBoolean("InitialPlacementFinished");
        } else {
            action.initialPulseTick = 0;
            action.nextDelayedAttemptTick = 0;
        }
        return action;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void save(CompoundTag tag) {
        tag.putInt("InitialMaxBlocks", this.initialMaxBlocks);
        tag.putInt("DelayedAttemptInterval", this.delayedAttemptInterval);
        tag.putFloat("DelayedAttemptChance", this.delayedAttemptChance);
        tag.putInt("InitialFiresPlaced", this.initialFiresPlaced);
        tag.putInt("InitialPulseTick", this.initialPulseTick);
        tag.putInt("NextDelayedAttemptTick", this.nextDelayedAttemptTick);
        tag.putBoolean("InitialPlacementFinished", this.initialPlacementFinished);
    }

    @Override
    public boolean handlesOwnBlockProcessing() {
        return true;
    }

    @Override
    public void processBlocks(AreaEffectEntity area, float radius) {
        this.preparePulse(area);
        final RandomSource random = area.level().random;
        if (this.initialPulseTick == area.tickCount) {
            while (!this.initialPlacementFinished && this.initialFiresPlaced < this.initialMaxBlocks) {
                if (random.nextFloat() >= this.initialPlacementChance()) {
                    this.initialPlacementFinished = true;
                    return;
                }
                if (!this.tryPlaceRandomFire(area, radius)) {
                    return;
                }
            }
        } else if (this.delayedAttemptSucceeded && !this.placedThisPulse) {
            this.tryPlaceRandomFire(area, radius);
        }
    }

    private boolean tryPlaceRandomFire(AreaEffectEntity area, float radius) {
        final RandomSource random = area.level().random;
        final int verticalRange = Mth.ceil(area.getVerticalRadius());
        for (int attempt = 0; attempt < RANDOM_PLACEMENT_TRIES; attempt++) {
            final double offsetX = (random.nextDouble() - random.nextDouble()) * radius;
            final double offsetZ = (random.nextDouble() - random.nextDouble()) * radius;
            final int y = Mth.floor(area.getY()) + random.nextInt(verticalRange * 2 + 1) - verticalRange;
            final BlockPos pos = BlockPos.containing(area.getX() + offsetX, y, area.getZ() + offsetZ);
            if (this.canPlaceFire(area, pos)) {
                this.placeFire(area, pos);
                return true;
            }
        }
        return false;
    }

    private boolean canPlaceFire(AreaEffectEntity area, BlockPos pos) {
        final Level level = area.level();
        if (!area.mobGriefing() || !area.canOwnerModifyBlock(pos) || !ChunkSafety.isLoaded(level, pos)) {
            return false;
        }
        final BlockState state = level.getBlockState(pos);
        return state.canBeReplaced() && state.getFluidState().isEmpty() && BaseFireBlock.canBePlacedAt(level, pos, Direction.UP);
    }

    private void placeFire(AreaEffectEntity area, BlockPos pos) {
        area.level().setBlockAndUpdate(pos, BaseFireBlock.getState(area.level(), pos));
        if (this.initialPulseTick == area.tickCount) {
            this.initialFiresPlaced++;
        }
        this.placedThisPulse = true;
    }

    private void preparePulse(AreaEffectEntity area) {
        if (this.lastPulseTick == area.tickCount) {
            return;
        }
        this.lastPulseTick = area.tickCount;
        this.placedThisPulse = false;
        this.delayedAttemptSucceeded = false;
        if (this.initialPulseTick < 0) {
            this.initialPulseTick = area.tickCount;
            this.nextDelayedAttemptTick = area.tickCount + this.delayedAttemptInterval;
            return;
        }
        if (area.tickCount >= this.nextDelayedAttemptTick) {
            this.delayedAttemptSucceeded = area.level().random.nextFloat() < this.delayedAttemptChance;
            this.nextDelayedAttemptTick = area.tickCount + this.delayedAttemptInterval;
        }
    }

    /** 首批点火的概率随已点数量递减，最低 20% */
    private float initialPlacementChance() {
        return Math.max(0.2F, 1.0F - 0.2F * Math.max(0, this.initialFiresPlaced - 1));
    }
}
