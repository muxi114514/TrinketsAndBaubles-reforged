package xzeroair.trinkets.items.potions;

import java.util.function.Supplier;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.IBrewingRecipe;

/**
 * 药水 → 药水的酿造配方，对普通/喷溅/滞留三种瓶都生效。对应 1.12 PotionHelper.addMix。
 *
 * 移植说明：1.20.1 原版 PotionBrewing.addMix 是私有的；Forge 自带的 BrewingRecipe 只按物品匹配、
 * 分辨不出瓶里是哪种药水，故自实现 IBrewingRecipe 按瓶中药水判定。
 */
public class PotionMixRecipe implements IBrewingRecipe {

    private final Supplier<Potion> from;
    private final Ingredient ingredient;
    private final Supplier<Potion> to;

    public PotionMixRecipe(Supplier<Potion> from, Ingredient ingredient, Supplier<Potion> to) {
        this.from = from;
        this.ingredient = ingredient;
        this.to = to;
    }

    @Override
    public boolean isInput(ItemStack input) {
        return isPotionContainer(input) && PotionUtils.getPotion(input) == this.from.get();
    }

    @Override
    public boolean isIngredient(ItemStack ingredient) {
        return this.ingredient.test(ingredient);
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        if (!this.isInput(input) || !this.isIngredient(ingredient)) {
            return ItemStack.EMPTY;
        }
        return PotionUtils.setPotion(new ItemStack(input.getItem()), this.to.get());
    }

    private static boolean isPotionContainer(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }
}
