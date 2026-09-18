package xzeroair.trinkets.client.tooltip;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.attributes.AttributeConfigParser;
import xzeroair.trinkets.attributes.AttributeEntry;

/**
 * 把属性配置条目格式化为原版样式的「+N 属性名」行（数值为 0 或属性不存在的条目跳过）。
 * 物品说明与种族属性界面共用。
 */
@OnlyIn(Dist.CLIENT)
public final class AttributeLines {

    public static List<Component> format(List<? extends String> config) {
        return AttributeConfigParser.parseAll(config).stream()
                .filter(entry -> entry.amount() != 0 && !entry.attributes().isEmpty())
                .map(AttributeLines::line)
                .toList();
    }

    private static Component line(AttributeEntry entry) {
        final Attribute attribute = entry.attributes().get(0);
        final double shown = entry.operation() == 0 ? entry.amount() : entry.amount() * 100.0D;
        final Component name = Component.translatable(attribute.getDescriptionId());
        final String value = ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(Math.abs(shown));
        final boolean positive = entry.amount() > 0;
        return Component.literal(" ").append(Component.translatable("attribute.modifier." + (positive ? "plus." : "take.") + entry.operation(),
                value, name).withStyle(positive ? ChatFormatting.BLUE : ChatFormatting.RED));
    }

    private AttributeLines() {
    }
}
