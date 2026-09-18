package xzeroair.trinkets.traits.abilities;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.ISleepAbility;
import xzeroair.trinkets.util.config.abilities.WellRestedAbilityConfig;
import xzeroair.trinkets.util.helpers.PotionHelper;

/**
 * 充分休息：睡满一整夜醒来后获得配置中的效果；randomBonuses &gt; 0 时改为随机抽取若干条（可重复抽中，重复则叠加）。
 *
 * 移植说明：1.12 同时实现了 IHeldAbility 但方法为空，不再保留。
 */
public class AbilityWellRested extends Ability implements ISleepAbility {

    private final WellRestedAbilityConfig config;

    public AbilityWellRested(@Nonnull WellRestedAbilityConfig config) {
        super(AbilityNames.WELL_RESTED);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public void onWakeUp(@Nonnull LivingEntity entity, boolean wakeImmediately, boolean updatedWorld, boolean setSpawn) {
        if (entity.level().isClientSide) {
            return;
        }
        // 事件在清零睡眠计时之前触发，此时仍可判断是否睡满
        if (entity instanceof Player player && !player.isSleepingLongEnough()) {
            return;
        }
        final List<? extends String> bonuses = this.config.sleepBonuses.get();
        if (bonuses.isEmpty()) {
            return;
        }
        final int picks = Math.min(this.config.randomBonuses.get(), bonuses.size());
        if (picks > 0) {
            for (int i = 0; i < picks; i++) {
                this.apply(entity, bonuses.get(this.random.nextInt(bonuses.size())), true);
            }
        } else {
            for (String bonus : bonuses) {
                this.apply(entity, bonus, false);
            }
        }
    }

    private void apply(LivingEntity entity, String entry, boolean combine) {
        final PotionHelper.ParsedEffect parsed = PotionHelper.parse(entry);
        if (parsed == null) {
            return;
        }
        final MobEffectInstance active = entity.getEffect(parsed.effect());
        if (combine && active != null) {
            active.update(parsed.toInstance());
        } else {
            entity.addEffect(parsed.toInstance());
        }
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final List<? extends String> bonuses = this.config.sleepBonuses.get();
        final int picks = Math.min(this.config.randomBonuses.get(), bonuses.size());
        variables.effects("all", picks <= 0, bonuses, true)
                .effects("random", picks > 0, bonuses, true)
                .number("picks", picks);
    }
}
