package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;

/** 鞘翅飞行（滑翔）的配置段（对应 1.12 ConfigAbilityElytraFlight）。 */
public class ElytraFlightAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final BooleanValue liftEnabled;
    public final BooleanValue collisionDamage;
    public final DoubleValue cost;
    public final DoubleValue liftCost;
    public final DoubleValue liftStrength;

    public ElytraFlightAbilityConfig(ForgeConfigSpec.Builder builder, String name, double defaultLiftCost) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.liftEnabled = builder
                .comment("Pressing jump while gliding gives an upward boost")
                .translation("xat.config.abilities.elytra.lift")
                .define("liftEnabled", true);
        this.collisionDamage = builder
                .comment("Crashing into a wall while gliding deals damage, like vanilla elytra")
                .translation("xat.config.abilities.elytra.collision")
                .define("collisionDamage", true);
        this.cost = builder
                .comment("Mana spent every second while gliding; 0 makes gliding free")
                .translation("xat.config.abilities.elytra.cost")
                .defineInRange("cost", 0.0D, 0.0D, 10000.0D);
        this.liftCost = builder
                .comment("Mana spent per lift boost")
                .translation("xat.config.abilities.elytra.lift_cost")
                .defineInRange("liftCost", defaultLiftCost, 0.0D, 10000.0D);
        this.liftStrength = builder
                .comment("Upward velocity added per lift boost")
                .translation("xat.config.abilities.elytra.lift_strength")
                .defineInRange("liftStrength", 0.42D, 0.0D, 10.0D);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
