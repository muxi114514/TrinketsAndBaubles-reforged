package xzeroair.trinkets.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import xzeroair.trinkets.capabilities.Capabilities;
import xzeroair.trinkets.network.NetworkHandler;
import xzeroair.trinkets.network.SyncPlayerDataPacket;

/** 0.34.1: sends a player their own race data. */
public final class PlayerDataSync {
    public static void sync(ServerPlayer player) {
        player.getCapability(Capabilities.ENTITY_PROPERTIES).ifPresent(data -> {
            CompoundTag tag = data.saveToNBT(new CompoundTag());
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlayerDataPacket(tag));
        });
    }

    private PlayerDataSync() {
    }
}

