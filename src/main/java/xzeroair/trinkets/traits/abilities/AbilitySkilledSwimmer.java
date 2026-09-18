package xzeroair.trinkets.traits.abilities;

import javax.annotation.Nonnull;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fml.DistExecutor;

import xzeroair.trinkets.client.keybinds.LocalMovementInput;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.util.config.abilities.SkilledSwimmerAbilityConfig;

/**
 * 熟练泳者：在水/岩浆中获得三维自由游动（朝视线方向上浮下潜、抵消下沉），深海探索者附魔进一步加速。
 *
 * 移植说明：
 * - 新模式需要读本地玩家的按键，1.12 在公共类里直接引用 Minecraft 客户端类，专用服会崩；
 *   这里经 DistExecutor 调客户端专用的 {@link LocalMovementInput} 取按键快照，服务端不加载客户端类。
 * - 1.12 的 Better Diving 联动随 P7 兼容层补齐。
 */
public class AbilitySkilledSwimmer extends Ability implements ITickableAbility {

    /** 游动推力 = 基础移速 × 游泳速度属性 × 本值 */
    private static final double SWIM_SPEED_SCALE = 0.2D;
    /** 深海探索者每级在推力上追加的比例 */
    private static final double DEPTH_STRIDER_BONUS = 0.667D;
    private static final double OLD_MAX_VERTICAL = 0.25D;
    private static final double OLD_SINK_WATER = 1.25D;
    private static final double OLD_SINK_LAVA = 1.75D;

    /** 一帧的移动按键快照 */
    public record MovementInput(boolean forward, boolean back, boolean left, boolean right, boolean up, boolean down) {
    }

    private final SkilledSwimmerAbilityConfig config;

    public AbilitySkilledSwimmer(@Nonnull SkilledSwimmerAbilityConfig config) {
        super(AbilityNames.SKILLED_SWIMMER);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        if (this.isSpectator(entity) || this.isCreativeFlying(entity)) {
            return;
        }
        // 实体自身所在方块必在已加载区块内
        if (!(entity.isInWater() || entity.isInLava()) || entity.level().getBlockState(entity.blockPosition()).isAir()) {
            return;
        }
        if (this.config.oldTweaks.get()) {
            this.handleMovementOld(entity);
        } else {
            this.handleMovement(entity);
        }
    }

    private void handleMovementOld(LivingEntity entity) {
        final double threshold = entity.isInLava() ? 0.09D : 0.1D;
        final double buoyancy = OLD_MAX_VERTICAL;
        final Vec3 motion = entity.getDeltaMovement();
        if (!entity.isShiftKeyDown()) {
            double motionY = 0;
            if (movingForward(entity, motion) && (Math.abs(motion.x) > threshold || Math.abs(motion.z) > threshold)) {
                motionY += Mth.clamp(entity.getLookAngle().y, -buoyancy, buoyancy);
            }
            entity.setDeltaMovement(motion.x, motionY, motion.z);
        } else if (!movingForward(entity, motion) && !(motion.y > 0)) {
            entity.setDeltaMovement(motion.x, motion.y * (entity.isInLava() ? OLD_SINK_LAVA : OLD_SINK_WATER), motion.z);
        }
    }

    private void handleMovement(LivingEntity entity) {
        if (!entity.level().isClientSide) {
            addMotion(entity, 0, 0.02D, 0);
            return;
        }
        if (!(entity instanceof Player player) || !player.isLocalPlayer()) {
            return;
        }
        final MovementInput input = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> LocalMovementInput::sample);
        if (input == null) {
            return;
        }
        double slow = this.calculateSlow(entity);
        final double speed = this.getSwimSpeed(entity);
        double strafe = 0;
        double forward = 0;
        double up = 0;
        if (input.forward()) {
            forward++;
        }
        if (input.back()) {
            forward--;
        }
        if (input.right()) {
            strafe--;
        }
        if (input.left()) {
            strafe++;
        }
        if (input.up()) {
            up++;
        }
        if (input.down()) {
            up--;
            slow *= 0.3D;
        }
        addMotion(entity, 0, 0.02D, 0);
        final boolean horizontal = input.forward() != input.back() || input.right() != input.left();
        final float yaw = entity.getYRot();
        if (horizontal) {
            move2D(entity, strafe, forward, -slow, yaw);
        }
        if (!horizontal && input.up() == input.down()) {
            return;
        }
        float pitch = entity.getXRot();
        // 前进+上浮/下潜时把视线俯仰角折半，改为斜向游动
        if (input.forward() && !input.back()) {
            if (input.up() && !input.down()) {
                pitch = (pitch - 90.0F) / 2.0F;
                up = 0;
            } else if (input.down() && !input.up()) {
                pitch = (pitch + 90.0F) / 2.0F;
                up = 0;
            }
        } else if (input.back() && !input.forward()) {
            if (input.up() && !input.down()) {
                pitch = (pitch + 90.0F) / 2.0F;
                up = 0;
            } else if (input.down() && !input.up()) {
                pitch = (pitch - 90.0F) / 2.0F;
                up = 0;
            }
        }
        move3D(entity, strafe, up, forward, speed, yaw, pitch);
        final boolean depthStrider = EnchantmentHelper.getDepthStrider(entity) > 0;
        if (input.up()) {
            scaleMotionY(entity, depthStrider ? 0.772D : 0.6D);
        }
        if (input.down() && depthStrider) {
            scaleMotionY(entity, 0.936D);
        }
    }

    private double getSwimSpeed(LivingEntity entity) {
        final double swimSpeedBase = baseMovementSpeed(entity) * Math.max(swimSpeed(entity) * SWIM_SPEED_SCALE, 0D);
        double speed = swimSpeedBase;
        final int depthStrider = EnchantmentHelper.getDepthStrider(entity);
        if (depthStrider > 0) {
            speed += swimSpeedBase * DEPTH_STRIDER_BONUS * depthStrider;
        }
        return speed;
    }

    private double calculateSlow(LivingEntity entity) {
        final double swimSpeedBase = baseMovementSpeed(entity) * Math.max(swimSpeed(entity), 0D);
        double slow = swimSpeedBase * 0.2D;
        double depthStrider = Math.min(EnchantmentHelper.getDepthStrider(entity), 3);
        if (depthStrider > 0) {
            if (!entity.onGround()) {
                depthStrider *= 0.5D;
            } else if (entity.isBlocking()) {
                depthStrider *= 0.02D;
            }
            slow += ((swimSpeedBase - slow) * depthStrider) / 3.0D;
        }
        return slow;
    }

    private static double baseMovementSpeed(LivingEntity entity) {
        final AttributeInstance movement = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        return movement != null ? movement.getBaseValue() : 0.1D;
    }

    private static double swimSpeed(LivingEntity entity) {
        final AttributeInstance instance = entity.getAttribute(ForgeMod.SWIM_SPEED.get());
        return instance != null ? instance.getValue() : 1D;
    }

    private static void move2D(Entity entity, double strafe, double forward, double speed, double yaw) {
        double d = strafe * strafe + forward * forward;
        if (d < 1.0E-4D) {
            return;
        }
        d = speed / Math.max(Math.sqrt(d), 1.0D);
        strafe *= d;
        forward *= d;
        final double sin = Math.sin(yaw * Mth.DEG_TO_RAD);
        final double cos = Math.cos(yaw * Mth.DEG_TO_RAD);
        addMotion(entity, strafe * cos - forward * sin, 0, forward * cos + strafe * sin);
    }

    private static void move3D(Entity entity, double strafe, double up, double forward, double speed, double yaw, double pitch) {
        double d = strafe * strafe + up * up + forward * forward;
        if (d < 1.0E-4D) {
            return;
        }
        d = speed / Math.max(Math.sqrt(d), 1.0D);
        strafe *= d;
        up *= d;
        forward *= d;
        final double sinYaw = Math.sin(yaw * Mth.DEG_TO_RAD);
        final double cosYaw = Math.cos(yaw * Mth.DEG_TO_RAD);
        final double sinPitch = Math.sin(pitch * Mth.DEG_TO_RAD);
        final double cosPitch = Math.cos(pitch * Mth.DEG_TO_RAD);
        addMotion(entity, strafe * cosYaw - forward * sinYaw * cosPitch,
                up - forward * sinPitch,
                forward * cosYaw * cosPitch + strafe * sinYaw);
    }

    private static boolean movingForward(Entity entity, Vec3 motion) {
        final Direction facing = entity.getDirection();
        return facing.getStepX() * motion.x > 0 || facing.getStepZ() * motion.z > 0;
    }

    private static void scaleMotionY(Entity entity, double factor) {
        final Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x, motion.y * factor, motion.z);
    }

    private static void addMotion(Entity entity, double x, double y, double z) {
        entity.setDeltaMovement(entity.getDeltaMovement().add(x, y, z));
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final boolean old = this.config.oldTweaks.get();
        variables.flag("modern", !old)
                .flag("old", old)
                .percent("speed", true, SWIM_SPEED_SCALE)
                .percent("strider", true, DEPTH_STRIDER_BONUS)
                .number("buoyancy", OLD_MAX_VERTICAL)
                .number("sinkwater", OLD_SINK_WATER)
                .number("sinklava", OLD_SINK_LAVA);
    }
}
