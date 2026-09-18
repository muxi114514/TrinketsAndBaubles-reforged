package xzeroair.trinkets.client.renderer;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.client.particles.ModParticleRenderTypes;
import xzeroair.trinkets.entity.BreathProjectile;

/**
 * 龙息投射物本体：dragon_breath.png 图集前 15 帧的「成长」动画，满帧后淡出。
 * 拖尾由 {@link xzeroair.trinkets.client.particles.BreathWispParticle} 负责。对应 1.12 RenderThrownProjectile。
 *
 * 移植说明：1.12 在 RenderWorldLastEvent 里维护一张全局拖尾表逐帧重画；1.20.1 改为投射物每 tick 在客户端
 * 生成一个短命粒子，由粒子引擎管理生命周期，无需缓存与清理。
 */
@OnlyIn(Dist.CLIENT)
public class BreathProjectileRenderer extends EntityRenderer<BreathProjectile> {

    public static final int FRAME_COUNT = 31;
    private static final int GROWTH_FRAME_COUNT = 15;

    public BreathProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(BreathProjectile entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        final float age = entity.tickCount + partialTicks;
        final int frame = Math.min(GROWTH_FRAME_COUNT - 1, (int) age);
        final float alpha = age < GROWTH_FRAME_COUNT ? 1.0F
                : Math.max(0.0F, 1.0F - (age - GROWTH_FRAME_COUNT) / (FRAME_COUNT - GROWTH_FRAME_COUNT));
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.5D, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        renderFrame(poseStack, buffers.getBuffer(RenderType.entityTranslucent(ModParticleRenderTypes.FIRE_BREATH_TEXTURE)),
                frame, entity.getColor(), alpha, 1.0F);
        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffers, light);
    }

    /** 从 16×2 图集取第 frame 帧（0~15 在第二排，16~31 在第一排）画一张居中方片 */
    public static void renderFrame(PoseStack poseStack, VertexConsumer consumer, int frame, int color, float alpha, float scale) {
        final int column = frame % 16;
        final float v0 = frame < 16 ? 0.5F : 0.0F;
        final float u0 = column / 16.0F;
        final float u1 = u0 + 1.0F / 16.0F;
        final float v1 = v0 + 0.5F;
        final int r = color >> 16 & 255;
        final int g = color >> 8 & 255;
        final int b = color & 255;
        final int a = (int) (alpha * 255);
        final Matrix4f pose = poseStack.last().pose();
        final Matrix3f normal = poseStack.last().normal();
        final float h = 0.5F * scale;
        vertex(consumer, pose, normal, -h, -h, u0, v1, r, g, b, a);
        vertex(consumer, pose, normal, h, -h, u1, v1, r, g, b, a);
        vertex(consumer, pose, normal, h, h, u1, v0, r, g, b, a);
        vertex(consumer, pose, normal, -h, h, u0, v0, r, g, b, a);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, float x, float y, float u, float v,
            int r, int g, int b, int a) {
        consumer.vertex(pose, x, y, 0.0F).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(BreathProjectile entity) {
        return ModParticleRenderTypes.FIRE_BREATH_TEXTURE;
    }
}
