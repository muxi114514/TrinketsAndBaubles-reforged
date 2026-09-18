package xzeroair.trinkets.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.traits.elements.IElementProvider;
import xzeroair.trinkets.traits.elements.ItemElements;
import xzeroair.trinkets.util.Reference;

/**
 * 创造模式标签页。
 * 移植说明：1.12 的 CreativeTabs 匿名类在 1.20.1 改为注册表对象，内容物由 displayItems 回调填充。
 */
public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Reference.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("trinketstab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + Reference.MODID))
                    .icon(() -> new ItemStack(ModItems.GLOWING_GEM.get()))
                    .displayItems((params, output) -> ModItems.ITEMS.getEntries().forEach(entry -> {
                        final Item item = entry.get();
                        output.accept(item);
                        // 有元素变种的物品（龙之眼、巨龙之戒）逐个列出各元素版本
                        if (item instanceof IElementProvider provider) {
                            for (Element element : provider.getSubElements()) {
                                final ItemStack stack = new ItemStack(item);
                                ItemElements.setPrimary(stack, element);
                                output.accept(stack);
                            }
                        }
                    }))
                    .build());

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }

    private ModCreativeTabs() {
    }
}
