package xzeroair.trinkets.client.race;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 种族特征的两种绘制方式。对应 1.12 DrawingHelper#Draw（贴图四边形）与 BipedJsonModel（按着色索引上色的 json 模型）。
 *
 * 移植说明：1.12 用 Tessellator 立即模式 + GlStateManager 关光照/关剔除/开混合；
 * 1.20.1 改为向 entityTranslucent（自带混合、不剔除）渲染类型提交顶点，状态由渲染类型管理。
 */
@OnlyIn(Dist.CLIENT)
public final class TraitDrawing {

    private static final RandomSource RANDOM = RandomSource.create();

    /**
     * 贴图四边形，参数含义与 1.12 DrawingHelper.Draw 完全一致（便于逐行对照移植）：
     * (x,y,z) 起点、(u,v) 贴图起点、uWidth/vHeight 贴图区域、width/height 四边形尺寸、texWidth/texHeight 贴图总尺寸。
     */
    public static void quad(TraitRenderContext ctx, ResourceLocation texture, double x, double y, double z, float u, float v,
            int uWidth, int vHeight, double width, double height, float texWidth, float texHeight, int rgb) {
        final VertexConsumer consumer = ctx.buffers().getBuffer(RenderType.entityTranslucent(texture));
        final PoseStack.Pose last = ctx.pose().last();
        final float r = (rgb >> 16 & 255) / 255.0F;
        final float g = (rgb >> 8 & 255) / 255.0F;
        final float b = (rgb & 255) / 255.0F;
        final float u0 = u / texWidth;
        final float u1 = (u + uWidth) / texWidth;
        final float v0 = v / texHeight;
        final float v1 = (v + vHeight) / texHeight;
        vertex(consumer, last, ctx.light(), x, y + height, z, u0, v1, r, g, b);
        vertex(consumer, last, ctx.light(), x + width, y + height, z, u1, v1, r, g, b);
        vertex(consumer, last, ctx.light(), x + width, y, z, u1, v0, r, g, b);
        vertex(consumer, last, ctx.light(), x, y, z, u0, v0, r, g, b);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light, double x, double y, double z, float u, float v,
            float r, float g, float b) {
        consumer.vertex(pose.pose(), (float) x, (float) y, (float) z)
                .color(r, g, b, 1.0F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    /**
     * 绘制附加 json 模型（ModelEvent.RegisterAdditional 注册）。着色索引 0/1/2 分别取 color/altColor/auxColor，
     * 传 -1 表示白色；没有着色索引的面保持原色。
     */
    public static void model(TraitRenderContext ctx, ResourceLocation id, int color, int altColor, int auxColor) {
        model(ctx.pose(), ctx.buffers(), ctx.light(), id, color, altColor, auxColor);
    }

    public static void model(PoseStack pose, MultiBufferSource buffers, int light, ResourceLocation id, int color, int altColor,
            int auxColor) {
        final BakedModel model = Minecraft.getInstance().getModelManager().getModel(id);
        final VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));
        final PoseStack.Pose last = pose.last();
        for (Direction direction : Direction.values()) {
            quads(consumer, last, light, model.getQuads(null, direction, RANDOM), color, altColor, auxColor);
        }
        quads(consumer, last, light, model.getQuads(null, null, RANDOM), color, altColor, auxColor);
    }

    private static void quads(VertexConsumer consumer, PoseStack.Pose pose, int light, List<BakedQuad> quads, int color,
            int altColor, int auxColor) {
        for (BakedQuad quad : quads) {
            int rgb = -1;
            if (quad.isTinted()) {
                rgb = switch (quad.getTintIndex()) {
                    case 2 -> auxColor;
                    case 1 -> altColor;
                    default -> color;
                };
            }
            final float r = rgb == -1 ? 1.0F : (rgb >> 16 & 255) / 255.0F;
            final float g = rgb == -1 ? 1.0F : (rgb >> 8 & 255) / 255.0F;
            final float b = rgb == -1 ? 1.0F : (rgb & 255) / 255.0F;
            consumer.putBulkData(pose, quad, r, g, b, light, OverlayTexture.NO_OVERLAY);
        }
    }

    private TraitDrawing() {
    }
}
