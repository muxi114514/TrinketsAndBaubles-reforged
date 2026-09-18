package xzeroair.trinkets.traits.abilities;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.common.ToolActions;

import xzeroair.trinkets.loot.BlockDropContext;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.IMiningAbility;
import xzeroair.trinkets.util.config.abilities.SkilledMinerAbilityConfig;
import xzeroair.trinkets.util.helpers.BlockMatcher;

/**
 * 熟练矿工：手持镐时挖掘速度按方块硬度给出保底（且封顶防瞬破）；可采集比镐高一级的方块；名单内矿石自带时运；挖矿额外掉经验。
 *
 * 移植说明：
 * - 1.12 的「自然时运」与「挖掘等级 +1」都走 brokeBlock 里「复制主手工具、注入时运附魔、以高一级工具自行重新破坏方块」这条路，
 *   另一条经 HarvestDropsEvent 的掉落路径被原作者注释禁用（留有「按差值追加掉落」的草稿）。在 1.20.1 的破坏事件里自行重破方块
 *   会重复触发事件与掉落，故拆成两处：挖掘等级 +1 走 HarvestCheck（{@link #canHarvest}），自然时运走全局战利品修改器
 *   （{@link #blockDrops}，按 1.12 草稿的差值算法：用注入时运的工具重掷一次，只追加比现有掉落多出的部分，不覆盖其它模组的掉落改动）。
 * - 1.12 里自然时运只在开启 skilledMiner 时才实际生效（注入时运的工具只用于重破方块）；这里两项按各自开关独立生效。
 * - 镐的判定由 1.12 的 getToolClasses 包含 "pickaxe"，改为 1.20.1 的 ToolActions.PICKAXE_DIG；
 *   挖掘等级由整数 harvestLevel 改为 TierSortingRegistry 的有序 Tier 列表。
 */
public class AbilitySkilledMiner extends Ability implements IMiningAbility {

    private static final float STATIC_MINING_TARGET_MULTIPLIER = 5F;
    private static final float STATIC_MINING_MAX_SAFE_MULTIPLIER = 20F;

    private final SkilledMinerAbilityConfig config;
    private final BlockMatcher bonusExpBlocks;
    private final BlockMatcher minExpBlocks;
    private final BlockMatcher fortuneBlocks;

    public AbilitySkilledMiner(@Nonnull SkilledMinerAbilityConfig config) {
        super(AbilityNames.SKILLED_MINER);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
        this.bonusExpBlocks = BlockMatcher.of(config.bonusExpBlocks.get());
        this.minExpBlocks = BlockMatcher.of(config.minExpBlocks.get());
        this.fortuneBlocks = BlockMatcher.of(config.fortuneBlocks.get());
    }

    /** 速度取「当前速度」与「硬度 × 5」中较大者，再以「硬度 × 20」封顶 */
    @Override
    public float breakingBlock(LivingEntity entity, BlockState state, BlockPos pos, float originalSpeed, float newSpeed) {
        if (!this.config.staticMining.get()) {
            return newSpeed;
        }
        if (!canUsePickaxeOn(entity.getMainHandItem(), state)) {
            return newSpeed;
        }
        final float hardness = state.getDestroySpeed(entity.level(), pos);
        if (hardness <= 0F) {
            return newSpeed;
        }
        return Math.min(Math.max(newSpeed, hardness * STATIC_MINING_TARGET_MULTIPLIER),
                hardness * STATIC_MINING_MAX_SAFE_MULTIPLIER);
    }

    @Override
    public int brokeBlock(LivingEntity entity, Level level, BlockState state, BlockPos pos, int expToDrop) {
        if (level.isClientSide) {
            return expToDrop;
        }
        final ItemStack held = entity.getMainHandItem();
        if (!isPickaxe(held) || !state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return expToDrop;
        }
        final boolean silkTouching = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, held) > 0;
        return expToDrop + this.getAdditionalMiningXp(state, silkTouching);
    }

    /** 挖掘等级 +1：镐比方块所需等级低一级时仍可采集（1.12 canBreakBlock(tool, bonusToolLevel = 1)） */
    @Override
    public boolean canHarvest(LivingEntity entity, BlockState state, boolean canHarvest) {
        if (canHarvest || !this.config.skilledMiner.get()) {
            return canHarvest;
        }
        final ItemStack held = entity.getMainHandItem();
        return isPickaxe(held) && state.is(BlockTags.MINEABLE_WITH_PICKAXE) && nextTierCanHarvest(held, state);
    }

    /** 自然时运：名单内方块按「无时运 → 满级时运；有时运 → 叠加（fortuneMix）」重掷，追加多出的掉落 */
    @Override
    public float blockDrops(LivingEntity entity, BlockDropContext context, List<ItemStack> drops, float dropChance) {
        if (!this.config.fortune.get() || context.isSilkTouching() || !isPickaxe(context.tool())
                || !this.fortuneBlocks.matches(context.state())) {
            return dropChance;
        }
        final int fortune = context.fortuneLevel();
        final int natural = this.naturalFortuneLevel(fortune);
        if (natural <= fortune) {
            return dropChance;
        }
        final ItemStack boosted = context.tool().copy();
        final Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(boosted);
        enchantments.put(Enchantments.BLOCK_FORTUNE, natural);
        EnchantmentHelper.setEnchantments(enchantments, boosted);
        addAdditionalDrops(drops, context.dropsWithTool(boosted));
        return dropChance;
    }

    private int naturalFortuneLevel(int fortune) {
        final int max = Enchantments.BLOCK_FORTUNE.getMaxLevel();
        if (fortune <= 0) {
            return max;
        }
        return this.config.fortuneMix.get() ? fortune + max : fortune;
    }

    /** 1.12 草稿 addAdditionalFortuneDrops：每种掉落只补足到重掷结果的数量 */
    private static void addAdditionalDrops(List<ItemStack> drops, List<ItemStack> boostedDrops) {
        for (ItemStack boosted : boostedDrops) {
            final int existing = drops.stream().filter(drop -> ItemStack.isSameItemSameTags(drop, boosted)).mapToInt(ItemStack::getCount).sum();
            final int additional = boosted.getCount() - existing;
            if (additional > 0) {
                drops.add(boosted.copyWithCount(additional));
            }
        }
    }

    private int getAdditionalMiningXp(BlockState state, boolean silkTouching) {
        if (silkTouching) {
            return 0;
        }
        int xp = 0;
        if (this.config.bonusExp.get() && this.bonusExpBlocks.matches(state)) {
            xp += this.rollMiningXp();
        }
        if (this.config.minExp.get() && xp < 1 && this.minExpBlocks.matches(state)) {
            xp = 1;
        }
        return xp;
    }

    private int rollMiningXp() {
        final int max = this.config.bonusExpMax.get();
        final int min = this.config.bonusExpMin.get();
        final int rolled = max < 1 ? min : this.random.nextInt(max);
        return Math.max(min, rolled);
    }

    /**
     * 镐能否作用于该方块：方块不需要特定工具即可；否则须是镐类方块，且本工具或「高一级的工具」能采集
     * （对应 1.12 canUsePickaxeOn(stack, state, bonusToolLevel = 1)）。
     */
    private static boolean canUsePickaxeOn(ItemStack stack, BlockState state) {
        if (!isPickaxe(stack)) {
            return false;
        }
        if (!state.requiresCorrectToolForDrops()) {
            return true;
        }
        if (!state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return false;
        }
        return stack.isCorrectToolForDrops(state) || nextTierCanHarvest(stack, state);
    }

    private static boolean nextTierCanHarvest(ItemStack stack, BlockState state) {
        if (!(stack.getItem() instanceof TieredItem tiered)) {
            return false;
        }
        final List<Tier> tiers = TierSortingRegistry.getSortedTiers();
        final int index = tiers.indexOf(tiered.getTier());
        return index >= 0 && index + 1 < tiers.size()
                && TierSortingRegistry.isCorrectTierForDrops(tiers.get(index + 1), state);
    }

    private static boolean isPickaxe(ItemStack stack) {
        return !stack.isEmpty() && stack.canPerformAction(ToolActions.PICKAXE_DIG);
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final boolean fortune = this.config.fortune.get();
        final boolean bonusExp = this.config.bonusExp.get();
        final int min = this.config.bonusExpMin.get();
        final int max = this.config.bonusExpMax.get();
        variables.flag("fortune", fortune)
                .number("level", Enchantments.BLOCK_FORTUNE.getMaxLevel())
                .flag("mix", fortune && this.config.fortuneMix.get())
                .flag("tier", this.config.skilledMiner.get())
                .number("target", this.config.staticMining.get(), STATIC_MINING_TARGET_MULTIPLIER)
                .number("cap", STATIC_MINING_MAX_SAFE_MULTIPLIER)
                .number("expmin", bonusExp, min)
                .number("expmax", bonusExp, Math.max(min, max - 1))
                .flag("minexp", this.config.minExp.get());
    }
}
