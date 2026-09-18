package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/** 凶狠打击的配置段（对应 1.12 ConfigAbilityViciousStrike）：近战概率施加流血；含污秽之地联动开关（1.12 COMPAT.DEFILED_LANDS.BLEED）。 */
public class ViciousStrikeAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final IntValue chance;
    public final IntValue duration;
    public final BooleanValue defiledLandsBleed;

    public ViciousStrikeAbilityConfig(ForgeConfigSpec.Builder builder, String name, int defaultDuration, int defaultChance) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.chance = builder
                .comment("1 in N chance to cause bleeding; 0 always bleeds")
                .translation("xat.config.abilities.vicious_strike.chance")
                .defineInRange("chance", defaultChance, 0, 10000);
        this.duration = builder
                .comment("Bleed duration in ticks (a third of this for non-Faelis)")
                .translation("xat.config.abilities.vicious_strike.duration")
                .defineInRange("duration", defaultDuration, 1, 72000);
        this.defiledLandsBleed = builder
                .comment("With Defiled Lands installed, apply its Bleeding effect instead")
                .translation("xat.config.compat.defiled_lands.bleed")
                .define("defiledLandsBleed", true);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
