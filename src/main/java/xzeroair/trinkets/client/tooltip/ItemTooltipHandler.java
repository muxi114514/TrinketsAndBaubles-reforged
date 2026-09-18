package xzeroair.trinkets.client.tooltip;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.capabilities.trinket.TrinketProperties;
import xzeroair.trinkets.items.base.AccessoryBase;
import xzeroair.trinkets.items.base.RaceFood;
import xzeroair.trinkets.items.trinkets.TrinketRaceBase;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 本模组物品的说明文本（客户端，Forge 事件总线）。对应 1.12 ItemBase / FoodBase / AccessoryBase 的 addInformation。
 *
 * 结构：物品自身说明与元素 → 变身后（种族） → 被动 / 主动 / 联动能力 → 佩戴时（属性、效果、伤害规则）。
 * 折叠模式（默认）只列能力名与简短数值，按住 Shift 展开每个能力的具体数值与实时状态。
 *
 * 移植说明：1.12 各物品类直接覆写 addInformation 调用客户端工具；这里集中到 tooltip 事件，物品类保持两端通用。
 */
@Mod.EventBusSubscriber(modid = Reference.MODID, value = Dist.CLIENT)
public final class ItemTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        final ItemStack stack = event.getItemStack();
        final ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !Reference.MODID.equals(id.getNamespace())) {
            return;
        }
        final Player player = event.getEntity();
        final List<Component> lines = new ArrayList<>();
        ItemDescriptions.append(stack, id, player, lines);

        final TooltipSections sections = new TooltipSections();
        if (stack.getItem() instanceof TrinketRaceBase ring) {
            RaceTooltips.append(sections, ring.getRace(), ring.getPrimaryElement(stack), player);
        } else if (stack.getItem() instanceof RaceFood food) {
            RaceTooltips.append(sections, food.getRace(), food.getPrimaryElement(stack), player);
        } else if (stack.getItem() instanceof AccessoryBase accessory) {
            final TrinketProperties properties = TrinketProperties.get(stack);
            if (properties != null) {
                final Element element = accessory.getPrimaryElement(stack);
                AbilityTooltips.append(sections, properties.getAbilities(), id.toString(), stack, player, element);
            }
        }
        if (stack.getItem() instanceof AccessoryBase accessory) {
            appendWorn(sections, accessory, stack);
        }
        final boolean collapsed = TrinketsConfig.CLIENT.tooltip.collapsed.get() && !Screen.hasShiftDown();
        sections.render(lines, collapsed);
        event.getToolTip().addAll(Math.min(1, event.getToolTip().size()), lines);
    }

    /** 佩戴时：属性修饰、获得 / 免疫的效果、伤害类型规则（均为简短数值，折叠时也显示） */
    private static void appendWorn(TooltipSections sections, AccessoryBase accessory, ItemStack stack) {
        final String title = I18n.get("xat.tooltip.section.worn");
        sections.addSummary(title, ConfigLines.attributes(accessory.getAttributeConfig(stack)));
        sections.addSummary(title, ConfigLines.effects(accessory.getEffectsToAdd(stack), accessory.getEffectsToRemove(stack)));
        sections.addSummary(title, ConfigLines.damageRules(accessory.getDamageTypesToIgnore(stack)));
    }

    private ItemTooltipHandler() {
    }
}
