package xzeroair.trinkets.network;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 数据包基类：携带「目标实体 id + 一份 NBT」，是本模组绝大多数同步包的载荷形状。
 *
 * 移植说明：1.12 另有一个 ThreadSafePacket 子类，靠 addScheduledTask 把处理切回主线程；
 * 1.20.1 由 SimpleChannel 的 consumerMainThread 原生保证，故该类整个不移。
 * ByteBufUtils.readTag/writeTag 也由 FriendlyByteBuf 的 readNbt/writeNbt 取代。
 */
public abstract class BasicPacket {

    protected final int entityID;
    @Nullable
    protected final CompoundTag tag;

    protected BasicPacket(int entityID, @Nullable CompoundTag tag) {
        this.entityID = entityID;
        this.tag = tag;
    }

    protected BasicPacket(FriendlyByteBuf buf) {
        this.entityID = buf.readInt();
        this.tag = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.entityID);
        buf.writeNbt(this.tag);
    }

    public int getEntityID() {
        return this.entityID;
    }

    @Nullable
    public CompoundTag getTag() {
        return this.tag;
    }
}
