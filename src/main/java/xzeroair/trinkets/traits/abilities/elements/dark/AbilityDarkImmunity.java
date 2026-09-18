package xzeroair.trinkets.traits.abilities.elements.dark;

import javax.annotation.Nonnull;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.init.ModDamageTypes;
import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.IAttackAbility;
import xzeroair.trinkets.util.config.abilities.ElementAffinityAbilityConfigs;

/**
 * 暗系免疫：开启「暗伤治疗」时，凋零伤害被取消并等量回血、其余暗系伤害按倍率转为治疗；
 * 关闭时直接免疫全部暗系伤害（xat:is_dark 标签）。
 */
public class AbilityDarkImmunity extends Ability implements IAttackAbility {

    private final ElementAffinityAbilityConfigs.DarkImmunity config;

    public AbilityDarkImmunity(@Nonnull ElementAffinityAbilityConfigs.DarkImmunity config) {
        super(AbilityNames.IMMUNITY_DARK);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public boolean attacked(LivingEntity attacked, DamageSource source, float dmg, boolean cancel) {
        if (this.healsFromDark()) {
            if (dmg > 0 && source.is(DamageTypes.WITHER)) {
                attacked.heal(dmg);
                return true;
            }
        } else if (source.is(ModDamageTypes.IS_DARK)) {
            return true;
        }
        return cancel;
    }

    @Override
    public float hurt(LivingEntity attacked, DamageSource source, float dmg) {
        if (dmg > 0 && this.healsFromDark() && source.is(ModDamageTypes.IS_DARK)) {
            final float heal = dmg * this.config.healMultiplier.get().floatValue();
            attacked.heal(heal);
            return Math.max(dmg - heal, 0);
        }
        return dmg;
    }

    private boolean healsFromDark() {
        return this.config.healFromDark.get() && this.config.healMultiplier.get() > 0;
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final boolean heals = this.healsFromDark();
        variables.flag("immune", !heals)
                .flag("heal", heals)
                .number("multiplier", heals, this.config.healMultiplier.get())
                .translated("wither", true, MobEffects.WITHER.getDescriptionId());
    }
}
