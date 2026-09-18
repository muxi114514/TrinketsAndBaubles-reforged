package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/**
 * 元素免疫类能力的配置段（对应 1.12 ConfigAbilityImmunityFire / Ice / Lightning，三者字段相同）。
 * duration > 0 时同时给予对应的抗性效果；1.12 默认 3600。
 */
public class ImmunityAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final IntValue duration;

    public ImmunityAbilityConfig(ForgeConfigSpec.Builder builder, String name, int defaultDuration, boolean defaultEnabled) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, defaultEnabled);
        this.duration = builder
                .comment("Also grant the matching resistance effect; 0 disables the effect")
                .translation("xat.config.abilities.immunity.duration")
                .defineInRange("duration", defaultDuration, 0, Integer.MAX_VALUE);
        builder.pop();
    }

    public ImmunityAbilityConfig(ForgeConfigSpec.Builder builder, String name, int defaultDuration) {
        this(builder, name, defaultDuration, true);
    }

    public ImmunityAbilityConfig(ForgeConfigSpec.Builder builder, String name) {
        this(builder, name, 3600);
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
