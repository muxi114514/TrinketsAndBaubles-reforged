package xzeroair.trinkets.util.config.server.race;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import xzeroair.trinkets.util.helpers.damage.DamageTypeRules;

/**
 * 元素形态对种族通用配置的替换项（对应 1.12 ConfigFire/Ice/LightningDragon 中的
 * EFFECTS_TO_ADD / EFFECTS_TO_REMOVE / DAMAGE_TYPES_TO_IGNORE / ATTRIBUTES）。
 * 元素形态生效时整体替换本族同名配置，而非叠加（与 1.12 一致）。
 */
public class ElementOverrideConfig {

    public final ConfigValue<List<? extends String>> effectsToAdd;
    public final ConfigValue<List<? extends String>> effectsToRemove;
    public final ConfigValue<List<? extends String>> damageTypesToIgnore;
    public final ConfigValue<List<? extends String>> attributes;

    public ElementOverrideConfig(ForgeConfigSpec.Builder builder, List<String> defaultEffectsToRemove,
            List<String> defaultDamageTypes, List<String> defaultAttributes) {
        this.effectsToAdd = builder
                .comment("Effects granted while in this elemental form")
                .translation("xat.config.races.effects")
                .defineListAllowEmpty("effectsToAdd", List.of(), ElementOverrideConfig::isString);
        this.effectsToRemove = builder
                .comment("Effects this elemental form is immune to")
                .translation("xat.config.races.resistances")
                .defineListAllowEmpty("effectsToRemove", defaultEffectsToRemove, ElementOverrideConfig::isString);
        this.damageTypesToIgnore = builder
                .comment(DamageTypeRules.SYNTAX)
                .translation("xat.config.races.damagetypes")
                .defineListAllowEmpty("damageTypesToIgnore", defaultDamageTypes, ElementOverrideConfig::isString);
        this.attributes = builder
                .comment("Attribute modifiers for this elemental form")
                .translation("xat.config.races.attributes")
                .defineListAllowEmpty("attributes", defaultAttributes, ElementOverrideConfig::isString);
    }

    private static boolean isString(Object value) {
        return value instanceof String;
    }
}
