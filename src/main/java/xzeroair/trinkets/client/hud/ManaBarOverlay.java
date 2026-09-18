package xzeroair.trinkets.client.hud;

import javax.annotation.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.client.ClientManaCache;
import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.client.ManaHudConfig;

/**
 * 魔力条覆盖层。对应 1.12 client/events/ScreenOverlayEvents + gui/hud/mana/ManaGui。
 *
 * 显示规则（与 1.12 一致）：魔力未满或有待消耗时显示；满且无消耗持续 80 帧后隐藏，除非开启常显或正在调整位置。
 * 贴图宽 106、每行高 16：第 0 行是底槽，第 1~7 行是不同样式的填充；待消耗的部分不画填充。
 */
@OnlyIn(Dist.CLIENT)
public final class ManaBarOverlay implements IGuiOverlay {

    public static final ManaBarOverlay INSTANCE = new ManaBarOverlay();

    private static final ResourceLocation TEXTURE = new ResourceLocation(Reference.MODID, "textures/gui/mana_bar.png");
    private static final int TEXTURE_WIDTH = 106;
    private static final int ROW_HEIGHT = 16;
    private static final int HIDE_AFTER_FRAMES = 80;
    private static final int COST_TIMEOUT_FRAMES = 20;

    private float lastMana = -1;
    private float lastMaxMana = -1;
    private float lastCost;
    private int idleFrames;
    private int costFrames;
    /** 位置调整界面打开期间的预览坐标（强制显示）；null 表示未在调整 */
    @Nullable
    private int[] preview;

    private ManaBarOverlay() {
    }

    /** 预览位置只存在内存里，关闭调整界面时才写配置——Forge 配置每次 set 都会自动存盘 */
    public void setPreview(@Nullable int[] position) {
        this.preview = position;
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        final ManaHudConfig config = TrinketsConfig.CLIENT.manaHud;
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || gui.getMinecraft().options.hideGui || !config.shown.get() || !TrinketsConfig.SERVER.magic.manaEnabled.get()) {
            return;
        }
        final MagicStats stats = MagicStats.get(player);
        if (stats == null) {
            return;
        }
        final float maxMana = stats.getMaxMana();
        final float mana = Math.min(stats.getMana(), maxMana);
        final float cost = Math.min(this.currentCost(), mana);
        if (!this.shouldShow(config, mana, maxMana, cost)) {
            return;
        }
        final int x;
        final int y;
        final int[] previewPosition = this.preview;
        if (previewPosition != null) {
            x = Mth.clamp(previewPosition[0], 0, screenWidth);
            y = Mth.clamp(previewPosition[1], 0, screenHeight);
        } else if (config.usePixelPosition.get()) {
            x = Mth.clamp(config.xPixels.get(), 0, screenWidth);
            y = Mth.clamp(config.yPixels.get(), 0, screenHeight);
        } else {
            x = Mth.clamp((int) Math.round(screenWidth * config.translatedX.get()), 0, screenWidth);
            y = Mth.clamp((int) Math.round(screenHeight * config.translatedY.get()), 0, screenHeight);
        }
        this.drawBar(graphics, config, x, y, mana, maxMana, cost);
    }

    /** 服务端下发的待消耗值 20 帧内未刷新则视为结束 */
    private float currentCost() {
        final float cost = ClientManaCache.getManaCost();
        if (cost != this.lastCost) {
            this.lastCost = cost;
            this.costFrames = cost > 0 ? COST_TIMEOUT_FRAMES : 0;
            this.idleFrames = 0;
        } else if (this.costFrames > 0 && --this.costFrames <= 0) {
            ClientManaCache.setManaCost(0);
            this.lastCost = 0;
        }
        return this.lastCost;
    }

    private boolean shouldShow(ManaHudConfig config, float mana, float maxMana, float cost) {
        if (mana != this.lastMana || maxMana != this.lastMaxMana) {
            this.lastMana = mana;
            this.lastMaxMana = maxMana;
            this.idleFrames = 0;
        }
        if (mana != maxMana || cost > 0) {
            this.idleFrames = 0;
            return true;
        }
        if (this.idleFrames < HIDE_AFTER_FRAMES) {
            this.idleFrames++;
            return true;
        }
        return this.preview != null || (maxMana > 0 && config.alwaysShown.get());
    }

    private void drawBar(GuiGraphics graphics, ManaHudConfig config, int x, int y, float mana, float maxMana, float cost) {
        final int width = config.width.get();
        final int height = config.height.get();
        final int textureHeight = ROW_HEIGHT * 8;
        final int filled = maxMana <= 0 ? 0 : (int) (width * ((mana - cost) / maxMana));
        graphics.pose().pushPose();
        if (!config.horizontal.get()) {
            graphics.pose().rotateAround(Axis.ZP.rotationDegrees(-90.0F), x, y, 0.0F);
        }
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, x, y, 0, 0, width, height, TEXTURE_WIDTH, textureHeight);
        if (mana > 0 && filled > 0) {
            final int row = ROW_HEIGHT + config.texture.get() * ROW_HEIGHT;
            graphics.blit(TEXTURE, x, y, 0, row, filled, height, TEXTURE_WIDTH, textureHeight);
        }
        RenderSystem.disableBlend();
        if (config.showText.get()) {
            final String text = (int) mana + "/" + (int) Math.max(maxMana, 0);
            final int textX = x + width / 2 - Minecraft.getInstance().font.width(text) / 2 + 1;
            final int textY = y + height / 2 - 4;
            graphics.drawString(Minecraft.getInstance().font, text, textX, textY, 0xFFFFFFFF, true);
        }
        graphics.pose().popPose();
    }
}
