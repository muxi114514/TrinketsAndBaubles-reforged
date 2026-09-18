package xzeroair.trinkets.util.handlers;

/**
 * 计数器：正计时到 length 触发，或倒计时到 0 触发。
 * 纯逻辑类，1.12 原版逐字移植（无 MC API 依赖）。
 */
public class Counter {

    protected String name;
    protected int length;
    protected int tick;
    protected boolean countdown;
    protected boolean shouldTick;
    protected boolean autoReset;
    protected boolean saveNBT;

    public Counter(String name, int length) {
        this(name, length, false);
    }

    public Counter(String name, int length, boolean countdown) {
        this(name, length, countdown, true, true);
    }

    public Counter(String name, int length, boolean countdown, boolean saveNBT) {
        this(name, length, countdown, true, saveNBT);
    }

    public Counter(String name, int length, boolean countdown, boolean shouldTick, boolean saveNBT) {
        this(name, length, countdown, shouldTick, true, saveNBT);
    }

    public Counter(String name, int length, boolean countdown, boolean shouldTick, boolean autoReset, boolean saveNBT) {
        this.name = name;
        this.tick = countdown ? length : 0;
        this.length = length;
        this.countdown = countdown;
        this.shouldTick = shouldTick;
        this.autoReset = autoReset;
        this.saveNBT = saveNBT;
    }

    /** 推进一次；返回 true 表示本次到点 */
    public boolean Tick() {
        if (!this.countdown) {
            if (this.tick < this.length) {
                if (this.shouldTick()) {
                    this.tick++;
                }
                return false;
            }
            if (this.shouldTick() && this.shouldAutoReset()) {
                this.resetTick();
            }
            return true;
        }
        if (this.tick > 0) {
            if (this.shouldTick()) {
                this.tick--;
            }
            return false;
        }
        if (this.shouldTick() && this.shouldAutoReset()) {
            this.resetTick();
        }
        return true;
    }

    public int getTick() {
        return this.tick;
    }

    public Counter setTick(int tick) {
        if (this.tick != tick) {
            this.tick = tick;
        }
        return this;
    }

    public int getLength() {
        return this.length;
    }

    public Counter setLength(int length) {
        if (this.length != length) {
            this.length = length;
        }
        return this;
    }

    public String getName() {
        return this.name;
    }

    public boolean getCountdown() {
        return this.countdown;
    }

    public Counter setCountdown(boolean countdown) {
        if (this.countdown != countdown) {
            this.countdown = countdown;
        }
        return this;
    }

    public boolean shouldTick() {
        return this.shouldTick;
    }

    public boolean shouldAutoReset() {
        return this.autoReset;
    }

    public final boolean saveToNBT() {
        return this.saveNBT;
    }

    public void resetTick() {
        this.tick = this.countdown ? this.length : 0;
    }
}
