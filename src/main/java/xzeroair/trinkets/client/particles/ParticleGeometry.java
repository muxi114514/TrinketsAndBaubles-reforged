package xzeroair.trinkets.client.particles;

import java.util.List;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/** 自定义粒子共用的几何工具：折线条带、朝向镜头的方形贴片、闪电抖动点。 */
@OnlyIn(Dist.CLIENT)
final class ParticleGeometry {

    /** 对应 1.12 LightningVertex：每个轴随机偏移 -0.3 ~ 0.3（步长 0.1） */
    static Vec3 jitter(Vec3 point, RandomSource random) {
        return point.add(offset(random), offset(random), offset(random));
    }

    private static double offset(RandomSource random) {
        return random.nextInt(4) * (random.nextBoolean() ? -1 : 1) * 0.1D;
    }

    /**
     * 把世界坐标折线画成朝向镜头的等宽条带（POSITION_COLOR 格式，QUADS 模式）。
     * 线段两端点在镜头相对坐标下，条带侧向 = 线段方向 × 视线方向。
     */
    static void polyline(VertexConsumer buffer, Camera camera, List<Vec3> points, float halfWidth,
            float r, float g, float b, float a) {
        final Vec3 cam = camera.getPosition();
        for (int i = 0; i < points.size() - 1; i++) {
            final Vector3f start = relative(points.get(i), cam);
            final Vector3f end = relative(points.get(i + 1), cam);
            final Vector3f side = new Vector3f(end).sub(start).cross(start);
            if (side.lengthSquared() < 1.0E-8F) {
                continue;
            }
            side.normalize().mul(halfWidth);
            vertex(buffer, new Vector3f(start).add(side), r, g, b, a);
            vertex(buffer, new Vector3f(start).sub(side), r, g, b, a);
            vertex(buffer, new Vector3f(end).sub(side), r, g, b, a);
            vertex(buffer, new Vector3f(end).add(side), r, g, b, a);
        }
    }

    /** 朝向镜头的方形贴片（POSITION_TEX_COLOR 格式） */
    static void billboard(VertexConsumer buffer, Camera camera, double x, double y, double z, float halfSize,
            float u0, float u1, float v0, float v1, float r, float g, float b, float a) {
        final Vec3 cam = camera.getPosition();
        final float px = (float) (x - cam.x);
        final float py = (float) (y - cam.y);
        final float pz = (float) (z - cam.z);
        final Quaternionf rotation = camera.rotation();
        final Vector3f[] corners = {
                new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
        for (Vector3f corner : corners) {
            corner.rotate(rotation).mul(halfSize).add(px, py, pz);
        }
        buffer.vertex(corners[0].x(), corners[0].y(), corners[0].z()).uv(u1, v1).color(r, g, b, a).endVertex();
        buffer.vertex(corners[1].x(), corners[1].y(), corners[1].z()).uv(u1, v0).color(r, g, b, a).endVertex();
        buffer.vertex(corners[2].x(), corners[2].y(), corners[2].z()).uv(u0, v0).color(r, g, b, a).endVertex();
        buffer.vertex(corners[3].x(), corners[3].y(), corners[3].z()).uv(u0, v1).color(r, g, b, a).endVertex();
    }

    private static Vector3f relative(Vec3 point, Vec3 cam) {
        return new Vector3f((float) (point.x - cam.x), (float) (point.y - cam.y), (float) (point.z - cam.z));
    }

    private static void vertex(VertexConsumer buffer, Vector3f pos, float r, float g, float b, float a) {
        buffer.vertex(pos.x(), pos.y(), pos.z()).color(r, g, b, a).endVertex();
    }

    private ParticleGeometry() {
    }
}
