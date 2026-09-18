package xzeroair.trinkets.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 打开本模组界面的入口：服务端要求的种族选择界面（登录时世界可能尚未载入，延后到可显示时再打开），
 * 以及物品栏里的种族属性按钮。对应 1.12 OpenTrinketGui 的客户端处理与 GuiScreenEvents。
 * 仅在客户端主线程读写，无需同步。
 */
@Mod.EventBusSubscriber(modid = Reference.MODID, value = Dist.CLIENT)
public final class ScreenOpener {

    private static boolean pendingRaceSelection;
    private static boolean pendingFirstLogin;

    public static void requestRaceSelection(boolean firstLogin) {
        pendingRaceSelection = true;
        pendingFirstLogin = firstLogin;
        tryOpenRaceSelection();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && pendingRaceSelection) {
            tryOpenRaceSelection();
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        pendingRaceSelection = false;
    }

    private static void tryOpenRaceSelection() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen instanceof ReceivingLevelScreen) {
            return;
        }
        pendingRaceSelection = false;
        minecraft.setScreen(new RaceSelectionScreen(minecraft.player, pendingFirstLogin));
    }

    /** 生存物品栏加入种族属性按钮（创造物品栏与 1.12 一样不加） */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen inventory && TrinketsConfig.CLIENT.raceGui.enabled.get()) {
            event.addListener(new RacePropertiesButton(inventory));
        }
    }

    private ScreenOpener() {
    }
}
