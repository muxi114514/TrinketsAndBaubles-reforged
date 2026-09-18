package xzeroair.trinkets.util.recipes;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.traits.elements.ItemElements;
import xzeroair.trinkets.util.Reference;

/**
 * 按「物品 + 主元素」匹配的配料：没有元素 NBT 的物品视为中性（龙之眼、龙宝石、巨龙之戒的元素变种配方用）。
 *
 * 移植说明：1.12 的 xat:nbt 配料对饰品只比较元素（与存储经验），对其他物品比较完整 NBT。
 * 1.20.1 中非饰品的 NBT 匹配由 Forge 自带的 forge:partial_nbt 承担（如冰与火龙头骨的 Stage），
 * 这里只保留「元素相等」这一 Forge 无法表达的规则。0.33.4 的饰品从不存经验，故不比较经验。
 */
public class ElementIngredient extends AbstractIngredient {

    public static final ResourceLocation ID = new ResourceLocation(Reference.MODID, "element");

    private final Item item;
    private final ResourceLocation element;

    public ElementIngredient(Item item, ResourceLocation element) {
        super(Stream.of(new Ingredient.ItemValue(displayStack(item, element))));
        this.item = item;
        this.element = element;
    }

    /** 展示用的栈（JEI 等），中性不写 NBT */
    private static ItemStack displayStack(Item item, ResourceLocation element) {
        final ItemStack stack = new ItemStack(item);
        final Element resolved = ModElements.registry().getValue(element);
        if (resolved != null && !resolved.isNone()) {
            ItemElements.setPrimary(stack, resolved);
        }
        return stack;
    }

    @Override
    public boolean test(@Nullable ItemStack input) {
        if (input == null || !input.is(this.item)) {
            return false;
        }
        final ResourceLocation actual = ModElements.registry().getKey(ItemElements.getPrimary(input, ModElements.NEUTRAL.get()));
        return this.element.equals(actual);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Nonnull
    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Nonnull
    @Override
    public JsonElement toJson() {
        final JsonObject json = new JsonObject();
        json.addProperty("type", ID.toString());
        json.addProperty("item", String.valueOf(ForgeRegistries.ITEMS.getKey(this.item)));
        json.addProperty("element", this.element.toString());
        return json;
    }

    public static class Serializer implements IIngredientSerializer<ElementIngredient> {

        public static final Serializer INSTANCE = new Serializer();

        @Nonnull
        @Override
        public ElementIngredient parse(@Nonnull JsonObject json) {
            final Item item = CraftingHelper.getItem(GsonHelper.getAsString(json, "item"), true);
            final ResourceLocation element = ResourceLocation.tryParse(GsonHelper.getAsString(json, "element"));
            if (element == null) {
                throw new JsonSyntaxException("Invalid element id in " + json);
            }
            return new ElementIngredient(item, element);
        }

        @Nonnull
        @Override
        public ElementIngredient parse(FriendlyByteBuf buffer) {
            return new ElementIngredient(buffer.readRegistryIdUnsafe(ForgeRegistries.ITEMS), buffer.readResourceLocation());
        }

        @Override
        public void write(FriendlyByteBuf buffer, ElementIngredient ingredient) {
            buffer.writeRegistryIdUnsafe(ForgeRegistries.ITEMS, ingredient.item);
            buffer.writeResourceLocation(ingredient.element);
        }
    }
}
