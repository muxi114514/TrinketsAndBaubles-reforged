package xzeroair.trinkets.traits.abilities.interfaces;

import javax.annotation.Nullable;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * 容器相关（仅玩家）。
 * 移植说明：1.12 Container -> 1.20.1 AbstractContainerMenu；InventoryPlayer -> Inventory。
 */
public interface IContainerAbility extends IAbilityInterface {

    /** 玩家 tick 时传入其物品栏容器 */
    default void inventoryContainer(AbstractContainerMenu inventoryContainer) {
    }

    /** 玩家打开某个容器时触发 */
    default void openContainer(@Nullable AbstractContainerMenu openContainer) {
    }

    /** 玩家 tick 时传入其物品栏 */
    default void playerInventory(Inventory inventory) {
    }
}
