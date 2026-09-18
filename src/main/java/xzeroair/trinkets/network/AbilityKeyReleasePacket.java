package xzeroair.trinkets.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import xzeroair.trinkets.client.ClientPacketHandler;

/**
 * S2C：服务端拒绝了某次按键输入，让客户端对应的按键状态机强制松开。
 * 对应 1.12 KeybindPacket 的 ForceRelease 下行分支。
 */
public class AbilityKeyReleasePacket {

    private static final int MAX_KEY_LENGTH = 256;

    private final String handlerKey;

    public AbilityKeyReleasePacket(String handlerKey) {
        this.handlerKey = handlerKey;
    }

    public AbilityKeyReleasePacket(FriendlyByteBuf buf) {
        this.handlerKey = buf.readUtf(MAX_KEY_LENGTH);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.handlerKey, MAX_KEY_LENGTH);
    }

    public String getHandlerKey() {
        return this.handlerKey;
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleKeyRelease(this));
        context.get().setPacketHandled(true);
    }
}
