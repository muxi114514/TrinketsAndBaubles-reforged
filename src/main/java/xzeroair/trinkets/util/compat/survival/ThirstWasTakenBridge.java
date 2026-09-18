package xzeroair.trinkets.util.compat.survival;

import dev.ghen.thirst.foundation.common.capability.ModCapabilities;

import net.minecraft.world.entity.player.Player;

/**
 * Thirst Was Taken 接缝：唯一直接引用其类的地方，只经 {@link SurvivalCompat} 在该模组已加载时调用。
 */
final class ThirstWasTakenBridge {

    /** 与喝水同一入口，数值上限与同步由 Thirst Was Taken 自行处理 */
    static void addThirst(Player player, int thirst, int quenched) {
        player.getCapability(ModCapabilities.PLAYER_THIRST).ifPresent(data -> data.drink(player, thirst, quenched));
    }

    private ThirstWasTakenBridge() {
    }
}
