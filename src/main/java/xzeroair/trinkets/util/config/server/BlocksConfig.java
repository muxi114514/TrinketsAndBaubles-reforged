package xzeroair.trinkets.util.config.server;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/**
 * 方块配置（对应 1.12 ConfigBlocksMain）。1.12 的泰迪熊段是月光玫瑰段的复制品且无人读取，不移植。
 */
public class BlocksConfig {

    public final IntValue moonRoseEssence;
    public final IntValue moonRoseEssenceCooldown;

    public BlocksConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Block settings")
                .translation("xat.config.blocks")
                .push("blocks");
        builder.translation("xat.config.blocks.moon_rose").push("moon_rose");
        this.moonRoseEssence = builder
                .comment("How much magical essence a Moon Rose has")
                .translation("xat.config.blocks.moon_rose.essence")
                .defineInRange("essence", 10, 0, 10000);
        this.moonRoseEssenceCooldown = builder
                .comment("Ticks spent sneaking near a Moon Rose to absorb one essence")
                .translation("xat.config.blocks.moon_rose.essence_cooldown")
                .defineInRange("essenceCooldown", 300, 1, 72000);
        builder.pop(2);
    }
}
