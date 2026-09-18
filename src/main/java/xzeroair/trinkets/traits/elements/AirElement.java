package xzeroair.trinkets.traits.elements;

import xzeroair.trinkets.init.ModElements;


public class AirElement extends Element {

    public AirElement() {
        super("Air");
    }

    @Override
    public Element[] getStrengths() {
        return new Element[]{ModElements.POISON.get()};
    }

    @Override
    public Element[] getWeaknesses() {
        return new Element[]{ModElements.EARTH.get()};
    }

    @Override
    public int getPrimaryColor() {
        return 10610356;
    }

    @Override
    public int getSecondaryColor() {
        return 13816530;
    }
}
