package xzeroair.trinkets.init;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import xzeroair.trinkets.Trinkets;
import xzeroair.trinkets.items.potions.PotionMixRecipe;
import xzeroair.trinkets.items.potions.TransformationEffect;
import xzeroair.trinkets.races.EntityRace;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 酿造药水体系：闪耀/辉光/发光三级基底、冰雷抗性、12 种变身药水，以及它们的酿造配方。
 * 对应 1.12 init/ModPotionTypes 与 items/potions/PotionObject。
 *
 * 酿造链（与 1.12 一致）：粗制/尴尬/浓稠 + 发光粉 → 闪耀；闪耀 + 发光锭 → 辉光；辉光 + 发光宝石 → 发光；
 * 各变身/抗性药水由对应基底 + 催化剂（COMMON 配置）酿出；有时长的药水 + 红石 → 延长版（1.12 为 3 倍，抗性为 8/3 倍）。
 *
 * 移植说明：药水翻译键沿用 1.12 的「xat.名称」（Potion 构造参数中的基础名），延长版共用同一基础名。
 */
public class ModPotions {

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Reference.MODID);
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, Reference.MODID);

    private static final int BASE_DURATION = 0;
    private static final int RESISTANCE_DURATION = 3600;
    private static final int HUMAN_DURATION = 3600;
    private static final int RACE_DURATION = 1200;

    // ── 基底 ──
    public static final RegistryObject<MobEffect> SPARKLING_EFFECT = marker("sparkling", 16777160);
    public static final RegistryObject<MobEffect> GLITTERING_EFFECT = marker("glittering", 16777120);
    public static final RegistryObject<MobEffect> GLOWING_EFFECT = marker("glowing", 16777080);
    public static final RegistryObject<Potion> SPARKLING = potion("sparkling", SPARKLING_EFFECT, BASE_DURATION);
    public static final RegistryObject<Potion> GLITTERING = potion("glittering", GLITTERING_EFFECT, BASE_DURATION);
    public static final RegistryObject<Potion> GLOWING = potion("glowing", GLOWING_EFFECT, BASE_DURATION);

    // ── 抗性（效果本体在 ModEffects） ──
    public static final RegistryObject<Potion> ICE_RESISTANCE = potion("ice_resistance", ModEffects.ICE_RESISTANCE, RESISTANCE_DURATION);
    public static final RegistryObject<Potion> LONG_ICE_RESISTANCE = potion("extended_ice_resistance", "ice_resistance", ModEffects.ICE_RESISTANCE, RESISTANCE_DURATION * 8 / 3);
    public static final RegistryObject<Potion> LIGHTNING_RESISTANCE = potion("lightning_resistance", ModEffects.LIGHTNING_RESISTANCE, RESISTANCE_DURATION);
    public static final RegistryObject<Potion> LONG_LIGHTNING_RESISTANCE = potion("extended_lightning_resistance", "lightning_resistance", ModEffects.LIGHTNING_RESISTANCE, RESISTANCE_DURATION * 8 / 3);

    // ── 变身 ──
    public static final RacePotion HUMAN = race("human", ModRaces.HUMAN, ModElements.NEUTRAL, HUMAN_DURATION);
    public static final RacePotion FAIRY = race("fairy", ModRaces.FAIRY, ModElements.NEUTRAL, RACE_DURATION);
    public static final RacePotion DWARF = race("dwarf", ModRaces.DWARF, ModElements.NEUTRAL, RACE_DURATION);
    public static final RacePotion TITAN = race("titan", ModRaces.TITAN, ModElements.NEUTRAL, RACE_DURATION);
    public static final RacePotion GOBLIN = race("goblin", ModRaces.GOBLIN, ModElements.NEUTRAL, RACE_DURATION);
    public static final RacePotion ELF = race("elf", ModRaces.ELF, ModElements.NEUTRAL, RACE_DURATION);
    public static final RacePotion FAELIS = race("faelis", ModRaces.FAELIS, ModElements.NEUTRAL, RACE_DURATION);
    public static final RacePotion DRAGON = race("dragon", ModRaces.DRAGON, ModElements.NEUTRAL, RACE_DURATION);
    public static final RacePotion DRAGON_FIRE = race("dragon_fire", ModRaces.DRAGON, ModElements.FIRE, RACE_DURATION);
    public static final RacePotion DRAGON_ICE = race("dragon_ice", ModRaces.DRAGON, ModElements.ICE, RACE_DURATION);
    public static final RacePotion DRAGON_LIGHTNING = race("dragon_lightning", ModRaces.DRAGON, ModElements.LIGHTNING, RACE_DURATION);
    public static final RacePotion TAURUS = race("taurus", ModRaces.TAURUS, ModElements.NEUTRAL, RACE_DURATION);

    /** 一种变身药水的效果与普通/延长两个药水条目 */
    public record RacePotion(String name, RegistryObject<MobEffect> effect, RegistryObject<Potion> normal, RegistryObject<Potion> extended) {
    }

    private static RegistryObject<MobEffect> marker(String name, int color) {
        return EFFECTS.register(name, () -> new MarkerEffect(color));
    }

    private static RegistryObject<Potion> potion(String name, Supplier<MobEffect> effect, int duration) {
        return potion(name, name, effect, duration);
    }

    private static RegistryObject<Potion> potion(String id, String baseName, Supplier<MobEffect> effect, int duration) {
        return POTIONS.register(id, () -> new Potion(Reference.MODID + "." + baseName, new MobEffectInstance(effect.get(), duration)));
    }

    private static RacePotion race(String name, Supplier<EntityRace> race, Supplier<Element> element, int duration) {
        final RegistryObject<MobEffect> effect = EFFECTS.register(name,
                () -> new TransformationEffect(race, element, Reference.MODID + ":" + name, duration));
        return new RacePotion(name, effect, potion(name, effect, duration), potion("extended_" + name, name, effect, duration * 3));
    }

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
        POTIONS.register(modEventBus);
    }

    /** 在 commonSetup 的 enqueueWork 中调用（酿造注册表非线程安全；此时 COMMON 配置已加载） */
    public static void registerBrewing() {
        final Ingredient powder = Ingredient.of(ModItems.GLOWING_POWDER.get());
        for (Supplier<Potion> base : List.<Supplier<Potion>>of(() -> Potions.MUNDANE, () -> Potions.AWKWARD, () -> Potions.THICK)) {
            mix(base, powder, SPARKLING);
        }
        mix(SPARKLING, Ingredient.of(ModItems.GLOWING_INGOT.get()), GLITTERING);
        mix(GLITTERING, Ingredient.of(ModItems.GLOWING_GEM.get()), GLOWING);
        withExtended(GLOWING, catalyst("ice_resistance"), ICE_RESISTANCE, LONG_ICE_RESISTANCE);
        withExtended(GLOWING, catalyst("lightning_resistance"), LIGHTNING_RESISTANCE, LONG_LIGHTNING_RESISTANCE);
        race(SPARKLING, HUMAN);
        race(GLOWING, FAIRY);
        race(GLITTERING, DWARF);
        race(GLOWING, TITAN);
        race(SPARKLING, GOBLIN);
        race(GLITTERING, ELF);
        race(GLITTERING, FAELIS);
        race(GLOWING, DRAGON);
        race(DRAGON.normal(), DRAGON_FIRE);
        race(DRAGON.normal(), DRAGON_ICE);
        race(DRAGON.normal(), DRAGON_LIGHTNING);
        race(GLITTERING, TAURUS);
    }

    private static void race(Supplier<Potion> base, RacePotion potion) {
        withExtended(base, catalyst(potion.name()), potion.normal(), potion.extended());
    }

    private static void withExtended(Supplier<Potion> base, Ingredient catalyst, Supplier<Potion> normal, Supplier<Potion> extended) {
        mix(base, catalyst, normal);
        mix(normal, Ingredient.of(Items.REDSTONE), extended);
    }

    private static void mix(Supplier<Potion> from, Ingredient ingredient, Supplier<Potion> to) {
        if (!ingredient.isEmpty()) {
            BrewingRecipeRegistry.addRecipe(new PotionMixRecipe(from, ingredient, to));
        }
    }

    /** 解析催化剂配置：物品 id 或 #物品标签；无效条目记日志并跳过该配方 */
    private static Ingredient catalyst(String potionName) {
        final String raw = TrinketsConfig.COMMON.potions.catalyst(potionName);
        if (raw == null || raw.isBlank()) {
            return Ingredient.EMPTY;
        }
        final String id = raw.trim();
        if (id.startsWith("#")) {
            final ResourceLocation tag = ResourceLocation.tryParse(id.substring(1));
            return tag == null ? Ingredient.EMPTY : Ingredient.of(ItemTags.create(tag));
        }
        final ResourceLocation location = ResourceLocation.tryParse(id);
        final Item item = location == null ? null : ForgeRegistries.ITEMS.getValue(location);
        if (item == null || item == Items.AIR) {
            Trinkets.LOGGER.warn("Invalid brewing catalyst for {}: {}", potionName, raw);
            return Ingredient.EMPTY;
        }
        return Ingredient.of(item);
    }

    /** 基底药水的占位效果：只负责颜色与名称，无实际作用（1.12 时长即为 0） */
    private static final class MarkerEffect extends MobEffect {

        private MarkerEffect(int color) {
            super(MobEffectCategory.NEUTRAL, color);
        }
    }

    private ModPotions() {
    }
}
