package xzeroair.trinkets.traits.elements;

import xzeroair.trinkets.init.ModElements;


public class VoidElement extends Element {

    public VoidElement() {
        super("Void");
    }

    @Override
    public Element[] getStrengths() {
        return new Element[]{ModElements.AIR.get()};
    }

    @Override
    public int getPrimaryColor() {
        return 3289650;
    }

    @Override
    public int getSecondaryColor() {
        return 9509561;
    }
}
