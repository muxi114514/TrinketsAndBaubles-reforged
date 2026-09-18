package xzeroair.trinkets.items.base;

import javax.annotation.Nonnull;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/**
 * 食物基类。
 *
 * 移植说明：1.12 的 ItemFood 把营养/饱和/是否总能吃写在构造参数上，1.20.1 改为
 * Item.Properties.food(FoodProperties)；而「进食时长」与「进食动作」不属于 FoodProperties，
 * 仍需在物品上 override（对应 1.12 的 getMaxItemUseDuration / getItemUseAction）。
 */
public class FoodBase extends ItemBase {

    private final int useDuration;
    private final UseAnim useAnimation;

    public FoodBase(Properties properties, int useDuration, UseAnim useAnimation) {
        super(properties);
        this.useDuration = useDuration;
        this.useAnimation = useAnimation;
    }

    @Override
    public int getUseDuration(@Nonnull ItemStack stack) {
        return this.useDuration;
    }

    @Nonnull
    @Override
    public UseAnim getUseAnimation(@Nonnull ItemStack stack) {
        return this.useAnimation;
    }

    /** 1.12 各食物通用的营养配置：2 点饥饿、4F 饱和度、饱腹时也能吃 */
    public static FoodProperties food(int nutrition, float saturation, boolean alwaysEat) {
        final FoodProperties.Builder builder = new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationMod(saturation);
        if (alwaysEat) {
            builder.alwaysEat();
        }
        return builder.build();
    }

    public static Item.Properties props(int nutrition, float saturation, boolean alwaysEat) {
        return new Item.Properties().food(food(nutrition, saturation, alwaysEat));
    }
}
