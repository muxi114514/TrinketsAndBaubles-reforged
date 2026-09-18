package xzeroair.trinkets.blocks.entity;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import xzeroair.trinkets.init.ModBlockEntities;

/**
 * 保存放下时的泰迪熊物品。对应 1.12 TileEntityTeddyBear。
 *
 * 移植说明：1.12 同时写进 TE 自身 NBT 与 TileEntityProperties 能力 NBT 两份，且记录一个从未被渲染使用的 16 向旋转值；
 * 1.20.1 只存一份物品，朝向由方块状态表示。外观只由方块状态决定，客户端不需要物品数据，故不做同步包。
 */
public class TeddyBearBlockEntity extends BlockEntity {

    private static final String TAG = "TeddyBear";

    private ItemStack teddyBear = ItemStack.EMPTY;

    public TeddyBearBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEDDY_BEAR.get(), pos, state);
    }

    public ItemStack getTeddyBear() {
        return this.teddyBear;
    }

    public void setTeddyBear(ItemStack stack) {
        this.teddyBear = stack.copyWithCount(1);
        this.setChanged();
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        if (!this.teddyBear.isEmpty()) {
            tag.put(TAG, this.teddyBear.save(new CompoundTag()));
        }
    }

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        this.teddyBear = tag.contains(TAG) ? ItemStack.of(tag.getCompound(TAG)) : ItemStack.EMPTY;
    }
}
