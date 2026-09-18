package xzeroair.trinkets.util;

/**
 * 模组按键的名称（同时是翻译键），两端通用。
 *
 * 能力的 getKey() 返回这里的常量；客户端的 ModKeyMappings 以同一名称注册 KeyMapping 并按名称查表。
 * 单独成类是为了让能力（两端都会加载）不必引用仅客户端存在的 KeyMapping 类型。
 */
public final class KeyNames {

    public static final String CATEGORY = key("category");

    public static final String DRAGONS_EYE_TARGET = desc("trinket_target");
    public static final String DRAGONS_EYE_ABILITY = desc("trinket_toggle_effect");
    public static final String POLARIZED_STONE_ABILITY = desc("magnet_toggle_effect");
    public static final String AUX_KEY = desc("aux_key");
    public static final String ARCING_ORB_ABILITY = desc("trinket_arcing_attack");
    public static final String ARCING_ORB_DODGE = desc("trinket_arcing_dodge");
    public static final String ENDER_CROWN = desc("trinket_ender_crown");
    public static final String RACE_ABILITY = desc("race_ability");

    private static String key(String id) {
        return "key." + Reference.MODID + "." + id;
    }

    private static String desc(String id) {
        return key(id) + ".desc";
    }

    private KeyNames() {
    }
}
