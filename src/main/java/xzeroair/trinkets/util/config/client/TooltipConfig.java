package xzeroair.trinkets.util.config.client;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;

/**
 * 本模组物品说明的显示方式。
 */
public class TooltipConfig {

    public final BooleanValue collapsed;
    public final BooleanValue showStatus;

    public TooltipConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Item tooltip display")
                .translation("xat.config.client.tooltip")
                .push("tooltip");
        this.collapsed = builder
                .comment("Only list ability names by default; hold Shift for full details")
                .translation("xat.config.client.tooltip.collapsed")
                .define("collapsed", true);
        this.showStatus = builder
                .comment("Show live status (toggles, counters, current target) of abilities you are using")
                .translation("xat.config.client.tooltip.status")
                .define("showStatus", true);
        builder.pop();
    }
}
