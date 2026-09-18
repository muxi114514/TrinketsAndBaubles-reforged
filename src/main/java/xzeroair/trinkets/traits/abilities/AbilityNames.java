package xzeroair.trinkets.traits.abilities;

import xzeroair.trinkets.util.Reference;

/**
 * 能力注册名（对应 1.12 TrinketsRegistryNames.ModAbilities）。
 * 取值必须与 1.12 一致：lang 键 xat.ability.&lt;name&gt;.* 与已存档的能力 NBT 都以它为键。
 */
public final class AbilityNames {

    public static final String IMMUNITY_FIRE = "immunity_fire";
    public static final String IMMUNITY_ICE = "immunity_ice";
    public static final String IMMUNITY_LIGHTNING = "immunity_lightning";
    public static final String FROST_WALKER = "frost_walker";
    public static final String HEAVY = "heavy";
    public static final String LARGE_HANDS = "large_hands";
    public static final String CLIMBING = "climbing";
    public static final String SKILLED_MINER = "skilled_miner";
    public static final String SKILLED_SWIMMER = "skilled_swimmer";
    public static final String NIGHT_VISION = "night_vision";
    public static final String CREATIVE_FLIGHT = "creative_flight";
    public static final String ELYTRA_FLIGHT = "elytra_flight";
    public static final String IMMUNITY_DARK = "immunity_dark";
    public static final String IMMUNITY_POISON = "immunity_poison";
    public static final String IMMUNITY_WATER = "immunity_water";
    public static final String AFFINITY_DARK = "affinity_dark";
    public static final String AFFINITY_POISON = "affinity_poison";
    public static final String AFFINITY_WATER = "affinity_water";
    public static final String WEIGHTLESS = "weightless";
    public static final String WELL_RESTED = "well_rested";
    public static final String NULLIFY_KINETIC = "nullify_kinetic";
    public static final String REDUCE_KINETIC = "reduce_kinetic";
    public static final String SAFE_GUARD = "safe_guard";
    public static final String VICIOUS_STRIKE = "vicious_strike";
    public static final String SKILLED_ARCHER = "skilled_archer";
    public static final String STAMPEDE = "stampede";
    public static final String BLESSING_OF_LIFE = "blessing_of_life";
    public static final String LEVELING = "leveling";
    public static final String DODGING = "dodging";
    public static final String LIGHTNING_BOLT = "lightning_bolt";
    public static final String GREEDY_EYES = "greedy_eyes";
    public static final String RESTORATION_FIELD = "mending_bloom";
    public static final String WOLF_RIDER = "wolf_rider";
    public static final String MAGNETIC = "magnetic";
    public static final String REPEL = "repel";
    public static final String ENDER_QUEEN = "ender_queen";
    // 模组联动能力
    public static final String IMMUNITY_HEAT = "immunity_heat";
    public static final String IMMUNITY_COLD = "immunity_cold";
    public static final String IMMUNITY_THIRST = "immunity_thirst";
    public static final String IMMUNITY_PARASITES = "immunity_parasites";
    public static final String ABSORPTION_THIRST = "absorption_thirst";
    public static final String HARD_HEAD = "hard_head";
    public static final String CLEAR_VISION = "clear_vision";
    public static final String CLEAR_SPLASH = "clear_splash";
    public static final String ENDER_EYES = "ender_eyes";

    /** 带命名空间的完整键，AbilityHandler 以此为 active 表的键 */
    public static String key(String name) {
        return Reference.MODID + ":" + name;
    }

    private AbilityNames() {
    }
}
