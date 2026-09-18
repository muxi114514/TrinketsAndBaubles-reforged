package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;

/** 所有能力配置段的共同契约：能力构造时只需知道「是否启用」。 */
public interface IAbilityConfig {

    /**
     * 恒启用：用于不经种族配置的能力来源（如抗性药水给出的免疫能力）。
     * 1.12 在这种场景下读全局的 SERVER.ABILITIES.X，1.20.1 已无全局能力配置段。
     */
    IAbilityConfig ALWAYS_ENABLED = () -> true;

    boolean isEnabled();

    static BooleanValue defineEnabled(ForgeConfigSpec.Builder builder, boolean defaultEnabled) {
        return builder
                .comment("Enable this ability")
                .translation("xat.config.abilities.enabled")
                .define("enabled", defaultEnabled);
    }
}
