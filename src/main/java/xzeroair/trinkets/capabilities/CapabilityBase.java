package xzeroair.trinkets.capabilities;

import net.minecraft.nbt.CompoundTag;

import xzeroair.trinkets.util.handlers.TickHandler;

/**
 * 能力根基类：持有宿主对象、一份自用 NBT、一个计数器集合。
 *
 * @param <T> 自身类型（copyFrom 用）
 * @param <E> 宿主类型（实体 / 物品栈 / 方块实体）
 */
public abstract class CapabilityBase<T, E> implements ITrinketCapability<T> {

    private CompoundTag tag;
    private TickHandler tickHandler;

    private final E object;

    protected CapabilityBase(E object) {
        this.tickHandler = new TickHandler();
        this.tag = new CompoundTag();
        this.object = object;
    }

    public CompoundTag getTag() {
        if (this.tag == null) {
            this.tag = new CompoundTag();
        }
        return this.tag;
    }

    public E getObject() {
        return this.object;
    }

    public TickHandler getTickHandler() {
        if (this.tickHandler == null) {
            this.tickHandler = new TickHandler();
        }
        return this.tickHandler;
    }

    @Override
    public CompoundTag saveToNBT(CompoundTag tag) {
        return tag;
    }

    @Override
    public void loadFromNBT(CompoundTag tag) {
    }

    @Override
    public void copyFrom(T capability, boolean wasDeath, boolean keepInv) {
    }
}
