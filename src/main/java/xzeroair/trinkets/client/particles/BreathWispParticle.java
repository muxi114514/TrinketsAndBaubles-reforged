package xzeroair.trinkets.client.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 龙息拖尾：停在原地，播放图集第 16~30 帧并逐渐淡出（缩放 0.75）。对应 1.12 RenderThrownProjectile 的 BreathWisp。
 */
@OnlyIn(Dist.CLIENT)
public class BreathWispParticle extends Particle {

    private static final int START_FRAME = 16;
    private static final int FRAME_COUNT = 15;
    private static final int LIFETIME = 15;
    private static final float HALF_SIZE = 0.375F;

    public BreathWispParticle(ClientLevel level, double x, double y, double z, int color) {
        super(level, x, y, z);
        this.rCol = (color >> 16 & 255) / 255.0F;
        this.gCol = (color >> 8 & 255) / 255.0F;
        this.bCol = (color & 255) / 255.0F;
        this.lifetime = LIFETIME;
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
        final float age = this.age + partialTicks;
        final int frame = START_FRAME + Math.min(FRAME_COUNT - 1, (int) (age * FRAME_COUNT / LIFETIME));
        final float alpha = Math.max(0.0F, 1.0F - age / LIFETIME);
        final float u0 = (frame % 16) / 16.0F;
        ParticleGeometry.billboard(buffer, camera, this.x, this.y, this.z, HALF_SIZE,
                u0, u0 + 1.0F / 16.0F, 0.0F, 0.5F, this.rCol, this.gCol, this.bCol, alpha);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ModParticleRenderTypes.FIRE_BREATH;
    }
}
