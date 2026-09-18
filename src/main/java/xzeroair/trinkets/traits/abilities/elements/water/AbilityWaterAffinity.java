package xzeroair.trinkets.traits.abilities.elements.water;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;

import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.IMiningAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.util.config.abilities.ElementAffinityAbilityConfigs;

/**
 * 水系亲和：水下保持至少 bubbles 个气泡（或改给原版水下呼吸）；水下挖掘不受减速（抵消原版的水下与离地两次 1/5）。
 *
 * 移植说明：
 * - 每个气泡 30 点空气，1.20.1 与 1.12 相同。
 * - 移除能力时 1.12 会清掉任何水下呼吸；这里只清本能力给的短时效果（≤ 60 tick），不误删玩家自己喝的药水。
 * - 1.12 的 Better Diving 联动随 P7 兼容层补齐。
 */
public class AbilityWaterAffinity extends Ability implements ITickableAbility, IMiningAbility {

    private static final int AIR_PER_BUBBLE = 30;
    private static final int BREATHING_DURATION = 60;

    private final ElementAffinityAbilityConfigs.Water config;

    public AbilityWaterAffinity(@Nonnull ElementAffinityAbilityConfigs.Water config) {
        super(AbilityNames.AFFINITY_WATER);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        final int bubbles = this.config.bubbles.get();
        if (entity.level().isClientSide || bubbles <= 0) {
            return;
        }
        if (this.config.vanilla.get()) {
            if (entity.tickCount % 59 != 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, BREATHING_DURATION, 0, false, false));
            }
        } else if (entity.getAirSupply() < bubbles * AIR_PER_BUBBLE) {
            entity.setAirSupply(bubbles * AIR_PER_BUBBLE);
        }
    }

    @Override
    public void onAbilityRemoved(LivingEntity entity) {
        final MobEffectInstance active = entity.getEffect(MobEffects.WATER_BREATHING);
        if (this.config.vanilla.get() && active != null && !active.isInfiniteDuration()
                && active.getDuration() <= BREATHING_DURATION) {
            entity.removeEffect(MobEffects.WATER_BREATHING);
        }
    }

    @Override
    public float breakingBlock(LivingEntity entity, BlockState state, BlockPos pos, float originalSpeed, float newSpeed) {
        if (!this.config.underwaterMining.get() || !entity.isEyeInFluid(FluidTags.WATER)
                || EnchantmentHelper.hasAquaAffinity(entity)) {
            return newSpeed;
        }
        float speed = originalSpeed * 5F;
        if (!entity.onGround()) {
            speed *= 5F;
        }
        return Math.max(newSpeed, speed);
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final int bubbles = this.config.bubbles.get();
        final boolean vanilla = this.config.vanilla.get();
        variables.number("bubbles", bubbles > 0 && !vanilla, bubbles)
                .number("air", bubbles * AIR_PER_BUBBLE)
                .translated("breathing", bubbles > 0 && vanilla, MobEffects.WATER_BREATHING.getDescriptionId())
                .flag("mining", this.config.underwaterMining.get());
    }
}
