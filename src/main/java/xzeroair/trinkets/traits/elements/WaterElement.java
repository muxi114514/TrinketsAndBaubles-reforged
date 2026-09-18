package xzeroair.trinkets.traits.elements;

import xzeroair.trinkets.init.ModElements;


public class WaterElement extends Element {

    public WaterElement() {
        super("Water");
    }

    @Override
    public Element[] getStrengths() {
        return new Element[]{ModElements.FIRE.get()};
    }

    @Override
    public Element[] getWeaknesses() {
        return new Element[]{ModElements.LIGHTNING.get()};
    }

    @Override
    public int getPrimaryColor() {
        return 100;
    }

    @Override
    public int getSecondaryColor() {
        return 20680;
    }
}
