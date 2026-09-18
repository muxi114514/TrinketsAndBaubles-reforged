package xzeroair.trinkets.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import xzeroair.trinkets.traits.AbilityDispatcher;
import xzeroair.trinkets.traits.abilities.interfaces.IPickupExpAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ISleepAbility;

/**
 * 玩家睡眠与拾取经验事件 → 能力（Forge 事件总线）。对应 1.12 events/PlayerEventMC 的对应处理方法。
 */
public class PlayerEventHandler {

    /** 能力给出的结果与当前结果不同时才覆盖（与 1.12 一致） */
    @SubscribeEvent
    public void onSleepInBed(PlayerSleepInBedEvent event) {
        final LivingEntity entity = event.getEntity();
        AbilityDispatcher.forEach(entity, ISleepAbility.class, ability -> {
            final Player.BedSleepingProblem current = event.getResultStatus();
            final Player.BedSleepingProblem result = ability.onStartSleeping(entity, event.getPos(), current);
            if (result != null && result != current) {
                event.setResult(result);
            }
        });
    }

    /** 1.20.1 的醒来事件不再携带「是否设置出生点」，恒传 false（现有能力均不使用该参数） */
    @SubscribeEvent
    public void onWakeUp(PlayerWakeUpEvent event) {
        final LivingEntity entity = event.getEntity();
        AbilityDispatcher.forEach(entity, ISleepAbility.class,
                ability -> ability.onWakeUp(entity, event.wakeImmediately(), event.updateLevel(), false));
    }

    @SubscribeEvent
    public void onPickupXp(PlayerXpEvent.PickupXp event) {
        final Player player = event.getEntity();
        AbilityDispatcher.forEach(player, IPickupExpAbility.class, ability -> ability.onPickup(player, event.getOrb()));
    }
}
