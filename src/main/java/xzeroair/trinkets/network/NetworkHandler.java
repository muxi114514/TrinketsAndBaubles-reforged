package xzeroair.trinkets.network;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import xzeroair.trinkets.util.Reference;

/**
 * 网络通道。
 *
 * 移植说明：
 * - 1.12 的 SimpleNetworkWrapper + IMessageHandler 换成 SimpleChannel + messageBuilder。
 * - consumerMainThread 原生把处理切回主线程，1.12 的 ThreadSafePacket 因此不再需要。
 * - 1.12 要「发给自己」和「发给追踪者」各调一次；1.20.1 有 TRACKING_ENTITY_AND_SELF 一次搞定。
 * - 配置同步包（PacketConfigSync）不移：ModConfig.Type.SERVER 由 Forge 自动同步。
 * - VIP / 饰品栏容器相关包不移（前者不移植，后者由 Curios 承担）。
 */
public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Reference.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    public static void init() {
        CHANNEL.messageBuilder(SyncRaceDataPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncRaceDataPacket::encode)
                .decoder(SyncRaceDataPacket::new)
                .consumerMainThread(SyncRaceDataPacket::handle)
                .add();

        CHANNEL.messageBuilder(AbilityCacheSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AbilityCacheSyncPacket::encode)
                .decoder(AbilityCacheSyncPacket::new)
                .consumerMainThread(AbilityCacheSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncManaStatsPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncManaStatsPacket::encode)
                .decoder(SyncManaStatsPacket::new)
                .consumerMainThread(SyncManaStatsPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncManaCostToHudPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncManaCostToHudPacket::encode)
                .decoder(SyncManaCostToHudPacket::new)
                .consumerMainThread(SyncManaCostToHudPacket::handle)
                .add();

        CHANNEL.messageBuilder(AbilityKeyInputPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AbilityKeyInputPacket::encode)
                .decoder(AbilityKeyInputPacket::new)
                .consumerMainThread(AbilityKeyInputPacket::handle)
                .add();

        CHANNEL.messageBuilder(AbilityKeyReleasePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AbilityKeyReleasePacket::encode)
                .decoder(AbilityKeyReleasePacket::new)
                .consumerMainThread(AbilityKeyReleasePacket::handle)
                .add();

        CHANNEL.messageBuilder(EffectsRenderPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EffectsRenderPacket::encode)
                .decoder(EffectsRenderPacket::new)
                .consumerMainThread(EffectsRenderPacket::handle)
                .add();

        CHANNEL.messageBuilder(StatusMessagePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StatusMessagePacket::encode)
                .decoder(StatusMessagePacket::new)
                .consumerMainThread(StatusMessagePacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenRaceSelectionPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenRaceSelectionPacket::encode)
                .decoder(OpenRaceSelectionPacket::new)
                .consumerMainThread(OpenRaceSelectionPacket::handle)
                .add();

        CHANNEL.messageBuilder(SelectRacePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectRacePacket::encode)
                .decoder(SelectRacePacket::new)
                .consumerMainThread(SelectRacePacket::handle)
                .add();

        CHANNEL.messageBuilder(UpdateRaceProfilePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdateRaceProfilePacket::encode)
                .decoder(UpdateRaceProfilePacket::new)
                .consumerMainThread(UpdateRaceProfilePacket::handle)
                .add();
        // 触及距离包（1.12 IncreasedReachPacket）不移：1.20.1 由 ForgeMod.ENTITY_REACH 属性原生处理
    }

    public static void sendTo(Object packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /** 发给所有追踪该实体的玩家；实体本身是玩家时也包含自己 */
    public static void sendToTrackingAndSelf(Object packet, Entity entity) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    public static void sendToTracking(Object packet, Entity entity) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
    }

    public static void sendToDimension(Object packet, ResourceKey<Level> dimension) {
        CHANNEL.send(PacketDistributor.DIMENSION.with(() -> dimension), packet);
    }

    /** 发给同维度内距 (x, y, z) 不超过 radius 的玩家；只按坐标筛选，不触碰区块 */
    public static void sendToNear(Object packet, Level level, double x, double y, double z, double radius) {
        CHANNEL.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(x, y, z, radius, level.dimension())), packet);
    }

    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    private NetworkHandler() {
    }
}
