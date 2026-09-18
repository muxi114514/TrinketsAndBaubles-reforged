package xzeroair.trinkets.util.config.abilities;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/** 治愈之云（复苏领域）的配置段（对应 1.12 ConfigAbilityHealCloud）。 */
public class HealCloudAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final ConfigValue<List<? extends String>> effects;
    public final DoubleValue radius;
    public final DoubleValue verticalRadius;
    public final IntValue duration;
    public final IntValue waitTime;
    public final IntValue pulseInterval;
    public final IntValue reapplicationDelay;
    public final IntValue itemRepairAmount;
    public final IntValue growthAttemptsPerPulse;
    public final DoubleValue castRange;
    public final DoubleValue costPerSecond;
    public final DoubleValue costPerPotionEffect;
    public final DoubleValue costPerEffectLevel;

    public HealCloudAbilityConfig(ForgeConfigSpec.Builder builder, String name) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.effects = builder
                .comment("Effects applied inside the field. Format: modid:effect:duration:amplifier")
                .translation("xat.config.abilities.heal_cloud.effects")
                .defineListAllowEmpty("effects", List.of("minecraft:instant_health:60:0"), o -> o instanceof String);
        this.radius = builder
                .comment("Field radius")
                .translation("xat.config.abilities.heal_cloud.radius")
                .defineInRange("radius", 3.0D, 0.5D, 32.0D);
        this.verticalRadius = builder
                .comment("Field vertical radius")
                .translation("xat.config.abilities.heal_cloud.vertical_radius")
                .defineInRange("verticalRadius", 2.0D, 0.5D, 32.0D);
        this.duration = builder
                .comment("Field duration in ticks")
                .translation("xat.config.abilities.heal_cloud.duration")
                .defineInRange("duration", 120, 1, 72000);
        this.waitTime = builder
                .comment("Ticks before the field becomes active")
                .translation("xat.config.abilities.heal_cloud.wait_time")
                .defineInRange("waitTime", 10, 0, 1200);
        this.pulseInterval = builder
                .comment("Ticks between field pulses")
                .translation("xat.config.abilities.heal_cloud.pulse_interval")
                .defineInRange("pulseInterval", 20, 1, 1200);
        this.reapplicationDelay = builder
                .comment("Ticks before the same target can be affected again")
                .translation("xat.config.abilities.heal_cloud.reapplication_delay")
                .defineInRange("reapplicationDelay", 20, 1, 1200);
        this.itemRepairAmount = builder
                .comment("Durability restored to dropped items per pulse; 0 disables")
                .translation("xat.config.abilities.heal_cloud.item_repair")
                .defineInRange("itemRepairAmount", 1, 0, 10000);
        this.growthAttemptsPerPulse = builder
                .comment("Plants grown per pulse; 0 disables")
                .translation("xat.config.abilities.heal_cloud.growth")
                .defineInRange("growthAttemptsPerPulse", 4, 0, 256);
        this.castRange = builder
                .comment("Maximum casting distance")
                .translation("xat.config.abilities.heal_cloud.cast_range")
                .defineInRange("castRange", 15.0D, 1.0D, 64.0D);
        this.costPerSecond = builder
                .comment("Mana cost per second of field duration")
                .translation("xat.config.abilities.heal_cloud.cost_per_second")
                .defineInRange("costPerSecond", 20.0D, 0.0D, 10000.0D);
        this.costPerPotionEffect = builder
                .comment("Extra mana cost per configured effect")
                .translation("xat.config.abilities.heal_cloud.cost_per_effect")
                .defineInRange("costPerPotionEffect", 5.0D, 0.0D, 10000.0D);
        this.costPerEffectLevel = builder
                .comment("Extra mana cost per effect amplifier level")
                .translation("xat.config.abilities.heal_cloud.cost_per_level")
                .defineInRange("costPerEffectLevel", 5.0D, 0.0D, 10000.0D);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
