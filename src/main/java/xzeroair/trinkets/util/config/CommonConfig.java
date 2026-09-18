package xzeroair.trinkets.util.config;

import net.minecraftforge.common.ForgeConfigSpec;

import xzeroair.trinkets.util.config.common.PotionsConfig;

/** COMMON 配置：两端都在模组加载期读取、且不随世界变化的设置（目前只有酿造催化剂）。 */
public class CommonConfig {

    public final PotionsConfig potions;

    public CommonConfig(ForgeConfigSpec.Builder builder) {
        this.potions = new PotionsConfig(builder);
    }
}
