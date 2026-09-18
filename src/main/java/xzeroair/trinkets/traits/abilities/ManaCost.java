package xzeroair.trinkets.traits.abilities;

import javax.annotation.Nullable;

/**
 * 能力的主要法力消耗（tooltip 标题行显示，并与当前法力比较着色）。
 *
 * @param amount 点数；unit 为 MAX_PERCENT 时是占最大法力的比例（0.5 = 50%）
 */
public record ManaCost(float amount, Unit unit) {

    public enum Unit {
        /** 每次使用 */
        USE,
        /** 持续期间每秒 */
        SECOND,
        /** 满蓄力时（随蓄力比例递减） */
        FULL_CHARGE,
        /** 占最大法力的比例 */
        MAX_PERCENT
    }

    /** 消耗 ≤ 0 视为无消耗 */
    @Nullable
    public static ManaCost of(double amount, Unit unit) {
        return amount > 0 ? new ManaCost((float) amount, unit) : null;
    }

    /** 折算成实际点数 */
    public float points(float maxMana) {
        return this.unit == Unit.MAX_PERCENT ? this.amount * maxMana : this.amount;
    }
}
