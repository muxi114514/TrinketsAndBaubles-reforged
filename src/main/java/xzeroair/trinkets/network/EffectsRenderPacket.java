package xzeroair.trinkets.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import xzeroair.trinkets.client.ClientPacketHandler;

/**
 * S2C：在客户端播放一个视觉特效（闪电、闪电球、龙息火焰等）。对应 1.12 network/particles/EffectsRenderPacket。
 *
 * 移植说明：1.12 按「区块追踪者」发包，1.20.1 取区块会有加载风险，改为按坐标半径（NEAR）发送。
 */
public class EffectsRenderPacket {

    public static final int LIGHTNING = 1;
    public static final int LIGHTNING_ORB = 2;
    public static final int SWEEP = 3;
    public static final int FIRE_BREATH = 4;
    public static final int EXPLOSION = 5;
    public static final int GREED = 6;
    public static final int LIGHTNING_ORB_SILENT = 7;

    private static final double SEND_RADIUS = 64.0D;

    private final int effectID;
    private final int color;
    private final double x;
    private final double y;
    private final double z;
    private final double x2;
    private final double y2;
    private final double z2;
    private final float alpha;
    private final float intensity;

    public EffectsRenderPacket(int effectID, double x, double y, double z, double x2, double y2, double z2,
            int color, float alpha, float intensity) {
        this.effectID = effectID;
        this.x = x;
        this.y = y;
        this.z = z;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        this.color = color;
        this.alpha = alpha;
        this.intensity = intensity;
    }

    public EffectsRenderPacket(FriendlyByteBuf buf) {
        this.effectID = buf.readInt();
        this.color = buf.readInt();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.x2 = buf.readDouble();
        this.y2 = buf.readDouble();
        this.z2 = buf.readDouble();
        this.alpha = buf.readFloat();
        this.intensity = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.effectID);
        buf.writeInt(this.color);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeDouble(this.x2);
        buf.writeDouble(this.y2);
        buf.writeDouble(this.z2);
        buf.writeFloat(this.alpha);
        buf.writeFloat(this.intensity);
    }

    /** 服务端调用：发给以 (x, y, z) 为中心一定半径内的玩家 */
    public void sendNear(Level level) {
        if (!level.isClientSide) {
            NetworkHandler.sendToNear(this, level, this.x, this.y, this.z, SEND_RADIUS);
        }
    }

    /** 服务端调用：以实体位置为中心发送 */
    public void sendNear(Entity entity) {
        if (!entity.level().isClientSide) {
            NetworkHandler.sendToNear(this, entity.level(), entity.getX(), entity.getY(), entity.getZ(), SEND_RADIUS);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleEffect(this));
        context.get().setPacketHandled(true);
    }

    public int getEffectID() {
        return this.effectID;
    }

    public int getColor() {
        return this.color;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public double getX2() {
        return this.x2;
    }

    public double getY2() {
        return this.y2;
    }

    public double getZ2() {
        return this.z2;
    }

    public float getAlpha() {
        return this.alpha;
    }

    public float getIntensity() {
        return this.intensity;
    }
}
