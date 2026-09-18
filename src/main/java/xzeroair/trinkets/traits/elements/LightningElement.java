package xzeroair.trinkets.traits.elements;

import xzeroair.trinkets.init.ModElements;


public class LightningElement extends Element {

    public LightningElement() {
        super("Lightning");
    }

    @Override
    public Element[] getStrengths() {
        return new Element[]{ModElements.WATER.get()};
    }

    @Override
    public int getPrimaryColor() {
        return 4915275;
    }

    @Override
    public int getSecondaryColor() {
        return 14381019;
    }
}
