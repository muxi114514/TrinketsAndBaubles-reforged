package xzeroair.trinkets.traits.abilities.interfaces;

import net.minecraft.world.entity.LivingEntity;

/** 被闪电击中时介入。 */
public interface ILightningStrikeAbility extends IAbilityInterface {

    default boolean onStruckByLightning(LivingEntity entity, boolean cancel) {
        return cancel;
    }
}
