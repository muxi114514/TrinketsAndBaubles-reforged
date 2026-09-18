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
 * S2C：同步能力的禁用标记与临时数据，使客户端与服务端对「某来源的某能力是否生效」保持一致。
 */
public class AbilityCacheSyncPacket extends BasicPacket {

    public AbilityCacheSyncPacket(LivingEntity entity, CompoundTag tag) {
        super(entity.getId(), tag);
    }

    public AbilityCacheSyncPacket(FriendlyByteBuf buf) {
        super(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        final NetworkEvent.Context ctx = context.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleAbilityCacheSync(this));
        ctx.setPacketHandled(true);
    }
}
