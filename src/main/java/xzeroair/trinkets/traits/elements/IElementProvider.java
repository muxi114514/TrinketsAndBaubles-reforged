package xzeroair.trinkets.traits.elements;

import net.minecraft.world.item.ItemStack;

import xzeroair.trinkets.init.ModElements;

/** 能提供元素归属的对象（物品、实体、种族）。 */
public interface IElementProvider {

    default Element getPrimaryElement() {
        return ModElements.NEUTRAL.get();
    }

    default Element getSecondaryElement() {
        return ModElements.NEUTRAL.get();
    }

    default Element[] getSubElements() {
        return ModElements.EMPTY;
    }

    default Element getPrimaryElement(ItemStack stack) {
        return this.getPrimaryElement();
    }

    default Element getSecondaryElement(ItemStack stack) {
        return this.getSecondaryElement();
    }

    default Element[] getSubElements(ItemStack stack) {
        return this.getSubElements();
    }
}
