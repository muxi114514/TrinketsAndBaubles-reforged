package xzeroair.trinkets.util.compat.talents;

import com.seniors.talents.common.capability.AptitudeCapability;
import com.seniors.talents.registry.RegistrySkills;

import net.minecraft.world.entity.player.Player;

/**
 * Talents 接缝：唯一直接引用 Talents 类的地方，只在 Talents 已加载时才会被调用（从而才被类加载）。
 */
public final class TalentsBridge {

    /** 侧步技能是否已开启（Talents 服务端处理闪避包时也以此为前提） */
    public static boolean hasSidestep(Player player) {
        final AptitudeCapability aptitude = AptitudeCapability.get(player);
        return aptitude != null && aptitude.getToggleSkill(RegistrySkills.SIDESTEP.get());
    }

    private TalentsBridge() {
    }
}
