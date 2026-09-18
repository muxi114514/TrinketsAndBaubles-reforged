package xzeroair.trinkets.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.joml.Quaternionf;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.client.gui.widget.ColorPicker;
import xzeroair.trinkets.client.gui.widget.IntSlider;
import xzeroair.trinkets.network.NetworkHandler;
import xzeroair.trinkets.network.UpdateRaceProfilePacket;
import xzeroair.trinkets.races.EntityRacePropertiesHandler;
import xzeroair.trinkets.races.RaceAppearance;
import xzeroair.trinkets.races.RaceInformation;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.client.ManaHudConfig;

/**
 * 种族外观界面：是否显示特征、配色方案、特征变种与三组颜色，中间是可翻转的角色预览；关闭时把外观提交给服务端。
 * 对应 1.12 GuiEntityProperties。编辑直接作用于本地外观数据，预览即时生效。
 *
 * 移植说明：1.12 的配色方案按钮计数器从 0 开始而非取当前值，首次点击总会跳到「反色」，这里改为从当前值继续循环。
 */
@OnlyIn(Dist.CLIENT)
public class RacePropertiesScreen extends Screen {

    private static final String[] COLOR_OPTIONS = {"normal", "inverted", "solid"};
    private static final int SLIDER_WIDTH = 50;
    private static final int SLIDER_HEIGHT = 20;

    private final LocalPlayer player;
    private final List<ColorPicker> colors = new ArrayList<>();
    private boolean flip;

    public RacePropertiesScreen(LocalPlayer player) {
        super(Component.translatable("xat.config.client.race_gui"));
        this.player = player;
    }

    @Override
    protected void init() {
        this.colors.clear();
        final EntityProperties properties = EntityProperties.get(this.player);
        if (properties == null) {
            return;
        }
        final EntityRacePropertiesHandler handler = properties.getRaceHandler();
        final RaceAppearance appearance = handler.getAppearance();
        final RaceInformation information = handler.getRace().getInformation();
        final ManaHudConfig hud = TrinketsConfig.CLIENT.manaHud;

        this.addRenderableWidget(Button.builder(Component.literal("<--").withStyle(ChatFormatting.RED),
                button -> this.minecraft.setScreen(new InventoryScreen(this.player))).bounds(this.width - 24, 2, 22, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.xat.confirm").withStyle(ChatFormatting.GREEN),
                button -> this.onClose()).bounds(this.width - 80, this.height - 32, 60, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("xat.config.client.mana_hud"),
                button -> this.minecraft.setScreen(new ManaHudPositionScreen())).bounds(2, this.height - 22, 60, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(hud.horizontal.get() ? "H" : "V"), button -> {
            hud.horizontal.set(!hud.horizontal.get());
            button.setMessage(Component.literal(hud.horizontal.get() ? "H" : "V"));
        }).bounds(64, this.height - 22, 14, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(hud.alwaysShown.get() ? "A" : "^"), button -> {
            hud.alwaysShown.set(!hud.alwaysShown.get());
            button.setMessage(Component.literal(hud.alwaysShown.get() ? "A" : "^"));
        }).bounds(80, this.height - 22, 14, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.xat.race.stats"),
                button -> this.minecraft.setScreen(new AbilitiesScreen(this.player))).bounds(2, 2, 50, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(String.valueOf(appearance.showTraits())), button -> {
            appearance.setShowTraits(!appearance.showTraits());
            button.setMessage(Component.literal(String.valueOf(appearance.showTraits())));
        }).bounds(2, 40, 60, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.xat.race.flip"), button -> this.flip = !this.flip)
                .bounds(this.width / 2 - 30, 0, 60, 20).build());

        int left = 70;
        int top = 40;
        this.addRenderableWidget(Button.builder(colorOptionName(appearance.getColorOption()), button -> {
            appearance.setColorOption((Mth.clamp(appearance.getColorOption(), 0, 2) + 1) % COLOR_OPTIONS.length);
            button.setMessage(colorOptionName(appearance.getColorOption()));
        }).bounds(left, top, SLIDER_WIDTH + 22, SLIDER_HEIGHT).build());
        top += SLIDER_HEIGHT;
        this.addRenderableWidget(new IntSlider(left, top, SLIDER_WIDTH + 22, SLIDER_HEIGHT, variantLabel(), 0,
                information.getPrimaryTraitMaxVariants() - 1, appearance.getVariant(), appearance::setVariant));
        top += SLIDER_HEIGHT + 1;
        final ColorPicker primary = this.addColor(left, top, appearance.getPrimaryColor(), handler.getRaceCache().getPrimaryColor(),
                appearance::setPrimaryColor);
        this.addColor(left, primary.getNextY() + 2, appearance.getSecondaryColor(), handler.getRaceCache().getSecondaryColor(),
                appearance::setSecondaryColor);

        final int auxVariants = information.getSecondaryTraitMaxVariants();
        if (auxVariants > 0) {
            left = this.width - (SLIDER_WIDTH + 50);
            top = 60;
            this.addRenderableWidget(new IntSlider(left, top, SLIDER_WIDTH + 22, SLIDER_HEIGHT, variantLabel(), 0, auxVariants,
                    appearance.getAuxVariant(), appearance::setAuxVariant));
            this.addColor(left, top + SLIDER_HEIGHT + 1, appearance.getAuxColor(), information.getOptionalColor(), appearance::setAuxColor);
        }
    }

    private ColorPicker addColor(int left, int top, int color, int defaultColor, IntConsumer setter) {
        final ColorPicker picker = new ColorPicker(this.font, left, top, SLIDER_WIDTH, SLIDER_HEIGHT, color, defaultColor, setter);
        picker.addTo(this::addRenderableWidget);
        this.colors.add(picker);
        return picker;
    }

    private static Component colorOptionName(int option) {
        return Component.translatable("gui.xat.race.color." + COLOR_OPTIONS[Mth.clamp(option, 0, 2)]);
    }

    private static Component variantLabel() {
        return Component.translatable("gui.xat.race.trait.main.variant").append(": ");
    }

    @Override
    public void tick() {
        this.colors.forEach(ColorPicker::tick);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        final EntityProperties properties = EntityProperties.get(this.player);
        if (properties == null) {
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        this.renderPreview(graphics, properties, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderLabels(graphics, properties.getRaceHandler());
        this.colors.forEach(picker -> picker.render(graphics));
    }

    private void renderLabels(GuiGraphics graphics, EntityRacePropertiesHandler handler) {
        int backHeight = 30;
        if (handler.getRace().canFly()) {
            backHeight = 42;
            graphics.drawString(this.font, Component.translatable("gui.xat.race.flying"), 17, 62, handler.canFly() ? 0x00C800 : 0xC80000, true);
        }
        graphics.fill(2, 30, 62, 30 + backHeight, 0x80000000);
        graphics.drawString(this.font, Component.translatable("gui.xat.race.trait.show"), 6, 32, 0xFFFFFF, true);
        if (handler.getRace().isNone()) {
            return;
        }
        final int raceRight = labelBox(graphics, handler.getRace().getDisplayName(), 60, 4, 0xFFFFFF);
        final Element element = handler.getRaceCache().getPrimaryElement();
        if (!element.isNone()) {
            labelBox(graphics, element.getDisplayName(), raceRight + 4, 4, element.getPrimaryColor());
        }
    }

    /** 半透明底框 + 文字，返回底框右边界 */
    private int labelBox(GuiGraphics graphics, Component text, int x, int y, int color) {
        final int textWidth = this.font.width(text);
        final int boxWidth = textWidth + (textWidth % 2 == 0 ? 10 : 9);
        graphics.fill(x, y, x + boxWidth, y + 14, 0x80000000);
        graphics.drawString(this.font, text, x + 6, y + 3, color, true);
        return x + boxWidth;
    }

    /** 角色预览：按体型反向放大，使各种族显示高度一致（1.12 scale = 300 / 身高 × 30） */
    private void renderPreview(GuiGraphics graphics, EntityProperties properties, int mouseX, int mouseY) {
        final int centerX = this.width / 2;
        final int centerY = this.height / 2;
        graphics.fill(centerX - 50, centerY - 75, centerX + 50, centerY + 105, 0x80000000);
        final int scale = (int) (9000.0F / Math.max(properties.getHeightValue(), 1));
        final float angleX = (float) Math.atan((centerX - mouseX) / 40.0F);
        final float angleY = (float) Math.atan((centerY - 50 - mouseY) / 40.0F);
        final Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        final Quaternionf camera = new Quaternionf().rotateX(angleY * 20.0F * Mth.DEG_TO_RAD);
        pose.mul(camera);
        if (this.flip) {
            pose.rotateY((float) Math.PI);
        }
        final float bodyRot = this.player.yBodyRot;
        final float yRot = this.player.getYRot();
        final float xRot = this.player.getXRot();
        final float headRotO = this.player.yHeadRotO;
        final float headRot = this.player.yHeadRot;
        this.player.yBodyRot = 180.0F + angleX * 20.0F;
        this.player.setYRot(180.0F + angleX * 40.0F);
        this.player.setXRot(-angleY * 20.0F);
        this.player.yHeadRot = this.player.getYRot();
        this.player.yHeadRotO = this.player.getYRot();
        InventoryScreen.renderEntityInInventory(graphics, centerX, centerY + 100, scale, pose, camera, this.player);
        this.player.yBodyRot = bodyRot;
        this.player.setYRot(yRot);
        this.player.setXRot(xRot);
        this.player.yHeadRotO = headRotO;
        this.player.yHeadRot = headRot;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        final boolean editing = this.colors.stream().anyMatch(ColorPicker::isEditing);
        if (!editing && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.minecraft.setScreen(new InventoryScreen(this.player));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** 关闭或切换到其它界面时提交外观 */
    @Override
    public void removed() {
        final EntityProperties properties = EntityProperties.get(this.player);
        if (properties != null) {
            NetworkHandler.sendToServer(new UpdateRaceProfilePacket(properties.getRaceHandler().getAppearance().save(new CompoundTag())));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
