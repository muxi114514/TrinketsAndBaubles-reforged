package xzeroair.trinkets.traits.abilities.interfaces;

import net.minecraft.world.entity.LivingEntity;

/** 每 tick 驱动的能力。 */
public interface ITickableAbility extends IAbilityInterface {

    default void tickAbilityPre(LivingEntity entity) {
    }

    void tickAbility(LivingEntity entity);
}
