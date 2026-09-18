package xzeroair.trinkets.util.config.server;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;

/**
 * 食物配置（对应 1.12 ServerConfig.Foods）。
 * 移植说明：1.12 的 @Config.RequiresMcRestart 无 1.20.1 对应注解，
 * 注册开关只在模组构造期读一次，故以注释声明「改后需重启」。
 */
public class FoodConfig {

    public final BooleanValue enabled;
    public final BooleanValue transformationEffects;
    public final BooleanValue keepEffectsOnDeath;

    public FoodConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Food settings")
                .translation("xat.config.food")
                .push("food");

        enabled = builder
                .comment("Register this mod's foods. Requires a game restart to take effect.")
                .translation("xat.config.food.registry.enabled")
                .define("enabled", true);

        transformationEffects = builder
                .comment("Do race foods apply their transformation effect?")
                .translation("xat.config.food.transformation")
                .define("transformationEffects", true);

        keepEffectsOnDeath = builder
                .comment("Are transformation effects kept on death?")
                .translation("xat.config.food.transformation.keep")
                .define("keepEffectsOnDeath", true);

        builder.pop();
    }
}
