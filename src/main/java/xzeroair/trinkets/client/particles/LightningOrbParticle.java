package xzeroair.trinkets.client.particles;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 闪电球：从中心随机游走的一串电弧（每帧重生成），中心叠一个光点。对应 1.12 ParticleLightningOrb。
 *
 * 移植说明：1.12 在同一个粒子里先画线再换纹理画光点；1.20.1 按渲染类型批量绘制，
 * 光点拆成一个同寿命的 {@link GreedParticle} 伴生粒子。
 */
@OnlyIn(Dist.CLIENT)
public class LightningOrbParticle extends Particle {

    private static final float CORE_HALF_WIDTH = 0.02F;
    private static final float GLOW_HALF_WIDTH = 0.08F;

    public LightningOrbParticle(ClientLevel level, double x, double y, double z, int color, float alpha) {
        super(level, x, y, z);
        this.rCol = ((color >> 16) & 0xFF) / 255.0F;
        this.gCol = ((color >> 8) & 0xFF) / 255.0F;
        this.bCol = (color & 0xFF) / 255.0F;
        this.alpha = alpha;
        this.lifetime = 16;
        this.hasPhysics = false;
    }

    /** 生成电弧与中心光点 */
    public static void spawn(ClientLevel level, double x, double y, double z, int color, float alpha) {
        final Minecraft mc = Minecraft.getInstance();
        mc.particleEngine.add(new LightningOrbParticle(level, x, y, z, color, alpha));
        mc.particleEngine.add(new GreedParticle(level, x, y + 0.2D, z, color, alpha, 0.5F, true));
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
        final List<Vec3> points = new ArrayList<>(this.lifetime);
        Vec3 point = new Vec3(this.x, this.y + 0.2D, this.z);
        points.add(point);
        for (int i = 1; i < this.lifetime; i++) {
            point = ParticleGeometry.jitter(point, this.random);
            points.add(point);
        }
        ParticleGeometry.polyline(buffer, camera, points, CORE_HALF_WIDTH, this.rCol, this.gCol, this.bCol, this.alpha);
        ParticleGeometry.polyline(buffer, camera, points, GLOW_HALF_WIDTH, this.rCol, this.gCol, this.bCol, this.alpha * 0.5F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ModParticleRenderTypes.TRANSLUCENT_COLOR;
    }
}
