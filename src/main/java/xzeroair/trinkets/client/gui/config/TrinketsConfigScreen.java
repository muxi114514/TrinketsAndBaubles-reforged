package xzeroair.trinkets.client.gui.config;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;

import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 模组列表「配置」按钮打开的入口界面：客户端 / 通用 / 服务端三份配置。
 * 服务端配置只在单人游戏世界内可改（多人服务器的值由服务器下发，本地修改无效）。
 */
@OnlyIn(Dist.CLIENT)
public class TrinketsConfigScreen extends Screen {

    @Nullable
    private final Screen parent;

    public TrinketsConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("xat.config.gui.title"));
        this.parent = parent;
    }

    /** 由主类在客户端构造期调用 */
    public static void register(ModLoadingContext context) {
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new TrinketsConfigScreen(parent)));
    }

    @Override
    protected void init() {
        final int left = this.width / 2 - 100;
        int top = this.height / 4 + 24;
        this.addSection(left, top, "xat.config.gui.client", TrinketsConfig.CLIENT_SPEC, true);
        top += 24;
        this.addSection(left, top, "xat.config.gui.common", TrinketsConfig.COMMON_SPEC, true);
        top += 24;
        final boolean serverEditable = TrinketsConfig.SERVER_SPEC.isLoaded() && this.minecraft.hasSingleplayerServer();
        final Button server = this.addSection(left, top, "xat.config.gui.server", TrinketsConfig.SERVER_SPEC, serverEditable);
        if (!serverEditable) {
            server.setTooltip(Tooltip.create(Component.translatable("xat.config.gui.server.unavailable")));
        }
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(left, this.height - 28, 200, 20).build());
    }

    private Button addSection(int left, int top, String titleKey, ForgeConfigSpec spec, boolean available) {
        final Component title = Component.translatable(titleKey);
        final Button button = this.addRenderableWidget(Button.builder(title,
                pressed -> this.minecraft.setScreen(new ConfigSectionScreen(this, title, spec, List.of(), spec.getValues(), true)))
                .bounds(left, top, 200, 20).build());
        button.active = available && spec.isLoaded();
        return button;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 4, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
