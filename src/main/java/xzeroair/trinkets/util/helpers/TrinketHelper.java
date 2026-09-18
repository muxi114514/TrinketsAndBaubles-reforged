package xzeroair.trinkets.util.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

/**
 * 饰品栏查询（Curios）。对应 1.12 util/helpers/TrinketHelper 的 countAccessories / getAccessory 等。
 *
 * 移植说明：1.12 同时兼容自带饰品栏与 Baubles；1.20.1 以 Curios 为硬前置，自带饰品栏不再移植。
 */
public final class TrinketHelper {

    /** 所有已佩戴且满足条件的饰品（按 Curios 槽位顺序） */
    public static List<ItemStack> findEquipped(@Nullable LivingEntity entity, Predicate<ItemStack> filter) {
        final List<ItemStack> stacks = new ArrayList<>();
        if (entity == null) {
            return stacks;
        }
        CuriosApi.getCuriosInventory(entity).ifPresent(inventory -> {
            for (SlotResult result : inventory.findCurios(filter)) {
                stacks.add(result.stack());
            }
        });
        return stacks;
    }

    /** 第一个满足条件的已佩戴饰品，没有则返回空堆 */
    public static ItemStack getEquipped(@Nullable LivingEntity entity, Predicate<ItemStack> filter) {
        if (entity == null) {
            return ItemStack.EMPTY;
        }
        return CuriosApi.getCuriosInventory(entity).resolve()
                .flatMap(inventory -> inventory.findFirstCurio(filter))
                .map(SlotResult::stack)
                .orElse(ItemStack.EMPTY);
    }

    public static boolean isEquipped(@Nullable LivingEntity entity, Predicate<ItemStack> filter) {
        return !getEquipped(entity, filter).isEmpty();
    }

    private TrinketHelper() {
    }
}
