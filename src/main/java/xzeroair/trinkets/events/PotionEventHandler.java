package xzeroair.trinkets.events;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.traits.AbilityDispatcher;
import xzeroair.trinkets.traits.abilities.interfaces.IPotionAbility;

/**
 * 效果免疫的「阻断层」（Forge 事件总线）。
 *
 * 为什么必须有这一层：若只在 tick 里 removeEffect，效果其实已被添加并正常结算——
 * 中毒/凋零在被清掉之前已经造成过伤害，表现为「免疫了还在掉血」。
 * MobEffectEvent.Applicable 由 LivingEntity#canBeAffected 触发，DENY 后效果根本不会被添加，
 * 一次伤害都不会结算。
 *
 * 两层缺一不可：
 * - 阻断层（本类）：拦截新施加的效果；
 * - 清理层（EntityRacePropertiesHandler#whileTransformed 与各能力的 tick）：处理变身/获得能力前
 *   身上已有的存量效果。
 *
 * 对应 1.12 中 PotionApplicableEvent 的处理（EventHandler#potionApplied）。
 */
public class PotionEventHandler {

    @SubscribeEvent
    public void onEffectApplicable(MobEffectEvent.Applicable event) {
        final LivingEntity entity = event.getEntity();
        final MobEffectInstance effect = event.getEffectInstance();
        final EntityProperties properties = EntityProperties.get(entity);
        if (properties == null || effect == null) {
            return;
        }
        boolean deny = properties.getRaceHandler().potionBeingApplied(effect);
        if (!deny) {
            deny = AbilityDispatcher.chainCancel(entity, IPotionAbility.class, false,
                    (ability, cancel) -> ability.potionApplied(entity, effect, cancel));
        }
        if (deny) {
            event.setResult(Event.Result.DENY);
        }
    }
}
