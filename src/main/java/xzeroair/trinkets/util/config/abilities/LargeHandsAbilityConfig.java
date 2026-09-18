package xzeroair.trinkets.util.config.abilities;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;

import xzeroair.trinkets.enums.ActivationMethod;

/** 大手的配置段（对应 1.12 ConfigAbilityLargeHands）。 */
public class LargeHandsAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final EnumValue<ActivationMethod> miningExtended;
    public final ConfigValue<List<? extends String>> miningExtendedBlacklist;

    public LargeHandsAbilityConfig(ForgeConfigSpec.Builder builder, String name) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.miningExtended = builder
                .comment("When 3x3x3 area mining triggers: SNEAK, STAND, ALWAYS or NEVER")
                .translation("xat.config.abilities.large_hands.mining")
                .defineEnum("miningExtended", ActivationMethod.STAND);
        this.miningExtendedBlacklist = builder
                .comment("Blocks never broken by area mining; ids, *-wildcards or #tags")
                .translation("xat.config.abilities.large_hands.blacklist")
                .defineListAllowEmpty("miningExtendedBlacklist", DefaultBlockLists.LARGE_HANDS_BLACKLIST, o -> o instanceof String);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
