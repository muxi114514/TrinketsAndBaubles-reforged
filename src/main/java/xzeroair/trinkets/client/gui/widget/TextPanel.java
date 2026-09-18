package xzeroair.trinkets.client.gui.widget;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tesselator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ScrollPanel;

/**
 * 可滚动的说明文本面板，顶部可带一个图标。对应 1.12 各界面内部的 Info（GuiScrollingList 表头绘制文本）。
 * 文本在构造时按面板宽度折行一次。
 */
@OnlyIn(Dist.CLIENT)
public class TextPanel extends ScrollPanel {

    private static final int LINE_HEIGHT = 10;
    private static final int ICON_SIZE = 36;

    private final Font font;
    private final List<FormattedCharSequence> lines = new ArrayList<>();
    @Nullable
    private final ResourceLocation icon;
    private final int iconTextureSize;

    public TextPanel(Minecraft minecraft, int width, int top, int bottom, int left, List<Component> text) {
        this(minecraft, width, top, bottom, left, text, null, 0);
    }

    /**
     * @param icon            顶部图标（整张贴图绘制为 36×36），null 表示无
     * @param iconTextureSize 图标贴图边长
     */
    public TextPanel(Minecraft minecraft, int width, int top, int bottom, int left, List<Component> text,
            @Nullable ResourceLocation icon, int iconTextureSize) {
        super(minecraft, Math.max(width, 16), Math.max(bottom - top, 16), top, left);
        this.font = minecraft.font;
        this.icon = icon;
        this.iconTextureSize = iconTextureSize;
        for (Component line : text) {
            if (line.getString().isEmpty()) {
                this.lines.add(FormattedCharSequence.EMPTY);
            } else {
                this.lines.addAll(this.font.split(line, Math.max(this.width - 12, 8)));
            }
        }
    }

    @Override
    protected int getContentHeight() {
        final int iconHeight = this.icon == null ? 0 : ICON_SIZE + LINE_HEIGHT;
        return Math.max(iconHeight + this.lines.size() * LINE_HEIGHT + 8, this.bottom - this.top - 8);
    }

    @Override
    protected void drawPanel(GuiGraphics graphics, int entryRight, int relativeY, Tesselator tess, int mouseX, int mouseY) {
        int top = relativeY;
        if (this.icon != null) {
            RenderSystem.enableBlend();
            graphics.blit(this.icon, this.left + this.width / 2 - ICON_SIZE / 2, top, ICON_SIZE, ICON_SIZE, 0, 0,
                    this.iconTextureSize, this.iconTextureSize, this.iconTextureSize, this.iconTextureSize);
            RenderSystem.disableBlend();
            top += ICON_SIZE + LINE_HEIGHT;
        }
        for (FormattedCharSequence line : this.lines) {
            graphics.drawString(this.font, line, this.left + 4, top, 0xFFFFFF, true);
            top += LINE_HEIGHT;
        }
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
    }
}
