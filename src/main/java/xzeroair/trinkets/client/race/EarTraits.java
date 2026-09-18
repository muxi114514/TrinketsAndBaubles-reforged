package xzeroair.trinkets.client.race;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.util.Reference;

/**
 * 精灵 / 哥布林 / 法埃利斯的耳朵：挂在头部的两片贴图，同色时一层+描边，异色时主色层 + 副色层 + 副色描边。
 * 对应 1.12 RaceElfEars / RaceGoblinEars / RaceFaelisEars（三份几乎相同的代码，按参数合并）。
 *
 * 贴图 16×64 纵向四格：0 单色、16 主色层、32 副色层、48 描边。
 * 移植说明：1.12 潜行时额外上移 0.2（旧模型潜行是整体平移）；1.20.1 潜行姿势由头部骨骼位置表达，不再平移。
 */
@OnlyIn(Dist.CLIENT)
public final class EarTraits {

    /**
     * @param pitch      每只耳朵单独 push 并加的前倾角（哥布林 -10°）；0 表示两耳共用一次旋转（精灵、法埃利斯）
     */
    private record Style(ResourceLocation texture, float scale, double y, double z, double offsetX, float yaw, float pitch) {
    }

    private static final Style ELF = new Style(texture("elf"), 0.30F, -1.5D, -0.4D, 0.72D, 26.0F, 0.0F);
    private static final Style GOBLIN = new Style(texture("goblin"), 0.34F, -1.2D, -0.6D, 0.50D, 30.0F, -10.0F);
    private static final Style FAELIS = new Style(texture("faelis"), 0.30F, -2.4D, -0.72D, -0.3D, 26.0F, 0.0F);

    public static void elf(TraitRenderContext ctx) {
        render(ctx, ELF);
    }

    public static void goblin(TraitRenderContext ctx) {
        render(ctx, GOBLIN);
    }

    public static void faelis(TraitRenderContext ctx) {
        render(ctx, FAELIS);
    }

    private static void render(TraitRenderContext ctx, Style style) {
        final PoseStack pose = ctx.pose();
        pose.pushPose();
        ctx.model().head.translateAndRotate(pose);
        if (ctx.wearing(EquipmentSlot.HEAD)) {
            pose.translate(0.0F, -0.02F, -0.045F);
            pose.scale(1.1F, 1.1F, 1.1F);
        }
        pose.scale(style.scale(), style.scale(), style.scale());
        if (style.pitch() == 0.0F) {
            pose.mulPose(Axis.YP.rotationDegrees(-style.yaw()));
            ear(ctx, style, style.offsetX(), 1.0D);
            pose.mulPose(Axis.YP.rotationDegrees(style.yaw() * 2));
            ear(ctx, style, -style.offsetX(), -1.0D);
        } else {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-style.yaw()));
            pose.mulPose(Axis.XP.rotationDegrees(style.pitch()));
            ear(ctx, style, style.offsetX(), 1.0D);
            pose.popPose();
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(style.yaw()));
            pose.mulPose(Axis.XP.rotationDegrees(style.pitch()));
            ear(ctx, style, -style.offsetX(), -1.0D);
            pose.popPose();
        }
        pose.popPose();
    }

    /** width 为负时贴图水平翻转（左耳） */
    private static void ear(TraitRenderContext ctx, Style style, double x, double width) {
        final int primary = ctx.colors().primary();
        final int secondary = ctx.colors().secondary();
        final ResourceLocation tex = style.texture();
        if (primary == secondary) {
            TraitDrawing.quad(ctx, tex, x, style.y(), style.z(), 0, 0, 16, 16, width, 1.0D, 16, 64, primary);
            TraitDrawing.quad(ctx, tex, x, style.y(), style.z() + 0.0001D, 0, 48, 16, 16, width, 1.0D, 16, 64, primary);
        } else {
            TraitDrawing.quad(ctx, tex, x, style.y(), style.z(), 0, 16, 16, 16, width, 1.0D, 16, 64, primary);
            TraitDrawing.quad(ctx, tex, x, style.y(), style.z(), 0, 32, 16, 16, width, 1.0D, 16, 64, secondary);
            TraitDrawing.quad(ctx, tex, x, style.y(), style.z() + 0.0001D, 0, 48, 16, 16, width, 1.0D, 16, 64, secondary);
        }
    }

    private static ResourceLocation texture(String race) {
        return new ResourceLocation(Reference.MODID, "textures/races/" + race + "/ears.png");
    }

    private EarTraits() {
    }
}
