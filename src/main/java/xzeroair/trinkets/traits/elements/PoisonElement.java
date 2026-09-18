package xzeroair.trinkets.traits.elements;

import xzeroair.trinkets.init.ModElements;


public class PoisonElement extends Element {

    public PoisonElement() {
        super("Poison");
    }

    @Override
    public Element[] getStrengths() {
        return new Element[]{ModElements.WATER.get()};
    }

    @Override
    public Element[] getWeaknesses() {
        return new Element[]{ModElements.FIRE.get()};
    }

    @Override
    public int getPrimaryColor() {
        return 3307570;
    }

    @Override
    public int getSecondaryColor() {
        return 38912;
    }
}
