package xzeroair.trinkets.traits.elements;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import xzeroair.trinkets.init.ModElements;

/**
 * 物品堆上的元素存取：`{Elements:{primary:"xat:fire", secondary:"xat:neutral"}}`。
 *
 * 移植说明：1.12 经物品 capability（TrinketProperties → ItemElementalAttributes）保存，并靠 getNBTShareTag 同步；
 * 其序列化格式正是上面这段 NBT（龙食物等配方也按此匹配）。1.20.1 直接写物品 NBT，天然随物品同步，无需 capability。
 */
public final class ItemElements {

    private static final String ELEMENTS_TAG = "Elements";
    private static final String PRIMARY_TAG = "primary";
    private static final String SECONDARY_TAG = "secondary";

    public static Element getPrimary(ItemStack stack, Element fallback) {
        return read(stack, PRIMARY_TAG, fallback);
    }

    public static Element getSecondary(ItemStack stack, Element fallback) {
        return read(stack, SECONDARY_TAG, fallback);
    }

    public static void setPrimary(ItemStack stack, Element element) {
        write(stack, PRIMARY_TAG, element);
    }

    public static void setSecondary(ItemStack stack, Element element) {
        write(stack, SECONDARY_TAG, element);
    }

    private static Element read(ItemStack stack, String key, Element fallback) {
        final CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ELEMENTS_TAG, Tag.TAG_COMPOUND)) {
            return fallback;
        }
        final ResourceLocation id = ResourceLocation.tryParse(tag.getCompound(ELEMENTS_TAG).getString(key));
        final Element element = id == null ? null : ModElements.registry().getValue(id);
        return element == null ? fallback : element;
    }

    private static void write(ItemStack stack, String key, Element element) {
        final ResourceLocation id = ModElements.registry().getKey(element);
        if (id != null) {
            stack.getOrCreateTagElement(ELEMENTS_TAG).putString(key, id.toString());
        }
    }

    private ItemElements() {
    }
}
