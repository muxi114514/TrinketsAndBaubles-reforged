package xzeroair.trinkets.races;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;

import xzeroair.trinkets.util.config.server.RaceConfig;
import xzeroair.trinkets.util.helpers.EntityChecks;

/** 种族骑乘规则（对应 1.12 各种族处理器里重复 9 遍的 mountEntity）。 */
public final class RaceMountRules {

    /**
     * 创造模式放行；总开关关闭则一律拒绝；船另受 canControlBoats 限制；
     * 名单为空放行，否则命中与否由 mountWhitelist 决定允许还是拒绝。
     */
    public static boolean canMount(LivingEntity rider, Entity mount, RaceConfig<?> config) {
        if (rider instanceof Player player && player.isCreative()) {
            return true;
        }
        if (!config.canMount.get() || !canSteer(rider, mount, config)) {
            return false;
        }
        if (config.mountBlacklist.get().isEmpty()) {
            return true;
        }
        return EntityChecks.isListed(mount, config.mountBlacklist.get()) == config.mountWhitelist.get();
    }

    /** 船只在本族不能掌舵时，只有已有他人驾驶才允许乘坐 */
    private static boolean canSteer(LivingEntity rider, Entity mount, RaceConfig<?> config) {
        if (config.canControlBoats.get() || !(mount instanceof Boat boat)) {
            return true;
        }
        final Entity controller = boat.getControllingPassenger();
        return controller != null && controller != rider;
    }

    private RaceMountRules() {
    }
}
