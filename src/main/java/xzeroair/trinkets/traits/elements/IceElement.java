package xzeroair.trinkets.traits.elements;

import xzeroair.trinkets.init.ModElements;


public class IceElement extends Element {

    public IceElement() {
        super("Ice");
    }

    @Override
    public Element[] getStrengths() {
        return new Element[]{ModElements.ICE.get()};
    }

    @Override
    public Element[] getWeaknesses() {
        return new Element[]{ModElements.FIRE.get()};
    }

    @Override
    public int getPrimaryColor() {
        return 1999615;
    }

    @Override
    public int getSecondaryColor() {
        return 15132415;
    }
}
