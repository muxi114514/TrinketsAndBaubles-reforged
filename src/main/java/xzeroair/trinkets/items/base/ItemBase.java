package xzeroair.trinkets.items.base;

import net.minecraft.world.item.Item;

/**
 * 普通物品基类。
 * 移植说明：1.12 原版在构造里 setRegistryName/setUnlocalizedName/setCreativeTab，
 * 1.20.1 这些分别由 DeferredRegister 的注册名、自动推导的翻译键、创造栏回调接管，故构造仅收 Properties。
 * 原版另实现 IElementProvider（元素子类型），随 P2 元素系统补入。
 */
public class ItemBase extends Item {

    public ItemBase(Properties properties) {
        super(properties);
    }

    public ItemBase() {
        this(new Properties());
    }
}
