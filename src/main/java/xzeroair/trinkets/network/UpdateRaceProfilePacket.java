package xzeroair.trinkets.network;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.races.EntityRacePropertiesHandler;
import xzeroair.trinkets.races.RaceInformation;

/**
 * C2S：种族外观界面关闭时提交的外观（颜色、变种、是否显示特征）。服务端按当前种族的变种数校验，越界整份拒绝。
 *
 * 移植说明：1.12 仅在接受时重新同步；这里无论接受与否都重新同步，被拒绝时客户端界面里的预览改动会被服务端数据纠正。
 */
public class UpdateRaceProfilePacket {

    @Nullable
    private final CompoundTag profile;

    public UpdateRaceProfilePacket(CompoundTag profile) {
        this.profile = profile;
    }

    public UpdateRaceProfilePacket(FriendlyByteBuf buf) {
        this.profile = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(this.profile);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        final ServerPlayer player = context.get().getSender();
        context.get().setPacketHandled(true);
        final EntityProperties properties = EntityProperties.get(player);
        if (this.profile == null || properties == null) {
            return;
        }
        final EntityRacePropertiesHandler handler = properties.getRaceHandler();
        final RaceInformation information = handler.getRace().getInformation();
        handler.getAppearance().loadProfile(this.profile, information.getPrimaryTraitMaxVariants(),
                information.getSecondaryTraitMaxVariants());
        properties.scheduleResync();
    }
}
