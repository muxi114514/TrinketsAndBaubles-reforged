package xzeroair.trinkets.network;

import java.util.function.Supplier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import xzeroair.trinkets.client.ClientPacketHandler;

/**
 * S2C：把实体的种族属性能力同步到客户端。
 */
public class SyncRaceDataPacket extends BasicPacket {

    public SyncRaceDataPacket(LivingEntity entity, CompoundTag tag) {
        super(entity.getId(), tag);
    }

    public SyncRaceDataPacket(FriendlyByteBuf buf) {
        super(buf);
    }

    /**
     * 处理入口。
     * 客户端逻辑隔离在 ClientPacketHandler 中，经 DistExecutor 触达——
     * 直接在此引用 Minecraft 类会让专用服务器在类加载期就崩溃。
     */
    public void handle(Supplier<NetworkEvent.Context> context) {
        final NetworkEvent.Context ctx = context.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSyncRaceData(this));
        ctx.setPacketHandled(true);
    }
}
