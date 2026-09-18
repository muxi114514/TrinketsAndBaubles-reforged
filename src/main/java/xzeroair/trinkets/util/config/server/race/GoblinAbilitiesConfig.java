package xzeroair.trinkets.util.config.server.race;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;

import xzeroair.trinkets.util.config.abilities.AbilityConfig;
import xzeroair.trinkets.util.config.abilities.ClimbingAbilityConfig;

/**
 * 哥布林的能力配置（对应 1.12 GoblinConfig.ABILITIES 与三项种族开关）。
 */
public class GoblinAbilitiesConfig {

    public final ClimbingAbilityConfig climbing;
    public final AbilityConfig wolfRider;
    public final BooleanValue explosiveResistance;
    public final BooleanValue friendlyCreepers;
    public final BooleanValue creepersExplodeOnContact;

    public GoblinAbilitiesConfig(ForgeConfigSpec.Builder builder) {
        this.climbing = new ClimbingAbilityConfig(builder, "climbing");
        this.wolfRider = new AbilityConfig(builder, "wolf_rider");
        this.explosiveResistance = builder
                .comment("Explosion damage x (current health / 20), clamped to 1%..100%; fire damage x0.8, lava x0.5")
                .translation("xat.config.races.goblin.resistance")
                .define("naturalExplosiveResistance", true);
        this.friendlyCreepers = builder
                .comment("Creepers drop you as their attack target")
                .translation("xat.config.races.goblin.creeper.friendly")
                .define("friendlyCreepers", true);
        this.creepersExplodeOnContact = builder
                .comment("Creepers you hit ignite immediately")
                .translation("xat.config.races.goblin.creeper.explode")
                .define("creepersExplodeOnContact", true);
    }
}
