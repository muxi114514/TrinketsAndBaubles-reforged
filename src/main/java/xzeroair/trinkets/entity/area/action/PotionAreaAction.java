package xzeroair.trinkets.entity.area.action;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.entity.area.AreaEffectAction;
import xzeroair.trinkets.entity.area.AreaEffectEntity;

/** 给范围内的生物施加药水效果；瞬时效果（治疗/伤害）按强度直接结算。 */
public class PotionAreaAction implements AreaEffectAction {

    public static final String TYPE = "potion";

    private final MobEffectInstance effect;
    private final double instantStrength;

    public PotionAreaAction(MobEffectInstance effect) {
        this(effect, 0.5D);
    }

    public PotionAreaAction(MobEffectInstance effect, double instantStrength) {
        this.effect = effect;
        this.instantStrength = instantStrength;
    }

    public static PotionAreaAction load(CompoundTag tag) {
        final MobEffectInstance effect = MobEffectInstance.load(tag.getCompound("Effect"));
        if (effect == null) {
            throw new IllegalArgumentException("Missing potion effect");
        }
        return new PotionAreaAction(effect, tag.contains("InstantStrength") ? tag.getDouble("InstantStrength") : 0.5D);
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void save(CompoundTag tag) {
        tag.put("Effect", this.effect.save(new CompoundTag()));
        tag.putDouble("InstantStrength", this.instantStrength);
    }

    @Override
    public boolean canAffectEntity(AreaEffectEntity area, Entity entity) {
        return entity instanceof LivingEntity living && living.isAffectedByPotions();
    }

    @Override
    public void affectEntity(AreaEffectEntity area, Entity entity) {
        final LivingEntity living = (LivingEntity) entity;
        if (this.effect.getEffect().isInstantenous()) {
            this.effect.getEffect().applyInstantenousEffect(area, area.getOwner(), living, this.effect.getAmplifier(), this.instantStrength);
        } else {
            living.addEffect(new MobEffectInstance(this.effect), area.getOwner());
        }
    }
}
