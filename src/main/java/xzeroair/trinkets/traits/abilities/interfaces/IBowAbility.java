package xzeroair.trinkets.traits.abilities.interfaces;

import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.ArrowNockEvent;

/**
 * 弓箭相关。
 * 移植说明：1.12 的 ProjectileImpactEvent.Arrow 子事件在 1.20.1 已合并为 ProjectileImpactEvent，
 * 需要时自行判断 event.getProjectile() 的类型。
 */
public interface IBowAbility extends IItemUseAbility {

    default void knockArrow(ArrowNockEvent event) {
    }

    default void looseArrow(ArrowLooseEvent event) {
    }

    default void arrowImpact(ProjectileImpactEvent event) {
    }
}
