package xzeroair.trinkets.util.config.abilities;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/**
 * 熟练弓手的配置段（对应 1.12 ConfigAbilitySkilledArcher）。
 *
 * 移植说明：弓重量表的条目格式由 1.12 的「物品;meta;重量」简化为「物品;重量」（物品可用 * 通配），
 * 仍兼容三段式旧条目（取首尾两段）。1.12 的材质匹配写法（ObjectMaterial:...）依赖 meta 与材质名，
 * 1.20.1 无对应概念，不再支持。
 */
public class SkilledArcherAbilityConfig implements IAbilityConfig {

    /** 弓的默认拉力：原版弓 40；斯巴达武器与冰与火斯巴达（RLCraft）全部长弓 70（1.12 只列木长弓 longbow_wood，1.20.1 改名 wooden_longbow） */
    private static final List<String> DEFAULT_BOWS =
            List.of("minecraft:bow;40.0", "spartanweaponry:aluminum_longbow;70.0", "spartanweaponry:bronze_longbow;70.0",
                    "spartanweaponry:constantan_longbow;70.0", "spartanweaponry:copper_longbow;70.0",
                    "spartanweaponry:diamond_longbow;70.0", "spartanweaponry:electrum_longbow;70.0",
                    "spartanweaponry:golden_longbow;70.0", "spartanweaponry:invar_longbow;70.0",
                    "spartanweaponry:iron_longbow;70.0", "spartanweaponry:lead_longbow;70.0",
                    "spartanweaponry:leather_longbow;70.0", "spartanweaponry:netherite_longbow;70.0",
                    "spartanweaponry:nickel_longbow;70.0", "spartanweaponry:platinum_longbow;70.0",
                    "spartanweaponry:silver_longbow;70.0", "spartanweaponry:steel_longbow;70.0",
                    "spartanweaponry:tin_longbow;70.0", "spartanweaponry:wooden_longbow;70.0",
                    "spartanfire_rlc:desert_myrmex_chitin_longbow;70.0",
                    "spartanfire_rlc:desert_myrmex_stinger_longbow;70.0", "spartanfire_rlc:dragon_bone_longbow;70.0",
                    "spartanfire_rlc:fire_dragonsteel_longbow;70.0", "spartanfire_rlc:flamed_dragon_bone_longbow;70.0",
                    "spartanfire_rlc:ice_dragonsteel_longbow;70.0", "spartanfire_rlc:iced_dragon_bone_longbow;70.0",
                    "spartanfire_rlc:jungle_myrmex_chitin_longbow;70.0",
                    "spartanfire_rlc:jungle_myrmex_stinger_longbow;70.0",
                    "spartanfire_rlc:lightning_dragon_bone_longbow;70.0",
                    "spartanfire_rlc:lightning_dragonsteel_longbow;70.0");


    /** 弓的重量如何换算为伤害倍率 */
    public enum ScalingMode {
        LINEAR, SOFT, KINETIC
    }

    public final BooleanValue enabled;
    public final DoubleValue chargeShotCost;
    public final BooleanValue chargeShotExplodes;
    public final EnumValue<ScalingMode> scalingMode;
    public final IntValue chargeShotTime;
    public final DoubleValue chargeShotDamageMultiplier;
    public final DoubleValue chargeShotMinDamageMultiplier;
    public final DoubleValue chargeShotManaDamageMultiplier;
    public final DoubleValue defaultWeight;
    public final ConfigValue<List<? extends String>> bowBlacklist;
    public final ConfigValue<List<? extends String>> bows;

    public SkilledArcherAbilityConfig(ForgeConfigSpec.Builder builder, String name, double defaultCost) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.chargeShotCost = builder
                .comment("Mana cost scale of a sneaking charged shot")
                .translation("xat.config.abilities.skilled_archer.cost")
                .defineInRange("chargeShotCost", defaultCost, 0.0D, 10000.0D);
        this.chargeShotExplodes = builder
                .comment("A fully drawn, fully mana-charged arrow explodes on impact")
                .translation("xat.config.abilities.skilled_archer.explodes")
                .define("chargeShotExplodes", true);
        this.scalingMode = builder
                .comment("How bow weight scales arrow damage")
                .translation("xat.config.abilities.skilled_archer.scaling")
                .defineEnum("scalingMode", ScalingMode.KINETIC);
        this.chargeShotTime = builder
                .comment("Ticks to fully draw a bow")
                .translation("xat.config.abilities.skilled_archer.time")
                .defineInRange("chargeShotTime", 60, 1, 1200);
        this.chargeShotDamageMultiplier = builder
                .comment("Base damage multiplier of shots")
                .translation("xat.config.abilities.skilled_archer.damage")
                .defineInRange("chargeShotDamageMultiplier", 1.5D, 0.0D, 100.0D);
        this.chargeShotMinDamageMultiplier = builder
                .comment("Lower bound of the damage multiplier")
                .translation("xat.config.abilities.skilled_archer.min_damage")
                .defineInRange("chargeShotMinDamageMultiplier", 1.0D, 0.0D, 100.0D);
        this.chargeShotManaDamageMultiplier = builder
                .comment("Extra multiplier when the full mana cost was paid")
                .translation("xat.config.abilities.skilled_archer.mana_damage")
                .defineInRange("chargeShotManaDamageMultiplier", 1.0D, 0.0D, 100.0D);
        this.defaultWeight = builder
                .comment("Reference draw weight; bows heavier than this deal more damage")
                .translation("xat.config.abilities.skilled_archer.default_weight")
                .defineInRange("defaultWeight", 60.0D, 1.0D, 10000.0D);
        this.bowBlacklist = builder
                .comment("Bows that never benefit (* wildcard)")
                .translation("xat.config.abilities.skilled_archer.blacklist")
                .defineListAllowEmpty("bowBlacklist", List.of("*:*crossbow*"), o -> o instanceof String);
        this.bows = builder
                .comment("Bow draw weights. Format: item;weight (* wildcard). Unlisted bows get no bonus")
                .translation("xat.config.abilities.skilled_archer.bows")
                .defineListAllowEmpty("bows", DEFAULT_BOWS, o -> o instanceof String);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
