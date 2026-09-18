package xzeroair.trinkets.util.config.server.race;

import net.minecraftforge.common.ForgeConfigSpec;

import xzeroair.trinkets.util.config.abilities.ClimbingAbilityConfig;
import xzeroair.trinkets.util.config.abilities.ElytraFlightAbilityConfig;
import xzeroair.trinkets.util.config.abilities.FlightAbilityConfig;
import xzeroair.trinkets.util.config.abilities.HealCloudAbilityConfig;

/**
 * 妖精的能力配置（对应 1.12 FairyConfig.ABILITIES）。
 * 默认值取自 1.12：飞行免费、滑翔升力免费。已含全部能力。
 */
public class FairyAbilitiesConfig {

    public final FlightAbilityConfig flight;
    public final ElytraFlightAbilityConfig elytraFlight;
    public final ClimbingAbilityConfig climbing;
    public final HealCloudAbilityConfig restorationField;

    public FairyAbilitiesConfig(ForgeConfigSpec.Builder builder) {
        this.flight = new FlightAbilityConfig(builder, "flight", 0.0D);
        this.elytraFlight = new ElytraFlightAbilityConfig(builder, "elytra_flight", 0.0D);
        this.climbing = new ClimbingAbilityConfig(builder, "climbing");
        this.restorationField = new HealCloudAbilityConfig(builder, "mending_bloom");
    }
}
