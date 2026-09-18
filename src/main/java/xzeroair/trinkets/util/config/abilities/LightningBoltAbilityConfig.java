package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/** 雷击的配置段（对应 1.12 ConfigAbilityLightningBolt）。 */
public class LightningBoltAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final DoubleValue attackDamage;
    public final DoubleValue attackCost;
    public final IntValue chargeTime;

    public LightningBoltAbilityConfig(ForgeConfigSpec.Builder builder, String name, double defaultDamage,
            double defaultCost, int defaultChargeTime) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.attackDamage = builder
                .comment("Damage at full charge")
                .translation("xat.config.abilities.lightning_bolt.damage")
                .defineInRange("attackDamage", defaultDamage, 0.0D, 10000.0D);
        this.attackCost = builder
                .comment("Mana spent at full charge")
                .translation("xat.config.abilities.lightning_bolt.cost")
                .defineInRange("attackCost", defaultCost, 0.0D, 10000.0D);
        this.chargeTime = builder
                .comment("Ticks needed to reach full charge")
                .translation("xat.config.abilities.lightning_bolt.charge_time")
                .defineInRange("chargeTime", defaultChargeTime, 1, 1200);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
