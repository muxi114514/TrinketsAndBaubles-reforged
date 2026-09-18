package xzeroair.trinkets.util.config.server.race;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

import xzeroair.trinkets.util.config.abilities.ClimbingAbilityConfig;
import xzeroair.trinkets.util.config.abilities.NightVisionAbilityConfig;

/**
 * 法埃利斯的本族配置（对应 1.12 FaelisConfig.ABILITIES 与其专有字段）：能力、徒手格斗、喝奶增益、重甲惩罚。
 * 1.12 的夜视默认关闭（ConfigAbilityNightVision(false)）。
 */
public class FaelisAbilitiesConfig {

    public final ClimbingAbilityConfig climbing;
    public final NightVisionAbilityConfig nightVision;

    public final BooleanValue barehandCombat;
    public final DoubleValue barehandCombatBonus;
    public final ConfigValue<List<? extends String>> bareHands;
    public final BooleanValue milkBonus;
    public final IntValue milkBonusDuration;
    public final ConfigValue<List<? extends String>> milk;
    public final ConfigValue<List<? extends String>> milkBuffs;
    public final BooleanValue heavyArmorPenalty;
    public final BooleanValue milkInvigorated;
    public final ConfigValue<List<? extends String>> heavyArmor;

    public FaelisAbilitiesConfig(ForgeConfigSpec.Builder builder) {
        this.climbing = new ClimbingAbilityConfig(builder, "climbing");
        this.nightVision = new NightVisionAbilityConfig(builder, "night_vision", false);
        this.barehandCombat = builder
                .comment("Unarmed melee attacks deal bonus damage")
                .translation("xat.config.races.faelis.barehand")
                .define("barehandCombat", true);
        this.barehandCombatBonus = builder
                .comment("Bonus damage when both hands count as bare")
                .translation("xat.config.races.faelis.barehand_bonus")
                .defineInRange("barehandCombatBonus", 3.25D, 0.0D, 1000.0D);
        this.bareHands = builder
                .comment("Held items that still count as bare hands, adding their value as damage. Format: item[;hand];value")
                .translation("xat.config.races.faelis.bare_hands")
                .defineListAllowEmpty("bareHands", List.of(), FaelisAbilitiesConfig::isString);
        this.milkBonus = builder
                .comment("Drinking milk grants Invigorated and the milk buffs")
                .translation("xat.config.races.faelis.milk")
                .define("milkBonus", true);
        this.milkBonusDuration = builder
                .comment("Default Invigorated duration in ticks")
                .translation("xat.config.races.faelis.milk_duration")
                .defineInRange("milkBonusDuration", 600, 1, 72000);
        this.milk = builder
                .comment("Items that count as milk. Format: item[;level[;duration]]")
                .translation("xat.config.races.faelis.milk_items")
                .defineListAllowEmpty("milk", List.of("minecraft:milk_bucket"), FaelisAbilitiesConfig::isString);
        this.milkBuffs = builder
                .comment("Extra effects granted by milk. Format: modid:effect:duration:amplifier")
                .translation("xat.config.races.faelis.milk_buffs")
                .defineListAllowEmpty("milkBuffs", List.of("minecraft:speed:3600:0", "minecraft:strength:3600:0",
                        "minecraft:jump_boost:3600:0"), FaelisAbilitiesConfig::isString);
        this.heavyArmorPenalty = builder
                .comment("Heavy armor slows movement and lowers jump height")
                .translation("xat.config.races.faelis.heavy")
                .define("heavyArmorPenalty", true);
        this.milkInvigorated = builder
                .comment("Invigorated cancels the heavy armor penalty")
                .translation("xat.config.races.faelis.heavy_invigorated")
                .define("milkInvigorated", true);
        this.heavyArmor = builder
                .comment("Armor weights. Format: item or material[;slot];weight")
                .translation("xat.config.races.faelis.heavy_list")
                .defineListAllowEmpty("heavyArmor", List.of(
                        "minecraft:chainmail_helmet;0.01",
                        "minecraft:chainmail_chestplate;0.09",
                        "minecraft:chainmail_leggings;0.075",
                        "minecraft:chainmail_boots;0.025",
                        "iron;head;0.025",
                        "iron;chest;0.15",
                        "iron;legs;0.075",
                        "iron;feet;0.05",
                        "minecraft:golden_helmet;0.04",
                        "minecraft:golden_chestplate;0.2",
                        "minecraft:golden_leggings;0.1",
                        "minecraft:golden_boots;0.06",
                        "diamond;0.075"), FaelisAbilitiesConfig::isString);
    }

    private static boolean isString(Object value) {
        return value instanceof String;
    }
}
