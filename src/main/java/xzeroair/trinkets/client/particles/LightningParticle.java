package xzeroair.trinkets.client.particles;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 两点间的锯齿闪电，每帧重新抖动（与 1.12 ParticleLightning 相同的闪烁观感）。
 * 画两遍：细芯全透明度 + 4 倍宽的半透明外晕。
 */
@OnlyIn(Dist.CLIENT)
public class LightningParticle extends Particle {

    private static final int SEGMENTS = 15;
    /** 1.12 以像素线宽表示粗细，这里换算为世界单位的半宽 */
    private static final float WIDTH_PER_UNIT = 0.01F;

    private final Vec3 end;
    private final float scale;

    public LightningParticle(ClientLevel level, double x, double y, double z, double x2, double y2, double z2,
            int color, float alpha, float scale) {
        super(level, x, y, z);
        this.end = new Vec3(x2, y2, z2);
        this.rCol = ((color >> 16) & 0xFF) / 255.0F;
        this.gCol = ((color >> 8) & 0xFF) / 255.0F;
        this.bCol = (color & 0xFF) / 255.0F;
        this.alpha = alpha;
        this.scale = scale;
        this.lifetime = 16;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        final Vec3 start = new Vec3(this.x, this.y, this.z);
        final List<Vec3> points = new ArrayList<>(SEGMENTS + 1);
        for (int step = 1; step <= SEGMENTS; step++) {
            final Vec3 point = start.lerp(this.end, step / (double) SEGMENTS);
            points.add(step == 1 || step == SEGMENTS ? point : ParticleGeometry.jitter(point, this.random));
        }
        ParticleGeometry.polyline(buffer, camera, points, this.scale * WIDTH_PER_UNIT, this.rCol, this.gCol, this.bCol, this.alpha);
        ParticleGeometry.polyline(buffer, camera, points, this.scale * WIDTH_PER_UNIT * 4.0F, this.rCol, this.gCol, this.bCol, this.alpha * 0.5F);
    }

    /** 闪电可能横跨很长距离，不参与视锥剔除 */
    @Override
    public boolean shouldCull() {
        return false;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ModParticleRenderTypes.ADDITIVE_COLOR;
    }
}
