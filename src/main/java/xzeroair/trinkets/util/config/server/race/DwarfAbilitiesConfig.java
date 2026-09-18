package xzeroair.trinkets.util.config.server.race;

import net.minecraftforge.common.ForgeConfigSpec;

import xzeroair.trinkets.util.config.abilities.SkilledMinerAbilityConfig;

/**
 * 矮人的能力配置（对应 1.12 DwarfConfig.ABILITIES）。
 * 已含全部能力。
 */
public class DwarfAbilitiesConfig {

    public final SkilledMinerAbilityConfig skilledMiner;

    public DwarfAbilitiesConfig(ForgeConfigSpec.Builder builder) {
        this.skilledMiner = new SkilledMinerAbilityConfig(builder, "skilled_miner");
    }
}
