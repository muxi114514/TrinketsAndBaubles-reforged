package xzeroair.trinkets.traits.abilities.compat;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;

import xzeroair.trinkets.traits.abilities.interfaces.IAbilityInterface;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.util.compat.survival.SurvivalCompat.Hazard;
import xzeroair.trinkets.util.config.server.SurvivalConfig;

/**
 * 按生存联动段给种族或饰品添加免疫能力（对应 1.12 addSurvivalAbilities）；负责该危害的模组未启用时不添加。
 */
public final class SurvivalAbilities {

    /**
     * @param required 只在该元素形态下生效（巨龙 / 龙之眼的元素分支）；null 表示不限元素
     */
    public static void addTo(Consumer<IAbilityInterface> adder, @Nullable SurvivalConfig config, @Nullable Element required) {
        if (config == null) {
            return;
        }
        add(adder, config.immuneToHeat, Hazard.HEAT, required);
        add(adder, config.immuneToCold, Hazard.COLD, required);
        add(adder, config.immuneToThirst, Hazard.THIRST, required);
        add(adder, config.immuneToParasites, Hazard.PARASITES, required);
    }

    private static void add(Consumer<IAbilityInterface> adder, BooleanValue enabled, Hazard hazard, @Nullable Element required) {
        if (!enabled.get() || !hazard.isActive()) {
            return;
        }
        final AbilitySurvivalImmunity ability = new AbilitySurvivalImmunity(hazard);
        if (required != null) {
            ability.setRequiredElement(required);
        }
        adder.accept(ability);
    }

    private SurvivalAbilities() {
    }
}
