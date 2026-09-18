package xzeroair.trinkets.client.keybinds;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyConflictContext;

import xzeroair.trinkets.util.KeyNames;

/**
 * 模组按键（对应 1.12 client/keybinds/ModKeyBindings）。按键名即翻译键，沿用 1.12 的 key.xat.*.desc 以复用译文。
 *
 * 移植说明：
 * - 1.12 的能力通过 getKey() 返回按键的「显示名」，客户端再用 isKeyDownFromName 把显示名解析回物理按键。
 *   显示名会随语言变化、绑到鼠标键时无法解析、两个按键绑同一物理键时还会互相串——
 *   1.20.1 改为 getKey() 返回按键「名称」（见 KeyNames），这里按名称查表后直接调 KeyMapping#isDown，行为一致且没有上述问题。
 * - 1.12 的 TRINKET_GUI（打开自带饰品栏）随饰品栏一并由 Curios 取代，不移植。
 */
@OnlyIn(Dist.CLIENT)
public final class ModKeyMappings {

    private static final Map<String, KeyMapping> BY_NAME = new HashMap<>();

    public static final KeyMapping DRAGONS_EYE_TARGET = create(KeyNames.DRAGONS_EYE_TARGET, GLFW.GLFW_KEY_P);
    public static final KeyMapping DRAGONS_EYE_ABILITY = create(KeyNames.DRAGONS_EYE_ABILITY, GLFW.GLFW_KEY_BACKSLASH);
    public static final KeyMapping POLARIZED_STONE_ABILITY = create(KeyNames.POLARIZED_STONE_ABILITY, GLFW.GLFW_KEY_BACKSLASH);
    public static final KeyMapping AUX_KEY = create(KeyNames.AUX_KEY, GLFW.GLFW_KEY_LEFT_CONTROL);
    public static final KeyMapping ARCING_ORB_ABILITY = create(KeyNames.ARCING_ORB_ABILITY, GLFW.GLFW_KEY_R);
    public static final KeyMapping ARCING_ORB_DODGE = create(KeyNames.ARCING_ORB_DODGE, InputConstants.UNKNOWN.getValue());
    public static final KeyMapping ENDER_CROWN = create(KeyNames.ENDER_CROWN, GLFW.GLFW_KEY_R);
    public static final KeyMapping RACE_ABILITY = create(KeyNames.RACE_ABILITY, GLFW.GLFW_KEY_R);

    private static KeyMapping create(String name, int defaultKey) {
        final KeyMapping mapping = new KeyMapping(name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
                defaultKey, KeyNames.CATEGORY);
        BY_NAME.put(name, mapping);
        return mapping;
    }

    public static Iterable<KeyMapping> all() {
        return BY_NAME.values();
    }

    @Nullable
    public static KeyMapping byName(String name) {
        return name == null || name.isEmpty() ? null : BY_NAME.get(name);
    }

    /** 名称为空或未注册视为未按下（例如闪避能力在未启用按键模式时返回空名） */
    public static boolean isDown(String name) {
        final KeyMapping mapping = byName(name);
        return mapping != null && mapping.isDown();
    }

    private ModKeyMappings() {
    }
}
