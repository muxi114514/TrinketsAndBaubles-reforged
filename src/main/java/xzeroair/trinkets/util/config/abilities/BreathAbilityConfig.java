package xzeroair.trinkets.util.config.abilities;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/** 吐息类能力的配置段（对应 1.12 ConfigAbilityBreath，火/冰/雷/龙息四种共用）。 */
public class BreathAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final BooleanValue terrain;
    public final DoubleValue damage;
    public final DoubleValue cost;
    public final IntValue frequency;
    public final ConfigValue<List<? extends String>> effects;

    public BreathAbilityConfig(ForgeConfigSpec.Builder builder, String name, String... defaultEffects) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.terrain = builder
                .comment("Breath can set fires, freeze liquids and place snow")
                .translation("xat.config.abilities.breath.terrain")
                .define("terrain", true);
        this.damage = builder
                .comment("Damage per breath projectile")
                .translation("xat.config.abilities.breath.damage")
                .defineInRange("damage", 1.0D, 0.0D, 10000.0D);
        this.cost = builder
                .comment("Mana spent per breath projectile")
                .translation("xat.config.abilities.breath.cost")
                .defineInRange("cost", 10.0D, 0.0D, 10000.0D);
        this.frequency = builder
                .comment("Ticks between breath projectiles while holding the key")
                .translation("xat.config.abilities.breath.frequency")
                .defineInRange("frequency", 3, 0, 200);
        this.effects = builder
                .comment("Effects applied to targets. Format: modid:effect:duration:amplifier")
                .translation("xat.config.abilities.breath.effects")
                .defineListAllowEmpty("effects", List.of(defaultEffects), o -> o instanceof String);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
