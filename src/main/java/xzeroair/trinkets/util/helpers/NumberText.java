package xzeroair.trinkets.util.helpers;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 说明文本里的数字格式（两端通用）：去掉多余的小数 0，比例转百分数，tick 转秒。
 */
public final class NumberText {

    /** 最多两位小数：3.0 → "3"，0.25 → "0.25" */
    public static String of(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return String.valueOf(value);
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /** 比例转百分数：0.25 → "25%" */
    public static String percent(double fraction) {
        return of(fraction * 100.0D) + "%";
    }

    /** 「1/N 概率」转百分数；N ≤ 0 视为 0% */
    public static String oneIn(int n) {
        return n <= 0 ? "0%" : percent(1.0D / n);
    }

    /** tick 转秒 */
    public static String seconds(int ticks) {
        return of(ticks / 20.0D);
    }

    private NumberText() {
    }
}
