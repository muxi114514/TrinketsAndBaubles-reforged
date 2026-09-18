package xzeroair.trinkets.traits.abilities.interfaces;

import net.minecraft.world.entity.LivingEntity;

/** 跳跃与坠落。 */
public interface IJumpAbility extends IAbilityInterface {

    default void jump(LivingEntity entity) {
    }

    default float fallDistance(LivingEntity entity, float distance) {
        return distance;
    }

    default float fallDamageMultiplier(LivingEntity entity, float multiplier) {
        return multiplier;
    }

    default boolean fall(LivingEntity entity, float distance, float multiplier, boolean cancel) {
        return cancel;
    }
}
