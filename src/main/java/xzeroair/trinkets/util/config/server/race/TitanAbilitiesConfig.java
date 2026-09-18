package xzeroair.trinkets.util.config.server.race;

import net.minecraftforge.common.ForgeConfigSpec;

import xzeroair.trinkets.util.config.abilities.HeavyAbilityConfig;
import xzeroair.trinkets.util.config.abilities.LargeHandsAbilityConfig;

/**
 * 泰坦的能力配置（对应 1.12 TitanConfig.ABILITIES）。
 * 已含全部能力。
 */
public class TitanAbilitiesConfig {

    public final HeavyAbilityConfig heavy;
    public final LargeHandsAbilityConfig largeHands;

    public TitanAbilitiesConfig(ForgeConfigSpec.Builder builder) {
        this.heavy = new HeavyAbilityConfig(builder, "heavy");
        this.largeHands = new LargeHandsAbilityConfig(builder, "large_hands");
    }
}
