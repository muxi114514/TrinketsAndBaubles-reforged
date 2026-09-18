package xzeroair.trinkets.enums;

/** 能力的触发条件（对应 1.12 同名枚举）。 */
public enum ActivationMethod {
    SNEAK,
    STAND,
    ALWAYS,
    NEVER;

    /** 当前潜行状态下是否满足触发条件 */
    public boolean isActive(boolean sneaking) {
        return switch (this) {
            case SNEAK -> sneaking;
            case STAND -> !sneaking;
            case ALWAYS -> true;
            case NEVER -> false;
        };
    }
}
