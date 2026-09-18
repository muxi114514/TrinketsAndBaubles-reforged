package xzeroair.trinkets.util.config.server;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;

/**
 * 种族或饰品的生存类联动段（对应 1.12 ConfigSurvivalCompat）：获得哪些温度/口渴/寄生虫免疫。
 * 炎热、寒冷对接冷汗；口渴、寄生虫对接 Simple Difficulty Reforge 的效果。
 */
public class SurvivalConfig {

    public final BooleanValue immuneToHeat;
    public final BooleanValue immuneToCold;
    public final BooleanValue immuneToThirst;
    public final BooleanValue immuneToParasites;

    public SurvivalConfig(ForgeConfigSpec.Builder builder, boolean heat, boolean cold, boolean thirst, boolean parasites) {
        builder.comment("Survival mod compatibility (Cold Sweat / Simple Difficulty)")
                .translation("xat.config.survival")
                .push("survival");
        this.immuneToHeat = builder
                .comment("Immune to overheating (Cold Sweat)")
                .translation("xat.config.survival.heat")
                .define("immuneToHeat", heat);
        this.immuneToCold = builder
                .comment("Immune to freezing (Cold Sweat)")
                .translation("xat.config.survival.cold")
                .define("immuneToCold", cold);
        this.immuneToThirst = builder
                .comment("Immune to the Thirsty effect from dirty water (Simple Difficulty)")
                .translation("xat.config.survival.thirst")
                .define("immuneToThirst", thirst);
        this.immuneToParasites = builder
                .comment("Immune to the Parasites effect from dirty water (Simple Difficulty)")
                .translation("xat.config.survival.parasites")
                .define("immuneToParasites", parasites);
        builder.pop();
    }

    public static SurvivalConfig none(ForgeConfigSpec.Builder builder) {
        return new SurvivalConfig(builder, false, false, false, false);
    }
}
