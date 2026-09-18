package xzeroair.trinkets.client.keybinds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.Trinkets;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.network.AbilityKeyInputPacket;
import xzeroair.trinkets.network.NetworkHandler;
import xzeroair.trinkets.traits.AbilityHandler.AbilityHolder;
import xzeroair.trinkets.traits.abilities.interfaces.IAbilityInterface;
import xzeroair.trinkets.traits.abilities.interfaces.IKeyBindInterface;
import xzeroair.trinkets.traits.abilities.interfaces.IMovementAbility;

/**
 * 客户端按键采样：每 tick 为本地玩家身上的每个按键类能力读取按键、推进状态机、本地先行执行（预测），
 * 再把输入发给服务端复现。对应 1.12 capabilities/race/KeybindHandler#handleClientInput。
 *
 * 顺序约定（与 1.12 一致）：先对 6 个移动键统一采样一次，保证同一 tick 内各能力看到的移动输入一致；
 * 某个能力的主按键回调返回 false 时，本 tick 不再处理后续能力。
 *
 * 移植说明：1.12 把状态机挂在两端共有的 EntityProperties 上；实际只有客户端使用，
 * 这里改为客户端静态持有，本地玩家下线时清空。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientAbilityInput {

    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private static final int FORWARD = 2;
    private static final int BACK = 3;
    private static final int JUMP = 4;
    private static final int SNEAK = 5;

    private static final KeyHandler[] MOVEMENT = new KeyHandler[AbilityKeyInputPacket.MOVEMENT_KEYS];
    private static final Map<String, KeyHandler> PRIMARY = new HashMap<>();

    static {
        for (int i = 0; i < MOVEMENT.length; i++) {
            MOVEMENT[i] = new KeyHandler();
        }
    }

    public static void tick(LocalPlayer player) {
        final EntityProperties properties = EntityProperties.get(player);
        if (properties == null) {
            return;
        }
        final int[] movement = sampleMovement(Minecraft.getInstance().options);
        final List<Map.Entry<String, IAbilityInterface>> abilities = new ArrayList<>();
        for (Map.Entry<String, AbilityHolder> entry : properties.getAbilityHandler().getActiveAbilities().entrySet()) {
            abilities.add(Map.entry(entry.getKey(), entry.getValue().getAbility()));
        }
        for (Map.Entry<String, IAbilityInterface> entry : abilities) {
            if (!(entry.getValue() instanceof IKeyBindInterface keybind)) {
                continue;
            }
            try {
                if (!handleAbility(player, entry.getKey(), keybind, movement)) {
                    return;
                }
            } catch (Exception e) {
                Trinkets.LOGGER.error("Trinkets had an error with ability input: {}", entry.getKey(), e);
            }
        }
    }

    /** 服务端拒绝后强制松开对应按键 */
    public static void forceRelease(String handlerKey) {
        final KeyHandler handler = PRIMARY.get(handlerKey);
        if (handler != null) {
            handler.forceRelease();
        }
    }

    public static void reset() {
        PRIMARY.clear();
        for (int i = 0; i < MOVEMENT.length; i++) {
            MOVEMENT[i] = new KeyHandler();
        }
    }

    private static int[] sampleMovement(Options options) {
        final KeyMapping[] keys = {options.keyLeft, options.keyRight, options.keyUp, options.keyDown, options.keyJump, options.keyShift};
        final int[] states = new int[keys.length];
        for (int i = 0; i < keys.length; i++) {
            MOVEMENT[i].updateKeyState(keys[i].isDown());
            states[i] = MOVEMENT[i].getState();
        }
        return states;
    }

    private static boolean handleAbility(LocalPlayer player, String abilityKey, IKeyBindInterface keybind, int[] movement) {
        final String primaryName = keybind.getKey();
        final String auxName = keybind.getAuxKey();
        final boolean primaryDown = ModKeyMappings.isDown(primaryName);
        final boolean auxDown = ModKeyMappings.isDown(auxName);
        final String handlerKey = primaryName.isEmpty() ? "" : abilityKey + "." + primaryName;
        final KeyHandler primary = primaryName.isEmpty() ? null : PRIMARY.computeIfAbsent(handlerKey, k -> new KeyHandler());
        if (primary != null) {
            primary.updateKeyState(primaryDown);
        }
        final int primaryState = primary == null ? KeyHandler.NONE : primary.getState();

        boolean sendMovement = false;
        CompoundTag payload = null;
        if (keybind instanceof IMovementAbility movementAbility && anyMovement(movement)) {
            payload = movementAbility.createMovementPayload(player, primaryState, primaryDown, auxDown,
                    movement[LEFT], movement[RIGHT], movement[FORWARD], movement[BACK], movement[JUMP], movement[SNEAK]);
            sendMovement = movementAbility.onMovement(player, primaryState, primaryDown, auxDown,
                    movement[LEFT], movement[RIGHT], movement[FORWARD], movement[BACK], movement[JUMP], movement[SNEAK], payload);
        }

        final boolean primaryContinues = primaryState == KeyHandler.NONE
                || keybind.onKeyState(player, primaryState, auxDown);

        if (sendMovement) {
            NetworkHandler.sendToServer(new AbilityKeyInputPacket(abilityKey, handlerKey, primaryState, true,
                    primaryDown, auxDown, movement.clone(), payload));
        } else if (primaryState != KeyHandler.NONE) {
            NetworkHandler.sendToServer(new AbilityKeyInputPacket(abilityKey, handlerKey, primaryState, false,
                    primaryDown, auxDown, noMovement(), null));
        }
        return primaryContinues;
    }

    private static boolean anyMovement(int[] movement) {
        for (int state : movement) {
            if (state != KeyHandler.NONE) {
                return true;
            }
        }
        return false;
    }

    private static int[] noMovement() {
        final int[] states = new int[AbilityKeyInputPacket.MOVEMENT_KEYS];
        Arrays.fill(states, KeyHandler.NONE);
        return states;
    }

    private ClientAbilityInput() {
    }
}
