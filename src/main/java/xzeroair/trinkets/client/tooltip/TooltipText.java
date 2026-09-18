package xzeroair.trinkets.client.tooltip;

import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.client.keybinds.ModKeyMappings;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.DescriptionVariables.Kind;
import xzeroair.trinkets.traits.abilities.DescriptionVariables.Variable;

/**
 * 说明文本的统一配色与占位符替换。
 *
 * 为什么在代码里配色：1.12 的语言文件每行自带 § 颜色，各语言、各行配色不一，数值也常与正文同色。
 * 这里先去掉语言文本自带的格式码，再按角色统一着色（正文灰、数值青、按键黄、名称金），语言文件只需写纯文字。
 */
@OnlyIn(Dist.CLIENT)
public final class TooltipText {

    public static final String TEXT = ChatFormatting.GRAY.toString();
    public static final String VALUE = ChatFormatting.AQUA.toString();
    public static final String KEY = ChatFormatting.YELLOW.toString();
    public static final String NAME = ChatFormatting.GOLD.toString();
    public static final String SECTION = ChatFormatting.DARK_AQUA.toString();
    public static final String STATUS = ChatFormatting.DARK_GREEN.toString();
    public static final String MUTED = ChatFormatting.DARK_GRAY.toString();
    public static final String GOOD = ChatFormatting.GREEN.toString();
    public static final String BAD = ChatFormatting.RED.toString();

    /** 能力名称行与说明行的缩进 */
    public static final String NAME_INDENT = " ";
    public static final String DETAIL_INDENT = "   ";

    /**
     * 替换占位符并着色；任一被引用的变量 enabled 为 false 时返回空串（按配置隐藏整行）。
     *
     * @param base 正文颜色，数值等高亮片段结束后恢复为该颜色
     */
    public static String format(String template, String base, List<Variable> variables) {
        String text = strip(template).replace("$gui:", "").replace("$item:", "");
        // 嵌套语言键先展开，展开出的占位符随后统一替换
        for (Variable variable : variables) {
            if (variable.kind() == Kind.LANG) {
                final String token = "@" + variable.name() + ":";
                if (text.contains(token)) {
                    if (!variable.enabled()) {
                        return "";
                    }
                    text = text.replace(token, strip(I18n.get(variable.value())));
                }
            }
        }
        for (Variable variable : variables) {
            if (variable.kind() == Kind.LANG) {
                continue;
            }
            final String token = (variable.kind() == Kind.KEYBIND ? "*" : "$") + variable.name() + ":";
            if (text.contains(token)) {
                if (!variable.enabled()) {
                    return "";
                }
                text = text.replace(token, value(variable, base));
            }
        }
        return isBlank(text) ? "" : base + text;
    }

    /** 翻译带 %s 参数的语言键，参数按数值高亮 */
    public static String translate(String key, String base, Object... args) {
        final Object[] styled = Arrays.stream(args).map(arg -> VALUE + arg + base).toArray();
        // 只去掉模板自带的格式码，保留参数的高亮
        final String template = strip(Language.getInstance().getOrDefault(key));
        try {
            return base + String.format(template, styled);
        } catch (IllegalFormatException e) {
            return base + template;
        }
    }

    public static String toggle(boolean on) {
        return (on ? GOOD : BAD) + I18n.get(on ? "xat.tooltip.state.on" : "xat.tooltip.state.off");
    }

    public static String keyName(String name) {
        final KeyMapping mapping = ModKeyMappings.byName(name);
        return mapping == null ? name : mapping.getTranslatedKeyMessage().getString();
    }

    /** 列表分隔符（中文顿号 / 英文逗号） */
    public static String separator() {
        return I18n.get("xat.tooltip.separator");
    }

    public static String strip(String text) {
        final String stripped = ChatFormatting.stripFormatting(text);
        return stripped == null ? "" : stripped;
    }

    public static boolean isBlank(String text) {
        return strip(text).isBlank();
    }

    private static String value(Variable variable, String base) {
        return switch (variable.kind()) {
            case OPTION -> variable.value().isEmpty() ? "" : VALUE + variable.value() + base;
            case TRANSLATED -> VALUE + strip(I18n.get(variable.value())) + base;
            case TOGGLE -> toggle(Boolean.parseBoolean(variable.value())) + base;
            case EFFECTS -> EffectText.join(split(variable.value()), false, base);
            case TIMED_EFFECTS -> EffectText.join(split(variable.value()), true, base);
            case ENTITIES -> entities(split(variable.value()), base);
            case ITEMS -> items(split(variable.value()), base);
            case KEYBIND -> KEY + "[" + keyName(variable.value()) + "]" + base;
            case LANG -> "";
        };
    }

    private static List<String> split(String value) {
        return value.isEmpty() ? List.of() : List.of(value.split(DescriptionVariables.LIST_SEPARATOR));
    }

    public static String entities(List<String> ids, String base) {
        return ids.stream().map(id -> VALUE + entityName(id.trim()) + base).collect(Collectors.joining(separator()));
    }

    public static String items(List<String> ids, String base) {
        return ids.stream().map(id -> VALUE + itemName(id.split(";")[0].trim()) + base).collect(Collectors.joining(separator()));
    }

    private static String itemName(String id) {
        final ResourceLocation location = ResourceLocation.tryParse(id);
        final Item item = location == null ? null : ForgeRegistries.ITEMS.getValue(location);
        return item == null || item == Items.AIR ? id : item.getDescription().getString();
    }

    private static String entityName(String id) {
        if (id.endsWith(":*")) {
            return I18n.get("xat.tooltip.entities.mod", id.substring(0, id.length() - 2));
        }
        final ResourceLocation location = ResourceLocation.tryParse(id);
        final EntityType<?> type = location == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(location);
        return type == null || !location.equals(ForgeRegistries.ENTITY_TYPES.getKey(type)) ? id : type.getDescription().getString();
    }

    private TooltipText() {
    }
}
