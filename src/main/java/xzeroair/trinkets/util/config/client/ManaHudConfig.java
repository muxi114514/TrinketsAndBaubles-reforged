package xzeroair.trinkets.util.config.client;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/**
 * 魔力条 HUD 设置（对应 1.12 ConfigManaBarHud）。
 *
 * 移植说明：1.12 的 rendLocPre（在原版 HUD 之前/之后绘制）对应 1.20.1 的覆盖层注册位置，只能在启动时确定，
 * 改为固定绘制在最上层，不再提供该项。
 */
public class ManaHudConfig {

    public final BooleanValue shown;
    public final BooleanValue alwaysShown;
    public final BooleanValue horizontal;
    public final BooleanValue showText;
    public final DoubleValue translatedX;
    public final DoubleValue translatedY;
    public final BooleanValue usePixelPosition;
    public final IntValue xPixels;
    public final IntValue yPixels;
    public final IntValue width;
    public final IntValue height;
    public final IntValue texture;

    public ManaHudConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Mana bar HUD")
                .translation("xat.config.client.mana_hud")
                .push("manaHud");
        this.shown = builder
                .comment("Show the mana bar")
                .translation("xat.config.client.mana_hud.shown")
                .define("shown", true);
        this.alwaysShown = builder
                .comment("Keep the mana bar visible even when mana is full")
                .translation("xat.config.client.mana_hud.always")
                .define("alwaysShown", false);
        this.horizontal = builder
                .comment("Draw the bar horizontally; false draws it vertically")
                .translation("xat.config.client.mana_hud.horizontal")
                .define("horizontal", true);
        this.showText = builder
                .comment("Show current/maximum mana on the bar")
                .translation("xat.config.client.mana_hud.text")
                .define("showText", true);
        this.translatedX = builder
                .comment("Horizontal position as a fraction of the screen width")
                .translation("xat.config.client.mana_hud.x")
                .defineInRange("translatedX", 0.19D, 0.0D, 1.0D);
        this.translatedY = builder
                .comment("Vertical position as a fraction of the screen height")
                .translation("xat.config.client.mana_hud.y")
                .defineInRange("translatedY", 0.94D, 0.0D, 1.0D);
        this.usePixelPosition = builder
                .comment("Position the bar with pixel coordinates instead of screen fractions")
                .translation("xat.config.client.mana_hud.pixel_mode")
                .define("usePixelPosition", false);
        this.xPixels = builder
                .comment("Horizontal position in pixels")
                .translation("xat.config.client.mana_hud.x_pixels")
                .defineInRange("xPixels", 0, 0, 10000);
        this.yPixels = builder
                .comment("Vertical position in pixels")
                .translation("xat.config.client.mana_hud.y_pixels")
                .defineInRange("yPixels", 0, 0, 10000);
        this.width = builder
                .comment("Bar width in pixels")
                .translation("xat.config.client.mana_hud.width")
                .defineInRange("width", 106, 1, 106);
        this.height = builder
                .comment("Bar height in pixels")
                .translation("xat.config.client.mana_hud.height")
                .defineInRange("height", 16, 1, 16);
        this.texture = builder
                .comment("Bar fill style (row in the mana bar texture)")
                .translation("xat.config.client.mana_hud.texture")
                .defineInRange("texture", 0, 0, 6);
        builder.pop();
    }
}
