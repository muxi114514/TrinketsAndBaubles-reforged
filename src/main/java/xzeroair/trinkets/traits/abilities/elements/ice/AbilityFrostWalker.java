package xzeroair.trinkets.traits.abilities.elements.ice;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.FrostWalkerEnchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import xzeroair.trinkets.init.ModBlocks;
import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.IAttackAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.util.config.abilities.IAbilityConfig;
import xzeroair.trinkets.util.helpers.ChunkSafety;
import xzeroair.trinkets.util.helpers.LiquidFreezer;

/**
 * 霜行者：脚下水面结冰、岩浆冷却成冷却岩浆（等效冰霜行者附魔等级 +1），免疫岩浆块的烫脚伤害。
 *
 * 移植说明：
 * - 1.12 自写 BlockHelperUtil.freezeWater 逐格扫描并 setBlock；1.20.1 直接复用原版
 *   FrostWalkerEnchantment.onEntityMoved，但**调用前先守卫其作用半径覆盖的区块全部已加载**——
 *   原版方法内部直接 getBlockState，跨区块边界时会同步加载区块、卡住服务端主线程。
 * - 1.12 靠 EntityProperties 的 prevBlockpos 判断「是否移动」，那套速度追踪未移植，
 *   这里在能力实例上记录上一个方块坐标，效果相同。
 * - 冻结岩浆（1.12 BlockHelperUtil.freezeLava）原版附魔没有，走 LiquidFreezer 生成冷却岩浆（blocks/CooledMagmaBlock，对应 1.12 TempBlock）。
 */
public class AbilityFrostWalker extends Ability implements ITickableAbility, IAttackAbility {

    /** 原版冰霜行者的半径公式为 min(16, 2 + 等级) */
    private static final int MAX_RADIUS = 16;
    /** 冻结半径 = 本值 + (冰霜行者附魔等级 + 1)，与原版冰霜行者一致 */
    private static final int BASE_RADIUS = 2;

    @Nullable
    private BlockPos lastPos;

    public AbilityFrostWalker(@Nonnull IAbilityConfig config) {
        super(AbilityNames.FROST_WALKER);
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public boolean attacked(LivingEntity attacked, @Nonnull DamageSource source, float dmg, boolean cancel) {
        return source.is(DamageTypes.HOT_FLOOR) || cancel;
    }

    @Override
    public void tickAbility(@Nonnull LivingEntity entity) {
        final Level level = entity.level();
        if (level.isClientSide || this.isSpectator(entity) || !(entity instanceof Player)) {
            return;
        }
        if (EnchantmentHelper.getDepthStrider(entity) > 0) {
            return;
        }
        final BlockPos pos = entity.blockPosition();
        if (pos.equals(this.lastPos)) {
            return;
        }
        this.lastPos = pos;
        if (!entity.onGround()) {
            return;
        }
        final int enchantLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FROST_WALKER, entity) + 1;
        final int radius = Math.min(MAX_RADIUS, BASE_RADIUS + enchantLevel);
        if (!ChunkSafety.isAreaLoaded(level, pos.getX(), pos.getZ(), radius)) {
            return;
        }
        FrostWalkerEnchantment.onEntityMoved(entity, level, pos, enchantLevel);
        LiquidFreezer.freeze(level, entity.position(), radius, false);
    }

    @Override
    public void describe(DescriptionVariables variables) {
        variables.number("radius", BASE_RADIUS + 1)
                .number("max", MAX_RADIUS)
                .translated("ice", true, Blocks.FROSTED_ICE.getDescriptionId())
                .translated("magma", true, ModBlocks.COOLED_MAGMA.get().getDescriptionId());
    }
}
