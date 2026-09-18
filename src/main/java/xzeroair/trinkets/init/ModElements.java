package xzeroair.trinkets.init;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import xzeroair.trinkets.traits.elements.AirElement;
import xzeroair.trinkets.traits.elements.DarkElement;
import xzeroair.trinkets.traits.elements.EarthElement;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.traits.elements.FireElement;
import xzeroair.trinkets.traits.elements.IceElement;
import xzeroair.trinkets.traits.elements.LightElement;
import xzeroair.trinkets.traits.elements.LightningElement;
import xzeroair.trinkets.traits.elements.NeutralElement;
import xzeroair.trinkets.traits.elements.PoisonElement;
import xzeroair.trinkets.traits.elements.VoidElement;
import xzeroair.trinkets.traits.elements.WaterElement;
import xzeroair.trinkets.util.Reference;

/**
 * 元素注册表（对应 1.12 Registries#registerNewRegistry 的 elements 部分 + init/Elements）。
 *
 * 移植说明：1.12 在 RegistryEvent.NewRegistry 里手工 RegistryBuilder().setType().create()，
 * 再用静态字段在类初始化时反查；1.20.1 由 DeferredRegister.makeRegistry 一步建表，
 * 条目以 RegistryObject 延迟持有——因此元素之间的强弱引用不再受类初始化顺序制约。
 */
public class ModElements {

    public static final ResourceLocation REGISTRY_ID = new ResourceLocation(Reference.MODID, "elements");

    public static final DeferredRegister<Element> ELEMENTS = DeferredRegister.create(REGISTRY_ID, Reference.MODID);

    private static final Supplier<IForgeRegistry<Element>> REGISTRY = ELEMENTS.makeRegistry(RegistryBuilder::new);

    public static final RegistryObject<Element> NEUTRAL = ELEMENTS.register("neutral", NeutralElement::new);
    public static final RegistryObject<Element> AIR = ELEMENTS.register("air", AirElement::new);
    public static final RegistryObject<Element> DARK = ELEMENTS.register("dark", DarkElement::new);
    public static final RegistryObject<Element> EARTH = ELEMENTS.register("earth", EarthElement::new);
    public static final RegistryObject<Element> FIRE = ELEMENTS.register("fire", FireElement::new);
    public static final RegistryObject<Element> ICE = ELEMENTS.register("ice", IceElement::new);
    public static final RegistryObject<Element> LIGHT = ELEMENTS.register("light", LightElement::new);
    public static final RegistryObject<Element> LIGHTNING = ELEMENTS.register("lightning", LightningElement::new);
    public static final RegistryObject<Element> POISON = ELEMENTS.register("poison", PoisonElement::new);
    public static final RegistryObject<Element> VOID = ELEMENTS.register("void", VoidElement::new);
    public static final RegistryObject<Element> WATER = ELEMENTS.register("water", WaterElement::new);

    public static final Element[] EMPTY = new Element[0];

    public static IForgeRegistry<Element> registry() {
        return REGISTRY.get();
    }

    public static Element byName(ResourceLocation id) {
        return registry().getValue(id);
    }

    public static void register(IEventBus modEventBus) {
        ELEMENTS.register(modEventBus);
    }

    private ModElements() {
    }
}
