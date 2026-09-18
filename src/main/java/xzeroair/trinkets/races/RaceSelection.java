package xzeroair.trinkets.races;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 种族选择规则：哪些种族可以在选择界面里选。服务端据此校验选择包，客户端据此过滤列表
 * （SERVER 配置随登录同步，两端读到的是同一份黑名单）。
 */
public final class RaceSelection {

    /** 名单项可写种族名（Dragon）或注册名（xat:dragon），不区分大小写 */
    public static boolean isSelectable(@Nullable EntityRace race) {
        if (race == null || race.isNone()) {
            return false;
        }
        final ResourceLocation id = race.getRegistryName();
        for (final String entry : TrinketsConfig.SERVER.races.selectionBlacklist.get()) {
            if (race.getName().equalsIgnoreCase(entry) || (id != null && id.toString().equalsIgnoreCase(entry))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValid(@Nullable EntityRace race, @Nullable Element element) {
        return element != null && isSelectable(race);
    }

    private RaceSelection() {
    }
}
