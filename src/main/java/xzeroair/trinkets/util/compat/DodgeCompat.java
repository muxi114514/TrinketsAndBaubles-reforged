package xzeroair.trinkets.util.compat;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import xzeroair.trinkets.Trinkets;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.traits.abilities.AbilityDodge;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.util.compat.talents.TalentsBridge;

/**
 * 其它模组的闪避联动：玩家用 Elenai Dodge 2 或 Talents「侧步」闪避成功时，若拥有闪避能力（电弧宝珠），扣魔力并触发麻痹周围生物等效果；
 * 此时电弧宝珠自己的双击闪避让位，避免一次按键闪两次。对应 1.12 ElenaiDodgeCompat。
 *
 * 移植说明：1.12 监听 Elenai Dodge 1 的 ServerDodgeEvent；两个 1.20.1 模组都没有公开事件，
 * 改由 mixin/compat 下的 @Pseudo mixin 在其「闪避生效」的服务端处理处调用 {@link #onDodge}。
 */
public final class DodgeCompat {

    public static void onDodge(@Nullable ServerPlayer player) {
        if (player == null) {
            return;
        }
        final EntityProperties properties = EntityProperties.get(player);
        if (properties == null) {
            return;
        }
        try {
            if (properties.getAbilityHandler().getAbility(AbilityNames.key(AbilityNames.DODGING)) instanceof AbilityDodge dodge) {
                dodge.onExternalDodge(player);
            }
        } catch (RuntimeException e) {
            Trinkets.LOGGER.error("Trinkets had an error handling an external dodge", e);
        }
    }

    /** 闪避已由其它模组接管：装有 Elenai Dodge 2，或 Talents 的侧步技能已开启 */
    public static boolean isHandledExternally(Player player) {
        if (ModCompat.elenaiDodge()) {
            return true;
        }
        if (!ModCompat.talents()) {
            return false;
        }
        try {
            return TalentsBridge.hasSidestep(player);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    private DodgeCompat() {
    }
}
