package xzeroair.trinkets.network;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import xzeroair.trinkets.Trinkets;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.traits.abilities.interfaces.IAbilityInterface;
import xzeroair.trinkets.traits.abilities.interfaces.IKeyBindInterface;
import xzeroair.trinkets.traits.abilities.interfaces.IMovementAbility;

/**
 * C2S：某个能力的按键输入（主按键状态 + 可选的移动键快照）。对应 1.12 network/keybinds/KeybindPacket 的上行部分。
 *
 * 服务端校验照搬 1.12（该版本专门做过「网络安全」加固）：能力键非空、状态码在 0~2、主按键必须附带处理器键、
 * 同时带主按键与移动快照时「主按键是否按下」必须与状态码一致、至少带其中一种输入；
 * 另外限制字符串长度，防止恶意客户端发超长字符串。能力不存在或类型不符时静默丢弃。
 * 主按键回调返回 false（拒绝继续）时回发 {@link AbilityKeyReleasePacket}，让客户端状态机强制松开。
 *
 * 移植说明：1.12 把全部字段塞进一个 NBT 再逐项 hasKey 校验；此处改为定长字段，校验更直接、包体更小。
 */
public class AbilityKeyInputPacket {

    public static final int MOVEMENT_KEYS = 6;
    private static final int MAX_KEY_LENGTH = 256;

    private final String ability;
    private final String handlerKey;
    private final int primaryState;
    private final boolean hasMovement;
    private final boolean primaryDown;
    private final boolean auxDown;
    /** 左、右、前、后、跳、潜行；-1 表示该键本 tick 无事件 */
    private final int[] movement;
    @Nullable
    private final CompoundTag payload;

    public AbilityKeyInputPacket(String ability, String handlerKey, int primaryState, boolean hasMovement,
            boolean primaryDown, boolean auxDown, int[] movement, @Nullable CompoundTag payload) {
        this.ability = ability;
        this.handlerKey = handlerKey;
        this.primaryState = primaryState;
        this.hasMovement = hasMovement;
        this.primaryDown = primaryDown;
        this.auxDown = auxDown;
        this.movement = movement;
        this.payload = payload;
    }

    public AbilityKeyInputPacket(FriendlyByteBuf buf) {
        this.ability = buf.readUtf(MAX_KEY_LENGTH);
        this.handlerKey = buf.readUtf(MAX_KEY_LENGTH);
        this.primaryState = buf.readInt();
        this.hasMovement = buf.readBoolean();
        this.primaryDown = buf.readBoolean();
        this.auxDown = buf.readBoolean();
        this.movement = new int[MOVEMENT_KEYS];
        for (int i = 0; i < MOVEMENT_KEYS; i++) {
            this.movement[i] = buf.readInt();
        }
        this.payload = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.ability, MAX_KEY_LENGTH);
        buf.writeUtf(this.handlerKey, MAX_KEY_LENGTH);
        buf.writeInt(this.primaryState);
        buf.writeBoolean(this.hasMovement);
        buf.writeBoolean(this.primaryDown);
        buf.writeBoolean(this.auxDown);
        for (int i = 0; i < MOVEMENT_KEYS; i++) {
            buf.writeInt(this.movement[i]);
        }
        buf.writeNbt(this.payload);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        final ServerPlayer player = context.get().getSender();
        context.get().setPacketHandled(true);
        if (player == null || !this.isValid()) {
            return;
        }
        final EntityProperties properties = EntityProperties.get(player);
        if (properties == null) {
            return;
        }
        final IAbilityInterface ability = properties.getAbilityHandler().getAbility(this.ability);
        final boolean hasPrimary = this.primaryState != -1;
        if ((this.hasMovement && !(ability instanceof IMovementAbility)) || (hasPrimary && !(ability instanceof IKeyBindInterface))) {
            return;
        }
        try {
            if (this.hasMovement) {
                ((IMovementAbility) ability).onMovement(player, this.primaryState, this.primaryDown, this.auxDown,
                        this.movement[0], this.movement[1], this.movement[2], this.movement[3], this.movement[4], this.movement[5],
                        this.payload);
            }
            if (hasPrimary && !((IKeyBindInterface) ability).onKeyState(player, this.primaryState, this.auxDown)) {
                NetworkHandler.sendTo(new AbilityKeyReleasePacket(this.handlerKey), player);
            }
        } catch (Exception e) {
            Trinkets.LOGGER.error("Trinkets had an error with ability input: {}", this.ability, e);
        }
    }

    private boolean isValid() {
        if (this.ability.isEmpty()) {
            return false;
        }
        final boolean hasPrimary = this.primaryState != -1;
        if (!hasPrimary && !this.hasMovement) {
            return false;
        }
        if (hasPrimary && (!isValidState(this.primaryState) || this.handlerKey.isEmpty())) {
            return false;
        }
        if (!this.hasMovement) {
            return true;
        }
        if (hasPrimary && (this.primaryState < IKeyBindInterface.KEY_RELEASE) != this.primaryDown) {
            return false;
        }
        boolean anyMovement = false;
        for (int state : this.movement) {
            if (state != -1) {
                if (!isValidState(state)) {
                    return false;
                }
                anyMovement = true;
            }
        }
        return anyMovement;
    }

    private static boolean isValidState(int state) {
        return state >= IKeyBindInterface.KEY_PRESS && state <= IKeyBindInterface.KEY_RELEASE;
    }
}
