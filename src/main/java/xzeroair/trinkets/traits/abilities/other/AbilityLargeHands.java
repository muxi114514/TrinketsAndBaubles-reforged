package xzeroair.trinkets.traits.abilities.other;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;

import xzeroair.trinkets.enums.ActivationMethod;
import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.IAttackAbility;
import xzeroair.trinkets.traits.abilities.interfaces.IMiningAbility;
import xzeroair.trinkets.util.config.abilities.LargeHandsAbilityConfig;
import xzeroair.trinkets.util.helpers.AreaMiningHelper;
import xzeroair.trinkets.util.helpers.BlockMatcher;
import xzeroair.trinkets.util.helpers.ChunkSafety;

/**
 * 大手：近战溅射周围实体 25% 伤害；空手按对应木质工具的效率挖掘；满足触发条件时 3×3×3 范围挖掘。
 *
 * 移植说明：
 * - 溅射伤害 1.12 用 EntityDamageSourceIndirect 构造，借「间接伤害不再触发溅射」防止无限递归。
 *   1.20.1 构造「直接来源 = 主目标、施加者 = 攻击者」的伤害源，二者不同即被判为间接伤害，防递归机制不变。
 * - 1.12 的 correctGroundedDigSpeed（空中挖掘速度 ×5）补偿的是 1.12 体型实现下原版 onGround 不准的问题，
 *   1.20.1 经 EntityEvent.Size 改的是真实尺寸，onGround 本身准确，该补偿不再移植。
 * - 范围挖掘的准入规则照搬 1.12：可编辑、工具对其有效、**目标硬度不超过被挖方块硬度**（挖泥土不会连带挖石头）、
 *   工具能采集；另按区块安全规范，先确认整个范围覆盖的区块均已加载，否则本次不做范围挖掘。
 */
public class AbilityLargeHands extends Ability implements IAttackAbility, IMiningAbility {

    private static final float SPLASH_RATIO = 0.25F;
    private static final int AREA_SIZE = 3;

    private final LargeHandsAbilityConfig config;
    private final BlockMatcher blacklist;

    /** 范围挖掘时会对每个方块再触发破坏事件，以此防止重入 */
    private boolean breakingExtendedArea;

    public AbilityLargeHands(@Nonnull LargeHandsAbilityConfig config) {
        super(AbilityNames.LARGE_HANDS);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
        this.blacklist = BlockMatcher.of(config.miningExtendedBlacklist.get());
    }

    // ── 溅射 ──

    @Override
    public float hurtEntity(LivingEntity target, DamageSource source, float dmg) {
        if (this.isIndirectDamage(source)) {
            return dmg;
        }
        final Entity attacker = source.getEntity();
        final DamageSource splashSource = splashSource(target, attacker);
        final AABB area = target.getBoundingBox().inflate(1);
        final List<Entity> splash = target.level().getEntities(target, area,
                hit -> hit.isPickable() && !hit.isSpectator() && hit != attacker);
        for (Entity hit : splash) {
            hit.hurt(splashSource, dmg * SPLASH_RATIO);
        }
        return dmg;
    }

    private static DamageSource splashSource(LivingEntity target, @Nullable Entity attacker) {
        final ResourceKey<DamageType> type = attacker instanceof Player ? DamageTypes.PLAYER_ATTACK
                : attacker instanceof LivingEntity ? DamageTypes.MOB_ATTACK : DamageTypes.GENERIC;
        final Holder<DamageType> holder = target.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type);
        return new DamageSource(holder, target, attacker);
    }

    // ── 空手挖掘 ──

    /**
     * 空手时按对应木质工具的效率挖：事件给出的速度已含急迫/挖掘疲劳/水下等乘法修正，
     * 空手的基础效率是 1，故乘上木质工具对该方块的效率即可；方块本不可空手采集时再 ×2（与 1.12 一致）。
     */
    @Override
    public float breakingBlock(LivingEntity entity, BlockState state, BlockPos pos, float originalSpeed, float newSpeed) {
        if (!(entity instanceof Player player) || !player.getMainHandItem().isEmpty()) {
            return newSpeed;
        }
        final ItemStack tool = harvestToolFor(state, ItemStack.EMPTY);
        if (tool.isEmpty()) {
            return newSpeed;
        }
        float speed = newSpeed * tool.getDestroySpeed(state);
        if (state.getDestroySpeed(entity.level(), pos) > 0F && !ForgeHooks.isCorrectToolForDrops(state, player)) {
            speed *= 2F;
        }
        return speed;
    }

    // ── 范围挖掘 ──

    @Override
    public int brokeBlock(LivingEntity entity, Level level, BlockState state, BlockPos pos, int expToDrop) {
        if (level.isClientSide || this.breakingExtendedArea || !(entity instanceof ServerPlayer player)) {
            return expToDrop;
        }
        if (!this.config.miningExtended.get().isActive(player.isShiftKeyDown()) || this.blacklist.matches(state)) {
            return expToDrop;
        }
        final ItemStack tool = harvestToolFor(state, player.getMainHandItem());
        if (!canHarvest(tool, state) || !isToolEffective(tool, state)) {
            return expToDrop;
        }
        if (!ChunkSafety.isAreaLoaded(level, pos.getX(), pos.getZ(), AREA_SIZE)) {
            return expToDrop;
        }
        final float originHardness = state.getDestroySpeed(level, pos);
        final List<BlockPos> area = AreaMiningHelper.collectArea(player, pos, AREA_SIZE, AREA_SIZE, AREA_SIZE, null);
        this.breakingExtendedArea = true;
        try {
            for (BlockPos target : area) {
                if (this.canBreakInArea(player, tool, level, originHardness, target)) {
                    AreaMiningHelper.breakWithTool(player, tool, target);
                }
            }
        } finally {
            this.breakingExtendedArea = false;
        }
        return 0;
    }

    /** 对应 1.12 BlockHelperUtil.canBreakBlock */
    private boolean canBreakInArea(Player player, ItemStack tool, Level level, float originHardness, BlockPos target) {
        final BlockState state = level.getBlockState(target);
        if (state.isAir() || this.blacklist.matches(state)) {
            return false;
        }
        if (!player.mayUseItemAt(target, Direction.UP, tool) || !isToolEffective(tool, state)) {
            return false;
        }
        final float hardness = state.getDestroySpeed(level, target);
        if (originHardness < 0F || hardness < 0F || hardness > originHardness) {
            return false;
        }
        return canHarvest(tool, state);
    }

    /** 空手时按方块所需工具换成对应的木质工具（对应 1.12 getHarvestTool） */
    private static ItemStack harvestToolFor(BlockState state, ItemStack held) {
        if (!held.isEmpty()) {
            return held.copy();
        }
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return new ItemStack(Items.WOODEN_PICKAXE);
        }
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return new ItemStack(Items.WOODEN_SHOVEL);
        }
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return new ItemStack(Items.WOODEN_AXE);
        }
        return ItemStack.EMPTY;
    }

    private static boolean canHarvest(ItemStack tool, BlockState state) {
        return !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
    }

    /**
     * 工具类型是否对该方块有效（对应 1.12 isToolEffective：工具类别与方块所需工具一致，或方块不需要特定工具）。
     * 1.20.1 以「是否属于任一可挖标签」判断方块是否需要工具，以「效率 &gt; 1」判断工具类别是否对口。
     */
    private static boolean isToolEffective(ItemStack tool, BlockState state) {
        final boolean needsTool = state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.is(BlockTags.MINEABLE_WITH_AXE) || state.is(BlockTags.MINEABLE_WITH_HOE);
        return !needsTool || tool.getDestroySpeed(state) > 1.0F;
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final ActivationMethod mode = this.config.miningExtended.get();
        variables.percent("splash", true, SPLASH_RATIO)
                .activation("mode", mode != ActivationMethod.NEVER, mode)
                .number("size", AREA_SIZE);
    }
}
