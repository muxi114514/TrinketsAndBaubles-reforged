package xzeroair.trinkets.util.config.server.items;

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import xzeroair.trinkets.util.config.server.SurvivalConfig;
import xzeroair.trinkets.util.helpers.damage.DamageTypeRules;

/**
 * 单件饰品的配置（对应 1.12 ConfigGlowRing 等 14 份结构相同的类）：开关、佩戴时的效果增删、伤害类型规则、属性修饰，
 * 以及由泛型 A 表示的本饰品能力配置段。
 *
 * 移植说明：
 * - 1.12 的 ENABLED 带 RequiresMcRestart，关闭后不注册物品；1.20.1 的 SERVER 配置加载晚于注册，
 *   改为「物品仍注册，但不生效且不能佩戴」。
 * - 1.12 的 BAUBLES 槽位类型配置由 Curios 物品标签（data/curios/tags/items/）取代，可用数据包调整。
 * - 1.12 的 COMPAT.SURVIVAL 联动段对应 survival。
 *
 * @param <A> 能力配置段类型；无能力的饰品为 Void
 */
public class AccessoryConfig<A> {

    public final BooleanValue enabled;
    public final ConfigValue<List<? extends String>> effectsToAdd;
    public final ConfigValue<List<? extends String>> effectsToRemove;
    public final ConfigValue<List<? extends String>> damageTypesToIgnore;
    public final ConfigValue<List<? extends String>> attributes;
    /** 生存类联动（冷汗 / Simple Difficulty） */
    public final SurvivalConfig survival;

    @Nullable
    public final A abilities;

    public AccessoryConfig(ForgeConfigSpec.Builder builder, String name, Defaults defaults,
            @Nullable Function<ForgeConfigSpec.Builder, A> abilitiesFactory) {
        builder.translation("xat.config.items." + name).push(name);
        this.enabled = builder
                .comment("Enable this item. Disabled items stay registered but do nothing and cannot be worn")
                .translation("xat.config.items.enabled")
                .define("enabled", true);
        this.effectsToAdd = builder
                .comment("Effects granted while worn. Format: [modid:]effect[:duration[:amplifier]]")
                .translation("xat.config.items.effects")
                .defineListAllowEmpty("effectsToAdd", defaults.effectsToAdd(), AccessoryConfig::isString);
        this.effectsToRemove = builder
                .comment("Effects the wearer is immune to. Same format as effectsToAdd")
                .translation("xat.config.items.resistances")
                .defineListAllowEmpty("effectsToRemove", defaults.effectsToRemove(), AccessoryConfig::isString);
        this.damageTypesToIgnore = builder
                .comment(DamageTypeRules.SYNTAX)
                .translation("xat.config.items.damagetypes")
                .defineListAllowEmpty("damageTypesToIgnore", defaults.damageTypes(), AccessoryConfig::isString);
        this.attributes = builder
                .comment("Attribute modifiers applied while worn. Format: Name:attribute, Amount:value, Operation:0|1|2")
                .translation("xat.config.items.attributes")
                .defineListAllowEmpty("attributes", defaults.attributes(), AccessoryConfig::isString);
        this.survival = new SurvivalConfig(builder, defaults.heat(), defaults.cold(), defaults.thirst(), defaults.parasites());
        this.abilities = abilitiesFactory == null ? null : abilitiesFactory.apply(builder);
        builder.pop();
    }

    static boolean isString(Object value) {
        return value instanceof String;
    }

    /** 四个列表与生存联动开关的默认值（取自 1.12 各饰品配置类） */
    public record Defaults(List<String> effectsToAdd, List<String> effectsToRemove, List<String> damageTypes,
            List<String> attributes, boolean heat, boolean cold, boolean thirst, boolean parasites) {

        public static final Defaults NONE = new Defaults(List.of(), List.of(), List.of(), List.of());

        public Defaults(List<String> effectsToAdd, List<String> effectsToRemove, List<String> damageTypes, List<String> attributes) {
            this(effectsToAdd, effectsToRemove, damageTypes, attributes, false, false, false, false);
        }

        public Defaults withSurvival(boolean heatImmune, boolean coldImmune, boolean thirstImmune, boolean parasitesImmune) {
            return new Defaults(this.effectsToAdd, this.effectsToRemove, this.damageTypes, this.attributes,
                    heatImmune, coldImmune, thirstImmune, parasitesImmune);
        }

        public static Defaults attributes(String... attributes) {
            return new Defaults(List.of(), List.of(), List.of(), List.of(attributes));
        }

        public static Defaults immunities(List<String> effectsToRemove, List<String> damageTypes) {
            return new Defaults(List.of(), effectsToRemove, damageTypes, List.of());
        }
    }
}
