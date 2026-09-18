package xzeroair.trinkets.traits.abilities.interfaces;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 物品使用过程（开始 / 每 tick / 中止 / 完成）。 */
public interface IItemUseAbility extends IAbilityInterface {

    default int onItemStartUse(LivingEntity entity, ItemStack stack, int duration) {
        return duration;
    }

    default int onItemUseTick(LivingEntity entity, ItemStack stack, int duration) {
        return duration;
    }

    default void onItemUseStop(LivingEntity entity, ItemStack stack, int duration) {
    }

    default void onItemUseFinish(LivingEntity entity, ItemStack stack, int duration) {
    }
}
