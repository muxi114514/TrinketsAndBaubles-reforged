package xzeroair.trinkets.client.tooltip;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.ManaCost;
import xzeroair.trinkets.traits.abilities.interfaces.IAbilityInterface;
import xzeroair.trinkets.traits.abilities.interfaces.IKeyBindInterface;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.helpers.NumberText;

/**
 * 单个能力的说明：标题行（名称、按键、法力消耗）、说明行（能力键.tooltipN）、实时状态行（能力键.statusN）。
 * 物品说明、能力界面与种族选择界面共用。
 */
@OnlyIn(Dist.CLIENT)
public final class AbilityLines {

    private static final int MAX_LINES = 10;

    /** 去掉旧语言文件格式码后的名称 */
    public static String name(IAbilityInterface ability) {
        return TooltipText.strip(ability.getDisplayName().getString());
    }

    /**
     * 标题行：名称 [按键] · 法力消耗；法力消耗按当前法力是否足够着绿/红色。已禁用的能力整行变灰并注明。
     */
    public static Component header(IAbilityInterface ability, boolean disabled, @Nullable MagicStats magic) {
        if (disabled) {
            return Component.literal(TooltipText.NAME_INDENT + TooltipText.MUTED + name(ability) + " "
                    + I18n.get("xat.tooltip.ability.disabled"));
        }
        final StringBuilder text = new StringBuilder(TooltipText.NAME_INDENT).append(TooltipText.NAME).append(name(ability));
        if (ability instanceof IKeyBindInterface keyBind && !keyBind.getKey().isEmpty()) {
            text.append(' ').append(TooltipText.KEY).append('[').append(TooltipText.keyName(keyBind.getKey())).append(']');
        }
        final String mana = manaText(ability.getManaCost(), magic);
        if (!mana.isEmpty()) {
            text.append(TooltipText.MUTED).append(" · ").append(mana);
        }
        return Component.literal(text.toString());
    }

    public static void appendDetails(IAbilityInterface ability, List<Component> lines) {
        final DescriptionVariables variables = new DescriptionVariables();
        ability.describe(variables);
        appendKeyLines(ability.getTranslationKey() + ".tooltip", TooltipText.TEXT, TooltipText.DETAIL_INDENT, variables, lines);
    }

    public static void appendStatus(IAbilityInterface ability, ItemStack source, List<Component> lines) {
        final DescriptionVariables variables = new DescriptionVariables();
        ability.describeStatus(variables, source);
        if (!variables.entries().isEmpty()) {
            appendKeyLines(ability.getTranslationKey() + ".status", TooltipText.STATUS, TooltipText.DETAIL_INDENT, variables, lines);
        }
    }

    /** 逐行读取「前缀1~10」并格式化，空行跳过 */
    public static void appendKeyLines(String keyPrefix, String base, String indent, DescriptionVariables variables, List<Component> lines) {
        for (int i = 1; i <= MAX_LINES; i++) {
            final String key = keyPrefix + i;
            if (!I18n.exists(key)) {
                continue;
            }
            final String text = TooltipText.format(I18n.get(key), base, variables.entries());
            if (!text.isEmpty()) {
                lines.add(Component.literal(indent + text));
            }
        }
    }

    /** 法力消耗文字；法力系统关闭或无消耗时为空 */
    private static String manaText(@Nullable ManaCost cost, @Nullable MagicStats magic) {
        if (cost == null || !TrinketsConfig.SERVER.magic.manaEnabled.get()) {
            return "";
        }
        final String amount = cost.unit() == ManaCost.Unit.MAX_PERCENT ? NumberText.percent(cost.amount()) : NumberText.of(cost.amount());
        final String color = magic == null ? TooltipText.VALUE
                : magic.getMana() >= cost.points(magic.getMaxMana()) ? TooltipText.GOOD : TooltipText.BAD;
        final String key = "xat.tooltip.mana." + cost.unit().name().toLowerCase(Locale.ROOT);
        return color + TooltipText.strip(I18n.get(key, amount));
    }

    private AbilityLines() {
    }
}
