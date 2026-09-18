package xzeroair.trinkets.traits.abilities.interfaces;

import net.minecraft.world.entity.LivingEntity;

/** 介入治疗量。 */
public interface IHealAbility extends IAbilityInterface {

    float onHeal(LivingEntity entity, float healAmount);
}
