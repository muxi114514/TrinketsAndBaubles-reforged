package xzeroair.trinkets.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.util.Reference;

/**
 * 本模组语言文本的占位符替换。对应 1.12 util/helpers/TranslationHelper。
 *
 * 语言文本里的占位符（与 1.12 相同）：
 * <ul>
 * <li>{@code $名称:} 变量</li>
 * <li>{@code *名称:} 按键名</li>
 * <li>{@code @名称:} 嵌套语言键（相对前缀）</li>
 * <li>{@code $gui:} / {@code $item:} 行首标记：仅在对应渲染位置显示</li>
 * <li>{@code #bold:} 等：格式代码</li>
 * </ul>
 * 任一被引用的条目 enabled 为 false 时，整行返回空串（1.12 语义：按配置隐藏整行说明）。
 *
 * 移植说明：1.12 的 16 个颜色字段与已废弃的 addTextColorFromLangKey 未被使用，不移植。
 */
@OnlyIn(Dist.CLIENT)
public final class TranslationHelper {

    /** 渲染位置：与 1.12 EnumRenderLocation 的 id 一致 */
    public static final int LOCATION_NONE = 0;
    public static final int LOCATION_GUI = 1;
    public static final int LOCATION_ITEM = 2;

    public static String formatAddVariables(String text, KeyEntry... entries) {
        return formatAddVariables(text, LOCATION_NONE, entries);
    }

    public static String formatAddVariables(String text, int renderLocation, KeyEntry... entries) {
        if ((text.startsWith("$gui:") && renderLocation != LOCATION_GUI)
                || (text.startsWith("$item:") && renderLocation != LOCATION_ITEM)) {
            return "";
        }
        String result = text.replace("$gui:", "").replace("$item:", "");
        for (KeyEntry entry : entries) {
            if (!result.contains(entry.token())) {
                continue;
            }
            if (!entry.enabled()) {
                return "";
            }
            final String value = entry instanceof LangEntry ? I18n.get(entry.value()).trim() : entry.value();
            result = result.replace(entry.token(), value);
        }
        return result.replace("#underline:", ChatFormatting.UNDERLINE.toString())
                .replace("#strikethrough:", ChatFormatting.STRIKETHROUGH.toString())
                .replace("#italic:", ChatFormatting.ITALIC.toString())
                .replace("#bold:", ChatFormatting.BOLD.toString());
    }

    /** 翻译；无此键时返回空串（而不是原样返回键名） */
    public static String translate(String key) {
        if (!I18n.exists(key)) {
            return "";
        }
        final String text = I18n.get(key);
        return isBlank(text) ? "" : text;
    }

    public static String toggleText(boolean on) {
        return I18n.get(Reference.MODID + (on ? ".tooltip.on" : ".tooltip.off"));
    }

    public static String enabledText(boolean enabled) {
        return I18n.get(Reference.MODID + (enabled ? ".tooltip.enabled" : ".tooltip.disabled"));
    }

    /** 属性数值文本：operation &gt; 0 按百分比显示；正数绿色、负数红色 */
    public static String attributeValue(int operation, double amount) {
        final double shown = operation > 0 ? Math.round(amount * 100) : amount;
        final String number = (shown > 0 ? "+" : "") + (operation > 0 ? (long) shown + "%" : shown);
        return (shown > 0 ? ChatFormatting.GREEN : ChatFormatting.RED) + number;
    }

    /** 去掉格式代码与空格后是否为空 */
    public static boolean isBlank(String text) {
        return ChatFormatting.stripFormatting(text).replace(" ", "").isEmpty();
    }

    /** 替换条目：token 为语言文本中的占位符 */
    public static class KeyEntry {

        private final String token;
        private final String value;
        private final boolean enabled;

        protected KeyEntry(String token, boolean enabled, Object value) {
            this.token = token;
            this.enabled = enabled;
            this.value = String.valueOf(value);
        }

        public String token() {
            return this.token;
        }

        public String value() {
            return this.value;
        }

        public boolean enabled() {
            return this.enabled;
        }
    }

    /** {@code $名称:} 变量 */
    public static class OptionEntry extends KeyEntry {

        public OptionEntry(String name, Object value) {
            this(name, true, value);
        }

        public OptionEntry(String name, boolean enabled, Object value) {
            super("$" + name + ":", enabled, value);
        }
    }

    /** {@code *名称:} 按键名 */
    public static class KeyBindEntry extends KeyEntry {

        public KeyBindEntry(String name, String keyName) {
            this(name, true, keyName);
        }

        public KeyBindEntry(String name, boolean enabled, String keyName) {
            super("*" + name + ":", enabled, keyName);
        }
    }

    /** {@code @名称:} 嵌套语言键，替换为 前缀.名称 的译文 */
    public static class LangEntry extends KeyEntry {

        public LangEntry(String prefix, String name) {
            this(prefix, name, true);
        }

        public LangEntry(String prefix, String name, boolean enabled) {
            super("@" + name + ":", enabled, prefix + "." + name);
        }
    }

    private TranslationHelper() {
    }
}
