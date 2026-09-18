package xzeroair.trinkets.traits.abilities.elements.poison;

import javax.annotation.Nonnull;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.init.ModDamageTypes;
import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.IAttackAbility;
import xzeroair.trinkets.util.config.abilities.ElementAffinityAbilityConfigs;

/**
 * 毒系亲和：非魔法/火焰/爆炸伤害命中未中毒的目标时 1/chance 概率使其中毒；
 * 对中毒目标造成毒系伤害（xat:is_poison 标签）时按倍率增伤。
 */
public class AbilityPoisonAffinity extends Ability implements IAttackAbility {

    private final ElementAffinityAbilityConfigs.Poison config;

    public AbilityPoisonAffinity(@Nonnull ElementAffinityAbilityConfigs.Poison config) {
        super(AbilityNames.AFFINITY_POISON);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public float hurtEntity(@Nonnull LivingEntity target, DamageSource source, float dmg) {
        final int chance = this.config.chance.get();
        if (!target.level().isClientSide && !target.hasEffect(MobEffects.POISON) && isPhysical(source)
                && chance > 0 && this.random.nextInt(chance) == 0) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, this.config.duration.get(), 0, false, true));
        }
        final float multiplier = this.config.damageMultiplier.get().floatValue();
        if (multiplier <= 1 || !target.hasEffect(MobEffects.POISON) || !source.is(ModDamageTypes.IS_POISON)) {
            return dmg;
        }
        return dmg * multiplier;
    }

    /** 对应 1.12 的 !isMagicDamage() &amp;&amp; !isFireDamage() &amp;&amp; !isExplosion() */
    private static boolean isPhysical(DamageSource source) {
        return !source.is(DamageTypes.MAGIC) && !source.is(DamageTypes.INDIRECT_MAGIC)
                && !source.is(DamageTypeTags.IS_FIRE) && !source.is(DamageTypeTags.IS_EXPLOSION);
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final int chance = this.config.chance.get();
        final double multiplier = this.config.damageMultiplier.get();
        variables.oneIn("chance", chance > 0, chance)
                .seconds("duration", chance > 0, this.config.duration.get())
                .translated("poison", true, MobEffects.POISON.getDescriptionId())
                .number("multiplier", multiplier > 1, multiplier);
    }
}
