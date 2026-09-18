package xzeroair.trinkets.util.config.server;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

/**
 * 杂项配置（对应 1.12 ServerConfig.MiscConfigs）。
 * 移植说明：原版 VIPS 项随 vip/ 包一并不移植。1.12 的「So Many Enchantments 水下行者是否叠加」只针对一个附魔，
 * 1.20.1 整合包里的附魔模组（jlme）没有同类附魔，改为可填任意附魔 id 的名单，默认为空。
 */
public class MiscConfig {

    public final BooleanValue depthStriderStacks;
    public final ConfigValue<List<? extends String>> nonStackingSwimEnchantments;
    public final BooleanValue preventMovementWhileTransforming;
    public final BooleanValue reachInteractionFix;
    public final ConfigValue<List<? extends String>> disabledBlessings;

    public MiscConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Miscellaneous settings")
                .translation("xat.config.misc")
                .push("misc");

        depthStriderStacks = builder
                .comment("Does Depth Strider Stack with Swim Speed Attributes?")
                .translation("xat.config.misc.depth")
                .define("depthStriderStacks", false);

        nonStackingSwimEnchantments = builder
                .comment("Other swim speed enchantments that do not stack with Swim Speed Attributes (enchantment ids)")
                .translation("xat.config.misc.swim_enchantments")
                .defineListAllowEmpty("nonStackingSwimEnchantments", List.of(), o -> o instanceof String);

        preventMovementWhileTransforming = builder
                .comment("If enabled, the player will be unable to move when transforming from one race to another")
                .translation("xat.config.misc.movement")
                .define("preventMovementWhileTransforming", false);

        reachInteractionFix = builder
                .comment("Vanilla MC doesn't handle interaction with increased reach properly, this fixes it")
                .translation("xat.config.misc.interaction.fix")
                .define("reachInteractionFix", true);

        disabledBlessings = builder
                .comment("Hidden feature, add anything into this list to disable it.")
                .translation("xat.config.misc.blessings")
                .defineListAllowEmpty("disabledBlessings", List.of(), o -> o instanceof String);

        builder.pop();
    }
}
