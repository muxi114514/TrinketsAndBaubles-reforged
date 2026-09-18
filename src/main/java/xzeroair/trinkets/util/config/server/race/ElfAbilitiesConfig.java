package xzeroair.trinkets.util.config.server.race;

import net.minecraftforge.common.ForgeConfigSpec;

import xzeroair.trinkets.util.config.abilities.SkilledArcherAbilityConfig;

/** 精灵的能力配置（对应 1.12 ElfConfig.ABILITIES）。已含全部能力。 */
public class ElfAbilitiesConfig {

    public final SkilledArcherAbilityConfig skilledArcher;

    public ElfAbilitiesConfig(ForgeConfigSpec.Builder builder) {
        this.skilledArcher = new SkilledArcherAbilityConfig(builder, "skilled_archer", 20.0D);
    }
}
