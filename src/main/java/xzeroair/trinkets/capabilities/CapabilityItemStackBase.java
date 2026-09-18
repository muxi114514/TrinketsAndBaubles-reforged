package xzeroair.trinkets.capabilities;

import net.minecraft.world.item.ItemStack;

/** 物品栈能力基类（饰品自身数据用）。 */
public abstract class CapabilityItemStackBase<T extends CapabilityItemStackBase<T>> extends CapabilityBase<T, ItemStack> {

    protected CapabilityItemStackBase(ItemStack stack) {
        super(stack);
    }

    public ItemStack getStack() {
        return this.getObject();
    }
}
