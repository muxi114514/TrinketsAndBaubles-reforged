package xzeroair.trinkets.traits.abilities;

import javax.annotation.Nonnull;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.init.ModEffects;
import xzeroair.trinkets.init.ModRaces;
import xzeroair.trinkets.items.potions.BleedEffect;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.IAttackAbility;
import xzeroair.trinkets.util.compat.ModCompat;
import xzeroair.trinkets.util.config.abilities.ViciousStrikeAbilityConfig;
import xzeroair.trinkets.util.helpers.NumberText;

/**
 * 凶狠打击：直接近战命中时 1/chance 概率使目标流血；已在流血则延长时长并叠加等级。
 * 攻击者是法埃利斯时每次叠 1 级、全额时长，否则不叠级、时长为三分之一。
 *
 * 装有污秽之地（Defiled Lands）且开启联动时改用其「流血」效果：每次在原等级上再叠 1 级（法埃利斯再 +1），最高 IV 级。
 * 移植说明：1.12 该分支写成 Math.max(等级, 3)，导致一出手就是最高级；按其「最高 3」的本意改为 Math.min。
 */
public class AbilityViciousStrike extends Ability implements IAttackAbility {

    private static final ResourceLocation DEFILED_BLEEDING = new ResourceLocation(ModCompat.DEFILED_LANDS, "bleeding");
    private static final int MAX_DEFILED_AMPLIFIER = 3;

    private final ViciousStrikeAbilityConfig config;

    public AbilityViciousStrike(@Nonnull ViciousStrikeAbilityConfig config) {
        super(AbilityNames.VICIOUS_STRIKE);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public float hurtEntity(LivingEntity target, DamageSource source, float dmg) {
        if (!this.isAbilityEnabled() || this.isIndirectDamage(source) || target.level().isClientSide) {
            return dmg;
        }
        final int chance = this.config.chance.get();
        if (chance > 0 && this.random.nextInt(chance) != 0) {
            return dmg;
        }
        final int amplifier = isFaelis(source.getEntity()) ? 1 : 0;
        final int baseDuration = this.config.duration.get();
        final int duration = amplifier > 0 ? baseDuration : baseDuration / 3;
        if (this.config.defiledLandsBleed.get() && ModCompat.defiledLands()) {
            final MobEffect bleeding = ModCompat.effect(DEFILED_BLEEDING);
            if (bleeding != null) {
                applyDefiledBleeding(target, bleeding, duration, amplifier);
                return dmg;
            }
        }
        final MobEffect bleed = ModEffects.BLEED.get();
        final MobEffectInstance active = target.getEffect(bleed);
        if (active != null) {
            target.addEffect(new MobEffectInstance(bleed, active.getDuration() + duration,
                    active.getAmplifier() + amplifier, false, false));
        } else {
            target.addEffect(new MobEffectInstance(bleed, duration, amplifier, false, false));
        }
        return dmg;
    }

    private static void applyDefiledBleeding(LivingEntity target, MobEffect bleeding, int duration, int amplifier) {
        final MobEffectInstance active = target.getEffect(bleeding);
        int level = amplifier;
        if (active != null && active.getAmplifier() + amplifier <= MAX_DEFILED_AMPLIFIER) {
            level = active.getAmplifier() + 1 + amplifier;
        }
        target.addEffect(new MobEffectInstance(bleeding, duration, Math.min(level, MAX_DEFILED_AMPLIFIER), false, false));
    }

    private static boolean isFaelis(Object attacker) {
        if (!(attacker instanceof LivingEntity living)) {
            return false;
        }
        final EntityProperties properties = EntityProperties.get(living);
        return properties != null && properties.getCurrentRaceCache().compareRace(ModRaces.FAELIS.get());
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final int chance = this.config.chance.get();
        final int duration = this.config.duration.get();
        final boolean defiled = this.config.defiledLandsBleed.get() && ModCompat.defiledLands() && ModCompat.effect(DEFILED_BLEEDING) != null;
        variables.option("chance", true, chance < 1 ? NumberText.percent(1) : NumberText.oneIn(chance))
                .seconds("duration", true, duration)
                .seconds("reduced", true, duration / 3)
                .translated("bleed", !defiled, ModEffects.BLEED.get().getDescriptionId())
                .number("bleeddamage", BleedEffect.DAMAGE_PER_LEVEL)
                .flag("defiled", defiled)
                .number("maxlevel", MAX_DEFILED_AMPLIFIER + 1);
    }
}
