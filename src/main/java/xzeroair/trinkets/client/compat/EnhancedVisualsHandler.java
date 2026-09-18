package xzeroair.trinkets.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;

import team.creative.enhancedvisuals.api.event.SelectEndermanEvent;
import team.creative.enhancedvisuals.api.event.SplashEvent;
import team.creative.enhancedvisuals.api.event.VisualExplosionEvent;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.interfaces.IAbilityInterface;
import xzeroair.trinkets.util.compat.ModCompat;

/**
 * Enhanced Visuals 接缝（客户端）：本地玩家拥有对应能力时取消其画面干扰事件。对应 1.12 EnhancedVisualsRenderEvent。
 * 唯一直接引用 Enhanced Visuals 类的地方，只在该模组已加载时注册（从而才被类加载）。
 */
@OnlyIn(Dist.CLIENT)
public final class EnhancedVisualsHandler {

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(EnhancedVisualsHandler::onSplash);
        MinecraftForge.EVENT_BUS.addListener(EnhancedVisualsHandler::onExplosion);
        MinecraftForge.EVENT_BUS.addListener(EnhancedVisualsHandler::onSelectEnderman);
    }

    private static void onSplash(SplashEvent event) {
        if (hasEnabledAbility(AbilityNames.CLEAR_SPLASH)) {
            event.setCanceled(true);
        }
    }

    private static void onExplosion(VisualExplosionEvent event) {
        if (hasEnabledAbility(AbilityNames.CLEAR_VISION)) {
            event.setCanceled(true);
        }
    }

    private static void onSelectEnderman(SelectEndermanEvent event) {
        if (hasEnabledAbility(AbilityNames.ENDER_EYES)) {
            event.setCanceled(true);
        }
    }

    private static boolean hasEnabledAbility(String name) {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !ModCompat.enhancedVisuals()) {
            return false;
        }
        final EntityProperties properties = EntityProperties.get(player);
        final IAbilityInterface ability = properties == null ? null : properties.getAbilityHandler().getAbility(AbilityNames.key(name));
        return ability != null && ability.isAbilityEnabled();
    }

    private EnhancedVisualsHandler() {
    }
}
