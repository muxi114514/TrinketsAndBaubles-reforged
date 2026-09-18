package xzeroair.trinkets.traits.abilities.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 睡眠相关。
 * 移植说明：1.12 的 EntityPlayer.SleepResult 在 1.20.1 为 Player.BedSleepingProblem。
 */
public interface ISleepAbility extends IAbilityInterface {

    default Player.BedSleepingProblem onStartSleeping(LivingEntity entity, BlockPos pos, Player.BedSleepingProblem result) {
        return result;
    }

    void onWakeUp(LivingEntity entity, boolean wakeImmediately, boolean updatedWorld, boolean setSpawn);
}
