package xzeroair.trinkets.traits.abilities.interfaces;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 物品在背包中每 tick 触发。 */
public interface ITickableInventoryAbility extends IContainerAbility {

    default void onUpdate(ItemStack stack, Level level, Entity entity, int itemSlot, boolean inHand) {
    }
}
