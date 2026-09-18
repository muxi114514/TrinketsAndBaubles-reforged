package xzeroair.trinkets.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.client.RaceGuiConfig;

/**
 * 物品栏里打开种族属性界面的小按钮。对应 1.12 GuiEntityPropertiesButton：贴图上半为常态、下半为悬停态，悬停时在下方显示「打开」。
 * 位置每帧按物品栏左上角重算（配方书开合会平移物品栏），配方书打开时隐藏。
 */
@OnlyIn(Dist.CLIENT)
public class RacePropertiesButton extends AbstractButton {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Reference.MODID, "textures/gui/mana_button.png");

    private final InventoryScreen parent;

    public RacePropertiesButton(InventoryScreen parent) {
        super(0, 0, TrinketsConfig.CLIENT.raceGui.width.get(), TrinketsConfig.CLIENT.raceGui.height.get(),
                Component.translatable("gui.xat.button.open"));
        this.parent = parent;
        this.updatePosition();
    }

    private void updatePosition() {
        final RaceGuiConfig config = TrinketsConfig.CLIENT.raceGui;
        this.setX(this.parent.getGuiLeft() + config.x.get());
        this.setY(this.parent.getGuiTop() + config.y.get());
        this.visible = !this.parent.getRecipeBookComponent().isVisible();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.updatePosition();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        final Minecraft minecraft = Minecraft.getInstance();
        final boolean hovered = this.isHoveredOrFocused();
        graphics.blit(TEXTURE, this.getX(), this.getY(), this.width, this.height, 0, hovered ? 32 : 0, 32, 32, 32, 64);
        if (hovered) {
            graphics.drawCenteredString(minecraft.font, this.getMessage(), this.getX() + 5, this.getY() + this.height, 0xFFFFFF);
        }
    }

    @Override
    public void onPress() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.setScreen(new RacePropertiesScreen(minecraft.player));
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
