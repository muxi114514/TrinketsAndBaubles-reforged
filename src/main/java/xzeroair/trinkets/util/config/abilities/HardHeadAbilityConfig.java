package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/** 硬头的配置段（对应 1.12 ConfigAbilityHardHead）：First Aid 头部被打空时按概率保住头部血量。 */
public class HardHeadAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final IntValue chance;

    public HardHeadAbilityConfig(ForgeConfigSpec.Builder builder, String name) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.chance = builder
                .comment("1 in N chance to shrug off a lethal head shot; 0 always")
                .translation("xat.config.abilities.hard_head.chance")
                .defineInRange("chance", 100, 0, 10000);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
