package xzeroair.trinkets.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 狼王渲染：原版狼模型按体型放大（高度 1.2 / 原版 0.85），始终使用愤怒狼贴图。对应 1.12 RenderAlphaWolf。
 */
@OnlyIn(Dist.CLIENT)
public class AlphaWolfRenderer extends WolfRenderer {

    private static final ResourceLocation ANGRY_WOLF = new ResourceLocation("textures/entity/wolf/wolf_angry.png");
    private static final float VANILLA_WOLF_HEIGHT = 0.85F;

    public AlphaWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void scale(Wolf wolf, PoseStack poseStack, float partialTick) {
        final float scale = wolf.getBbHeight() / VANILLA_WOLF_HEIGHT;
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(Wolf wolf) {
        return ANGRY_WOLF;
    }
}
