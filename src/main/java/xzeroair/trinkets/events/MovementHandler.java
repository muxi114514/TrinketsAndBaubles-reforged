package xzeroair.trinkets.events;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.init.ModAttributes;
import xzeroair.trinkets.traits.AbilityDispatcher;
import xzeroair.trinkets.traits.abilities.interfaces.IJumpAbility;

/**
 * 跳跃与坠落事件 → 种族钩子 + 能力。对应 1.12 events/MovementHandler。
 *
 * 另处理自定义跳跃属性（起跳加速与坠落缓冲）与飞行速度属性（对应 1.12 EntityProperties#flySpeedHandler）。
 */
public class MovementHandler {

    @SubscribeEvent
    public void onLivingJump(LivingEvent.LivingJumpEvent event) {
        final LivingEntity entity = event.getEntity();
        applyJumpAttribute(entity);
        final EntityProperties properties = EntityProperties.get(entity);
        if (properties == null) {
            return;
        }
        properties.getRaceHandler().jump();
        AbilityDispatcher.forEach(entity, IJumpAbility.class, ability -> ability.jump(entity));
    }

    /**
     * 每个能力内部按「坠落距离 → 伤害倍率 → 是否取消」三步串联，结果传给下一个能力，且不中断——
     * 与 1.12 的逐能力交错顺序一致。
     */
    @SubscribeEvent
    public void onLivingFall(LivingFallEvent event) {
        final LivingEntity entity = event.getEntity();
        reduceFallByJump(entity, event);
        final EntityProperties properties = EntityProperties.get(entity);
        if (properties == null) {
            return;
        }
        properties.getRaceHandler().fall(event);
        if (event.isCanceled()) {
            return;
        }
        final float[] distance = {event.getDistance()};
        final float[] multiplier = {event.getDamageMultiplier()};
        final boolean[] cancel = {false};
        AbilityDispatcher.forEach(entity, IJumpAbility.class, ability -> {
            distance[0] = ability.fallDistance(entity, distance[0]);
            multiplier[0] = ability.fallDamageMultiplier(entity, multiplier[0]);
            cancel[0] = ability.fall(entity, distance[0], multiplier[0], cancel[0]);
        });
        if (cancel[0]) {
            event.setCanceled(true);
            return;
        }
        event.setDistance(distance[0]);
        event.setDamageMultiplier(multiplier[0]);
    }

    /**
     * 跳跃属性：起跳竖直速度按倍率放大；疾跑起跳的水平加速按 1 + (倍率-1)×0.1 微调（限制在 0.9~1.1）。
     * 只在有修饰器时生效，与 1.12 一致（无修饰器时保持原版跳跃）。
     */
    private static void applyJumpAttribute(LivingEntity entity) {
        final AttributeInstance jump = entity.getAttribute(ModAttributes.JUMP.get());
        if (jump == null || jump.getModifiers().isEmpty()) {
            return;
        }
        final double height = jump.getValue();
        final Vec3 motion = entity.getDeltaMovement();
        double x = motion.x;
        double z = motion.z;
        if (entity.isSprinting()) {
            final double horizontal = Mth.clamp(1.0D + (height - 1.0D) * 0.1D, 0.9D, 1.1D);
            final float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
            x -= Mth.sin(yaw) * 0.2D * horizontal;
            z += Mth.cos(yaw) * 0.2D * horizontal;
        }
        entity.setDeltaMovement(x, motion.y + (0.42D * height - 0.42D), z);
    }

    /** 跳得越高，摔落时越能缓冲：坠落距离减去跳跃倍率 */
    private static void reduceFallByJump(LivingEntity entity, LivingFallEvent event) {
        final AttributeInstance jump = entity.getAttribute(ModAttributes.JUMP.get());
        if (jump == null || jump.getModifiers().isEmpty()) {
            return;
        }
        final double height = Math.round(jump.getValue() * 1000.0D) / 1000.0D;
        if (height > 0) {
            final float distance = Math.max((float) (event.getDistance() - height), 0.0F);
            event.setDistance(distance);
            entity.fallDistance = distance;
        }
    }

    /**
     * 飞行速度属性同步到玩家能力。与 1.12 一致：创造/旁观跳过，仅在属性带修饰器时接管；
     * 修饰器被撤掉后恢复一次基础值（1.12 会残留旧速度），之后不再干涉其他模组设置的飞行速度。
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        final Player player = event.player;
        if (event.phase != TickEvent.Phase.END || player.isCreative() || player.isSpectator()) {
            return;
        }
        final AttributeInstance flySpeed = player.getAttribute(ModAttributes.FLY_SPEED.get());
        final EntityProperties properties = EntityProperties.get(player);
        if (flySpeed == null || properties == null) {
            return;
        }
        final boolean modified = !flySpeed.getModifiers().isEmpty();
        if (!modified && !properties.isFlySpeedControlled()) {
            return;
        }
        properties.setFlySpeedControlled(modified);
        final float value = (float) (modified ? flySpeed.getValue() : flySpeed.getBaseValue());
        if (Math.abs(player.getAbilities().getFlyingSpeed() - value) > 1.0E-4F) {
            player.getAbilities().setFlyingSpeed(value);
            if (!player.level().isClientSide) {
                player.onUpdateAbilities();
            }
        }
    }
}
