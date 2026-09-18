package xzeroair.trinkets.util.config.client;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/**
 * 物品栏里「种族属性界面」按钮（对应 1.12 ConfigRacePropertiesGui）。坐标相对物品栏界面左上角。
 *
 * 移植说明：1.12 的按钮 id 与贴图区域参数只服务于旧 GuiButton 列表与可换贴图，1.20.1 控件无 id、贴图固定，不再提供。
 */
public class RaceGuiConfig {

    public final BooleanValue enabled;
    public final IntValue x;
    public final IntValue y;
    public final IntValue width;
    public final IntValue height;

    public RaceGuiConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Configuration for the race customization and information GUI")
                .translation("xat.config.client.race_gui")
                .push("raceGui");
        this.enabled = builder
                .comment("Show the button that opens the race customization GUI in the inventory")
                .translation("xat.config.client.race_gui.enabled")
                .define("enabled", true);
        this.x = builder
                .comment("Button X position relative to the inventory")
                .translation("xat.config.client.race_gui.x")
                .defineInRange("x", 28, -1000, 1000);
        this.y = builder
                .comment("Button Y position relative to the inventory")
                .translation("xat.config.client.race_gui.y")
                .defineInRange("y", 66, -1000, 1000);
        this.width = builder
                .comment("Button width")
                .translation("xat.config.client.race_gui.width")
                .defineInRange("width", 10, 1, 64);
        this.height = builder
                .comment("Button height")
                .translation("xat.config.client.race_gui.height")
                .defineInRange("height", 10, 1, 64);
        builder.pop();
    }
}
