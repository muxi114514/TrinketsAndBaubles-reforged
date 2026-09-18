package xzeroair.trinkets.client.tooltip;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.util.helpers.NumberText;
import xzeroair.trinkets.util.helpers.PotionHelper;

/**
 * 效果配置条目（[modid:]effect[:duration[:amplifier]]）的显示文字：名称 + 罗马数字等级 [+ 持续秒数]。
 */
@OnlyIn(Dist.CLIENT)
public final class EffectText {

    private static final int MAX_ROMAN_LEVEL = 10;

    /** 未注册的效果（对应模组未安装）跳过 */
    public static String join(List<String> configs, boolean withDuration, String base) {
        return configs.stream().map(PotionHelper::parse).filter(Objects::nonNull)
                .map(parsed -> TooltipText.VALUE + of(parsed, withDuration) + base)
                .collect(Collectors.joining(TooltipText.separator()));
    }

    public static String of(PotionHelper.ParsedEffect parsed, boolean withDuration) {
        final String text = parsed.effect().getDisplayName().getString() + " " + level(parsed.amplifier() + 1);
        return withDuration ? text + " " + I18n.get("xat.tooltip.seconds", NumberText.seconds(parsed.duration())) : text;
    }

    /** 原版只内置 I~X 的等级译名，更高等级直接显示数字 */
    public static String level(int level) {
        return level >= 1 && level <= MAX_ROMAN_LEVEL ? I18n.get("enchantment.level." + level) : String.valueOf(level);
    }

    private EffectText() {
    }
}
