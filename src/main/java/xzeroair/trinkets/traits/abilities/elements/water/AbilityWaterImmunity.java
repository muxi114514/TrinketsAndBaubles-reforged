package xzeroair.trinkets.traits.abilities.elements.water;

import javax.annotation.Nonnull;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.init.ModDamageTypes;
import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.interfaces.IAttackAbility;
import xzeroair.trinkets.util.config.abilities.IAbilityConfig;

/** 水系免疫：取消水系伤害（xat:is_water 标签）。 */
public class AbilityWaterImmunity extends Ability implements IAttackAbility {

    public AbilityWaterImmunity(@Nonnull IAbilityConfig config) {
        super(AbilityNames.IMMUNITY_WATER);
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public boolean attacked(LivingEntity attacked, DamageSource source, float dmg, boolean cancel) {
        return source.is(ModDamageTypes.IS_WATER) || cancel;
    }
}
