package xzeroair.trinkets.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.capabilities.race.RaceCache;
import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.init.ModRaces;
import xzeroair.trinkets.races.EntityRace;
import xzeroair.trinkets.races.RaceSelection;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * C2S：在种族选择界面确认的种族与主元素，写入出生种族。
 *
 * 服务端校验同 1.12：种族存在且不是 None、元素存在、不在选择黑名单；未开启选择菜单时还需持有授权（指令授予，60 秒内一次）。
 * 移植说明：1.12 以注册表数字 id 传输，1.20.1 注册表无稳定数字 id，改传注册名。
 */
public class SelectRacePacket {

    private final ResourceLocation race;
    private final ResourceLocation element;

    public SelectRacePacket(ResourceLocation race, ResourceLocation element) {
        this.race = race;
        this.element = element;
    }

    public SelectRacePacket(FriendlyByteBuf buf) {
        this.race = buf.readResourceLocation();
        this.element = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.race);
        buf.writeResourceLocation(this.element);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        final ServerPlayer player = context.get().getSender();
        context.get().setPacketHandled(true);
        final EntityRace race = ModRaces.registry().getValue(this.race);
        final Element primary = ModElements.registry().getValue(this.element);
        final EntityProperties properties = EntityProperties.get(player);
        if (player == null || properties == null || !RaceSelection.isValid(race, primary)) {
            return;
        }
        if (!TrinketsConfig.SERVER.races.selectionMenu.get() && !properties.isRaceSelectionAuthorized()) {
            return;
        }
        properties.setOriginalRaceCache(new RaceCache(race, primary));
        properties.consumeRaceSelectionAuthorization();
        properties.scheduleResync();
    }
}
