package xzeroair.trinkets.client.particles;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.util.Reference;

/**
 * 自定义粒子的渲染批次。
 *
 * 移植说明：1.12 的粒子在 renderParticle 里自行开关 GL 状态、画线段（GL_LINE_STRIP + glLineWidth）。
 * 1.20.1 是核心模式渲染，粗线宽不可用，且粒子按「渲染类型」批量绘制：
 * 状态切换放进 begin/end，闪电改画朝向镜头的细长四边形。
 */
@OnlyIn(Dist.CLIENT)
public final class ModParticleRenderTypes {

    public static final ResourceLocation GREED_TEXTURE = new ResourceLocation(Reference.MODID, "textures/particle/greed.png");
    public static final ResourceLocation FIRE_BREATH_TEXTURE = new ResourceLocation(Reference.MODID, "textures/particle/dragon_breath.png");

    /** 叠加混合的纯色四边形（闪电） */
    public static final ParticleRenderType ADDITIVE_COLOR = new ColorType("xat:additive_color", true);
    /** 普通半透明混合的纯色四边形（闪电球的电弧） */
    public static final ParticleRenderType TRANSLUCENT_COLOR = new ColorType("xat:translucent_color", false);
    /** 贪婪之眼的光点：无深度测试，隔墙可见 */
    public static final ParticleRenderType GREED = new TexturedType("xat:greed", GREED_TEXTURE, false);
    /** 闪电球中心的光点：有深度测试 */
    public static final ParticleRenderType GREED_DEPTH = new TexturedType("xat:greed_depth", GREED_TEXTURE, true);
    public static final ParticleRenderType FIRE_BREATH = new TexturedType("xat:fire_breath", FIRE_BREATH_TEXTURE, true);

    private record ColorType(String name, boolean additive) implements ParticleRenderType {

        @Override
        public void begin(BufferBuilder builder, TextureManager textureManager) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableBlend();
            if (this.additive) {
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            } else {
                RenderSystem.defaultBlendFunc();
            }
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private record TexturedType(String name, ResourceLocation texture, boolean depthTest) implements ParticleRenderType {

        @Override
        public void begin(BufferBuilder builder, TextureManager textureManager) {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, this.texture);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
            if (!this.depthTest) {
                RenderSystem.disableDepthTest();
            }
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            if (!this.depthTest) {
                RenderSystem.enableDepthTest();
            }
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private ModParticleRenderTypes() {
    }
}
