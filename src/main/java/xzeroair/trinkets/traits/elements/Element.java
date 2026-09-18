package xzeroair.trinkets.traits.elements;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.util.Reference;

/**
 * 元素。
 *
 * 移植说明：1.12 中本类继承 IForgeRegistryEntry.Impl 并自带注册名；该接口在 1.19+ 已被 Forge 移除，
 * 1.20.1 的注册表条目是普通对象，注册名由注册表持有（用 ModElements.registry().getKey(element) 反查）。
 * 强弱关系在 1.12 直接引用静态 Elements.X；此处改为运行时从 RegistryObject 取值，避免类初始化顺序问题。
 */
public class Element {

    protected final String name;

    public Element(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public Element[] getStrengths() {
        return new Element[0];
    }

    public Element[] getWeaknesses() {
        return new Element[0];
    }

    public String getTranslationKey() {
        return Reference.MODID + ".element." + this.name.toLowerCase();
    }

    public Component getDisplayName() {
        return Component.translatable(this.getTranslationKey() + ".name");
    }

    public int getPrimaryColor() {
        return 16777215;
    }

    public int getSecondaryColor() {
        return 16777215;
    }

    public ResourceLocation getRegistryName() {
        return ModElements.registry().getKey(this);
    }

    public boolean isNone() {
        return this == ModElements.NEUTRAL.get();
    }
}
