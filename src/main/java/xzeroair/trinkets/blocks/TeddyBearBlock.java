package xzeroair.trinkets.blocks;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import xzeroair.trinkets.blocks.entity.TeddyBearBlockEntity;

/**
 * 放下的泰迪熊：朝向 + 外观变种，方块实体保存放下时的物品（掉落时原样返还，保留名称/制作者/附魔）。
 * 需要下方为坚固顶面，无碰撞箱。对应 1.12 blocks/BlockTeddyBear 与 TileEntityTeddyBear。
 */
public class TeddyBearBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final EnumProperty<TeddyBearType> VARIANT = EnumProperty.create("variant", TeddyBearType.class);

    private static final VoxelShape NORTH_SOUTH = Block.box(3.2D, 0.0D, 2.4D, 12.8D, 12.8D, 13.6D);
    private static final VoxelShape EAST_WEST = Block.box(2.4D, 0.0D, 3.2D, 13.6D, 12.8D, 12.8D);

    public TeddyBearBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(VARIANT, TeddyBearType.NORMAL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VARIANT);
    }

    /** 物品上的外观决定方块变种，面朝放置者 */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(VARIANT, TeddyBearType.of(context.getItemInHand()));
    }

    @Nonnull
    @Override
    public VoxelShape getShape(BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NORTH_SOUTH : EAST_WEST;
    }

    @Nonnull
    @Override
    public VoxelShape getCollisionShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos,
            @Nonnull CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean canSurvive(@Nonnull BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    /** 失去支撑时变为空气（原版随之按掉落处理，走 getDrops） */
    @Nonnull
    @Override
    public BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction direction, @Nonnull BlockState neighbor,
            @Nonnull LevelAccessor level, @Nonnull BlockPos pos, @Nonnull BlockPos neighborPos) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    /** 掉落放下时的那只泰迪熊（对应 1.12 getDrops 读方块实体） */
    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public List<ItemStack> getDrops(@Nonnull BlockState state, LootParams.Builder params) {
        final BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (entity instanceof TeddyBearBlockEntity teddy && !teddy.getTeddyBear().isEmpty()) {
            return List.of(teddy.getTeddyBear().copy());
        }
        return List.of();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new TeddyBearBlockEntity(pos, state);
    }
}
