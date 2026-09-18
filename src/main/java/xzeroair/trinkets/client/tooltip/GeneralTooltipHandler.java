package xzeroair.trinkets.client.tooltip;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import xzeroair.trinkets.capabilities.magic.ManaRecovery;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.init.ModRaces;
import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.server.race.FaelisAbilitiesConfig;
import xzeroair.trinkets.util.helpers.EquipmentWeights;
import xzeroair.trinkets.util.helpers.NumberText;

/**
 * 所有物品上的附加说明：法埃利斯的装备重量、可回复魔力的物品。对应 1.12 EventHandlerClient#ItemToolTipEvent。
 *
 * 移植说明：1.12 在游戏暂停界面不显示；矿物词典与护甲材质的调试行随矿物词典一并废弃，不移植。
 */
@Mod.EventBusSubscriber(modid = Reference.MODID, value = Dist.CLIENT)
public final class GeneralTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        final Player player = event.getEntity();
        if (player == null || Minecraft.getInstance().screen == null || Minecraft.getInstance().screen.isPauseScreen()) {
            return;
        }
        final ItemStack stack = event.getItemStack();
        final List<Component> tooltip = event.getToolTip();
        appendFaelisWeight(player, stack, tooltip);
        if (TrinketsConfig.SERVER.magic.manaEnabled.get()) {
            final ManaRecovery.Entry recovery = ManaRecovery.find(stack);
            if (recovery != null && recovery.amount() != 0F) {
                final String amount = NumberText.of(recovery.amount()) + (recovery.percent() ? "%" : "");
                final String key = recovery.percent() ? "xat.tooltip.mana.recovery_percent" : "xat.tooltip.mana.recovery";
                tooltip.add(Component.literal(TooltipText.translate(key, TooltipText.TEXT, amount)));
            }
        }
    }

    /** 当前是法埃利斯且开启重甲惩罚时，护甲显示重量，手持物分别显示主/副手重量 */
    private static void appendFaelisWeight(Player player, ItemStack stack, List<Component> tooltip) {
        final FaelisAbilitiesConfig config = TrinketsConfig.SERVER.races.faelis.abilities;
        final EntityProperties properties = EntityProperties.get(player);
        if (config == null || !config.heavyArmorPenalty.get() || properties == null
                || properties.getCurrentRaceCache().getRace() != ModRaces.FAELIS.get()) {
            return;
        }
        final List<? extends String> table = config.heavyArmor.get();
        if (stack.getItem() instanceof ArmorItem armor) {
            addWeight(tooltip, "xat.tooltip.weight", EquipmentWeights.weightOf(table, stack, armor.getEquipmentSlot()).orElse(0));
            return;
        }
        final double mainWeight = EquipmentWeights.weightOf(table, stack, EquipmentSlot.MAINHAND).orElse(0);
        final double offWeight = EquipmentWeights.weightOf(table, stack, EquipmentSlot.OFFHAND).orElse(0);
        if (mainWeight == offWeight) {
            addWeight(tooltip, "xat.tooltip.weight", mainWeight);
            return;
        }
        addWeight(tooltip, "xat.tooltip.weight.mainhand", mainWeight);
        addWeight(tooltip, "xat.tooltip.weight.offhand", offWeight);
    }

    /** 重量即移动速度与跳跃高度的倍率惩罚：0.09 → -9% */
    private static void addWeight(List<Component> tooltip, String key, double weight) {
        if (weight != 0) {
            final String effect = (weight > 0 ? "-" : "+") + NumberText.percent(Math.abs(weight));
            tooltip.add(Component.literal(TooltipText.translate(key, TooltipText.TEXT, NumberText.of(weight), effect)));
        }
    }

    private GeneralTooltipHandler() {
    }
}
