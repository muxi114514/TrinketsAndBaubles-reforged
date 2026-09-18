package xzeroair.trinkets.client.renderer;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.entity.area.AreaEffectEntity;

/**
 * 区域效果的地面光圈：按区域颜色画一圈呼吸闪烁的细环。对应 1.12 RenderAreaEffectEntity。
 *
 * 移植说明：1.12 用 GL_LINE_LOOP + 4 像素线宽；核心模式下粗线不可用，改为贴地的窄环带四边形。
 */
@OnlyIn(Dist.CLIENT)
public class AreaEffectRenderer extends EntityRenderer<AreaEffectEntity> {

    private static final int SEGMENTS = 64;
    private static final float RING_WIDTH = 0.06F;

    public AreaEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AreaEffectEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        if (entity.shouldRenderCircle()) {
            final int color = entity.getColor();
            final float red = (color >> 16 & 255) / 255.0F;
            final float green = (color >> 8 & 255) / 255.0F;
            final float blue = (color & 255) / 255.0F;
            final float time = entity.tickCount + partialTicks;
            final float radius = Math.max(0.1F, entity.getRadius() + Mth.sin(time * 0.18F) * 0.08F);
            final float alpha = 0.45F + 0.2F * Mth.sin(time * 0.12F);
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.03D, 0.0D);
            final Matrix4f pose = poseStack.last().pose();
            final VertexConsumer consumer = buffers.getBuffer(RenderType.debugQuads());
            for (int i = 0; i < SEGMENTS; i++) {
                final float a0 = Mth.TWO_PI * i / SEGMENTS;
                final float a1 = Mth.TWO_PI * (i + 1) / SEGMENTS;
                vertex(consumer, pose, a0, radius - RING_WIDTH, red, green, blue, alpha);
                vertex(consumer, pose, a0, radius + RING_WIDTH, red, green, blue, alpha);
                vertex(consumer, pose, a1, radius + RING_WIDTH, red, green, blue, alpha);
                vertex(consumer, pose, a1, radius - RING_WIDTH, red, green, blue, alpha);
            }
            poseStack.popPose();
        }
        super.render(entity, yaw, partialTicks, poseStack, buffers, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, float angle, float radius, float r, float g, float b, float a) {
        consumer.vertex(pose, Mth.cos(angle) * radius, 0.0F, Mth.sin(angle) * radius).color(r, g, b, a).endVertex();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ResourceLocation getTextureLocation(AreaEffectEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
