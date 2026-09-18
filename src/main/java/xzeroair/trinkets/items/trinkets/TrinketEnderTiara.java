package xzeroair.trinkets.items.trinkets;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.SlotContext;

import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.traits.abilities.AbilityEnderQueen;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.compat.AbilityClearVision;
import xzeroair.trinkets.util.compat.ModCompat;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.server.items.AccessoryAbilitiesConfigs.EnderCrown;
import xzeroair.trinkets.util.helpers.TrinketHelper;

/**
 * 末影王冠：饰品栏提供末影女王能力；也能戴在头盔位（仅外观与让末影人不仇视，不提供能力，与 1.12 一致）。
 * 两处不能同时各戴一顶。对应 1.12 TrinketEnderTiara。
 */
public class TrinketEnderTiara extends SimpleAccessory<EnderCrown> {

    public TrinketEnderTiara() {
        super("a45dbc1c-17e9-40b4-b6a3-09dea74355b7", ModElements.VOID, () -> TrinketsConfig.SERVER.items.enderCrown,
                (config, abilities) -> {
                    abilities.add(new AbilityEnderQueen(config.enderQueen));
                    if (ModCompat.enhancedVisuals()) {
                        abilities.add(new AbilityClearVision(AbilityNames.ENDER_EYES, config.enderEyes));
                    }
                });
    }

    @Nullable
    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.HEAD;
    }

    /** 头盔位：饰品栏里已戴着一顶时不能再戴 */
    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, Entity entity) {
        return armorType == EquipmentSlot.HEAD
                && !(entity instanceof LivingEntity living && TrinketHelper.isEquipped(living, worn -> worn.is(this)));
    }

    /** 饰品栏：头盔位已戴着一顶时不能再戴 */
    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return !slotContext.entity().getItemBySlot(EquipmentSlot.HEAD).is(this) && super.canEquip(slotContext, stack);
    }
}
