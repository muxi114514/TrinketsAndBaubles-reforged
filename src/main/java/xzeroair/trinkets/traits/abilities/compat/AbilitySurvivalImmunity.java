package xzeroair.trinkets.traits.abilities.compat;

import java.util.List;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.IPotionAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.util.compat.ModCompat;
import xzeroair.trinkets.util.compat.survival.SurvivalCompat;
import xzeroair.trinkets.util.compat.survival.SurvivalCompat.Hazard;

/**
 * 生存类免疫（炎热 / 寒冷 / 口渴 / 寄生虫）。两层免疫：施加对应效果时拒绝（阻断层），每 tick 清除已有效果并修正体温（清理层）。
 *
 * 移植说明：1.12 是 AbilityHeatImmunity / AbilityColdImmunity / AbilityThirstImmunity / AbilityParasitesImmunity 四个
 * 只差危害类型的类，合并为本类；注册名沿用 1.12，旧存档与语言键不受影响。
 */
public class AbilitySurvivalImmunity extends Ability implements ITickableAbility, IPotionAbility {

    private final Hazard hazard;

    public AbilitySurvivalImmunity(Hazard hazard) {
        super(nameOf(hazard));
        this.hazard = hazard;
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        SurvivalCompat.protect(entity, this.hazard);
    }

    @Override
    public boolean potionApplied(LivingEntity entity, MobEffectInstance effect, boolean cancel) {
        return cancel || this.hazard.isHazardEffect(effect);
    }

    private static String nameOf(Hazard hazard) {
        return switch (hazard) {
            case HEAT -> AbilityNames.IMMUNITY_HEAT;
            case COLD -> AbilityNames.IMMUNITY_COLD;
            case THIRST -> AbilityNames.IMMUNITY_THIRST;
            case PARASITES -> AbilityNames.IMMUNITY_PARASITES;
        };
    }

    @Override
    public void describe(DescriptionVariables variables) {
        switch (this.hazard) {
            case HEAT, COLD -> variables.number("limit", SurvivalCompat.TEMPERATURE_LIMIT);
            case THIRST -> variables.effects("effect", true, List.of(ModCompat.SIMPLE_DIFFICULTY + ":thirsty"), false);
            case PARASITES -> variables.effects("effect", true, List.of(ModCompat.SIMPLE_DIFFICULTY + ":parasites"), false);
        }
    }

    @Override
    public String getCompatModId() {
        return this.hazard == Hazard.HEAT || this.hazard == Hazard.COLD ? ModCompat.COLD_SWEAT : ModCompat.SIMPLE_DIFFICULTY;
    }
}
