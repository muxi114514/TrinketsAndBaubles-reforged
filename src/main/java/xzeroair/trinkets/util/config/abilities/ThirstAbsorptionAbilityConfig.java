package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/** 吸水的配置段（对应 1.12 ConfigAbilitySurvivalThirstAbsorption）：在水中每 frequency tick 回复 amount 点水分。 */
public class ThirstAbsorptionAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final IntValue amount;
    public final IntValue frequency;

    public ThirstAbsorptionAbilityConfig(ForgeConfigSpec.Builder builder, String name) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.amount = builder
                .comment("Hydration restored each time")
                .translation("xat.config.abilities.water_absorption.amount")
                .defineInRange("amount", 1, 0, 20);
        this.frequency = builder
                .comment("Ticks between each restore")
                .translation("xat.config.abilities.water_absorption.frequency")
                .defineInRange("frequency", 20, 1, 72000);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
