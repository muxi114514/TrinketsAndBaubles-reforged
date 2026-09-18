package xzeroair.trinkets.traits.elements;

import xzeroair.trinkets.init.ModElements;


public class FireElement extends Element {

    public FireElement() {
        super("Fire");
    }

    @Override
    public Element[] getStrengths() {
        return new Element[]{ModElements.FIRE.get()};
    }

    @Override
    public Element[] getWeaknesses() {
        return new Element[]{ModElements.ICE.get()};
    }

    @Override
    public int getPrimaryColor() {
        return 16711680;
    }

    @Override
    public int getSecondaryColor() {
        return 16737832;
    }
}
