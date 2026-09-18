package xzeroair.trinkets.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import xzeroair.trinkets.client.ClientPacketHandler;

/**
 * S2C：能力即将消耗的魔力，供魔力条显示预扣区段。对应 1.12 network/mana/SyncManaCostToHudPacket。
 * 载荷只有一个浮点数，不走「实体 id + NBT」的 BasicPacket 形状。
 */
public class SyncManaCostToHudPacket {

    private final float cost;

    public SyncManaCostToHudPacket(float cost) {
        this.cost = cost;
    }

    public SyncManaCostToHudPacket(FriendlyByteBuf buf) {
        this.cost = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(this.cost);
    }

    public float getCost() {
        return this.cost;
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleManaCost(this));
        context.get().setPacketHandled(true);
    }
}
