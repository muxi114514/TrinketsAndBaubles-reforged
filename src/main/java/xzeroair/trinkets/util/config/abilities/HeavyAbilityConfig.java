package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;

/** 沉重能力的配置段（对应 1.12 ConfigAbilityHeavy）。 */
public class HeavyAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final BooleanValue trample;

    public HeavyAbilityConfig(ForgeConfigSpec.Builder builder, String name) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.trample = builder
                .comment("Trample farmland and break plants underfoot while not sneaking")
                .translation("xat.config.abilities.heavy.trample")
                .define("trample", true);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
