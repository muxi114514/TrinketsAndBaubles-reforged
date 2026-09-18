package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;

/**
 * 动能抵消/削减的配置段（对应 1.12 ConfigAbilityKinetic）。
 * amount 为坠落/撞墙伤害倍率：0 = 完全免疫，1 = 不生效；cost &gt; 0 时每次生效消耗魔力，魔力不足则不生效。
 */
public class KineticAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final DoubleValue amount;
    public final DoubleValue cost;

    public KineticAbilityConfig(ForgeConfigSpec.Builder builder, String name, double defaultAmount, double defaultCost) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.amount = builder
                .comment("Fall / kinetic damage multiplier; 0 negates it completely")
                .translation("xat.config.abilities.kinetic.amount")
                .defineInRange("amount", defaultAmount, 0.0D, 10.0D);
        this.cost = builder
                .comment("Mana spent each time the ability reduces damage; 0 makes it free")
                .translation("xat.config.abilities.kinetic.cost")
                .defineInRange("cost", defaultCost, 0.0D, 10000.0D);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
