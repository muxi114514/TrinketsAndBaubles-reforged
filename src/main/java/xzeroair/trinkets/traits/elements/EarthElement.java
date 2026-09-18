package xzeroair.trinkets.traits.elements;

import xzeroair.trinkets.init.ModElements;


public class EarthElement extends Element {

    public EarthElement() {
        super("Earth");
    }

    @Override
    public Element[] getStrengths() {
        return new Element[]{ModElements.LIGHTNING.get()};
    }

    @Override
    public int getPrimaryColor() {
        return 6765824;
    }

    @Override
    public int getSecondaryColor() {
        return 5277000;
    }
}
