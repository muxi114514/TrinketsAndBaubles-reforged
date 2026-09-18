package xzeroair.trinkets.client.tooltip;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 配置驱动的通用条目（种族与饰品共用）：属性修饰、获得的效果、免疫的效果、伤害类型规则。
 */
@OnlyIn(Dist.CLIENT)
public final class ConfigLines {

    public static List<Component> attributes(List<? extends String> config) {
        final List<Component> lines = new ArrayList<>();
        AttributeLines.format(config).forEach(line -> lines.add(Component.literal(TooltipText.DETAIL_INDENT).append(line)));
        return lines;
    }

    public static List<Component> effects(List<? extends String> granted, List<? extends String> immune) {
        final List<Component> lines = new ArrayList<>();
        addEffectLine(lines, "xat.tooltip.effects.granted", granted);
        addEffectLine(lines, "xat.tooltip.effects.immune", immune);
        return lines;
    }

    public static List<Component> damageRules(List<? extends String> rules) {
        return DamageRuleText.describe(rules, TooltipText.TEXT).stream().map(ConfigLines::line).toList();
    }

    /** 未安装对应模组的效果不显示；全部不可用时整行省略 */
    private static void addEffectLine(List<Component> lines, String labelKey, List<? extends String> configs) {
        final String effects = EffectText.join(List.copyOf(configs), false, TooltipText.TEXT);
        if (!effects.isEmpty()) {
            lines.add(line(TooltipText.TEXT + TooltipText.strip(I18n.get(labelKey)) + effects));
        }
    }

    private static Component line(String text) {
        return Component.literal(TooltipText.DETAIL_INDENT + text);
    }

    private ConfigLines() {
    }
}
