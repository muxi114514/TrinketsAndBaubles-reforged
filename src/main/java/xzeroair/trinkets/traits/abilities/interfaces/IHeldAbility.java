package xzeroair.trinkets.traits.abilities.interfaces;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 手持物品时触发。 */
public interface IHeldAbility extends IAbilityInterface {

    default void heldMainHand(ItemStack stack, LivingEntity entity) {
    }

    default void heldOffhand(ItemStack stack, LivingEntity entity) {
    }
}
