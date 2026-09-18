package xzeroair.trinkets.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import xzeroair.trinkets.client.ClientPacketHandler;

/**
 * S2C：让客户端按本地语言显示一条带变量的提示（动作栏或聊天栏）。
 *
 * 为什么需要：本模组沿用 1.12 的语言文件，文本里用 {@code $变量名:} 做占位符，
 * 原版 {@code Component.translatable} 只认 %s，服务端也拿不到玩家的语言；
 * 故由服务端只发「键 + 变量」，客户端翻译后替换。变量值本身也可以是待翻译的键。
 */
public class StatusMessagePacket {

    private static final int MAX_LENGTH = 1024;

    /** 一个变量；translate 为 true 时 value 是语言键 */
    public record Arg(String name, String value, boolean translate) {
    }

    private final String key;
    private final boolean actionBar;
    private final List<Arg> args;

    public StatusMessagePacket(String key, boolean actionBar) {
        this.key = key;
        this.actionBar = actionBar;
        this.args = new ArrayList<>();
    }

    public StatusMessagePacket(FriendlyByteBuf buf) {
        this.key = buf.readUtf(MAX_LENGTH);
        this.actionBar = buf.readBoolean();
        final int size = buf.readVarInt();
        this.args = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.args.add(new Arg(buf.readUtf(MAX_LENGTH), buf.readUtf(MAX_LENGTH), buf.readBoolean()));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.key, MAX_LENGTH);
        buf.writeBoolean(this.actionBar);
        buf.writeVarInt(this.args.size());
        for (Arg arg : this.args) {
            buf.writeUtf(arg.name(), MAX_LENGTH);
            buf.writeUtf(arg.value(), MAX_LENGTH);
            buf.writeBoolean(arg.translate());
        }
    }

    /** 字面值变量 */
    public StatusMessagePacket with(String name, Object value) {
        this.args.add(new Arg(name, String.valueOf(value), false));
        return this;
    }

    /** 由客户端翻译的变量 */
    public StatusMessagePacket withKey(String name, String langKey) {
        this.args.add(new Arg(name, langKey, true));
        return this;
    }

    /** 服务端调用；非玩家或在客户端调用时忽略 */
    public void send(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            NetworkHandler.sendTo(this, player);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleStatusMessage(this));
        context.get().setPacketHandled(true);
    }

    public String getKey() {
        return this.key;
    }

    public boolean isActionBar() {
        return this.actionBar;
    }

    public List<Arg> getArgs() {
        return this.args;
    }
}
