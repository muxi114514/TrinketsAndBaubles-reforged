package xzeroair.trinkets.util.helpers;

import java.util.function.Predicate;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 视线射线检测：先检测方块，再在方块命中点之前找最近的可碰撞实体。对应 1.12 util/helpers/RayTraceHelper#rayTrace。
 *
 * 区块安全：射线长度由调用方限制在几十格内，起点是在世界中活动的实体，沿途区块处于其加载半径内。
 */
public final class RayTraceHelper {

    private static final Predicate<Entity> TARGETS = entity -> !entity.isSpectator() && entity.isPickable();

    public static HitResult rayTrace(Entity entity, double distance) {
        final Vec3 start = entity.getEyePosition();
        final Vec3 look = entity.getViewVector(1.0F);
        final Vec3 end = start.add(look.scale(distance));
        final BlockHitResult block = entity.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity));
        final Vec3 limit = block.getType() != HitResult.Type.MISS ? block.getLocation() : end;
        final AABB searchBox = entity.getBoundingBox().expandTowards(look.scale(distance)).inflate(1.0D);
        final EntityHitResult hit = ProjectileUtil.getEntityHitResult(entity.level(), entity, start, limit, searchBox, TARGETS);
        return hit != null ? hit : block;
    }

    private RayTraceHelper() {
    }
}
