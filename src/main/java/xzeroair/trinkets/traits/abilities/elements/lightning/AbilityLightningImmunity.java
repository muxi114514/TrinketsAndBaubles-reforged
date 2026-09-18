package xzeroair.trinkets.traits.abilities.elements.lightning;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.api.ItemHandlerType;
import xzeroair.trinkets.init.ModEffects;
import xzeroair.trinkets.traits.AbilityHandler.AbilityHolder;
import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.IAttackAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ILightningStrikeAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.util.config.abilities.IAbilityConfig;

/**
 * 雷电免疫：取消雷电伤害；被雷劈时取消该次雷击并给 1 秒火焰抗性（防止雷击引燃）。
 *
 * 移植说明：雷电伤害判定由 1.12 的字符串比对改为 DamageTypeTags.IS_LIGHTNING；
 * 效果图标改用无限时长，理由同冰霜免疫。
 */
public class AbilityLightningImmunity extends Ability implements ITickableAbility, IAttackAbility, ILightningStrikeAbility {

    /** 被雷击时给予的抗火 II 时长 */
    private static final int STRIKE_FIRE_RESISTANCE_TICKS = 20;

    public AbilityLightningImmunity(@Nonnull IAbilityConfig config) {
        super(AbilityNames.IMMUNITY_LIGHTNING);
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        final AbilityHolder holder = this.getAbilityHolder();
        if (holder != null && holder.getInfo().getHandlerType() == ItemHandlerType.POTION) {
            return;
        }
        if (!entity.level().isClientSide && !entity.hasEffect(ModEffects.LIGHTNING_RESISTANCE.get())) {
            entity.addEffect(new MobEffectInstance(ModEffects.LIGHTNING_RESISTANCE.get(),
                    MobEffectInstance.INFINITE_DURATION, 0, false, false));
        }
    }

    @Override
    public boolean attacked(LivingEntity attacked, @Nonnull DamageSource source, float dmg, boolean cancel) {
        return source.is(DamageTypeTags.IS_LIGHTNING) || cancel;
    }

    @Override
    public boolean onStruckByLightning(LivingEntity entity, boolean cancel) {
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, STRIKE_FIRE_RESISTANCE_TICKS, 1));
        return true;
    }

    @Override
    public void onAbilityRemoved(LivingEntity entity) {
        final AbilityHolder holder = this.getAbilityHolder();
        if (holder != null && holder.getInfo().getHandlerType() == ItemHandlerType.POTION) {
            return;
        }
        entity.removeEffect(ModEffects.LIGHTNING_RESISTANCE.get());
    }

    @Override
    public void describe(DescriptionVariables variables) {
        variables.translated("potion", true, ModEffects.LIGHTNING_RESISTANCE.get().getDescriptionId())
                .effects("strike", true, List.of("minecraft:fire_resistance:" + STRIKE_FIRE_RESISTANCE_TICKS + ":1"), true);
    }
}
