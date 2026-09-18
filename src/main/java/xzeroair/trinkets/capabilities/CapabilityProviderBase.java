package xzeroair.trinkets.capabilities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

/**
 * 通用能力提供者。
 *
 * 移植说明：1.12 的 IStorage（原 CapabilityStorage）在 1.20.1 已从 Forge 移除——
 * 序列化不再经 Capability.writeNBT/readNBT 中转，改由本提供者直接调用 handler 的 saveToNBT/loadFromNBT。
 * 另外 hasCapability + getCapability 两个方法合并为返回 LazyOptional 的单方法。
 *
 * 只以 ITrinketCapability 为界：提供者仅负责存取与失效，不关心宿主是实体还是物品栈。
 *
 * @param <H> 能力实现类型
 */
public class CapabilityProviderBase<H extends ITrinketCapability<?>> implements ICapabilitySerializable<CompoundTag> {

    protected final Capability<H> capability;
    protected final H handler;
    protected final LazyOptional<H> optional;

    public CapabilityProviderBase(final Capability<H> capability, final H handler) {
        this.capability = capability;
        this.handler = handler;
        this.optional = LazyOptional.of(() -> handler);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == this.capability ? this.optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return this.handler.saveToNBT(new CompoundTag());
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.handler.loadFromNBT(nbt);
    }

    /** 宿主失效时调用，避免持有者泄漏 */
    public void invalidate() {
        this.optional.invalidate();
    }

    public H getInstance() {
        return this.handler;
    }
}
