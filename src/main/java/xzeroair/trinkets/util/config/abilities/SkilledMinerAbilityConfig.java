package xzeroair.trinkets.util.config.abilities;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/**
 * 熟练矿工的配置段（对应 1.12 ConfigAbilitySkilledMiner）。
 * fortune / fortuneMix / fortuneBlocks 驱动「自然时运」（全局战利品修改器），skilledMiner 驱动「挖掘等级 +1」（HarvestCheck）。
 */
public class SkilledMinerAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final BooleanValue fortune;
    public final BooleanValue fortuneMix;
    public final BooleanValue skilledMiner;
    public final BooleanValue staticMining;
    public final ConfigValue<List<? extends String>> fortuneBlocks;
    public final BooleanValue bonusExp;
    public final IntValue bonusExpMax;
    public final IntValue bonusExpMin;
    public final ConfigValue<List<? extends String>> bonusExpBlocks;
    public final BooleanValue minExp;
    public final ConfigValue<List<? extends String>> minExpBlocks;

    public SkilledMinerAbilityConfig(ForgeConfigSpec.Builder builder, String name) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.fortune = builder
                .comment("Ores drop as if mined with Fortune")
                .translation("xat.config.abilities.skilled_miner.fortune")
                .define("fortune", true);
        this.fortuneMix = builder
                .comment("Natural fortune stacks with the tool's own Fortune enchantment")
                .translation("xat.config.abilities.skilled_miner.fortune_mix")
                .define("fortuneMix", true);
        this.skilledMiner = builder
                .comment("Pickaxes can harvest blocks one tier above their normal tier")
                .translation("xat.config.abilities.skilled_miner.tier")
                .define("skilledMiner", true);
        this.staticMining = builder
                .comment("Mining speed with a suitable pickaxe scales with block hardness, capped for safety")
                .translation("xat.config.abilities.skilled_miner.static")
                .define("staticMining", true);
        this.fortuneBlocks = builder
                .comment("Blocks affected by natural fortune")
                .translation("xat.config.abilities.skilled_miner.fortune_blocks")
                .defineListAllowEmpty("fortuneBlocks", DefaultBlockLists.SKILLED_MINER_FORTUNE, o -> o instanceof String);

        builder.push("experience");
        this.bonusExp = builder
                .comment("Listed blocks drop extra experience")
                .translation("xat.config.abilities.skilled_miner.bonus_exp")
                .define("bonusExp", true);
        this.bonusExpMax = builder
                .comment("Upper bound (exclusive) of the random extra experience")
                .translation("xat.config.abilities.skilled_miner.bonus_exp_max")
                .defineInRange("bonusExpMax", 2, 0, 1000);
        this.bonusExpMin = builder
                .comment("Lower bound of the random extra experience")
                .translation("xat.config.abilities.skilled_miner.bonus_exp_min")
                .defineInRange("bonusExpMin", 0, 0, 1000);
        this.bonusExpBlocks = builder
                .translation("xat.config.abilities.skilled_miner.bonus_exp_blocks")
                .defineListAllowEmpty("bonusExpBlocks", DefaultBlockLists.SKILLED_MINER_BONUS_XP, o -> o instanceof String);
        this.minExp = builder
                .comment("Listed blocks always drop at least 1 experience")
                .translation("xat.config.abilities.skilled_miner.min_exp")
                .define("minExp", true);
        this.minExpBlocks = builder
                .translation("xat.config.abilities.skilled_miner.min_exp_blocks")
                .defineListAllowEmpty("minExpBlocks", DefaultBlockLists.SKILLED_MINER_MIN_XP, o -> o instanceof String);
        builder.pop();

        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
