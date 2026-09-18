package xzeroair.trinkets.util.config.server.items;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;

import xzeroair.trinkets.util.config.abilities.AbilityConfig;
import xzeroair.trinkets.util.config.abilities.DodgeAbilityConfig;
import xzeroair.trinkets.util.config.abilities.ElementAffinityAbilityConfigs;
import xzeroair.trinkets.util.config.abilities.EnderQueenAbilityConfig;
import xzeroair.trinkets.util.config.abilities.GreedyEyesAbilityConfig;
import xzeroair.trinkets.util.config.abilities.HardHeadAbilityConfig;
import xzeroair.trinkets.util.config.abilities.ImmunityAbilityConfig;
import xzeroair.trinkets.util.config.abilities.KineticAbilityConfig;
import xzeroair.trinkets.util.config.abilities.LightningBoltAbilityConfig;
import xzeroair.trinkets.util.config.abilities.MagneticAbilityConfig;
import xzeroair.trinkets.util.config.abilities.NightVisionAbilityConfig;
import xzeroair.trinkets.util.config.abilities.RepelAbilityConfig;
import xzeroair.trinkets.util.config.abilities.SafeGuardAbilityConfig;
import xzeroair.trinkets.util.config.abilities.SkilledSwimmerAbilityConfig;
import xzeroair.trinkets.util.config.abilities.ThirstAbsorptionAbilityConfig;
import xzeroair.trinkets.util.config.abilities.ViciousStrikeAbilityConfig;
import xzeroair.trinkets.util.config.abilities.WellRestedAbilityConfig;
import xzeroair.trinkets.util.config.server.SurvivalConfig;
import xzeroair.trinkets.util.config.server.race.ElementOverrideConfig;

/**
 * 各饰品的能力配置段（对应 1.12 各饰品配置类里的 ConfigAbilities 内部类），默认值取自 1.12。
 * 1.12 的 EXTERNAL 段（Enhanced Visuals / First Aid / 生存类联动能力）并入对应饰品的能力段。
 */
public final class AccessoryAbilitiesConfigs {

    public static class Weightless {
        public final AbilityConfig weightless;

        public Weightless(ForgeConfigSpec.Builder builder) {
            this.weightless = new AbilityConfig(builder, "weightless");
        }
    }

    public static class InertiaNull {
        public final KineticAbilityConfig nullKinetic;

        public InertiaNull(ForgeConfigSpec.Builder builder) {
            this.nullKinetic = new KineticAbilityConfig(builder, "null_kinetic", 0.0D, 0.0D);
        }
    }

    public static class GreaterInertia {
        public final KineticAbilityConfig reduceKinetic;

        public GreaterInertia(ForgeConfigSpec.Builder builder) {
            this.reduceKinetic = new KineticAbilityConfig(builder, "reduce_kinetic", 0.25D, 0.0D);
        }
    }

    public static class GlowRing {
        public final NightVisionAbilityConfig nightVision;

        public GlowRing(ForgeConfigSpec.Builder builder) {
            this.nightVision = new NightVisionAbilityConfig(builder, "night_vision");
        }
    }

    public static class SeaStone {
        public final ElementAffinityAbilityConfigs.Water waterAffinity;
        public final ImmunityAbilityConfig waterImmunity;
        public final SkilledSwimmerAbilityConfig skilledSwimmer;
        public final ThirstAbsorptionAbilityConfig waterAbsorption;
        public final AbilityConfig clearVision;

        public SeaStone(ForgeConfigSpec.Builder builder) {
            this.waterAffinity = new ElementAffinityAbilityConfigs.Water(builder, "water_affinity");
            this.waterImmunity = new ImmunityAbilityConfig(builder, "water_immunity");
            this.skilledSwimmer = new SkilledSwimmerAbilityConfig(builder, "skilled_swimmer");
            this.waterAbsorption = new ThirstAbsorptionAbilityConfig(builder, "water_absorption");
            this.clearVision = new AbilityConfig(builder, "clear_vision");
        }
    }

    public static class Polarized {
        public final MagneticAbilityConfig magnetic;
        public final RepelAbilityConfig repel;

        public Polarized(ForgeConfigSpec.Builder builder) {
            this.magnetic = new MagneticAbilityConfig(builder, "magnetic");
            this.repel = new RepelAbilityConfig(builder, "repel");
        }
    }

    public static class WitherRing {
        public final ElementAffinityAbilityConfigs.Dark darkAffinity;
        public final ElementAffinityAbilityConfigs.DarkImmunity darkImmunity;

        public WitherRing(ForgeConfigSpec.Builder builder) {
            this.darkAffinity = new ElementAffinityAbilityConfigs.Dark(builder, "dark_affinity");
            this.darkImmunity = new ElementAffinityAbilityConfigs.DarkImmunity(builder, "dark_immunity");
        }
    }

    public static class PoisonStone {
        public final ElementAffinityAbilityConfigs.Poison poisonAffinity;
        public final ImmunityAbilityConfig poisonImmunity;

        public PoisonStone(ForgeConfigSpec.Builder builder) {
            this.poisonAffinity = new ElementAffinityAbilityConfigs.Poison(builder, "poison_affinity");
            this.poisonImmunity = new ImmunityAbilityConfig(builder, "poison_immunity");
        }
    }

    public static class EnderCrown {
        public final EnderQueenAbilityConfig enderQueen;
        public final AbilityConfig enderEyes;

        public EnderCrown(ForgeConfigSpec.Builder builder) {
            this.enderQueen = new EnderQueenAbilityConfig(builder, "ender_queen");
            this.enderEyes = new AbilityConfig(builder, "ender_eyes");
        }
    }

    public static class DamageShield {
        public final SafeGuardAbilityConfig safeGuard;
        public final HardHeadAbilityConfig hardHead;
        public final AbilityConfig clearVision;

        public DamageShield(ForgeConfigSpec.Builder builder) {
            this.safeGuard = new SafeGuardAbilityConfig(builder, "safe_guard");
            this.hardHead = new HardHeadAbilityConfig(builder, "hard_head");
            this.clearVision = new AbilityConfig(builder, "clear_vision");
        }
    }

    public static class ArcingOrb {
        public final DodgeAbilityConfig dodge;
        public final LightningBoltAbilityConfig lightningBolt;

        public ArcingOrb(ForgeConfigSpec.Builder builder) {
            this.dodge = new DodgeAbilityConfig(builder, "dodge", 30.0D);
            this.lightningBolt = new LightningBoltAbilityConfig(builder, "lightning_bolt", 40.0D, 300.0D, 120);
        }
    }

    public static class TeddyBear {
        public final WellRestedAbilityConfig wellRested;

        public TeddyBear(ForgeConfigSpec.Builder builder) {
            this.wellRested = new WellRestedAbilityConfig(builder, "well_rested");
        }
    }

    public static class FaelisClaw {
        public final ViciousStrikeAbilityConfig viciousStrike;

        public FaelisClaw(ForgeConfigSpec.Builder builder) {
            this.viciousStrike = new ViciousStrikeAbilityConfig(builder, "vicious_strike", 300, 4);
        }
    }

    /** 龙之眼：中性形态的能力 + 三种元素形态各自的能力与配置替换项（对应 1.12 ConfigDragonsEye.ELEMENTS） */
    public static class DragonsEye {
        public final GreedyEyesAbilityConfig greedyEyes;
        public final NightVisionAbilityConfig nightVision;
        public final ImmunityAbilityConfig fireImmunity;
        public final Fire fire;
        public final Ice ice;
        public final Lightning lightning;

        public DragonsEye(ForgeConfigSpec.Builder builder) {
            this.greedyEyes = new GreedyEyesAbilityConfig(builder, "greedy_eyes", 0.0D);
            this.nightVision = new NightVisionAbilityConfig(builder, "night_vision");
            this.fireImmunity = new ImmunityAbilityConfig(builder, "fire_immunity", 3600, false);
            this.fire = new Fire(builder);
            this.ice = new Ice(builder);
            this.lightning = new Lightning(builder);
        }

        public static class Fire {
            public final ImmunityAbilityConfig fireImmunity;
            public final ElementOverrideConfig overrides;
            public final SurvivalConfig survival;

            Fire(ForgeConfigSpec.Builder builder) {
                builder.translation("xat.config.items.dragons_eye.fire").push("fire");
                this.fireImmunity = new ImmunityAbilityConfig(builder, "fire_immunity");
                this.overrides = new ElementOverrideConfig(builder, List.of("lycanitesmobs:smouldering", "iceandfire:melt"),
                        List.of("*;isFire"), List.of());
                this.survival = new SurvivalConfig(builder, true, false, false, false);
                builder.pop();
            }
        }

        public static class Ice {
            public final ImmunityAbilityConfig iceImmunity;
            public final AbilityConfig frostWalker;
            public final ElementOverrideConfig overrides;
            public final SurvivalConfig survival;

            Ice(ForgeConfigSpec.Builder builder) {
                builder.translation("xat.config.items.dragons_eye.ice").push("ice");
                this.iceImmunity = new ImmunityAbilityConfig(builder, "ice_immunity");
                this.frostWalker = new AbilityConfig(builder, "frost_walker");
                this.overrides = new ElementOverrideConfig(builder, List.of("iceandfire:frostbite", "iceandfire:frostburn"),
                        List.of("*;isIce"), List.of());
                this.survival = new SurvivalConfig(builder, false, true, false, false);
                builder.pop();
            }
        }

        public static class Lightning {
            public final ImmunityAbilityConfig lightningImmunity;
            public final ElementOverrideConfig overrides;
            public final SurvivalConfig survival;

            Lightning(ForgeConfigSpec.Builder builder) {
                builder.translation("xat.config.items.dragons_eye.lightning").push("lightning");
                this.lightningImmunity = new ImmunityAbilityConfig(builder, "lightning_immunity");
                this.overrides = new ElementOverrideConfig(builder, List.of("lycanitesmobs:paralysis", "iceandfire:voltage"),
                        List.of("*;isLightning"), List.of());
                this.survival = SurvivalConfig.none(builder);
                builder.pop();
            }
        }
    }

    private AccessoryAbilitiesConfigs() {
    }
}
