package xzeroair.trinkets.entity.area.action;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import xzeroair.trinkets.entity.area.AreaEffectAction;
import xzeroair.trinkets.entity.area.AreaEffectEntity;

/**
 * 修复范围内掉落物形态的受损物品。
 *
 * 移植说明：1.12 另有抽象的 ItemAreaAction 基类，只有本类一个子类，合并进来。
 */
public class RepairItemAreaAction implements AreaEffectAction {

    public static final String TYPE = "repair_item";

    private final int repairAmount;

    public RepairItemAreaAction(int repairAmount) {
        this.repairAmount = Math.max(0, repairAmount);
    }

    public static RepairItemAreaAction load(CompoundTag tag) {
        return new RepairItemAreaAction(tag.getInt("RepairAmount"));
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void save(CompoundTag tag) {
        tag.putInt("RepairAmount", this.repairAmount);
    }

    @Override
    public boolean canAffectEntity(AreaEffectEntity area, Entity entity) {
        if (this.repairAmount <= 0 || !(entity instanceof ItemEntity item)) {
            return false;
        }
        final ItemStack stack = item.getItem();
        return !stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged();
    }

    @Override
    public void affectEntity(AreaEffectEntity area, Entity entity) {
        final ItemEntity item = (ItemEntity) entity;
        final ItemStack stack = item.getItem().copy();
        final int previous = stack.getDamageValue();
        final int repaired = Math.max(0, previous - this.repairAmount);
        if (repaired != previous) {
            stack.setDamageValue(repaired);
            item.setItem(stack);
            area.spawnBonemealParticles(item.blockPosition(), 8, 0.35D, 0.2D);
        }
    }
}
