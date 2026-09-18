package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/** 践踏冲锋的配置段（对应 1.12 ConfigAbilityStampede）。 */
public class StampedeAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final DoubleValue attackDamage;
    public final DoubleValue attackCost;
    public final IntValue chargeTime;
    public final DoubleValue velocityMin;
    public final DoubleValue velocityMax;

    public StampedeAbilityConfig(ForgeConfigSpec.Builder builder, String name) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.attackDamage = builder
                .comment("Damage dealt to entities hit at full charge")
                .translation("xat.config.abilities.stampede.damage")
                .defineInRange("attackDamage", 10.0D, 0.0D, 10000.0D);
        this.attackCost = builder
                .comment("Mana spent for a full charge")
                .translation("xat.config.abilities.stampede.cost")
                .defineInRange("attackCost", 20.0D, 0.0D, 10000.0D);
        this.chargeTime = builder
                .comment("Ticks needed to reach full charge")
                .translation("xat.config.abilities.stampede.charge_time")
                .defineInRange("chargeTime", 40, 1, 1200);
        this.velocityMin = builder
                .comment("Dash velocity at minimum charge")
                .translation("xat.config.abilities.stampede.velocity_min")
                .defineInRange("velocityMin", 3.0D, 0.0D, 20.0D);
        this.velocityMax = builder
                .comment("Dash velocity at full charge")
                .translation("xat.config.abilities.stampede.velocity_max")
                .defineInRange("velocityMax", 6.0D, 0.0D, 20.0D);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
