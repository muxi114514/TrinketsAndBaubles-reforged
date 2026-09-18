package xzeroair.trinkets.traits.abilities.interfaces;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** 药水效果施加时介入；拒绝后即便用指令也加不上。 */
public interface IPotionAbility extends IAbilityInterface {

    boolean potionApplied(LivingEntity entity, MobEffectInstance effect, boolean cancel);
}
