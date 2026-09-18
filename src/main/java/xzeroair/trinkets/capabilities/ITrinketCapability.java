package xzeroair.trinkets.capabilities;

import net.minecraft.nbt.CompoundTag;

/**
 * 能力数据统一读写契约。
 * copyFrom 用于玩家死亡/换维度时把旧实例的数据搬到新实例（对应 PlayerEvent.Clone）。
 */
public interface ITrinketCapability<T> {

    CompoundTag saveToNBT(CompoundTag tag);

    void loadFromNBT(CompoundTag tag);

    void copyFrom(T capability, boolean wasDeath, boolean keepInv);
}
