package xzeroair.trinkets.traits.abilities.elements.dark;

import javax.annotation.Nonnull;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.IAttackAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.util.config.abilities.ElementAffinityAbilityConfigs;

/**
 * 暗系亲和：攻击时 1/chance 概率给目标凋零；对凋零中的目标造成伤害时吸血（每秒最多一次）。
 *
 * 移植说明：1.12 经 sendAbilityData 把配置同步给客户端；1.20.1 的 SERVER 配置自动同步，直接读配置。
 */
public class AbilityAffinityDark extends Ability implements ITickableAbility, IAttackAbility {

    private static final int LEECH_COOLDOWN = 20;

    private final ElementAffinityAbilityConfigs.Dark config;
    private int cooldown;

    public AbilityAffinityDark(@Nonnull ElementAffinityAbilityConfigs.Dark config) {
        super(AbilityNames.AFFINITY_DARK);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        if (this.cooldown > 0) {
            this.cooldown--;
        }
    }

    @Override
    public boolean attackEntity(LivingEntity target, DamageSource source, float dmg, boolean cancel) {
        final int chance = this.config.chance.get();
        if (!target.level().isClientSide && chance > 0 && !target.hasEffect(MobEffects.WITHER)
                && this.random.nextInt(chance) == 0) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, this.config.duration.get(), 0, false, true));
        }
        return cancel;
    }

    @Override
    public float damageEntity(LivingEntity target, DamageSource source, float dmg) {
        if (this.cooldown < 1 && target.hasEffect(MobEffects.WITHER) && source.getEntity() instanceof LivingEntity attacker) {
            if (this.config.trueLeech.get()) {
                attacker.heal(dmg);
            } else {
                final float leech = this.config.leechAmount.get().floatValue();
                if (leech > 0) {
                    attacker.heal(Math.min(dmg, leech));
                }
            }
            this.cooldown = LEECH_COOLDOWN;
        }
        return dmg;
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final int chance = this.config.chance.get();
        final boolean trueLeech = this.config.trueLeech.get();
        final double leech = this.config.leechAmount.get();
        variables.oneIn("chance", chance > 0, chance)
                .seconds("duration", chance > 0, this.config.duration.get())
                .translated("wither", true, MobEffects.WITHER.getDescriptionId())
                .number("leech", !trueLeech && leech > 0, leech)
                .flag("trueleech", trueLeech)
                .seconds("cooldown", true, LEECH_COOLDOWN);
    }
}
