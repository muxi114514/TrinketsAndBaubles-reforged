package xzeroair.trinkets.util.compat.firstaid;

/**
 * 一次 First Aid 分部位伤害结算后的身体状态（只暴露本模组能力用到的操作）。
 * 能力只依赖本接口，不直接引用 First Aid 的类，未装 First Aid 时能力类照常加载。
 */
public interface FirstAidDamage {

    float headHealth();

    float bodyHealth();

    /** 头部血量恢复到本次伤害之前 */
    void restoreHead();

    /** 全部部位回满 */
    void healAllParts();

    /** 通知 First Aid 把部位血量同步给客户端 */
    void scheduleResync();
}
