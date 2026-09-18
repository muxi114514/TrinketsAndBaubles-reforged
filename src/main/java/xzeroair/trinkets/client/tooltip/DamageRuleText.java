package xzeroair.trinkets.client.tooltip;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.util.helpers.NumberText;

/**
 * 伤害类型规则（damageTypesToIgnore，语法见 DamageTypeRules）的显示文字：「免疫火焰伤害」「魔法伤害 ×0.5」，附带来源、上限、元素条件。
 * 阶段前缀只决定结算时机、不影响效果，文字里不区分；同义规则（如 onAttacked 与 onHurt 各写一条）合并为一行。
 */
@OnlyIn(Dist.CLIENT)
public final class DamageRuleText {

    private static final String[] STAGES = {"onAttacked:", "onHurt:", "onDamaged:"};

    public static List<String> describe(List<? extends String> rules, String base) {
        final Set<String> lines = new LinkedHashSet<>();
        for (String rule : rules) {
            final String line = describe(rule, base);
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
        return List.copyOf(lines);
    }

    private static String describe(String raw, String base) {
        String text = raw.trim();
        for (String stage : STAGES) {
            if (text.startsWith(stage)) {
                text = text.substring(stage.length());
                break;
            }
        }
        String element = null;
        final int colon = text.indexOf(':');
        if (text.startsWith("ele") && colon > 3) {
            element = text.substring(3, colon);
            text = text.substring(colon + 1);
        }
        final String[] args = text.replaceAll("([\\[\\]|,;] ?)|( {2})", " ").trim().split(" +");
        if (args.length == 0 || args[0].isEmpty()) {
            return "";
        }
        String category = null;
        String entity = null;
        float limit = 0;
        float multiplier = 0;
        for (int i = 1; i < args.length; i++) {
            final String token = args[i];
            final float number = number(token.startsWith("isMin:") ? token.substring("isMin:".length()) : token);
            if (number > 0 && limit <= 0) {
                limit = number;
            } else if (number > 0) {
                multiplier = number;
            } else if (category == null && entity == null && token.startsWith("is") && !token.startsWith("isTrue:")) {
                category = token.substring(2);
            } else {
                entity = token.startsWith("isTrue:") ? token.substring("isTrue:".length()) : token;
            }
        }
        final String subject = subject(args[0], category);
        final StringBuilder line = new StringBuilder(multiplier > 0
                ? TooltipText.translate("xat.tooltip.damage.scaled", base, subject, NumberText.of(multiplier))
                : TooltipText.translate("xat.tooltip.damage.immune", base, subject));
        if (entity != null && !"*".equals(entity)) {
            line.append(TooltipText.translate("xat.tooltip.damage.from", base, entity));
        }
        if (limit > 0) {
            line.append(TooltipText.translate("xat.tooltip.damage.below", base, NumberText.of(limit)));
        }
        if (element != null) {
            line.append(TooltipText.translate("xat.tooltip.damage.element", base, element));
        }
        return line.toString();
    }

    /** 伤害名 * 配合类别时显示类别名；否则显示伤害名本身 */
    private static String subject(String name, @Nullable String category) {
        if (category != null) {
            final String key = "xat.tooltip.damage.category." + category.toLowerCase(Locale.ROOT);
            return I18n.exists(key) ? I18n.get(key) : category;
        }
        return "*".equals(name) ? I18n.get("xat.tooltip.damage.all") : name;
    }

    private static float number(String token) {
        try {
            return Float.parseFloat(token);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private DamageRuleText() {
    }
}
