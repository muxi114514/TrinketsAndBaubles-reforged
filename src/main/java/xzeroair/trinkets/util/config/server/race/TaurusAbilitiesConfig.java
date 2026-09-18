package xzeroair.trinkets.util.config.server.race;

import net.minecraftforge.common.ForgeConfigSpec;

import xzeroair.trinkets.util.config.abilities.StampedeAbilityConfig;

/** 牛头人的能力配置（对应 1.12 TaurusConfig.ABILITIES）。已含全部能力。 */
public class TaurusAbilitiesConfig {

    public final StampedeAbilityConfig stampede;

    public TaurusAbilitiesConfig(ForgeConfigSpec.Builder builder) {
        this.stampede = new StampedeAbilityConfig(builder, "stampede");
    }
}
