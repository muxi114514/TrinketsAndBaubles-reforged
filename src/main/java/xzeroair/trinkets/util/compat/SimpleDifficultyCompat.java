package xzeroair.trinkets.util.compat;

import net.minecraftforge.common.MinecraftForge;

import xzeroair.trinkets.Trinkets;

/**
 * Simple Difficulty Reforge 兼容修补。
 *
 * 问题：它的 baubles/CuriosEvents#onTrinketsEquipment 监听「玩家任意饰品栏变化」，方法体直接 instanceof
 * 「Trinkets and Baubles Reforked」（modid trinketsandbaubles）的龙之眼类，却只在装有 Curios 时就注册、不检查 Reforked 是否存在。
 * 整合包换成本移植版（不装 Reforked）后，玩家戴上或摘下任何饰品都会 NoClassDefFoundError 崩服。
 * 该监听只服务于 Reforked 的龙之眼，Reforked 不在时整个监听都无意义，故在其注册之后把它从 Forge 事件总线上移除。
 * 它的其余 Reforked 相关代码都以 ModList.isLoaded("trinketsandbaubles") 为前提，不受影响。
 */
public final class SimpleDifficultyCompat {

    private static final String CURIOS_EVENTS = "com.kettle.simpledifficultyreforged.baubles.CuriosEvents";
    private static final String TRINKETS_REFORKED = "trinketsandbaubles";

    /** 须在所有模组构造完成之后调用（其监听在模组构造期注册） */
    public static void removeBrokenCuriosListener() {
        if (!ModCompat.isLoaded(ModCompat.SIMPLE_DIFFICULTY) || ModCompat.isLoaded(TRINKETS_REFORKED)) {
            return;
        }
        try {
            // 只加载类不初始化；方法体里对 Reforked 类的引用要到执行时才解析，加载本身是安全的
            final Class<?> listener = Class.forName(CURIOS_EVENTS, false, SimpleDifficultyCompat.class.getClassLoader());
            MinecraftForge.EVENT_BUS.unregister(listener);
            Trinkets.LOGGER.info("Removed Simple Difficulty's Trinkets and Baubles Reforked curio listener (Reforked is not installed)");
        } catch (ClassNotFoundException | LinkageError e) {
            Trinkets.LOGGER.debug("Simple Difficulty curio listener not found, nothing to remove", e);
        }
    }

    private SimpleDifficultyCompat() {
    }
}
