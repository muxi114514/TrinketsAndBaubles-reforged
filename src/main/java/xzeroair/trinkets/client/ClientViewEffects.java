package xzeroair.trinkets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.init.ModItems;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.helpers.EntityChecks;
import xzeroair.trinkets.util.helpers.TrinketHelper;

/**
 * 画面效果：水之亲和去掉水下雾；火焰免疫/巨龙之戒去掉着火遮挡，海洋之石/水之亲和去掉水下遮挡。
 * 对应 1.12 PlayerCameraSetupEvents 的 renderFogDensityEvent 与 renderBlockOverlay。
 *
 * 移植说明：1.12 还判断「火元素龙之眼」，但它读的是物品的默认元素（恒为中性），该分支从未生效；火元素龙之眼
 * 提供火焰免疫能力，已被能力判断覆盖。第三人称相机距离随体型缩放见 mixin/client/CameraMixin。
 */
@Mod.EventBusSubscriber(modid = Reference.MODID, value = Dist.CLIENT)
public final class ClientViewEffects {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || event.getType() != FogType.WATER || !hasAbility(player, AbilityNames.AFFINITY_WATER)) {
            return;
        }
        event.setNearPlaneDistance(event.getFarPlaneDistance() * 0.75F);
        event.setFarPlaneDistance(event.getFarPlaneDistance() * 2.0F);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockOverlay(RenderBlockScreenEffectEvent event) {
        final boolean hide = switch (event.getOverlayType()) {
            case FIRE -> hasAbility(event.getPlayer(), AbilityNames.IMMUNITY_FIRE)
                    || TrinketHelper.isEquipped(event.getPlayer(), stack -> stack.is(ModItems.DRAGON_RING.get()));
            case WATER -> hasAbility(event.getPlayer(), AbilityNames.AFFINITY_WATER)
                    || TrinketHelper.isEquipped(event.getPlayer(), stack -> stack.is(ModItems.SEA_STONE.get()));
            default -> false;
        };
        if (hide) {
            event.setCanceled(true);
        }
    }

    private static boolean hasAbility(Player player, String name) {
        return EntityProperties.get(player) != null && EntityChecks.hasAbility(player, AbilityNames.key(name));
    }

    private ClientViewEffects() {
    }
}
