package xzeroair.trinkets.traits.abilities.elements.ice;

import javax.annotation.Nonnull;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.api.ItemHandlerType;
import xzeroair.trinkets.init.ModDamageTypes;
import xzeroair.trinkets.init.ModEffects;
import xzeroair.trinkets.traits.AbilityHandler.AbilityHolder;
import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.IAttackAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.util.config.abilities.IAbilityConfig;

/**
 * 冰霜免疫：取消冰冻伤害；来源不是药水时顺带给出「冰霜抗性」效果图标。
 *
 * 移植说明：
 * - 1.12 用 DamageTypeConfigParser.isIceDamage(damageType) 按字符串判定；1.20.1 伤害类型数据驱动，
 *   改用 xat:is_ice 标签（含原版 is_freezing 与龙息冰伤），数据包可追加其他模组的冰伤。
 * - 1.12 每 tick 补 400 tick 的效果，再在客户端用 setPotionDurationMax 把显示改成无限；
 *   1.20.1 原生支持无限时长效果（INFINITE_DURATION），一次施加即可，能力移除时清掉。
 */
public class AbilityIceImmunity extends Ability implements ITickableAbility, IAttackAbility {

    public AbilityIceImmunity(@Nonnull IAbilityConfig config) {
        super(AbilityNames.IMMUNITY_ICE);
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        // 来源就是抗性药水时不再反向给药水，避免「效果 ↔ 能力」互相续命
        final AbilityHolder holder = this.getAbilityHolder();
        if (holder != null && holder.getInfo().getHandlerType() == ItemHandlerType.POTION) {
            return;
        }
        if (!entity.level().isClientSide && !entity.hasEffect(ModEffects.ICE_RESISTANCE.get())) {
            entity.addEffect(new MobEffectInstance(ModEffects.ICE_RESISTANCE.get(),
                    MobEffectInstance.INFINITE_DURATION, 0, false, false));
        }
    }

    @Override
    public boolean attacked(LivingEntity attacked, @Nonnull DamageSource source, float dmg, boolean cancel) {
        return source.is(ModDamageTypes.IS_ICE) || cancel;
    }

    @Override
    public void onAbilityRemoved(LivingEntity entity) {
        final AbilityHolder holder = this.getAbilityHolder();
        if (holder != null && holder.getInfo().getHandlerType() == ItemHandlerType.POTION) {
            return;
        }
        entity.removeEffect(ModEffects.ICE_RESISTANCE.get());
    }

    @Override
    public void describe(DescriptionVariables variables) {
        variables.translated("potion", true, ModEffects.ICE_RESISTANCE.get().getDescriptionId());
    }
}
