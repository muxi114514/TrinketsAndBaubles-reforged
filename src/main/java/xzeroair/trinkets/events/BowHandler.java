package xzeroair.trinkets.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.ArrowNockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.traits.AbilityDispatcher;
import xzeroair.trinkets.traits.abilities.interfaces.IBowAbility;

/**
 * 拉弓、松弦与箭矢命中事件 → 种族钩子 + 能力（Forge 事件总线）。对应 1.12 CombatHandler 中的三个弓箭处理方法。
 *
 * 移植说明：1.12 的 ProjectileImpactEvent.Arrow 子事件在 1.20.1 已合并为 ProjectileImpactEvent，
 * 由能力自行判断投射物是否为箭。
 */
public class BowHandler {

    @SubscribeEvent
    public void onArrowNock(ArrowNockEvent event) {
        final EntityProperties properties = EntityProperties.get(event.getEntity());
        if (properties != null) {
            properties.getRaceHandler().bowNocked(event);
        }
        AbilityDispatcher.forEach(event.getEntity(), IBowAbility.class, ability -> ability.knockArrow(event));
    }

    @SubscribeEvent
    public void onArrowLoose(ArrowLooseEvent event) {
        final EntityProperties properties = EntityProperties.get(event.getEntity());
        if (properties != null) {
            properties.getRaceHandler().bowUsed(event);
        }
        AbilityDispatcher.forEach(event.getEntity(), IBowAbility.class, ability -> {
            if (!event.isCanceled()) {
                ability.looseArrow(event);
            }
        });
    }

    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile().getOwner() instanceof LivingEntity shooter)) {
            return;
        }
        AbilityDispatcher.forEach(shooter, IBowAbility.class, ability -> ability.arrowImpact(event));
    }
}
