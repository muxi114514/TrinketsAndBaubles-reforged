package xzeroair.trinkets.traits.abilities.elements.fire;

import javax.annotation.Nonnull;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.IAttackAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.util.config.abilities.ImmunityAbilityConfig;

/**
 * 火焰免疫：身上着火立刻熄灭；取消并归零火焰伤害；duration &gt; 0 时给原版火焰抗性。
 *
 * 移植说明：
 * - 火焰伤害判定由 1.12 的 source.isFireDamage() + 字符串补充表，改为 DamageTypeTags.IS_FIRE。
 * - 1.12 的 duration 语义是「每 tick 缺则补一段」，实际等价于常驻；1.20.1 用无限时长一次施加，
 *   duration 在这里只作为「是否给抗性效果」的开关。
 * - 1.12 装有 FireResistanceTiers 时会让出免疫判定并改给分级抗性，属 P7 模组联动，暂未接入。
 */
public class AbilityFireImmunity extends Ability implements ITickableAbility, IAttackAbility {

    private final boolean grantResistance;

    public AbilityFireImmunity(@Nonnull ImmunityAbilityConfig config) {
        super(AbilityNames.IMMUNITY_FIRE);
        this.setAbilityEnabled(config.isEnabled());
        this.grantResistance = config.duration.get() > 0;
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        if (entity.isOnFire()) {
            entity.clearFire();
        }
        if (this.grantResistance && !entity.level().isClientSide && !entity.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                    MobEffectInstance.INFINITE_DURATION, 0, false, false));
        }
    }

    @Override
    public void onAbilityRemoved(LivingEntity entity) {
        final MobEffectInstance active = entity.getEffect(MobEffects.FIRE_RESISTANCE);
        // 只清掉本能力给的常驻抗性，不误删玩家自己喝的限时火抗药水
        if (active != null && active.isInfiniteDuration()) {
            entity.removeEffect(MobEffects.FIRE_RESISTANCE);
        }
    }

    @Override
    public boolean attacked(LivingEntity attacked, DamageSource source, float dmg, boolean cancel) {
        return source.is(DamageTypeTags.IS_FIRE) || cancel;
    }

    @Override
    public float damaged(LivingEntity attacked, DamageSource source, float dmg) {
        return source.is(DamageTypeTags.IS_FIRE) ? 0 : dmg;
    }

    @Override
    public void describe(DescriptionVariables variables) {
        variables.translated("potion", this.grantResistance, MobEffects.FIRE_RESISTANCE.getDescriptionId());
    }
}
