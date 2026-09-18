package xzeroair.trinkets.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.traits.AbilityDispatcher;
import xzeroair.trinkets.traits.abilities.interfaces.IMiningAbility;

/**
 * 挖掘事件 → 种族钩子 + 能力。对应 1.12 events/BlockBreakEvents。
 *
 * 移植说明：1.12 的 HarvestDropsEvent 在 1.20.1 已被 Forge 移除（掉落改由战利品表决定），
 * IMiningAbility#blockDrops 改由全局战利品修改器 loot/AbilityBlockDropsModifier 分发。
 * 1.12 注释掉的 HarvestCheck 钩子在此启用（熟练矿工「挖掘等级 +1」需要）。
 */
public class BlockBreakHandler {

    /** 挖掘速度 ≥ 100 视为瞬破、≤ 0 视为无法挖掘，二者都不必再问后续能力（与 1.12 一致） */
    private static final float INSTANT_BREAK_SPEED = 100.0F;

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        final Player player = event.getEntity();
        final EntityProperties properties = EntityProperties.get(player);
        if (properties == null) {
            return;
        }
        properties.getRaceHandler().breakingBlock(event);
        if (event.isCanceled()) {
            return;
        }
        final BlockPos pos = event.getPosition().orElse(player.blockPosition());
        final float original = event.getNewSpeed();
        final float[] speed = {original};
        AbilityDispatcher.forEach(player, IMiningAbility.class, ability -> {
            if (speed[0] <= 0F || speed[0] >= INSTANT_BREAK_SPEED) {
                return;
            }
            speed[0] = ability.breakingBlock(player, event.getState(), pos, original, speed[0]);
        });
        if (speed[0] == original) {
            return;
        }
        if (speed[0] <= 0) {
            event.setCanceled(true);
        } else {
            event.setNewSpeed(speed[0]);
        }
    }

    /** 能否采集：只在原本不能采集时询问能力，任一能力放行即可（两端都会触发，决定掉落与挖掘速度惩罚） */
    @SubscribeEvent
    public void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (event.canHarvest()) {
            return;
        }
        final Player player = event.getEntity();
        final boolean canHarvest = AbilityDispatcher.chainCancel(player, IMiningAbility.class, false,
                (ability, harvest) -> ability.canHarvest(player, event.getTargetBlock(), harvest));
        if (canHarvest) {
            event.setCanHarvest(true);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        final Player player = event.getPlayer();
        final EntityProperties properties = EntityProperties.get(player);
        if (properties == null || !(event.getLevel() instanceof Level level)) {
            return;
        }
        properties.getRaceHandler().blockBroken(event);
        if (event.isCanceled()) {
            return;
        }
        final int exp = AbilityDispatcher.chainInt(player, IMiningAbility.class, event.getExpToDrop(),
                (ability, xp) -> ability.brokeBlock(player, level, event.getState(), event.getPos(), xp));
        event.setExpToDrop(exp);
    }
}
