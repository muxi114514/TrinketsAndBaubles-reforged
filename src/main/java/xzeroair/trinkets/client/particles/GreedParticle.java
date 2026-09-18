package xzeroair.trinkets.client.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 贪婪之眼的闪烁光点：greed.png 横向 16 帧动画，默认无深度测试（隔墙可见）。对应 1.12 ParticleGreed。
 */
@OnlyIn(Dist.CLIENT)
public class GreedParticle extends Particle {

    private static final int FRAMES = 16;

    private final float halfSize;
    private final boolean depthTest;

    public GreedParticle(ClientLevel level, double x, double y, double z, int color, float alpha, float halfSize, boolean depthTest) {
        super(level, x, y, z);
        this.rCol = ((color >> 16) & 0xFF) / 255.0F;
        this.gCol = ((color >> 8) & 0xFF) / 255.0F;
        this.bCol = (color & 0xFF) / 255.0F;
        this.alpha = alpha;
        this.halfSize = halfSize;
        this.depthTest = depthTest;
        this.lifetime = 16;
        this.hasPhysics = false;
    }

    public GreedParticle(ClientLevel level, double x, double y, double z, int color) {
        this(level, x, y, z, color, 1.0F, 0.25F, false);
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
        final int frame = (int) ((this.age + partialTicks) * (FRAMES - 1) / this.lifetime);
        if (frame >= FRAMES) {
            return;
        }
        final float u0 = frame / (float) FRAMES;
        ParticleGeometry.billboard(buffer, camera, this.x, this.y, this.z, this.halfSize,
                u0, u0 + 1.0F / FRAMES, 0.0F, 1.0F, this.rCol, this.gCol, this.bCol, this.alpha);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return this.depthTest ? ModParticleRenderTypes.GREED_DEPTH : ModParticleRenderTypes.GREED;
    }
}
