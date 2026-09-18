package xzeroair.trinkets.traits.abilities.interfaces;

import net.minecraft.world.entity.Entity;

/**
 * 按键触发的能力。客户端每 tick 采样按键并本地先行调用 onKeyState，再发包让服务端复现。
 *
 * 移植说明：
 * - 1.12 的 onKeyState 直接收 client 的 KeyHandler 对象、从中取 state；此处改为直接传入状态码
 *   （0=按下 1=持续 2=松开），解除对客户端类的依赖——该方法在服务端也会被调用。
 * - 1.12 的 getKey/getAuxKey 返回按键「显示名」且标注仅客户端；此处改为返回按键「名称」（取自 KeyNames 常量），
 *   纯字符串、两端通用，客户端按名称查 KeyMapping。返回空串表示该能力不绑定此键。
 */
public interface IKeyBindInterface extends IAbilityInterface {

    int KEY_PRESS = 0;
    int KEY_DOWN = 1;
    int KEY_RELEASE = 2;

    String getKey();

    default String getAuxKey() {
        return "";
    }

    default boolean onKeyState(Entity entity, int state, boolean aux) {
        return switch (state) {
            case KEY_PRESS -> this.onKeyPress(entity, aux);
            case KEY_DOWN -> this.onKeyDown(entity, aux);
            case KEY_RELEASE -> this.onKeyRelease(entity, aux);
            default -> false;
        };
    }

    default boolean onKeyPress(Entity entity, boolean aux) {
        return true;
    }

    default boolean onKeyDown(Entity entity, boolean aux) {
        return true;
    }

    default boolean onKeyRelease(Entity entity, boolean aux) {
        return true;
    }
}
