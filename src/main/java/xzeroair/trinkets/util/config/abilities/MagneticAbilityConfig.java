package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/**
 * 磁力（吸取掉落物与经验球）的配置段（对应 1.12 ConfigAbilityMagnetic）。
 * 1.12 的白名单两项带 @Config.Ignore（从未生效），不移植。
 */
public class MagneticAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final BooleanValue pickupInstant;
    public final BooleanValue pickupInstantXp;
    public final BooleanValue pickupXp;
    public final DoubleValue cost;
    public final IntValue frequency;
    public final DoubleValue force;
    public final IntValue rangeVertical;
    public final IntValue rangeHorizontal;

    public MagneticAbilityConfig(ForgeConfigSpec.Builder builder, String name) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.pickupInstant = builder
                .comment("Pick items up instantly instead of pulling them in")
                .translation("xat.config.abilities.magnetic.instant")
                .define("pickupInstant", true);
        this.pickupInstantXp = builder
                .comment("Absorb experience orbs instantly instead of pulling them in")
                .translation("xat.config.abilities.magnetic.instant_xp")
                .define("pickupInstantXp", true);
        this.pickupXp = builder
                .comment("Also attract experience orbs")
                .translation("xat.config.abilities.magnetic.xp")
                .define("pickupXp", true);
        this.cost = builder
                .comment("Mana spent every frequency ticks while active; 0 makes it free")
                .translation("xat.config.abilities.magnetic.cost")
                .defineInRange("cost", 0.0D, 0.0D, 10000.0D);
        this.frequency = builder
                .comment("Ticks between mana payments")
                .translation("xat.config.abilities.magnetic.frequency")
                .defineInRange("frequency", 20, 1, 12000);
        this.force = builder
                .comment("Pull strength when not picking up instantly")
                .translation("xat.config.abilities.magnetic.force")
                .defineInRange("force", 0.1D, 0.1D, 1.0D);
        this.rangeVertical = builder
                .comment("Vertical range in blocks")
                .translation("xat.config.abilities.magnetic.range_vertical")
                .defineInRange("rangeVertical", 6, 0, 32);
        this.rangeHorizontal = builder
                .comment("Horizontal range in blocks")
                .translation("xat.config.abilities.magnetic.range_horizontal")
                .defineInRange("rangeHorizontal", 12, 0, 32);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
