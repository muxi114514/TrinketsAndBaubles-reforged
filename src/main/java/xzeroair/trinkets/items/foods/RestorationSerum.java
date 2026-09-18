package xzeroair.trinkets.items.foods;

import javax.annotation.Nonnull;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.items.base.FoodBase;

/**
 * 恢复血清：清除物品附着的种族（若本就没有附着种族，连出生种族也一并清除）、魔力回满，配置开启时附带中毒与虚弱，
 * 喝完返还玻璃瓶。对应 1.12 items/foods/Restore_Item。
 *
 * 移植说明：
 * - 1.12 手工调用 FoodStats.addStats、播放打嗝声、记统计与触发进度；1.20.1 的 super.finishUsingItem
 *   会经 LivingEntity#eat 完成这些，无需重复。
 * - 清除种族缓存后，「当前种族」要等种族解析逻辑（1.12 EntityProperties#updateRace，随 P3c 移植）重新计算才会回落。
 */
public class RestorationSerum extends FoodBase {

    public static final int POISON_TICKS = 400;

    public RestorationSerum() {
        super(FoodBase.props(0, 0F, true), 32, UseAnim.DRINK);
    }

    @Nonnull
    @Override
    public ItemStack finishUsingItem(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity entity) {
        final ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide) {
            final EntityProperties properties = EntityProperties.get(entity);
            if (properties != null) {
                if (properties.getImbuedRaceCache().getRace().isNone()) {
                    properties.setOriginalRaceCache(null);
                }
                properties.setImbuedRaceCache(null);
            }
            final MagicStats magic = MagicStats.get(entity);
            if (magic != null) {
                magic.refillMana();
            }
            ManaReagent.inflictHarm(entity, POISON_TICKS);
        }
        if (result.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            final ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        }
        return result;
    }
}
