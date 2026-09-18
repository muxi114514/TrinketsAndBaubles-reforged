package xzeroair.trinkets.util.config.server;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/**
 * 魔力系统配置（对应 1.12 util/config/mana/EntityManaConfig，即 ServerConfig.MAGIC）。
 *
 * 移植说明：回复名单 1.12 格式为「物品;meta;数值」，1.20.1 去掉 metadata 改为「物品;数值」，默认值逐条迁移：
 * 所有 {@code ;*;} 通配直接去掉；{@code golden_apple;0} → golden_apple、{@code golden_apple;1} → enchanted_golden_apple；
 * botania 的 manacookie 在 1.20.1 改名 mana_cookie；simpledifficulty:juice 按 meta 区分的两条在 1.20.1 无对应物，删除。
 */
public class ManaConfig {

    public static final List<String> DEFAULT_RECOVERY = List.of(
            "xat:dwarf_stout;100%", "xat:elf_sap;100%", "xat:faelis_food;100%", "xat:fairy_dew;100%",
            "xat:goblin_soup;100%", "xat:titan_spirit;100%", "xat:taurus_tea;100%", "xat:dragon_gem;100%",
            "xat:mana_crystal;100%", "xat:mana_reagent;100%", "xat:mana_candy;50",
            "minecraft:golden_apple;20", "minecraft:enchanted_golden_apple;50",
            "botania:mana_cookie;10%");

    public final BooleanValue manaEnabled;
    public final IntValue manaUpdateTicks;
    public final IntValue manaRegenTimeout;
    public final DoubleValue bonusPerPoint;
    public final IntValue bonusMax;
    public final ConfigValue<List<? extends String>> recovery;
    public final BooleanValue crystalExplodes;
    public final BooleanValue reagentHarmful;

    public ManaConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Mana settings")
                .translation("xat.config.magic")
                .push("magic");

        this.manaEnabled = builder
                .comment("Enable the mana system; when disabled, abilities never run out of mana")
                .translation("xat.config.magic.enabled")
                .define("manaEnabled", true);
        this.manaUpdateTicks = builder
                .comment("Ticks between each mana regeneration step")
                .translation("xat.config.magic.update_ticks")
                .defineInRange("manaUpdateTicks", 20, 1, 72000);
        this.manaRegenTimeout = builder
                .comment("Ticks mana regeneration pauses after spending mana")
                .translation("xat.config.magic.regen_timeout")
                .defineInRange("manaRegenTimeout", 60, 0, 72000);
        this.bonusPerPoint = builder
                .comment("Max mana gained per bonus point (Mana Crystal)")
                .translation("xat.config.magic.bonus")
                .defineInRange("bonusPerPoint", 10.0D, 0.0D, 10000.0D);
        this.bonusMax = builder
                .comment("Maximum bonus points")
                .translation("xat.config.magic.bonus_max")
                .defineInRange("bonusMax", 90, 0, 10000);
        this.recovery = builder
                .comment("Items that restore mana when consumed. Format: item_id;amount or #tag;amount",
                        "Append % to restore a percentage of max mana instead of a flat amount")
                .translation("xat.config.magic.recovery")
                .defineListAllowEmpty("recovery", DEFAULT_RECOVERY, o -> o instanceof String);

        builder.push("items");
        this.crystalExplodes = builder
                .comment("Using a Mana Crystal on stone may shatter it into a Mana Reagent")
                .translation("xat.config.magic.crystal_explodes")
                .define("crystalExplodes", false);
        this.reagentHarmful = builder
                .comment("Mana Reagent and Restoration Serum inflict poison and weakness")
                .translation("xat.config.magic.reagent_harmful")
                .define("reagentHarmful", true);
        builder.pop();

        builder.pop();
    }
}
