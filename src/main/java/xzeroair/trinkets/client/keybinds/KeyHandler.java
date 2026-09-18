package xzeroair.trinkets.client.keybinds;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 单个按键的「按下 / 按住 / 松开」状态机，每客户端 tick 以当前是否按下驱动一次。
 * 对应 1.12 client/keybinds/KeyHandler 中实际被使用的那条路径（updateKeyState / forceRelease）。
 *
 * 状态码：-1 本 tick 无事件，0 刚按下，1 持续按住，2 松开。
 * 能力回调若返回 false（拒绝继续），服务端会下发强制松开，本按键在物理松开前不再产生事件。
 *
 * 移植说明：1.12 的状态机在两端各存一份；实际没有任何能力在服务端读取它，服务端只需要状态码，
 * 故 1.20.1 只保留客户端这一份。
 */
@OnlyIn(Dist.CLIENT)
public final class KeyHandler {

    public static final int NONE = -1;
    public static final int PRESS = 0;
    public static final int DOWN = 1;
    public static final int RELEASE = 2;

    private boolean pressed;
    private boolean released = true;
    private boolean forceRelease;
    private int ticks;
    private int state = NONE;

    public int getState() {
        return this.state;
    }

    public int heldDuration() {
        return this.ticks;
    }

    /** 服务端拒绝继续：本次按住期间不再产生事件，直到物理松开后重新按下 */
    public void forceRelease() {
        this.forceRelease = true;
        this.pressed = true;
        this.released = true;
        this.state = NONE;
    }

    /** 以本 tick 的物理按键状态推进一步，返回是否按下 */
    public boolean updateKeyState(boolean keyDown) {
        this.state = NONE;
        if (keyDown) {
            this.ticks++;
            if (!this.pressed && this.released) {
                this.pressed = true;
                this.forceRelease = false;
                this.state = PRESS;
                this.released = false;
            } else if (!this.released) {
                this.state = DOWN;
            }
            // 已被强制松开：物理松开之前不再产生任何事件（服务端据此校验「松开事件时按键必然未按下」）
            return true;
        }
        if (!this.released && !this.forceRelease) {
            this.state = RELEASE;
        }
        this.pressed = false;
        this.released = true;
        this.forceRelease = false;
        this.ticks = 0;
        return false;
    }
}
