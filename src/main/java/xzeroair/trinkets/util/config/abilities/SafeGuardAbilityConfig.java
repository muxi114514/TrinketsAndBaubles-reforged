package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/**
 * 安全守护的配置段（对应 1.12 ConfigAbilitySafeGuard）。
 *
 * 移植说明：1.12 把音量/音调放在服务端配置的 CLIENT 子段里，但声音是服务端广播的，
 * 实际是服务端值，故此处直接平铺为 soundVolume / soundPitch。
 */
public class SafeGuardAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final IntValue maxHits;
    public final DoubleValue minDamageToCount;
    public final DoubleValue minDamageToTrigger;
    public final DoubleValue explosionReducedAmount;
    public final ConfigValue<String> effect;
    public final IntValue effectLevel;
    public final BooleanValue effectStacks;
    public final IntValue effectStackLimit;
    public final DoubleValue soundVolume;
    public final DoubleValue soundPitch;

    public SafeGuardAbilityConfig(ForgeConfigSpec.Builder builder, String name) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.maxHits = builder
                .comment("Every hit after this many is completely blocked, then the counter resets")
                .translation("xat.config.abilities.safe_guard.max_hits")
                .defineInRange("maxHits", 3, 0, 1000);
        this.minDamageToCount = builder
                .comment("Hits weaker than this do not count")
                .translation("xat.config.abilities.safe_guard.min_count")
                .defineInRange("minDamageToCount", 1.0D, 0.0D, 10000.0D);
        this.minDamageToTrigger = builder
                .comment("Hits weaker than this are never blocked")
                .translation("xat.config.abilities.safe_guard.min_trigger")
                .defineInRange("minDamageToTrigger", 1.0D, 0.0D, 10000.0D);
        this.explosionReducedAmount = builder
                .comment("Explosion damage multiplier; 1 disables the reduction")
                .translation("xat.config.abilities.safe_guard.explosion")
                .defineInRange("explosionReducedAmount", 0.25D, 0.0D, 1.0D);
        this.effect = builder
                .comment("Effect kept on the wearer")
                .translation("xat.config.abilities.safe_guard.effect")
                .define("effect", "minecraft:resistance");
        this.effectLevel = builder
                .comment("Amplifier of the kept effect (0 = level I)")
                .translation("xat.config.abilities.safe_guard.effect_level")
                .defineInRange("effectLevel", 0, 0, 255);
        this.effectStacks = builder
                .comment("Applying the same effect again stacks one level on top of the kept one")
                .translation("xat.config.abilities.safe_guard.stacks")
                .define("effectStacks", true);
        this.effectStackLimit = builder
                .comment("Highest amplifier reachable by stacking")
                .translation("xat.config.abilities.safe_guard.stack_limit")
                .defineInRange("effectStackLimit", 3, 0, 255);
        this.soundVolume = builder
                .comment("Volume of the block sound; 0 disables")
                .translation("xat.config.abilities.safe_guard.volume")
                .defineInRange("soundVolume", 0.2D, 0.0D, 1.0D);
        this.soundPitch = builder
                .comment("Pitch of the block sound")
                .translation("xat.config.abilities.safe_guard.pitch")
                .defineInRange("soundPitch", 1.0D, 0.5D, 2.0D);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
