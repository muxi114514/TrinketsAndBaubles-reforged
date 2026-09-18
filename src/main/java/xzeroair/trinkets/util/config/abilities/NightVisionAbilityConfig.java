package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;

/** 夜视能力的配置段（对应 1.12 ConfigAbilityNightVision）。 */
public class NightVisionAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final DoubleValue cost;

    public NightVisionAbilityConfig(ForgeConfigSpec.Builder builder, String name) {
        this(builder, name, true);
    }

    public NightVisionAbilityConfig(ForgeConfigSpec.Builder builder, String name, boolean defaultEnabled) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, defaultEnabled);
        this.cost = builder
                .comment("Mana spent every second while active; 0 makes it free")
                .translation("xat.config.abilities.night_vision.cost")
                .defineInRange("cost", 0.0D, 0.0D, 10000.0D);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
