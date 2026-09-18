package xzeroair.trinkets.client.race;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.util.Reference;

/**
 * 妖精翅膀与巨龙翅膀。对应 1.12 RaceFairyWings（含 model/Wings）与 RaceDragonWings。
 * 参数、动画曲线逐项照搬 1.12，便于对照；旋转 GlStateManager.rotate(a, 轴) 对应 Axis.XXP.rotationDegrees(a)。
 */
@OnlyIn(Dist.CLIENT)
public final class WingTraits {

    public static final ModelLayerLocation FAIRY_WINGS_LAYER = new ModelLayerLocation(new ResourceLocation(Reference.MODID, "fairy_wings"), "main");

    private static final ResourceLocation FAIRY = texture("fairy/fairy_wings");
    private static final ResourceLocation FAIRY_FAKE = texture("fairy/fairy_wings_fake");
    private static final ResourceLocation DRAGON = texture("dragon/dragon_wings");
    private static final ResourceLocation DRAGON_ARMS = texture("dragon/dragon_wings_arms");
    private static final ResourceLocation DRAGON_LEATHER = texture("dragon/dragon_wings_leather");

    /** 模型翅膀飞行时的扇动角序列（度） */
    private static final float[] FLAP_CYCLE = {64F, 48F, 32F, 16F, 1F, 1F, 16F, 32F, 48F, 64F};

    private static ModelPart fakeWings;

    /** 1.12 model/Wings：两片厚度为 0 的 16×8 盒子 */
    public static LayerDefinition createFakeWingsLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -2.0F, 0.0F, 0, 16, 8),
                PartPose.offset(-2.0F, 1.0F, 2.0F));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -2.0F, 0.0F, 0, 16, 8),
                PartPose.offset(1.0F, 1.0F, 2.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    public static void bakeLayers(ModelPart fairyWings) {
        fakeWings = fairyWings;
    }

    // ── 妖精 ──

    public static void fairy(TraitRenderContext ctx) {
        final boolean modelWings = ctx.fake() ? ctx.colors().variant() == 0 : ctx.colors().variant() == 1;
        if (modelWings) {
            fairyModelWings(ctx);
        } else {
            fairyFlatWings(ctx);
        }
    }

    private static void fairyModelWings(TraitRenderContext ctx) {
        if (fakeWings == null) {
            return;
        }
        final PoseStack pose = ctx.pose();
        pose.pushPose();
        ctx.model().body.translateAndRotate(pose);
        if (ctx.wearing(EquipmentSlot.CHEST)) {
            pose.translate(0.0F, -0.1F, 0.06F);
            pose.scale(1.1F, 1.1F, 1.1F);
        }
        final ModelPart right = fakeWings.getChild("right_wing");
        final ModelPart left = fakeWings.getChild("left_wing");
        final double dx = ctx.player().getX() - ctx.player().xo;
        final double dz = ctx.player().getZ() - ctx.player().zo;
        final boolean onGround = ctx.player().onGround();
        final float flap = Mth.cos(ctx.limbSwing() * 0.6662F + Mth.PI) * 1.4F * ctx.limbSwingAmount();
        if (Math.abs(dx) >= 0.08D || Math.abs(dz) >= 0.08D || !onGround) {
            if (!onGround && !ctx.player().isPassenger()) {
                final double time = ctx.player().level().getDayTime() + ctx.player().tickCount + ctx.partialTicks();
                final float angle = FLAP_CYCLE[(int) ((time * 3.0D) % FLAP_CYCLE.length)] * Mth.DEG_TO_RAD;
                left.yRot = angle;
                right.yRot = -angle;
            } else {
                right.yRot = -flap;
                left.yRot = flap;
            }
        } else {
            right.yRot = -0.5F;
            left.yRot = 0.5F;
        }
        final var consumer = ctx.buffers().getBuffer(RenderType.entityTranslucent(FAIRY_FAKE));
        right.render(pose, consumer, ctx.light(), OverlayTexture.NO_OVERLAY);
        left.render(pose, consumer, ctx.light(), OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private static void fairyFlatWings(TraitRenderContext ctx) {
        final PoseStack pose = ctx.pose();
        final int frames = 60;
        final int angleTick = ctx.player().onGround() ? 0
                : Math.min((int) (((ctx.player().tickCount + ctx.partialTicks()) * 24) % frames), frames - 1);
        final float angle = Math.max(50F - angleTick, 0F);
        pose.pushPose();
        ctx.model().body.translateAndRotate(pose);
        pose.scale(0.0625F, 0.0625F, 0.0625F);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.translate(0, -2F, 0);
        if (ctx.wearing(EquipmentSlot.CHEST)) {
            pose.translate(-0.4F, -1F, 0F);
        }
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-1, 0, 0);
        TraitDrawing.quad(ctx, FAIRY, -18, 0, -1, 0, 0, 36, 42, 16, 16, 36, 42, ctx.colors().primary());
        pose.popPose();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-angle));
        pose.translate(-1, 0, 0);
        TraitDrawing.quad(ctx, FAIRY, -18, 0, 1, 0, 0, 36, 42, 16, 16, 36, 42, ctx.colors().primary());
        pose.popPose();
        pose.popPose();
    }

    // ── 巨龙 ──

    public static void dragon(TraitRenderContext ctx) {
        final PoseStack pose = ctx.pose();
        pose.pushPose();
        ctx.model().body.translateAndRotate(pose);
        pose.scale(0.0625F, 0.0625F, 0.0625F);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.translate(0, -2F, 0);
        if (ctx.wearing(EquipmentSlot.CHEST)) {
            pose.translate(-0.4F, -1F, 0F);
        }
        final boolean onGround = ctx.player().onGround();
        final float cycle = onGround ? 0F : (ctx.player().tickCount + ctx.partialTicks()) * 0.04F;
        final float forwardRatio = 0.45F;
        final float phase = cycle - Mth.floor(cycle);
        final float flap = phase < forwardRatio
                ? -Mth.cos(phase / forwardRatio * Mth.PI)
                : Mth.cos((phase - forwardRatio) / (1F - forwardRatio) * Mth.PI);
        final float forwardFlap = Math.max(0F, flap);
        final float backwardFlap = Math.max(0F, -flap);
        final float downFlap = 0.5F + 0.5F * flap;
        final DragonPose wing = new DragonPose(
                (ctx.slim() ? 0 : 0.2F) + ((onGround ? 1F : -1F) - downFlap * 1.5F),
                onGround ? -2F : -3F,
                (onGround ? 0F : 2F) + backwardFlap * 0.15F,
                onGround ? -40F : 60F + flap * 40F,
                onGround ? 0F : 10F,
                onGround ? 0F : 15F + flap * 17F,
                -flap * (12F + forwardFlap * 24F + backwardFlap * 12F));
        final float tipYaw = onGround ? 12F : wing.midYaw() * 1.05F + flap * Math.abs(flap) * 32F;
        dragonWing(ctx, wing, tipYaw, 1);
        dragonWing(ctx, wing, tipYaw, -1);
        pose.popPose();
    }

    private record DragonPose(float anchorX, float anchorY, float separation, float rootYaw, float rootPitch, float rootRoll,
            float midYaw) {
    }

    /** side 为 1 画一侧、-1 画镜像侧（1.12 两段几乎相同的代码只在符号上不同） */
    private static void dragonWing(TraitRenderContext ctx, DragonPose wing, float tipYaw, int side) {
        final PoseStack pose = ctx.pose();
        final double rootX = -12.0D;
        final double rootY = ctx.slim() ? -19.0D : -20.0D;
        final double midX = rootX - 16;
        final double tipX = midX - 8;
        pose.pushPose();
        pose.translate(wing.anchorX(), wing.anchorY(), side * wing.separation());
        pose.scale(0.9F, 0.9F, 0.9F);
        pose.mulPose(Axis.YP.rotationDegrees(side * wing.rootYaw()));
        pose.mulPose(Axis.XP.rotationDegrees(-side * wing.rootPitch()));
        pose.mulPose(Axis.ZP.rotationDegrees(-wing.rootRoll()));
        dragonSegment(ctx, rootX, rootY, 48, 16, 8);
        pose.pushPose();
        pose.translate(rootX, rootY, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-side * wing.midYaw()));
        pose.translate(-rootX, -rootY, 0);
        dragonSegment(ctx, midX, rootY, 16, 32, 16);
        pose.pushPose();
        pose.translate(midX, rootY, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-side * tipYaw));
        pose.translate(-midX, -rootY, 0);
        dragonSegment(ctx, tipX, rootY, 0, 16, 8);
        pose.popPose();
        pose.popPose();
        pose.popPose();
    }

    /** 同色用合成贴图；异色时骨架（主色）与翼膜（副色）分两层 */
    private static void dragonSegment(TraitRenderContext ctx, double x, double y, float u, int uWidth, int width) {
        final int primary = ctx.colors().primary();
        final int secondary = ctx.colors().secondary();
        if (primary == secondary) {
            TraitDrawing.quad(ctx, DRAGON, x, y, 0, u, 0, uWidth, 64, width, 32, 64, 64, primary);
        } else {
            TraitDrawing.quad(ctx, DRAGON_ARMS, x, y, 0, u, 0, uWidth, 64, width, 32, 64, 64, primary);
            TraitDrawing.quad(ctx, DRAGON_LEATHER, x, y, 0, u, 0, uWidth, 64, width, 32, 64, 64, secondary);
        }
    }

    private static ResourceLocation texture(String path) {
        return new ResourceLocation(Reference.MODID, "textures/races/" + path + ".png");
    }

    private WingTraits() {
    }
}
