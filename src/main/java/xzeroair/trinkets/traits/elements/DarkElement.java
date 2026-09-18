package xzeroair.trinkets.traits.elements;

import xzeroair.trinkets.init.ModElements;


public class DarkElement extends Element {

    public DarkElement() {
        super("Dark");
    }

    @Override
    public Element[] getStrengths() {
        return new Element[]{ModElements.LIGHT.get()};
    }

    @Override
    public Element[] getWeaknesses() {
        return new Element[]{ModElements.LIGHT.get()};
    }

    @Override
    public int getPrimaryColor() {
        return 3289650;
    }

    @Override
    public int getSecondaryColor() {
        return 1644825;
    }
}
