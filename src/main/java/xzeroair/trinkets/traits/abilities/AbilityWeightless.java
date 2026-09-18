package xzeroair.trinkets.traits.abilities;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.IJumpAbility;
import xzeroair.trinkets.traits.abilities.interfaces.IPotionAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.util.compat.ModCompat;
import xzeroair.trinkets.util.config.abilities.IAbilityConfig;

/**
 * 失重：离地后竖直速度归零（悬停），挥手时上升、潜行挥手时下降；免疫坠落。
 *
 * 装有恐怖生物（Lycanites Mobs）时免疫其「重量」效果：施加时拒绝（阻断层），佩戴前已有的在 tick 中清除（清理层）。
 * 移植说明：1.12 只有阻断层。
 * 玩家移动由客户端权威，本能力两端都 tick，与 1.12 一致。
 */
public class AbilityWeightless extends Ability implements ITickableAbility, IJumpAbility, IPotionAbility {

    /** 挥手时的上升 / 下降速度（格/刻） */
    private static final double VERTICAL_SPEED = 0.1D;
    private static final ResourceLocation LYCANITES_WEIGHT = new ResourceLocation(ModCompat.LYCANITES_MOBS, "weight");

    public AbilityWeightless(@Nonnull IAbilityConfig config) {
        super(AbilityNames.WEIGHTLESS);
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public void tickAbility(@Nonnull LivingEntity entity) {
        if (!entity.level().isClientSide && ModCompat.lycanitesMobs()) {
            ModCompat.removeEffect(entity, LYCANITES_WEIGHT);
        }
        if (entity.onGround()) {
            return;
        }
        double motionY = 0;
        if (entity.swinging) {
            motionY = entity.isShiftKeyDown() ? -VERTICAL_SPEED : VERTICAL_SPEED;
        }
        final Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x, motionY, motion.z);
    }

    @Override
    public boolean potionApplied(LivingEntity entity, MobEffectInstance effect, boolean cancel) {
        return cancel || (ModCompat.lycanitesMobs() && ModCompat.isEffect(effect, LYCANITES_WEIGHT));
    }

    @Override
    public boolean fall(LivingEntity entity, float distance, float multiplier, boolean cancel) {
        return true;
    }

    @Override
    public void describe(DescriptionVariables variables) {
        variables.number("speed", VERTICAL_SPEED)
                .effects("weight", ModCompat.lycanitesMobs(), List.of(LYCANITES_WEIGHT.toString()), false);
    }
}
