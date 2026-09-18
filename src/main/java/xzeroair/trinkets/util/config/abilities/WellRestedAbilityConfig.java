package xzeroair.trinkets.util.config.abilities;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/** 充分休息的配置段（对应 1.12 ConfigAbilityWellRested）。 */
public class WellRestedAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final ConfigValue<List<? extends String>> sleepBonuses;
    public final IntValue randomBonuses;

    public WellRestedAbilityConfig(ForgeConfigSpec.Builder builder, String name) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.sleepBonuses = builder
                .comment("Effects granted after a full night's sleep. Format: modid:effect:duration:amplifier")
                .translation("xat.config.abilities.well_rested.bonuses")
                .defineListAllowEmpty("sleepBonuses",
                        List.of("minecraft:regeneration:300:0", "minecraft:luck:600:0", "minecraft:health_boost:3600:1"),
                        o -> o instanceof String);
        this.randomBonuses = builder
                .comment("Grant this many random picks from the list instead of all of them; 0 grants all")
                .translation("xat.config.abilities.well_rested.random")
                .defineInRange("randomBonuses", 0, 0, 64);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
