package xzeroair.trinkets.util.helpers;

import javax.annotation.Nonnull;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** 实体间的吸引 / 排斥力（对应 1.12 util/helpers/Kinetics#applyForce）。 */
public final class Kinetics {

    /**
     * 吸引：10 格内按距离放大拉力并阻尼；排斥：仅在来源体宽 + 3 格内生效，直接改写速度并让投射物朝向新方向。
     * 结果速度统一钳制到 maxSpeed。
     */
    public static void applyForce(@Nonnull Entity origin, @Nonnull Entity target, boolean pull, double force, double maxSpeed,
            double damping) {
        final Vec3 from = target.position();
        final Vec3 to = origin.position();
        final Vec3 delta = to.subtract(from);
        final double distance = delta.length();
        final double reach = pull ? 10.0D : origin.getBbWidth() + 3.0D;
        if (distance <= 0.001D || distance >= reach) {
            return;
        }
        Vec3 motion;
        if (pull) {
            final Vec3 dir = delta.normalize();
            final double scaled = force * Math.max(distance, 2.0D);
            motion = target.getDeltaMovement().add(dir.scale(scaled)).scale(damping);
        } else {
            final Vec3 dir = from.subtract(to).normalize();
            motion = dir.scale(Math.min(force * 4.0D, maxSpeed));
            faceMotion(target, motion);
        }
        final double speed = motion.length();
        if (speed > maxSpeed) {
            motion = motion.scale(maxSpeed / speed);
        }
        target.setDeltaMovement(motion);
        target.hurtMarked = true;
    }

    private static void faceMotion(Entity target, Vec3 motion) {
        final double horizontal = motion.horizontalDistance();
        if (horizontal <= 0.0001D) {
            return;
        }
        final float yaw = (float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG);
        final float pitch = (float) (Mth.atan2(motion.y, horizontal) * Mth.RAD_TO_DEG);
        target.setYRot(yaw);
        target.setXRot(pitch);
        target.yRotO = yaw;
        target.xRotO = pitch;
    }

    private Kinetics() {
    }
}
