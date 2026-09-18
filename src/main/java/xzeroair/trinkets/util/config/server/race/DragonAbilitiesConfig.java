package xzeroair.trinkets.util.config.server.race;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;

import xzeroair.trinkets.util.config.abilities.AbilityConfig;
import xzeroair.trinkets.util.config.abilities.BreathAbilityConfig;
import xzeroair.trinkets.util.config.abilities.ElytraFlightAbilityConfig;
import xzeroair.trinkets.util.config.abilities.FlightAbilityConfig;
import xzeroair.trinkets.util.config.abilities.GreedyEyesAbilityConfig;
import xzeroair.trinkets.util.config.abilities.ImmunityAbilityConfig;
import xzeroair.trinkets.util.config.abilities.LightningBoltAbilityConfig;
import xzeroair.trinkets.util.config.abilities.NightVisionAbilityConfig;
import xzeroair.trinkets.util.config.server.SurvivalConfig;

/**
 * 巨龙的能力配置（对应 1.12 DragonConfig.ABILITIES 与 DragonConfig.ELEMENTS.FIRE/ICE/LIGHTNING）。
 * 巨龙的能力随主元素分支：中性龙只有火焰免疫，火/冰/雷龙各有一组元素能力。
 *
 * 默认值取自 1.12：飞行每秒耗 5 魔力、滑翔升力耗 10 魔力。
 * 生存类联动段（1.12 COMPAT.SURVIVAL）按元素分：火龙免疫炎热、冰龙免疫寒冷。
 * 「麻痹」是恐怖生物的效果，1.12 默认名单里的 iceandfire:paralysis 在冰与火 1.20.1 中不存在，已去掉。
 * ForgeConfigSpec 会给新增字段自动写入默认值，已生成的配置文件无需删除。
 */
public class DragonAbilitiesConfig {

    /** 所有巨龙共有 */
    public final FlightAbilityConfig flight;
    public final ElytraFlightAbilityConfig elytraFlight;
    public final NightVisionAbilityConfig nightVision;
    public final GreedyEyesAbilityConfig greedyEyes;

    /** 中性（无元素）巨龙 */
    public final ImmunityAbilityConfig fireImmunity;
    public final BreathAbilityConfig dragonBreath;

    public final Fire fire;
    public final Ice ice;
    public final Lightning lightning;

    public DragonAbilitiesConfig(ForgeConfigSpec.Builder builder) {
        this.flight = new FlightAbilityConfig(builder, "flight", 5.0D);
        this.elytraFlight = new ElytraFlightAbilityConfig(builder, "elytra_flight", 10.0D);
        this.nightVision = new NightVisionAbilityConfig(builder, "night_vision");
        this.greedyEyes = new GreedyEyesAbilityConfig(builder, "greedy_eyes", 0.0D);
        // 1.12 中性龙的火焰免疫默认关闭
        this.fireImmunity = new ImmunityAbilityConfig(builder, "fire_immunity", 3600, false);
        this.dragonBreath = new BreathAbilityConfig(builder, "breath_dragon");
        builder.translation("xat.config.races.dragon.elements").push("elements");
        this.fire = new Fire(builder);
        this.ice = new Ice(builder);
        this.lightning = new Lightning(builder);
        builder.pop();
    }

    public static class Fire {

        public final ImmunityAbilityConfig fireImmunity;
        public final BreathAbilityConfig fireBreath;
        public final ElementOverrideConfig overrides;
        public final SurvivalConfig survival;

        Fire(ForgeConfigSpec.Builder builder) {
            builder.translation("xat.element.fire.name").push("fire");
            this.fireImmunity = new ImmunityAbilityConfig(builder, "fire_immunity");
            this.fireBreath = new BreathAbilityConfig(builder, "breath_fire");
            this.survival = new SurvivalConfig(builder, true, false, false, false);
            this.overrides = new ElementOverrideConfig(builder, List.of("lycanitesmobs:smouldering"), List.of("*;isFire"), List.of(
                    "Name:minecraft:generic.max_health, Amount:0.25, Operation:1",
                    "Name:minecraft:generic.attack_damage, Amount:0.5, Operation:1",
                    "Name:minecraft:generic.armor_toughness, Amount:0.5, Operation:1",
                    "Name:xat:fly_speed, Amount:-0.6, Operation:2"));
            builder.pop();
        }
    }

    public static class Ice {

        public final ImmunityAbilityConfig iceImmunity;
        public final AbilityConfig frostWalker;
        public final BreathAbilityConfig iceBreath;
        public final ElementOverrideConfig overrides;
        public final SurvivalConfig survival;

        Ice(ForgeConfigSpec.Builder builder) {
            builder.translation("xat.element.ice.name").push("ice");
            this.iceImmunity = new ImmunityAbilityConfig(builder, "ice_immunity");
            this.frostWalker = new AbilityConfig(builder, "frost_walker");
            this.iceBreath = new BreathAbilityConfig(builder, "breath_ice", "minecraft:slowness:100:2");
            this.survival = new SurvivalConfig(builder, false, true, false, false);
            this.overrides = new ElementOverrideConfig(builder, List.of(), List.of("*;isIce"), List.of(
                    "Name:minecraft:generic.max_health, Amount:0.25, Operation:1",
                    "Name:minecraft:generic.attack_damage, Amount:0.5, Operation:1",
                    "Name:minecraft:generic.armor_toughness, Amount:0.5, Operation:1",
                    "Name:xat:fly_speed, Amount:-0.6, Operation:2"));
            builder.pop();
        }
    }

    public static class Lightning {

        public final ImmunityAbilityConfig lightningImmunity;
        public final LightningBoltAbilityConfig lightningBolt;
        public final BreathAbilityConfig lightningBreath;
        public final ElementOverrideConfig overrides;
        public final SurvivalConfig survival;

        Lightning(ForgeConfigSpec.Builder builder) {
            builder.translation("xat.element.lightning.name").push("lightning");
            this.lightningImmunity = new ImmunityAbilityConfig(builder, "lightning_immunity");
            this.lightningBolt = new LightningBoltAbilityConfig(builder, "lightning_bolt", 40.0D, 200.0D, 20);
            this.lightningBreath = new BreathAbilityConfig(builder, "breath_lightning", "minecraft:slowness:20:4", "minecraft:weakness:20:1");
            this.survival = SurvivalConfig.none(builder);
            this.overrides = new ElementOverrideConfig(builder, List.of("lycanitesmobs:paralysis"), List.of("*;isLightning"), List.of(
                    "Name:minecraft:generic.max_health, Amount:0.25, Operation:1",
                    "Name:minecraft:generic.attack_damage, Amount:0.5, Operation:1",
                    "Name:minecraft:generic.armor_toughness, Amount:0.5, Operation:1",
                    "Name:xat:fly_speed, Amount:-0.6, Operation:2"));
            builder.pop();
        }
    }
}
