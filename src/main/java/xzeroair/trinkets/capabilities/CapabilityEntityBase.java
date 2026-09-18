package xzeroair.trinkets.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import xzeroair.trinkets.util.helpers.NBTHelper;

/**
 * 实体能力基类：提供生命周期钩子。
 * 移植说明：1.12 的 onChangedDimension(int from, int to) 维度用 int 标识，
 * 1.20.1 维度是 ResourceKey&lt;Level&gt;，故签名随之变更。
 */
public abstract class CapabilityEntityBase<T extends CapabilityEntityBase<T, E>, E extends LivingEntity>
        extends CapabilityBase<T, E> {

    protected CapabilityEntityBase(E object) {
        super(object);
    }

    public E getEntity() {
        return this.getObject();
    }

    @Override
    public CompoundTag getTag() {
        return NBTHelper.getEntityTag(this.getEntity(), super.getTag());
    }

    protected boolean isCreativePlayer() {
        return this.getEntity() instanceof Player player && player.isCreative();
    }

    protected boolean isSpectatorPlayer() {
        return this.getEntity() instanceof Player player && player.isSpectator();
    }

    public void onUpdatePre() {
    }

    public abstract void onUpdate();

    public void onJoinWorld() {
    }

    public void onLogin() {
    }

    public void onLogoff() {
    }

    public void onChangedDimension(ResourceKey<Level> from, ResourceKey<Level> to) {
    }
}
