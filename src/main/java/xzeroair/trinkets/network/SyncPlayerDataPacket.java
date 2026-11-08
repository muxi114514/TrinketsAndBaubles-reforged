package xzeroair.trinkets.network;

import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import xzeroair.trinkets.client.ClientPacketHandler;

/** 0.34.1: a player's own race data, sent to that player (login, respawn, dimension change). */
public class SyncPlayerDataPacket {
    private final CompoundTag tag;

    public SyncPlayerDataPacket(CompoundTag tag) {
        this.tag = tag;
    }

    public CompoundTag getTag() {
        return this.tag;
    }

    public static void encode(SyncPlayerDataPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.tag);
    }

    public static SyncPlayerDataPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        if (tag == null) {
            tag = new CompoundTag();
        }
        return new SyncPlayerDataPacket(tag);
    }

    public static void handle(SyncPlayerDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handlePlayerData(packet)));
        context.setPacketHandled(true);
    }
}

