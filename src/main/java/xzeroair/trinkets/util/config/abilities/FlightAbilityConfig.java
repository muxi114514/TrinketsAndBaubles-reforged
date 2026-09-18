package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;

/** 创造飞行的配置段（对应 1.12 ConfigAbilityFlight）。 */
public class FlightAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final DoubleValue cost;

    public FlightAbilityConfig(ForgeConfigSpec.Builder builder, String name, double defaultCost) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.cost = builder
                .comment("Mana spent every second while flying; 0 makes flight free")
                .translation("xat.config.abilities.flight.cost")
                .defineInRange("cost", defaultCost, 0.0D, 10000.0D);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
