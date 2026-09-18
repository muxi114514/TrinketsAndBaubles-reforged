package xzeroair.trinkets.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import xzeroair.trinkets.client.ClientPacketHandler;

/**
 * S2C：让客户端打开种族选择界面（首次登录开启选择菜单时、或指令授权后）。对应 1.12 OpenTrinketGui(GUI_RACE_SELECTION)。
 *
 * @param firstLogin 首次登录时界面不提供「返回」按钮
 */
public class OpenRaceSelectionPacket {

    private final boolean firstLogin;

    public OpenRaceSelectionPacket(boolean firstLogin) {
        this.firstLogin = firstLogin;
    }

    public OpenRaceSelectionPacket(FriendlyByteBuf buf) {
        this.firstLogin = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.firstLogin);
    }

    public boolean isFirstLogin() {
        return this.firstLogin;
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenRaceSelection(this));
        context.get().setPacketHandled(true);
    }
}
