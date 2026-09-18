package xzeroair.trinkets.util.config;

import net.minecraftforge.common.ForgeConfigSpec;

import xzeroair.trinkets.util.config.client.ManaHudConfig;
import xzeroair.trinkets.util.config.client.RaceGuiConfig;
import xzeroair.trinkets.util.config.client.RenderConfig;
import xzeroair.trinkets.util.config.client.TooltipConfig;

/**
 * 客户端配置根（对应 1.12 ClientConfig）。
 * 仅存放不影响游戏逻辑的本地设置；影响逻辑的一律放 ServerConfig，以免两端不一致。
 */
public class ClientConfig {

    public final RenderConfig render;
    public final ManaHudConfig manaHud;
    public final RaceGuiConfig raceGui;
    public final TooltipConfig tooltip;

    // 1.12 的饰品栏 GUI 由 Curios 取代，debug.showID 并入 tooltip

    ClientConfig(ForgeConfigSpec.Builder builder) {
        this.render = new RenderConfig(builder);
        this.manaHud = new ManaHudConfig(builder);
        this.raceGui = new RaceGuiConfig(builder);
        this.tooltip = new TooltipConfig(builder);
    }
}
