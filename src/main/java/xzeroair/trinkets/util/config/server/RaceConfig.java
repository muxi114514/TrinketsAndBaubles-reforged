package xzeroair.trinkets.util.config.server;

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

import xzeroair.trinkets.util.helpers.damage.DamageTypeRules;

/**
 * 单个种族的配置（对应 1.12 各族的 XxxConfig，9 份内容高度一致的类）。
 *
 * 移植说明：1.12 为每个种族写了一个 90~190 行的配置类，通用字段完全相同、只是默认值不同，
 * 差异只在各自的 ABILITIES 段。此处收敛为这一个类：通用字段直接定义，
 * 能力段由泛型 A 表示、构造时经工厂在本族分组内建立，从而保留「各族能力分别调参」。
 *
 * @param <A> 本族能力配置段的类型；没有独立能力配置的种族为 Void
 */
public class RaceConfig<A> {

    public final BooleanValue canMount;
    public final BooleanValue canControlBoats;
    public final BooleanValue mountWhitelist;
    public final ConfigValue<List<? extends String>> mountBlacklist;
    public final ConfigValue<List<? extends String>> effectsToAdd;
    public final ConfigValue<List<? extends String>> effectsToRemove;
    public final ConfigValue<List<? extends String>> damageTypesToIgnore;
    public final ConfigValue<List<? extends String>> attributes;
    public final IntValue height;
    public final IntValue width;
    public final IntValue magicAffinity;
    /** 生存类联动（冷汗 / Simple Difficulty） */
    public final SurvivalConfig survival;

    /** 本族能力配置段；无独立能力配置的种族为 null */
    @Nullable
    public final A abilities;

    public RaceConfig(ForgeConfigSpec.Builder builder, String raceName, int defaultHeight, int defaultWidth,
            int defaultAffinity, @Nullable Function<ForgeConfigSpec.Builder, A> abilitiesFactory) {
        this(builder, raceName, defaultHeight, defaultWidth, defaultAffinity, List.of(), abilitiesFactory);
    }

    public RaceConfig(ForgeConfigSpec.Builder builder, String raceName, int defaultHeight, int defaultWidth,
            int defaultAffinity, List<String> defaultAttributes, @Nullable Function<ForgeConfigSpec.Builder, A> abilitiesFactory) {
        builder.comment(raceName + " settings")
                .translation("xat.config.races." + raceName)
                .push(raceName);

        canMount = builder
                .comment("Can this race ride entities at all?")
                .translation("xat.config.races.mount")
                .define("canMount", true);

        canControlBoats = builder
                .comment("Can this race steer boats?")
                .translation("xat.config.races.mount.boat")
                .define("canControlBoats", true);

        mountWhitelist = builder
                .comment("Treat the mount list as a whitelist instead of a blacklist")
                .translation("xat.config.races.mount.inverted")
                .define("mountWhitelist", false);

        mountBlacklist = builder
                .comment("Entity ids this race may not ride; \"modid:*\" matches a whole mod")
                .translation("xat.config.races.mount.blacklist")
                .defineListAllowEmpty("mountBlacklist", List.of(), RaceConfig::isString);

        effectsToAdd = builder
                .comment("Effects granted while transformed. Format: [modid:]effect[:duration[:amplifier]]")
                .translation("xat.config.races.effects")
                .defineListAllowEmpty("effectsToAdd", List.of(), RaceConfig::isString);

        effectsToRemove = builder
                .comment("Effects this race is immune to. Same format as effectsToAdd")
                .translation("xat.config.races.resistances")
                .defineListAllowEmpty("effectsToRemove", List.of(), RaceConfig::isString);

        damageTypesToIgnore = builder
                .comment(DamageTypeRules.SYNTAX)
                .translation("xat.config.races.damagetypes")
                .defineListAllowEmpty("damageTypesToIgnore", List.of(), RaceConfig::isString);

        attributes = builder
                .comment("Attribute modifiers applied while transformed. Format: Name:attribute, Amount:value, Operation:0|1|2")
                .translation("xat.config.races.attributes")
                .defineListAllowEmpty("attributes", defaultAttributes, RaceConfig::isString);

        builder.push("size");
        height = builder
                .comment("Height as a percentage of the default player height")
                .translation("xat.config.races.size.height")
                .defineInRange("height", defaultHeight, 1, 1000);
        width = builder
                .comment("Width as a percentage of the default player width")
                .translation("xat.config.races.size.width")
                .defineInRange("width", defaultWidth, 1, 1000);
        builder.pop();

        magicAffinity = builder
                .comment("Magic affinity; scales this race's mana pool")
                .translation("xat.config.races.magic.affinity")
                .defineInRange("magicAffinity", defaultAffinity, 0, 10000);

        survival = SurvivalConfig.none(builder);

        if (abilitiesFactory != null) {
            builder.translation("xat.config.races.abilities").push("abilities");
            abilities = abilitiesFactory.apply(builder);
            builder.pop();
        } else {
            abilities = null;
        }

        builder.pop();
    }

    private static boolean isString(Object value) {
        return value instanceof String;
    }
}
