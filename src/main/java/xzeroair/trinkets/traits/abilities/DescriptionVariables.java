package xzeroair.trinkets.traits.abilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import xzeroair.trinkets.enums.ActivationMethod;
import xzeroair.trinkets.util.helpers.NumberText;

/**
 * 说明文本里占位符的取值（纯数据，两端通用）。能力在 {@code describe} 中填写，由客户端 tooltip 层翻译并替换。
 *
 * 为什么不直接在能力里拼文本：1.12 的 addCustomDescriptionTags 直接调用客户端 TranslationHelper 与按键绑定，
 * 能力类因此在专用服务器上引用了客户端类；这里把「值」与「翻译/按键名解析」分开，能力类不再接触客户端代码。
 *
 * 任一被引用的变量 enabled 为 false 时，引用它的整行不显示；值为空串的变量只作行开关用。
 */
public final class DescriptionVariables {

    /** 列表类取值在 value 中以换行分隔 */
    public static final String LIST_SEPARATOR = "\n";

    public enum Kind {
        /** {@code $名称:} → 数值（高亮） */
        OPTION,
        /** {@code $名称:} → 值是语言键，替换为译文 */
        TRANSLATED,
        /** {@code $名称:} → 开 / 关 */
        TOGGLE,
        /** {@code $名称:} → 效果配置列表（名称 + 等级） */
        EFFECTS,
        /** {@code $名称:} → 效果配置列表（名称 + 等级 + 持续时间） */
        TIMED_EFFECTS,
        /** {@code $名称:} → 实体 id 列表（"modid:*" 表示整个模组） */
        ENTITIES,
        /** {@code $名称:} → 物品 id 列表（条目中 ";" 之后的附加参数忽略） */
        ITEMS,
        /** {@code *名称:} → 值是按键名（KeyNames），替换为当前绑定的按键 */
        KEYBIND,
        /** {@code @名称:} → 替换为「前缀.名称」的译文 */
        LANG
    }

    public record Variable(Kind kind, String name, boolean enabled, String value) {
    }

    private final List<Variable> variables = new ArrayList<>();

    public DescriptionVariables option(String name, Object value) {
        return this.option(name, true, value);
    }

    public DescriptionVariables option(String name, boolean enabled, Object value) {
        return this.add(Kind.OPTION, name, enabled, String.valueOf(value));
    }

    /** 数值（自动去掉多余小数） */
    public DescriptionVariables number(String name, double value) {
        return this.option(name, true, NumberText.of(value));
    }

    public DescriptionVariables number(String name, boolean enabled, double value) {
        return this.option(name, enabled, NumberText.of(value));
    }

    /** 比例，显示为百分数 */
    public DescriptionVariables percent(String name, boolean enabled, double fraction) {
        return this.option(name, enabled, NumberText.percent(fraction));
    }

    /** 「1/N 概率」，显示为百分数 */
    public DescriptionVariables oneIn(String name, boolean enabled, int n) {
        return this.option(name, enabled, NumberText.oneIn(n));
    }

    /** tick，显示为秒 */
    public DescriptionVariables seconds(String name, boolean enabled, int ticks) {
        return this.option(name, enabled, NumberText.seconds(ticks));
    }

    /** 只作行开关、不替换出文字 */
    public DescriptionVariables flag(String name, boolean enabled) {
        return this.option(name, enabled, "");
    }

    public DescriptionVariables translated(String name, boolean enabled, String langKey) {
        return this.add(Kind.TRANSLATED, name, enabled, langKey);
    }

    public DescriptionVariables toggle(String name, boolean enabled, boolean on) {
        return this.add(Kind.TOGGLE, name, enabled, String.valueOf(on));
    }

    /** 触发条件（潜行时 / 站立时 / 总是 / 从不） */
    public DescriptionVariables activation(String name, boolean enabled, ActivationMethod method) {
        return this.translated(name, enabled, "xat.tooltip.activation." + method.name().toLowerCase(Locale.ROOT));
    }

    /** 效果配置列表；withDuration 为 true 时显示每条的持续时间。列表为空时整行不显示 */
    public DescriptionVariables effects(String name, boolean enabled, List<? extends String> configs, boolean withDuration) {
        return this.add(withDuration ? Kind.TIMED_EFFECTS : Kind.EFFECTS, name, enabled && !configs.isEmpty(),
                String.join(LIST_SEPARATOR, configs));
    }

    /** 实体 id 列表；列表为空时整行不显示 */
    public DescriptionVariables entities(String name, boolean enabled, List<? extends String> ids) {
        return this.add(Kind.ENTITIES, name, enabled && !ids.isEmpty(), String.join(LIST_SEPARATOR, ids));
    }

    /** 物品 id 列表；列表为空时整行不显示 */
    public DescriptionVariables items(String name, boolean enabled, List<? extends String> ids) {
        return this.add(Kind.ITEMS, name, enabled && !ids.isEmpty(), String.join(LIST_SEPARATOR, ids));
    }

    public DescriptionVariables keybind(String name, String keyName) {
        return this.add(Kind.KEYBIND, name, true, keyName);
    }

    public DescriptionVariables lang(String prefix, String name, boolean enabled) {
        return this.add(Kind.LANG, name, enabled, prefix + "." + name);
    }

    public List<Variable> entries() {
        return Collections.unmodifiableList(this.variables);
    }

    private DescriptionVariables add(Kind kind, String name, boolean enabled, String value) {
        this.variables.add(new Variable(kind, name, enabled, value));
        return this;
    }
}
