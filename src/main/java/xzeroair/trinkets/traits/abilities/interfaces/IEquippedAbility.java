package xzeroair.trinkets.traits.abilities.interfaces;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 按盔甲部位触发。 */
public interface IEquippedAbility extends IAbilityInterface {

    default void head(ItemStack head, LivingEntity entity) {
    }

    default void chest(ItemStack chest, LivingEntity entity) {
    }

    default void legs(ItemStack legs, LivingEntity entity) {
    }

    default void feet(ItemStack feet, LivingEntity entity) {
    }
}
