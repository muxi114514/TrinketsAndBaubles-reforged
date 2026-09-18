package xzeroair.trinkets.items.foods;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.init.ModItems;
import xzeroair.trinkets.items.base.FoodBase;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.helpers.ChunkSafety;

/**
 * 魔力水晶：食用后额外魔力上限点数 +1；配置开启时对着石头顶面使用有 1/10 概率碎裂成魔力试剂并小爆一下。
 * 对应 1.12 items/foods/Mana_Crystal。
 *
 * 移植说明：
 * - 1.12 判定「材质为岩石且是完整方块」；材质系统于 1.20 移除，改为「属于 forge:stone 标签且碰撞箱为整格」。
 * - 1.12 的 createExplosion(..., false) 不破坏地形，对应 1.20.1 的 ExplosionInteraction.NONE。
 *   但即便不破坏方块，爆炸仍会沿射线读取半径内的方块状态——按区块安全规范，先确认波及范围的区块均已加载，
 *   否则本次不触发碎裂。
 */
public class ManaCrystal extends FoodBase {

    public static final float EXPLOSION_POWER = 3F;
    /** 对石头使用时 1/N 概率碎裂 */
    public static final int SHATTER_CHANCE = 10;
    /** 3 级爆炸射线最远约 6 格，留出余量 */
    private static final int EXPLOSION_GUARD_RADIUS = 8;

    public ManaCrystal() {
        super(FoodBase.props(2, 1F, true), 32, UseAnim.EAT);
    }

    @Nonnull
    @Override
    public ItemStack finishUsingItem(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity entity) {
        if (!level.isClientSide) {
            final MagicStats magic = MagicStats.get(entity);
            if (magic != null) {
                magic.setBonusMana(magic.getBonusMana() + 1);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Nonnull
    @Override
    public InteractionResult useOn(@Nonnull UseOnContext context) {
        if (!TrinketsConfig.SERVER.magic.crystalExplodes.get()) {
            return super.useOn(context);
        }
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final BlockState state = level.getBlockState(pos);
        final boolean shatter = level.random.nextInt(SHATTER_CHANCE) == 0
                && state.is(Tags.Blocks.STONE)
                && state.isCollisionShapeFullBlock(level, pos)
                && context.getClickedFace() == Direction.UP;
        if (!shatter) {
            if (!level.isClientSide) {
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return super.useOn(context);
        }
        if (!level.isClientSide) {
            if (!ChunkSafety.isAreaLoaded(level, pos.getX(), pos.getZ(), EXPLOSION_GUARD_RADIUS)) {
                return InteractionResult.PASS;
            }
            context.getItemInHand().shrink(1);
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1, pos.getZ() + 0.5D,
                    new ItemStack(ModItems.MANA_REAGENT.get())));
            level.explode(null, pos.getX(), pos.getY(), pos.getZ(), EXPLOSION_POWER, Level.ExplosionInteraction.NONE);
            level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
