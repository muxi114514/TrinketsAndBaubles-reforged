package xzeroair.trinkets.util.compat.survival;

import com.kettle.simpledifficultyreforged.api.SimpleDifficultyApi;

import net.minecraft.world.entity.player.Player;

/**
 * Simple Difficulty Reforge seam: the only class that references its API, only called through {@link SurvivalCompat}
 * when the mod is loaded.
 * <p>
 * Simple Difficulty Reforge can run SimpleDifficulty's own temperature and thirst (its "built-in" systems, used when
 * Cold Sweat / Thirst Was Taken aren't installed). This connects to them the way 1.12 Trinkets and Baubles' SDCompat
 * did: heat immunity keeps the player at Normal or colder, cold immunity at Normal or warmer, and the Stone of the
 * Sea's Water Absorption adds thirst.
 * <p>
 * Older Simple Difficulty Reforge versions don't have the API; then everything here reports "not running".
 * Simple Difficulty Reforge looks this class up by name to know Trinkets and Baubles does the immunity itself.
 */
final class SimpleDifficultyBridge {

    /** null = not checked yet */
    private static Boolean hasApi;

    /** Whether this Simple Difficulty Reforge version has the API (com.kettle.simpledifficultyreforged.api). */
    private static boolean hasApi() {
        if (hasApi == null) {
            try {
                Class.forName("com.kettle.simpledifficultyreforged.api.SimpleDifficultyApi", false, SimpleDifficultyBridge.class.getClassLoader());
                hasApi = true;
            } catch (ClassNotFoundException | LinkageError e) {
                hasApi = false;
            }
        }
        return hasApi;
    }

    /** The built-in temperature system is running (not Cold Sweat). */
    static boolean temperature() {
        try {
            return hasApi() && SimpleDifficultyApi.isTemperatureEnabled();
        } catch (IllegalStateException e) {
            // Its config isn't loaded yet
            return false;
        }
    }

    /** The built-in thirst system is running (not Thirst Was Taken). */
    static boolean thirst() {
        try {
            return hasApi() && SimpleDifficultyApi.isThirstEnabled();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    static void limitHeat(Player player) {
        SimpleDifficultyApi.limitHeat(player);
    }

    static void limitCold(Player player) {
        SimpleDifficultyApi.limitCold(player);
    }

    /** 1.12 addThirst: thirst only, no saturation */
    static void addThirst(Player player, int thirst) {
        SimpleDifficultyApi.addThirst(player, thirst, 0.0F);
    }

    private SimpleDifficultyBridge() {
    }
}
