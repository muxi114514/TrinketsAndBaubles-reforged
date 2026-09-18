package xzeroair.trinkets.traits.abilities.elements.poison;

import javax.annotation.Nonnull;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.init.ModDamageTypes;
import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.interfaces.IAttackAbility;
import xzeroair.trinkets.util.config.abilities.IAbilityConfig;

/** 毒系免疫：取消毒系伤害（xat:is_poison 标签）。中毒效果本身的拦截由佩戴它的饰品负责（与 1.12 一致）。 */
public class AbilityPoisonImmunity extends Ability implements IAttackAbility {

    public AbilityPoisonImmunity(@Nonnull IAbilityConfig config) {
        super(AbilityNames.IMMUNITY_POISON);
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public boolean attacked(LivingEntity attacked, DamageSource source, float dmg, boolean cancel) {
        return source.is(ModDamageTypes.IS_POISON) || cancel;
    }
}
