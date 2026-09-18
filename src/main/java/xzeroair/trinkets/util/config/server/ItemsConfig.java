package xzeroair.trinkets.util.config.server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;

import xzeroair.trinkets.util.config.server.items.AccessoryAbilitiesConfigs;
import xzeroair.trinkets.util.config.server.items.AccessoryConfig;
import xzeroair.trinkets.util.config.server.items.AccessoryConfig.Defaults;

/**
 * 饰品配置（对应 1.12 TrinketItemsConfig）。默认值取自 1.12 各饰品配置类，属性名换成 1.20.1 注册名。
 * 1.12 的「经验装置」配置被注释掉且物品未注册，不移植。
 * 1.12 在流血/麻痹效果里硬编码「戴着法埃利斯之爪/电弧宝珠就移除」，这里改为两件饰品的默认免疫名单，走两层免疫（阻断 + 清理）。
 */
public class ItemsConfig {

    public final AccessoryConfig<AccessoryAbilitiesConfigs.DragonsEye> dragonsEye;
    public final AccessoryConfig<AccessoryAbilitiesConfigs.EnderCrown> enderCrown;
    public final AccessoryConfig<AccessoryAbilitiesConfigs.DamageShield> damageShield;
    public final AccessoryConfig<AccessoryAbilitiesConfigs.GlowRing> glowRing;
    public final AccessoryConfig<AccessoryAbilitiesConfigs.PoisonStone> poisonStone;
    public final AccessoryConfig<AccessoryAbilitiesConfigs.WitherRing> witherRing;
    public final AccessoryConfig<AccessoryAbilitiesConfigs.Polarized> polarizedStone;
    public final AccessoryConfig<AccessoryAbilitiesConfigs.SeaStone> seaStone;
    public final AccessoryConfig<AccessoryAbilitiesConfigs.InertiaNull> inertiaNull;
    public final AccessoryConfig<AccessoryAbilitiesConfigs.GreaterInertia> greaterInertia;
    public final AccessoryConfig<AccessoryAbilitiesConfigs.Weightless> weightlessStone;
    public final AccessoryConfig<AccessoryAbilitiesConfigs.ArcingOrb> arcingOrb;
    public final AccessoryConfig<AccessoryAbilitiesConfigs.TeddyBear> teddyBear;
    public final AccessoryConfig<AccessoryAbilitiesConfigs.FaelisClaw> faelisClaw;

    /** 变身戒指开关，键为种族注册名路径（对应 1.12 TRANSFORMATION.XXX_RING.ENABLED） */
    private final Map<String, BooleanValue> transformationRings = new ConcurrentHashMap<>();

    public ItemsConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Trinket settings")
                .translation("xat.config.items")
                .push("items");

        builder.translation("xat.config.items.transformation").push("transformation_rings");
        for (String race : List.of("dwarf", "elf", "fairy", "goblin", "titan", "faelis", "dragon", "taurus")) {
            this.transformationRings.put(race, builder
                    .comment("Enable the " + race + " ring")
                    .translation("xat.config.items.transformation." + race)
                    .define(race, true));
        }
        builder.pop();

        this.dragonsEye = new AccessoryConfig<>(builder, "dragons_eye",
                new Defaults(List.of(), List.of(), List.of("onAttacked:*;isMagic", "onHurt:*;isMagic"), List.of()),
                AccessoryAbilitiesConfigs.DragonsEye::new);
        this.enderCrown = new AccessoryConfig<>(builder, "ender_tiara",
                Defaults.immunities(List.of("lycanitesmobs:instability"), List.of("*;isVoid")).withSurvival(false, true, false, false),
                AccessoryAbilitiesConfigs.EnderCrown::new);
        this.damageShield = new AccessoryConfig<>(builder, "damage_shield", Defaults.NONE,
                AccessoryAbilitiesConfigs.DamageShield::new);
        this.glowRing = new AccessoryConfig<>(builder, "glow_ring", Defaults.NONE,
                AccessoryAbilitiesConfigs.GlowRing::new);
        this.poisonStone = new AccessoryConfig<>(builder, "poison_stone",
                Defaults.immunities(List.of("minecraft:poison", "minecraft:hunger", "lycanitesmobs:plague"), List.of("*;isPoison")),
                AccessoryAbilitiesConfigs.PoisonStone::new);
        this.witherRing = new AccessoryConfig<>(builder, "wither_ring",
                Defaults.immunities(List.of("minecraft:nausea"), List.of()),
                AccessoryAbilitiesConfigs.WitherRing::new);
        this.polarizedStone = new AccessoryConfig<>(builder, "polarized_stone", Defaults.NONE,
                AccessoryAbilitiesConfigs.Polarized::new);
        this.seaStone = new AccessoryConfig<>(builder, "sea_stone",
                Defaults.attributes("Name:forge:swim_speed, Amount:4, Operation:2").withSurvival(false, false, true, true),
                AccessoryAbilitiesConfigs.SeaStone::new);
        this.inertiaNull = new AccessoryConfig<>(builder, "inertia_null_stone",
                Defaults.attributes("Name:minecraft:generic.knockback_resistance, Amount:1, Operation:0"),
                AccessoryAbilitiesConfigs.InertiaNull::new);
        this.greaterInertia = new AccessoryConfig<>(builder, "greater_inertia_stone",
                Defaults.attributes(
                        "Name:minecraft:generic.knockback_resistance, Amount:0.4, Operation:0",
                        "Name:minecraft:generic.movement_speed, Amount:0.5, Operation:2",
                        "Name:xat:jump, Amount:1, Operation:1",
                        "Name:forge:step_height_addition, Amount:0.6, Operation:0"),
                AccessoryAbilitiesConfigs.GreaterInertia::new);
        this.weightlessStone = new AccessoryConfig<>(builder, "weightless_stone", Defaults.NONE,
                AccessoryAbilitiesConfigs.Weightless::new);
        this.arcingOrb = new AccessoryConfig<>(builder, "arcing_orb",
                new Defaults(List.of(), List.of("xat:paralysis"), List.of(),
                        List.of("Name:minecraft:generic.movement_speed, Amount:0.25, Operation:1")),
                AccessoryAbilitiesConfigs.ArcingOrb::new);
        this.teddyBear = new AccessoryConfig<>(builder, "teddy_bear",
                Defaults.immunities(List.of("lycanitesmobs:fear", "lycanitesmobs:insomnia"), List.of()),
                AccessoryAbilitiesConfigs.TeddyBear::new);
        this.faelisClaw = new AccessoryConfig<>(builder, "faelis_claw",
                new Defaults(List.of(), List.of("xat:bleed"), List.of(),
                        List.of("Name:minecraft:generic.attack_damage, Amount:0.25, Operation:1")),
                AccessoryAbilitiesConfigs.FaelisClaw::new);

        builder.pop();
    }

    /** 变身戒指是否启用；未知种族视为启用 */
    public boolean isTransformationRingEnabled(String race) {
        final BooleanValue value = this.transformationRings.get(race);
        return value == null || value.get();
    }
}
