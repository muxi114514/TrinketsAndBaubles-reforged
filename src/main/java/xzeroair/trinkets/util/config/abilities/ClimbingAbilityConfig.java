package xzeroair.trinkets.util.config.abilities;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

/** 攀爬能力的配置段（对应 1.12 ConfigAbilityClimbing）。 */
public class ClimbingAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final BooleanValue useWhitelist;
    public final ConfigValue<List<? extends String>> blocks;

    public ClimbingAbilityConfig(ForgeConfigSpec.Builder builder, String name) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.useWhitelist = builder
                .comment("true: only the listed blocks are climbable; false: every block except the listed ones")
                .translation("xat.config.abilities.climbing.whitelist")
                .define("useWhitelist", true);
        this.blocks = builder
                .comment("Block ids, *-wildcards or #tags")
                .translation("xat.config.abilities.climbing.blocks")
                .defineListAllowEmpty("blocks", DefaultBlockLists.CLIMBABLE, o -> o instanceof String);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
