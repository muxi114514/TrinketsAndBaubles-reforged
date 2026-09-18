package xzeroair.trinkets.entity.ai;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 末影人追随女王：离得远时寻路跟随，太远则瞬移；每位女王最多 3 名随从同时跟随。对应 1.12 EnderMoveAI。
 */
public class EnderQueenFollowGoal extends Goal {

    private static final int MAX_ACTIVE_FOLLOWERS = 3;
    private static final int TELEPORT_COOLDOWN_TICKS = 40;
    private static final double FOLLOW_START_DISTANCE_SQUARED = 64.0D;
    private static final double FOLLOW_STOP_DISTANCE_SQUARED = 36.0D;
    private static final double TELEPORT_DISTANCE_SQUARED = 576.0D;

    private final EnderMan knight;
    private final EnderQueens.Locator locator;
    @Nullable
    private Player queen;
    private int timeToRecalcPath;
    private int teleportCooldown;
    private float oldWaterCost;

    public EnderQueenFollowGoal(EnderMan knight) {
        this.knight = knight;
        this.locator = new EnderQueens.Locator(knight);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!TrinketsConfig.SERVER.items.enderCrown.abilities.enderQueen.endermanFollow.get()) {
            return false;
        }
        this.queen = this.locator.locate();
        return this.queen != null && this.knight.distanceToSqr(this.queen) > FOLLOW_START_DISTANCE_SQUARED
                && this.countFollowers(this.queen) < MAX_ACTIVE_FOLLOWERS;
    }

    @Override
    public boolean canContinueToUse() {
        if (!EnderQueens.isActiveQueen(this.queen) || !this.knight.getPersistentData().getBoolean(EnderQueens.FOLLOWING_TAG)) {
            return false;
        }
        final double distance = this.knight.distanceToSqr(this.queen);
        if (!this.locator.hasSummoner() && distance > EnderQueens.DYNAMIC_QUEEN_RANGE_SQUARED) {
            return false;
        }
        return distance > FOLLOW_STOP_DISTANCE_SQUARED;
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        this.teleportCooldown = 0;
        this.oldWaterCost = this.knight.getPathfindingMalus(BlockPathTypes.WATER);
        this.knight.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.knight.getPersistentData().putBoolean(EnderQueens.FOLLOWING_TAG, true);
    }

    @Override
    public void stop() {
        this.queen = null;
        this.knight.getNavigation().stop();
        this.knight.setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterCost);
        this.knight.getPersistentData().remove(EnderQueens.FOLLOWING_TAG);
    }

    @Override
    public void tick() {
        if (this.queen == null || this.knight.isLeashed()) {
            return;
        }
        this.knight.getLookControl().setLookAt(this.queen, 10.0F, this.knight.getMaxHeadXRot());
        final double distance = this.knight.distanceToSqr(this.queen);
        if (this.teleportCooldown > 0) {
            this.teleportCooldown--;
        }
        if (distance > TELEPORT_DISTANCE_SQUARED && this.teleportCooldown <= 0) {
            this.teleportCooldown = TELEPORT_COOLDOWN_TICKS;
            if (EnderQueens.teleportTowards(this.knight, this.queen)) {
                return;
            }
        }
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.adjustedTickDelay(10);
            if (distance > FOLLOW_STOP_DISTANCE_SQUARED) {
                this.knight.getNavigation().moveTo(this.queen, 1.0D);
            } else {
                this.knight.getNavigation().stop();
            }
        }
    }

    private int countFollowers(Player queen) {
        return this.knight.level().getEntitiesOfClass(EnderMan.class, queen.getBoundingBox().inflate(16.0D, 4.0D, 16.0D),
                other -> other != this.knight && other.getPersistentData().getBoolean(EnderQueens.FOLLOWING_TAG)).size();
    }
}
