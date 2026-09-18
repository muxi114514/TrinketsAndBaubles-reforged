package xzeroair.trinkets.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import xzeroair.trinkets.items.base.FoodBase;
import xzeroair.trinkets.items.base.ItemBase;
import xzeroair.trinkets.items.base.RaceFood;
import xzeroair.trinkets.items.foods.ManaCrystal;
import xzeroair.trinkets.items.foods.ManaReagent;
import xzeroair.trinkets.items.foods.RestorationSerum;
import xzeroair.trinkets.items.trinkets.SimpleAccessory;
import xzeroair.trinkets.items.trinkets.TrinketCosmetic;
import xzeroair.trinkets.items.trinkets.TrinketDragonsEye;
import xzeroair.trinkets.items.trinkets.TrinketEnderTiara;
import xzeroair.trinkets.items.trinkets.TrinketRaceBase;
import xzeroair.trinkets.items.trinkets.TrinketTeddyBear;
import xzeroair.trinkets.traits.abilities.AbilityDodge;
import xzeroair.trinkets.traits.abilities.AbilityKinetic;
import xzeroair.trinkets.traits.abilities.AbilityMagnetic;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.AbilityNightVision;
import xzeroair.trinkets.traits.abilities.AbilityRepel;
import xzeroair.trinkets.traits.abilities.AbilitySafeGuard;
import xzeroair.trinkets.traits.abilities.AbilitySkilledSwimmer;
import xzeroair.trinkets.traits.abilities.AbilityViciousStrike;
import xzeroair.trinkets.traits.abilities.AbilityWeightless;
import xzeroair.trinkets.traits.abilities.compat.AbilityClearVision;
import xzeroair.trinkets.traits.abilities.compat.AbilityHardHead;
import xzeroair.trinkets.traits.abilities.compat.AbilityWaterAbsorption;
import xzeroair.trinkets.traits.abilities.elements.dark.AbilityAffinityDark;
import xzeroair.trinkets.traits.abilities.elements.dark.AbilityDarkImmunity;
import xzeroair.trinkets.traits.abilities.elements.lightning.AbilityLightningBolt;
import xzeroair.trinkets.traits.abilities.elements.poison.AbilityPoisonAffinity;
import xzeroair.trinkets.traits.abilities.elements.poison.AbilityPoisonImmunity;
import xzeroair.trinkets.traits.abilities.elements.water.AbilityWaterAffinity;
import xzeroair.trinkets.traits.abilities.elements.water.AbilityWaterImmunity;
import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.compat.ModCompat;
import xzeroair.trinkets.util.compat.survival.SurvivalCompat;
import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 物品注册表。
 *
 * 移植说明：1.12 原版按 baubles / trinkets 两套类做「装了 Baubles 用前者，否则用自带饰品栏」的
 * 二选一注册；1.20.1 统一由 Curios 承担饰品槽，两套类合并为一套，此处不再分支。
 * 注册名严格取自 1.12 的 TrinketsRegistryNames，以便资源与配方直接复用。
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Reference.MODID);

    // ── 材料 ──
    public static final RegistryObject<Item> GLOWING_POWDER = ITEMS.register("glowing_powder", ItemBase::new);
    public static final RegistryObject<Item> GLOWING_INGOT = ITEMS.register("glowing_ingot", ItemBase::new);
    public static final RegistryObject<Item> GLOWING_GEM = ITEMS.register("glowing_gem", ItemBase::new);
    public static final RegistryObject<Item> SPARK_POWDER = ITEMS.register("spark_powder", ItemBase::new);

    // ── 种族食物（营养 2 / 饱和 4F / 饱腹时也能吃，取自 1.12 RaceFood 默认值）──
    public static final RegistryObject<Item> DWARF_STOUT = ITEMS.register("dwarf_stout",
            () -> raceFood(16, UseAnim.DRINK, "dwarf"));
    public static final RegistryObject<Item> ELF_SAP = ITEMS.register("elf_sap",
            () -> raceFood(32, UseAnim.DRINK, "elf"));
    public static final RegistryObject<Item> FAELIS_FOOD = ITEMS.register("faelis_food",
            () -> raceFood(32, UseAnim.EAT, "faelis"));
    public static final RegistryObject<Item> FAIRY_DEW = ITEMS.register("fairy_dew",
            () -> raceFood(16, UseAnim.DRINK, "fairy"));
    public static final RegistryObject<Item> GOBLIN_SOUP = ITEMS.register("goblin_soup",
            () -> raceFood(32, UseAnim.DRINK, "goblin"));
    public static final RegistryObject<Item> TITAN_SPIRIT = ITEMS.register("titan_spirit",
            () -> raceFood(32, UseAnim.DRINK, "titan"));
    public static final RegistryObject<Item> DRAGON_GEM = ITEMS.register("dragon_gem",
            () -> raceFood(32, UseAnim.EAT, "dragon"));
    public static final RegistryObject<Item> TAURUS_TEA = ITEMS.register("taurus_tea",
            () -> raceFood(32, UseAnim.EAT, "taurus"));

    // ── 魔力系食物 ──
    // 糖果只靠回复名单回魔，无独立逻辑；按堆叠数量切换的 4 档贴图随 P6 客户端层补
    public static final RegistryObject<Item> MANA_CANDY = ITEMS.register("mana_candy",
            () -> new FoodBase(FoodBase.props(2, 1F, true), 16, UseAnim.EAT));
    public static final RegistryObject<Item> MANA_CRYSTAL = ITEMS.register("mana_crystal", ManaCrystal::new);
    public static final RegistryObject<Item> MANA_REAGENT = ITEMS.register("mana_reagent", ManaReagent::new);
    public static final RegistryObject<Item> RESTORATION_SERUM = ITEMS.register("restoration_serum", RestorationSerum::new);

    // ── 饰品（属性 UUID 取自 1.12）──
    public static final RegistryObject<Item> WEIGHTLESS_STONE = ITEMS.register("weightless_stone",
            () -> new SimpleAccessory<>("ba6e840e-46b2-4cb7-af4a-5f681333abe5", ModElements.LIGHT,
                    () -> TrinketsConfig.SERVER.items.weightlessStone,
                    (config, abilities) -> abilities.add(new AbilityWeightless(config.weightless))));
    public static final RegistryObject<Item> INERTIA_NULL_STONE = ITEMS.register("inertia_null_stone",
            () -> new SimpleAccessory<>("8192af5d-98de-4c1e-a125-e99864b99634", ModElements.NEUTRAL,
                    () -> TrinketsConfig.SERVER.items.inertiaNull,
                    (config, abilities) -> abilities.add(AbilityKinetic.nullify(config.nullKinetic))));
    public static final RegistryObject<Item> GREATER_INERTIA_STONE = ITEMS.register("greater_inertia_stone",
            () -> new SimpleAccessory<>("e119ae9a-93b2-4053-ab3c-81108c16ff27", ModElements.NEUTRAL,
                    () -> TrinketsConfig.SERVER.items.greaterInertia,
                    (config, abilities) -> abilities.add(AbilityKinetic.reduce(config.reduceKinetic))));
    public static final RegistryObject<Item> GLOW_RING = ITEMS.register("glow_ring",
            () -> new SimpleAccessory<>("c7100557-afaf-4e69-b538-ef4ed550b470", ModElements.LIGHT,
                    () -> TrinketsConfig.SERVER.items.glowRing,
                    (config, abilities) -> abilities.add(new AbilityNightVision(config.nightVision))));
    public static final RegistryObject<Item> SEA_STONE = ITEMS.register("sea_stone",
            () -> new SimpleAccessory<>("6029aecd-318e-4b45-8c36-2ddd7f481e36", ModElements.WATER,
                    () -> TrinketsConfig.SERVER.items.seaStone,
                    (config, abilities) -> {
                        abilities.add(new AbilityWaterAffinity(config.waterAffinity));
                        abilities.add(new AbilityWaterImmunity(config.waterImmunity));
                        abilities.add(new AbilitySkilledSwimmer(config.skilledSwimmer));
                        if (SurvivalCompat.canAbsorbWater()) {
                            abilities.add(new AbilityWaterAbsorption(config.waterAbsorption));
                        }
                        if (ModCompat.enhancedVisuals()) {
                            abilities.add(new AbilityClearVision(AbilityNames.CLEAR_SPLASH, config.clearVision));
                        }
                    }));
    public static final RegistryObject<Item> POLARIZED_STONE = ITEMS.register("polarized_stone",
            () -> new SimpleAccessory<>("1ed98d9e-3075-45e0-b6f7-fcdff24caed4", ModElements.EARTH,
                    () -> TrinketsConfig.SERVER.items.polarizedStone,
                    (config, abilities) -> {
                        abilities.add(new AbilityMagnetic(config.magnetic));
                        abilities.add(new AbilityRepel(config.repel));
                    }));
    public static final RegistryObject<Item> DRAGONS_EYE = ITEMS.register("dragons_eye", TrinketDragonsEye::new);
    public static final RegistryObject<Item> WITHER_RING = ITEMS.register("wither_ring",
            () -> new SimpleAccessory<>("bca63279-4a19-4891-b4b0-a5a2f76e4b90", ModElements.DARK,
                    () -> TrinketsConfig.SERVER.items.witherRing,
                    (config, abilities) -> {
                        abilities.add(new AbilityAffinityDark(config.darkAffinity));
                        abilities.add(new AbilityDarkImmunity(config.darkImmunity));
                    }));
    public static final RegistryObject<Item> POISON_STONE = ITEMS.register("poison_stone",
            () -> new SimpleAccessory<>("e86e5b58-1b62-4a54-bba1-6594de844c2e", ModElements.POISON,
                    () -> TrinketsConfig.SERVER.items.poisonStone,
                    (config, abilities) -> {
                        abilities.add(new AbilityPoisonAffinity(config.poisonAffinity));
                        abilities.add(new AbilityPoisonImmunity(config.poisonImmunity));
                    }));
    public static final RegistryObject<Item> ENDER_TIARA = ITEMS.register("ender_tiara", TrinketEnderTiara::new);
    public static final RegistryObject<Item> DAMAGE_SHIELD = ITEMS.register("damage_shield",
            () -> new SimpleAccessory<>("c0885371-20dd-4c56-86eb-78f24d9fe777", ModElements.LIGHT,
                    () -> TrinketsConfig.SERVER.items.damageShield,
                    (config, abilities) -> {
                        abilities.add(new AbilitySafeGuard(config.safeGuard));
                        if (ModCompat.firstAid()) {
                            abilities.add(new AbilityHardHead(config.hardHead));
                        }
                        if (ModCompat.enhancedVisuals()) {
                            abilities.add(new AbilityClearVision(AbilityNames.CLEAR_VISION, config.clearVision));
                        }
                    }));
    public static final RegistryObject<Item> ARCING_ORB = ITEMS.register("arcing_orb",
            () -> new SimpleAccessory<>("249e65db-7dea-4825-8489-e6aa99a70be1", ModElements.LIGHTNING,
                    () -> TrinketsConfig.SERVER.items.arcingOrb,
                    (config, abilities) -> {
                        abilities.add(new AbilityLightningBolt(config.lightningBolt));
                        abilities.add(new AbilityDodge(config.dodge));
                    }));
    public static final RegistryObject<Item> FAELIS_CLAW = ITEMS.register("faelis_claw",
            () -> new SimpleAccessory<>("4959ec73-142d-4b82-bd0d-cd6cd7431611", ModElements.AIR,
                    () -> TrinketsConfig.SERVER.items.faelisClaw,
                    (config, abilities) -> abilities.add(new AbilityViciousStrike(config.viciousStrike))));

    // ── 变身戒指 ──
    public static final RegistryObject<Item> FAIRY_RING = raceRing("fairy", false);
    public static final RegistryObject<Item> DWARF_RING = raceRing("dwarf", false);
    public static final RegistryObject<Item> TITAN_RING = raceRing("titan", false);
    public static final RegistryObject<Item> GOBLIN_RING = raceRing("goblin", false);
    public static final RegistryObject<Item> ELF_RING = raceRing("elf", false);
    public static final RegistryObject<Item> FAELIS_RING = raceRing("faelis", false);
    public static final RegistryObject<Item> DRAGON_RING = raceRing("dragon", true);
    public static final RegistryObject<Item> TAURUS_RING = raceRing("taurus", false);

    public static final RegistryObject<Item> TEDDY_BEAR = ITEMS.register("teddy_bear", TrinketTeddyBear::new);
    public static final RegistryObject<Item> COSMETIC = ITEMS.register("cosmetic", TrinketCosmetic::new);

    // ── 方块物品 ──
    public static final RegistryObject<Item> MOON_ROSE = ITEMS.register("moon_rose",
            () -> new BlockItem(ModBlocks.MOON_ROSE.get(), new Item.Properties()));

    private static RegistryObject<Item> raceRing(String race, boolean elementVariants) {
        return ITEMS.register(race + "_ring", () -> new TrinketRaceBase(race, elementVariants));
    }

    private static RaceFood raceFood(int useDuration, UseAnim animation, String raceId) {
        return new RaceFood(FoodBase.props(2, 4F, true), useDuration, animation, raceId);
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private ModItems() {
    }
}
