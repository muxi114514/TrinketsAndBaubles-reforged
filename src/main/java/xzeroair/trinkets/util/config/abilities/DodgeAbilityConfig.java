package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/** 闪避的配置段（对应 1.12 ConfigAbilityDodge）。 */
public class DodgeAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final DoubleValue cost;
    public final BooleanValue stuns;
    public final DoubleValue stunRadius;
    public final IntValue cooldown;
    public final BooleanValue keybindMovement;

    public DodgeAbilityConfig(ForgeConfigSpec.Builder builder, String name, double defaultCost) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.cost = builder
                .comment("Mana spent per dodge")
                .translation("xat.config.abilities.dodge.cost")
                .defineInRange("cost", defaultCost, 0.0D, 10000.0D);
        this.stuns = builder
                .comment("Dodging paralyses nearby entities")
                .translation("xat.config.abilities.dodge.stuns")
                .define("stuns", true);
        this.stunRadius = builder
                .comment("Horizontal radius of the stun")
                .translation("xat.config.abilities.dodge.stun_radius")
                .defineInRange("stunRadius", 2.0D, 0.0D, 32.0D);
        this.cooldown = builder
                .comment("Ticks between dodges")
                .translation("xat.config.abilities.dodge.cooldown")
                .defineInRange("cooldown", 20, 0, 1200);
        this.keybindMovement = builder
                .comment("Dodge by holding the dodge key and pressing a direction instead of double tapping")
                .translation("xat.config.abilities.dodge.keybind")
                .define("keybindMovement", false);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
