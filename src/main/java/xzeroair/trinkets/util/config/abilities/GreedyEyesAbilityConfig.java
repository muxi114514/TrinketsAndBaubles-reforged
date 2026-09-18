package xzeroair.trinkets.util.config.abilities;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

import xzeroair.trinkets.enums.ActivationMethod;

/**
 * 贪婪之眼（寻宝）的配置段（对应 1.12 ConfigAbilityGreedyEyes）。
 *
 * 移植说明：1.12 的矿物词典条目（oreCoal 等）迁移为 Forge 标签 #forge:ores/*；
 * 条目格式「方块或实体 id / #方块标签 ; 颜色」，颜色支持 #RRGGBB。
 */
public class GreedyEyesAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final DoubleValue cost;
    public final BooleanValue closest;
    public final IntValue frequency;
    public final ConfigValue<List<? extends String>> targets;
    public final IntValue rangeVertical;
    public final IntValue rangeHorizontal;
    public final IntValue particles;
    public final EnumValue<ActivationMethod> growlActivation;
    public final IntValue volume;

    public GreedyEyesAbilityConfig(ForgeConfigSpec.Builder builder, String name, double defaultCost) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.cost = builder
                .comment("Mana spent per scan")
                .translation("xat.config.abilities.greedy_eyes.cost")
                .defineInRange("cost", defaultCost, 0.0D, 10000.0D);
        this.closest = builder
                .comment("Only highlight the closest target")
                .translation("xat.config.abilities.greedy_eyes.closest")
                .define("closest", true);
        this.frequency = builder
                .comment("Ticks between scans")
                .translation("xat.config.abilities.greedy_eyes.frequency")
                .defineInRange("frequency", 79, 1, 12000);
        this.targets = builder
                .comment("Treasure list. Format: block id, entity id or #block_tag;#RRGGBB")
                .translation("xat.config.abilities.greedy_eyes.blocks")
                .defineListAllowEmpty("targets", List.of(
                        "#forge:ores/coal;#464646",
                        "#forge:ores/iron;#FFCC99",
                        "#forge:ores/gold;#FFD700",
                        "#forge:ores/lapis;#26619C",
                        "#forge:ores/redstone;#B02E26",
                        "#forge:ores/diamond;#00E6FF",
                        "#forge:ores/emerald;#00FF4D",
                        "#forge:ores/quartz;#EBEBEB",
                        "minecraft:chest;#FFD700",
                        "minecraft:chest_minecart;#FFD700"), o -> o instanceof String);
        this.rangeVertical = builder
                .comment("Vertical scan range in blocks")
                .translation("xat.config.abilities.greedy_eyes.range_vertical")
                .defineInRange("rangeVertical", 6, 0, 32);
        this.rangeHorizontal = builder
                .comment("Horizontal scan range in blocks")
                .translation("xat.config.abilities.greedy_eyes.range_horizontal")
                .defineInRange("rangeHorizontal", 12, 0, 32);
        this.particles = builder
                .comment("Maximum particles shown per scan when not only showing the closest target")
                .translation("xat.config.abilities.greedy_eyes.particles")
                .defineInRange("particles", 255, 0, 4096);
        this.growlActivation = builder
                .comment("When the dragon growl hints at the target direction")
                .translation("xat.config.abilities.greedy_eyes.growl")
                .defineEnum("growlActivation", ActivationMethod.SNEAK);
        this.volume = builder
                .comment("Growl volume in percent")
                .translation("xat.config.abilities.greedy_eyes.volume")
                .defineInRange("volume", 100, 0, 100);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
