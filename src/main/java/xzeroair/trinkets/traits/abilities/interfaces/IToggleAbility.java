package xzeroair.trinkets.traits.abilities.interfaces;

/** 可开关 / 多档位的能力。 */
public interface IToggleAbility extends IAbilityInterface {

    boolean isAbilityToggled();

    int getToggleMode();

    IToggleAbility toggleAbility(boolean enabled);

    IToggleAbility toggleAbility(int value);
}
