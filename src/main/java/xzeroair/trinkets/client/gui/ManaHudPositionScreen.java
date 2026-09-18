package xzeroair.trinkets.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.client.hud.ManaBarOverlay;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.client.ManaHudConfig;

/**
 * 魔力条位置调整：魔力条跟随鼠标，点击确定并关闭。对应 1.12 client/gui/hud/mana/ManaHud。
 *
 * 移植说明：1.12 每帧把鼠标位置写进配置；1.20.1 的配置每次写入都会自动存盘，故拖动期间只更新覆盖层的预览坐标，
 * 关闭界面（点击或 Esc）时按「屏幕比例 / 像素坐标」模式写一次配置，结果与 1.12 相同。
 */
@OnlyIn(Dist.CLIENT)
public class ManaHudPositionScreen extends Screen {

    private int lastMouseX = -1;
    private int lastMouseY = -1;

    public ManaHudPositionScreen() {
        super(Component.translatable("xat.config.client.mana_hud"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        ManaBarOverlay.INSTANCE.setPreview(new int[] {mouseX, mouseY});
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.onClose();
        return true;
    }

    @Override
    public void removed() {
        ManaBarOverlay.INSTANCE.setPreview(null);
        if (this.lastMouseX < 0 || this.width <= 0 || this.height <= 0) {
            return;
        }
        final ManaHudConfig config = TrinketsConfig.CLIENT.manaHud;
        if (config.usePixelPosition.get()) {
            config.xPixels.set(Mth.clamp(this.lastMouseX, 0, 10000));
            config.yPixels.set(Mth.clamp(this.lastMouseY, 0, 10000));
        } else {
            config.translatedX.set(Mth.clamp((double) this.lastMouseX / this.width, 0.0D, 1.0D));
            config.translatedY.set(Mth.clamp((double) this.lastMouseY / this.height, 0.0D, 1.0D));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
