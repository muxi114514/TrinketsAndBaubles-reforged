package xzeroair.trinkets.entity.area.action;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.entity.area.AreaEffectAction;
import xzeroair.trinkets.entity.area.AreaEffectEntity;

/** 对范围内的生物造成间接魔法伤害（中性吐息的残留区域）。 */
public class DamageAreaAction implements AreaEffectAction {

    public static final String TYPE = "damage";

    private final float damage;

    public DamageAreaAction(float damage) {
        this.damage = Math.max(0.0F, damage);
    }

    public static DamageAreaAction load(CompoundTag tag) {
        return new DamageAreaAction(tag.getFloat("Damage"));
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void save(CompoundTag tag) {
        tag.putFloat("Damage", this.damage);
    }

    @Override
    public boolean canAffectEntity(AreaEffectEntity area, Entity entity) {
        return this.damage > 0.0F && entity instanceof LivingEntity;
    }

    @Override
    public void affectEntity(AreaEffectEntity area, Entity entity) {
        entity.hurt(area.damageSources().indirectMagic(area, area.getOwner()), this.damage);
    }
}
