package xzeroair.trinkets.util.helpers.damage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.world.damagesource.DamageSource;

import xzeroair.trinkets.traits.elements.Element;

/**
 * 「忽略的伤害类型」规则（种族、饰品的 damageTypesToIgnore 配置）。对应 1.12 DamageTypeConfigParser。
 *
 * 语法：[阶段:][ele元素:]伤害名[;类别][;实体][;伤害上限[;倍率]]
 * - 阶段：onAttacked（取消攻击）/ onHurt（护甲前）/ onDamaged（护甲后）；省略表示三个阶段都生效。
 * - 伤害名：* / 旧 message id / 注册名 / #标签；类别：isFire、isMagic…；实体：[isTrue:]实体id 或 *。
 * - 伤害上限：本次伤害低于该值才命中（可带 isMin: 前缀，含义不变）；倍率：命中后伤害乘数，省略为 0（完全忽略）。
 * 例：onHurt:*;isFire;isTrue:minecraft:blaze;5.0;0.1
 *
 * 移植说明：
 * - 1.12 用正则 (ele(.*?):) 拆元素前缀，但取错了拆分段，且会误伤含 "ele" 的实体名（如 skeleton），
 *   导致元素限定从不生效；此处只识别规则开头的 eleXxx: 前缀。
 * - 1.12 未加阶段前缀、带倍率的规则在攻击阶段就会整个取消伤害，倍率永远用不上；
 *   此处无前缀规则在攻击阶段仅当倍率为 0 时取消，其余交给护甲前后阶段按倍率缩放。
 * - 规则按配置列表缓存解析结果，避免每次受击重复切分字符串。
 */
public final class DamageTypeRules {

    public enum Stage {
        ATTACKED("onAttacked:"),
        HURT("onHurt:"),
        DAMAGED("onDamaged:");

        private final String prefix;

        Stage(String prefix) {
            this.prefix = prefix;
        }
    }

    /** 命中与否及伤害乘数（未命中时乘数为 1） */
    public record Result(boolean matched, float multiplier) {
        static final Result FAIL = new Result(false, 1.0F);
        static final Result IGNORE = new Result(true, 0.0F);
    }

    private record Rule(@Nullable Stage stage, @Nullable String element, String[] args) {
    }

    /** 配置文件里的语法说明（种族、元素形态、饰品共用） */
    public static final String SYNTAX = "Damage rules. Format: [onAttacked:|onHurt:|onDamaged:][eleFire:]type[;category][;[isTrue:]entity][;maxDamage[;multiplier]]\n"
            + "type: * / legacy id (inFire, mob) / registry id / #tag; category: isFire, isMagic, isIce, isPoison...; multiplier defaults to 0 (immune)\n"
            + "Example: onHurt:*;isFire;isTrue:minecraft:blaze;5.0;0.1";

    private static final Map<List<? extends String>, List<Rule>> CACHE = new ConcurrentHashMap<>();

    /** 攻击阶段：是否应取消这次攻击 */
    public static boolean shouldCancel(DamageSource source, float dmg, @Nullable Element element, List<? extends String> configs) {
        for (Rule rule : rules(configs)) {
            if (rule.stage() != null && rule.stage() != Stage.ATTACKED) {
                continue;
            }
            final Result result = evaluate(rule, source, dmg, element);
            if (result.matched() && (rule.stage() == Stage.ATTACKED || result.multiplier() <= 0)) {
                return true;
            }
        }
        return false;
    }

    /** 护甲前/后阶段：返回缩放后的伤害（首条命中的规则生效） */
    public static float scale(Stage stage, DamageSource source, float dmg, @Nullable Element element, List<? extends String> configs) {
        for (Rule rule : rules(configs)) {
            if (rule.stage() != null && rule.stage() != stage) {
                continue;
            }
            final Result result = evaluate(rule, source, dmg, element);
            if (result.matched()) {
                return dmg * result.multiplier();
            }
        }
        return dmg;
    }

    /** 配置重载后丢弃旧列表的解析结果，避免缓存随重载无限增长 */
    public static void clearCache() {
        CACHE.clear();
    }

    private static List<Rule> rules(List<? extends String> configs) {
        if (configs.isEmpty()) {
            return List.of();
        }
        return CACHE.computeIfAbsent(configs, DamageTypeRules::parse);
    }

    private static List<Rule> parse(List<? extends String> configs) {
        final List<Rule> rules = new ArrayList<>(configs.size());
        for (String raw : configs) {
            String text = raw.trim();
            Stage stage = null;
            for (Stage candidate : Stage.values()) {
                if (text.startsWith(candidate.prefix)) {
                    stage = candidate;
                    text = text.substring(candidate.prefix.length());
                    break;
                }
            }
            String element = null;
            final int colon = text.indexOf(':');
            if (text.startsWith("ele") && colon > 3) {
                element = text.substring(3, colon);
                text = text.substring(colon + 1);
            }
            final String cleaned = clean(text);
            if (!cleaned.isEmpty()) {
                rules.add(new Rule(stage, element, cleaned.split(";")));
            }
        }
        return List.copyOf(rules);
    }

    /** 与 1.12 ConfigHelper.cleanConfigEntry 一致：逗号、竖线、方括号、空格都视作分隔符 */
    private static String clean(String text) {
        return text.replaceAll("([\\[\\]|,;] ?)|( {2})", " ").trim().replace(" ", ";");
    }

    private static Result evaluate(Rule rule, DamageSource source, float dmg, @Nullable Element element) {
        if (rule.element() != null && !DamageTypeChecks.matchesElement(rule.element(), element)) {
            return Result.FAIL;
        }
        final String[] args = rule.args();
        if (!DamageTypeChecks.matchesDamageName(source, args[0])) {
            return Result.FAIL;
        }
        if (args.length == 1) {
            return Result.IGNORE;
        }
        // 第二段依次尝试：伤害上限 → 类别 → 实体
        final String second = args[1];
        if (damageLimit(second) > 0) {
            return limitThenMultiplier(args, 1, dmg);
        }
        if (DamageTypeChecks.matchesCategory(source, second)) {
            return afterCategory(args, 2, source, dmg);
        }
        if (DamageTypeChecks.matchesEntity(source, second)) {
            return limitThenMultiplier(args, 2, dmg);
        }
        return Result.FAIL;
    }

    /** 类别之后可接伤害上限或实体 */
    private static Result afterCategory(String[] args, int index, DamageSource source, float dmg) {
        if (index >= args.length) {
            return Result.IGNORE;
        }
        if (damageLimit(args[index]) > 0) {
            return limitThenMultiplier(args, index, dmg);
        }
        if (DamageTypeChecks.matchesEntity(source, args[index])) {
            return limitThenMultiplier(args, index + 1, dmg);
        }
        return Result.FAIL;
    }

    /** index 处为伤害上限（没有则直接命中），其后可选倍率 */
    private static Result limitThenMultiplier(String[] args, int index, float dmg) {
        if (index >= args.length) {
            return Result.IGNORE;
        }
        final float limit = damageLimit(args[index]);
        if (limit <= 0 || dmg <= 0 || dmg >= limit) {
            return Result.FAIL;
        }
        if (index + 1 >= args.length) {
            return Result.IGNORE;
        }
        final float multiplier = parseFloat(args[index + 1]);
        return multiplier > 0 ? new Result(true, multiplier) : Result.FAIL;
    }

    private static float damageLimit(String token) {
        return parseFloat(token.startsWith("isMin:") ? token.substring("isMin:".length()) : token);
    }

    private static float parseFloat(String token) {
        try {
            return Float.parseFloat(token);
        } catch (NumberFormatException e) {
            return 0.0F;
        }
    }

    private DamageTypeRules() {
    }
}
