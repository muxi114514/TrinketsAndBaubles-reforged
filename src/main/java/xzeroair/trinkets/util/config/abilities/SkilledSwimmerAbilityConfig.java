package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;

/** 熟练泳者的配置段（对应 1.12 ConfigAbilitySkilledSwimmer）。 */
public class SkilledSwimmerAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final BooleanValue oldTweaks;

    public SkilledSwimmerAbilityConfig(ForgeConfigSpec.Builder builder, String name) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.oldTweaks = builder
                .comment("Use the old swimming behaviour: hover in place and follow the look direction")
                .translation("xat.config.abilities.skilled_swimmer.old")
                .define("oldTweaks", false);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
