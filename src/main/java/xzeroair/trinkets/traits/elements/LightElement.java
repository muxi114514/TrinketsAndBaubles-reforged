package xzeroair.trinkets.traits.elements;

import xzeroair.trinkets.init.ModElements;


public class LightElement extends Element {

    public LightElement() {
        super("Light");
    }

    @Override
    public Element[] getStrengths() {
        return new Element[]{ModElements.DARK.get()};
    }

    @Override
    public Element[] getWeaknesses() {
        return new Element[]{ModElements.DARK.get()};
    }

    @Override
    public int getPrimaryColor() {
        return 16777215;
    }

    @Override
    public int getSecondaryColor() {
        return 14474240;
    }
}
