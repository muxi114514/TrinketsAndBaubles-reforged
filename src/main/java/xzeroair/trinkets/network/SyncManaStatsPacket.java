package xzeroair.trinkets.network;

import java.util.function.Supplier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import xzeroair.trinkets.client.ClientPacketHandler;

/** S2C：把魔力数值（当前魔力、额外上限点数）同步给本人。对应 1.12 network/mana/SyncManaStatsPacket。 */
public class SyncManaStatsPacket extends BasicPacket {

    public SyncManaStatsPacket(LivingEntity entity, CompoundTag tag) {
        super(entity.getId(), tag);
    }

    public SyncManaStatsPacket(FriendlyByteBuf buf) {
        super(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSyncManaStats(this));
        context.get().setPacketHandled(true);
    }
}
