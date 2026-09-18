package xzeroair.trinkets.client.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 龙息火焰：dragon_breath.png（16×2 格图集）逐帧播放、随寿命缩小，运动方式同原版火焰。
 * 对应 1.12 ParticleFireBreath（继承 ParticleFlame）。
 *
 * 移植说明：1.12 第二排帧的 u 坐标越界靠纹理重复回绕，实际取的是第一排第 (帧-15) 格，此处直接按该结果取格。
 */
@OnlyIn(Dist.CLIENT)
public class FireBreathParticle extends Particle {

    private static final float TILE_U = 1.0F / 16.0F;
    private static final float TILE_V = 0.5F;

    private final float baseScale;

    public FireBreathParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
            int color, float scale) {
        super(level, x, y, z);
        // 与原版火焰相同：自身随机初速 ×0.01 再叠加给定速度，位置轻微抖动
        this.xd = this.xd * 0.01D + xSpeed;
        this.yd = this.yd * 0.01D + ySpeed;
        this.zd = this.zd * 0.01D + zSpeed;
        this.x += (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
        this.y += (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
        this.z += (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.rCol = ((color >> 16) & 0xFF) / 255.0F;
        this.gCol = ((color >> 8) & 0xFF) / 255.0F;
        this.bCol = (color & 0xFF) / 255.0F;
        this.alpha = 1.0F;
        this.baseScale = scale;
        this.lifetime = 31;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.96D;
        this.yd *= 0.96D;
        this.zd *= 0.96D;
        if (this.onGround) {
            this.xd *= 0.7D;
            this.zd *= 0.7D;
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        final float progress = (this.age + partialTicks) / this.lifetime;
        final int frame = (int) ((this.age + partialTicks) * 30.0F / this.lifetime);
        if (frame > this.lifetime - 1) {
            return;
        }
        final float scale = this.baseScale * (1.0F - progress * progress * 0.5F);
        final int column = frame < 16 ? frame : frame - 15;
        final float v0 = frame < 16 ? TILE_V : 0.0F;
        final float u0 = column * TILE_U;
        final double px = this.xo + (this.x - this.xo) * partialTicks;
        final double py = this.yo + (this.y - this.yo) * partialTicks;
        final double pz = this.zo + (this.z - this.zo) * partialTicks;
        ParticleGeometry.billboard(buffer, camera, px, py, pz, scale,
                u0, u0 + TILE_U, v0, v0 + TILE_V, this.rCol, this.gCol, this.bCol, this.alpha);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ModParticleRenderTypes.FIRE_BREATH;
    }
}
