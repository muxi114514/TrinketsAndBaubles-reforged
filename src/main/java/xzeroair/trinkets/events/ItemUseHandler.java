package xzeroair.trinkets.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.capabilities.magic.ManaRecovery;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.traits.AbilityDispatcher;
import xzeroair.trinkets.traits.abilities.interfaces.IItemUseAbility;
import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 物品使用事件 → 能力 + 魔力回复。对应 1.12 EventHandler 中 LivingEntityUseItemEvent 的四个处理方法。
 * 开始/持续阶段：能力可改写剩余时长，时长变负即不再交给后续能力；停止/完成阶段：通知全部能力。
 * 完成使用后按魔力回复名单回复魔力（仅服务端）。
 */
public class ItemUseHandler {

    @SubscribeEvent
    public void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        final LivingEntity entity = event.getEntity();
        final ItemStack stack = event.getItem();
        final int duration = AbilityDispatcher.chainDuration(entity, IItemUseAbility.class, event.getDuration(),
                (ability, d) -> ability.onItemStartUse(entity, stack, d));
        if (duration != event.getDuration()) {
            event.setDuration(duration);
        }
    }

    @SubscribeEvent
    public void onItemUseTick(LivingEntityUseItemEvent.Tick event) {
        final LivingEntity entity = event.getEntity();
        final ItemStack stack = event.getItem();
        final int duration = AbilityDispatcher.chainDuration(entity, IItemUseAbility.class, event.getDuration(),
                (ability, d) -> ability.onItemUseTick(entity, stack, d));
        if (duration != event.getDuration()) {
            event.setDuration(duration);
        }
    }

    @SubscribeEvent
    public void onItemUseStop(LivingEntityUseItemEvent.Stop event) {
        final LivingEntity entity = event.getEntity();
        AbilityDispatcher.forEach(entity, IItemUseAbility.class,
                ability -> ability.onItemUseStop(entity, event.getItem(), event.getDuration()));
    }

    @SubscribeEvent
    public void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        final LivingEntity entity = event.getEntity();
        AbilityDispatcher.forEach(entity, IItemUseAbility.class,
                ability -> ability.onItemUseFinish(entity, event.getItem(), event.getDuration()));
        final EntityProperties properties = EntityProperties.get(entity);
        if (properties != null) {
            properties.getRaceHandler().itemUseFinished(event.getItem());
        }
        applyManaRecovery(entity, event.getItem());
    }

    private static void applyManaRecovery(LivingEntity entity, ItemStack consumed) {
        if (entity.level().isClientSide || !TrinketsConfig.SERVER.magic.manaEnabled.get()) {
            return;
        }
        final MagicStats magic = MagicStats.get(entity);
        final ManaRecovery.Entry entry = ManaRecovery.find(consumed);
        if (magic == null || entry == null) {
            return;
        }
        final float amount = entry.resolve(magic.getMaxMana());
        if (amount != 0F) {
            magic.addMana(amount);
        }
    }
}
