package xzeroair.trinkets.traits.abilities.base;

import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.interfaces.IKeyBindInterface;
import xzeroair.trinkets.util.KeyNames;

/**
 * 种族主动技的基类：绑定「种族主动技」键，辅助键为 Aux 键。对应 1.12 traits/abilities/base/AbilityRaceSpecific。
 */
public class AbilityRaceSpecific extends Ability implements IKeyBindInterface {

    public AbilityRaceSpecific(String modID, String name) {
        super(modID, name);
    }

    public AbilityRaceSpecific(String name) {
        super(name);
    }

    @Override
    public String getKey() {
        return KeyNames.RACE_ABILITY;
    }

    @Override
    public String getAuxKey() {
        return KeyNames.AUX_KEY;
    }
}
