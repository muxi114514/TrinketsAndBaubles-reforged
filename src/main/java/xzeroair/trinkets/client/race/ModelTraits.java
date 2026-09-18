package xzeroair.trinkets.client.race;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.util.Reference;

/**
 * 用 json 模型绘制的特征：各种角、法埃利斯尾巴、牛头族铃铛；以及法埃利斯爪子（贴图）。
 * 对应 1.12 RaceHorns / RaceHornsInverted / RaceDragonHorns / RaceSuccubusHorns / RaceTaurusHorns / RaceTaurusFemaleHorns /
 * RaceTaurusBell / RaceFaelisTail / RaceFaelisClaws。
 *
 * 移植说明：
 * - 1.12 的 postRender(scale) 对应 ModelPart#translateAndRotate；scale 恒为 0.0625。
 * - 尾巴与铃铛的摆动状态在 1.12 存于实体能力的 ClientInfo，这里改为客户端弱引用表（实体卸载后自动回收），不进服务端数据。
 */
@OnlyIn(Dist.CLIENT)
public final class ModelTraits {

    public static final ResourceLocation HORNS = model("generic/horns");
    public static final ResourceLocation HORNS_INVERTED = model("generic/horns_inverted");
    public static final ResourceLocation DRAGON_HORNS = model("dragon/dragon_horns");
    public static final ResourceLocation SUCCUBUS_HORNS = model("succubus/succubus_horns");
    public static final ResourceLocation TAURUS_HORNS = model("taurus/tuarian_horns");
    public static final ResourceLocation TAURUS_HORNS_FEMALE = model("taurus/tuarian_horns_f");
    public static final ResourceLocation TAURUS_BELL = model("taurus/tuarian_cowbell");
    public static final ResourceLocation FAELIS_TAIL = model("faelis/tail");

    /** 需要在 ModelEvent.RegisterAdditional 中注册的模型 */
    public static final List<ResourceLocation> ALL = List.of(HORNS, HORNS_INVERTED, DRAGON_HORNS, SUCCUBUS_HORNS, TAURUS_HORNS,
            TAURUS_HORNS_FEMALE, TAURUS_BELL, FAELIS_TAIL);

    private static final ResourceLocation CLAWS = new ResourceLocation(Reference.MODID, "textures/races/faelis/claws.png");
    private static final float SCALE = 0.0625F;

    /** 渲染线程独占，无需并发容器 */
    private static final Map<Entity, Sway> SWAY = new WeakHashMap<>();

    // ── 角 ──

    public static void horns(TraitRenderContext ctx, int color) {
        headModel(ctx, pose -> {
            pose.mulPose(Axis.ZP.rotationDegrees(180F));
            pose.translate(0.0D, 1.2D, 0.42D);
            pose.mulPose(Axis.XP.rotationDegrees(135F));
            pose.translate(-8F * SCALE, -8F * SCALE, 8F * SCALE);
        }, HORNS, color, -1, -1);
    }

    public static void hornsInverted(TraitRenderContext ctx, int color) {
        headModel(ctx, pose -> {
            pose.mulPose(Axis.ZP.rotationDegrees(180F));
            pose.translate(0.0D, 0.0D, -0.7D);
            pose.mulPose(Axis.XP.rotationDegrees(-20F));
            pose.translate(-8F * SCALE, -8F * SCALE, 8F * SCALE);
        }, HORNS_INVERTED, color, -1, -1);
    }

    public static void dragonHorns(TraitRenderContext ctx, int color) {
        headModel(ctx, pose -> {
            pose.translate(0.27D, -0.48D, -0.3D);
            pose.mulPose(Axis.ZP.rotationDegrees(180F));
            pose.scale(0.54F, 0.54F, 0.54F);
        }, DRAGON_HORNS, color, -1, -1);
    }

    public static void succubusHorns(TraitRenderContext ctx, int color, int altColor, int auxColor) {
        headModel(ctx, pose -> {
            pose.mulPose(Axis.XP.rotationDegrees(190F));
            pose.mulPose(Axis.YP.rotationDegrees(180F));
            pose.scale(0.9F, 0.9F, 0.9F);
            pose.translate(-8F * SCALE, -4.4F * SCALE, 3F * SCALE);
            pose.translate(0.0D, 0.2D, -1.0D);
        }, SUCCUBUS_HORNS, color, altColor, auxColor);
    }

    public static void taurusHorns(TraitRenderContext ctx, boolean female, int color, int altColor, int auxColor) {
        headModel(ctx, pose -> {
            pose.mulPose(Axis.ZP.rotationDegrees(180F));
            pose.scale(1.5F, 1.5F, 1.5F);
            pose.translate(-8F * SCALE, -8F * SCALE, 8F * SCALE);
            pose.translate(0.0D, 0.2D, -1.0D);
        }, female ? TAURUS_HORNS_FEMALE : TAURUS_HORNS, color, altColor, auxColor);
    }

    private static void headModel(TraitRenderContext ctx, Consumer<PoseStack> transform, ResourceLocation model,
            int color, int altColor, int auxColor) {
        final PoseStack pose = ctx.pose();
        pose.pushPose();
        ctx.model().head.translateAndRotate(pose);
        transform.accept(pose);
        TraitDrawing.model(ctx, model, color, altColor, auxColor);
        pose.popPose();
    }

    // ── 随动作摆动的尾巴与铃铛 ──

    /** 摆动状态：振幅随移动增加、逐帧衰减，前后倾角跟随前进速度（1.12 ClientInfo 中的同名字段） */
    private static final class Sway {
        private double amplitude;
        private double tiltX;
    }

    public static void faelisTail(TraitRenderContext ctx, int color, int altColor) {
        final boolean slim = ctx.slim();
        final Sway sway = SWAY.computeIfAbsent(ctx.player(), key -> new Sway());
        final Motion motion = Motion.of(ctx);
        sway.amplitude = Math.min(sway.amplitude + motion.horizontal() * (slim ? 1.5D : 1.35D), slim ? 1.0D : 0.9D) * (slim ? 0.95D : 0.94D);
        final double followStrength = slim ? 6.0D : 4.5D;
        final double targetTilt = -motion.forward() * followStrength * sway.amplitude;
        sway.tiltX = (sway.tiltX + (targetTilt - sway.tiltX) * 0.08D) * 0.9D;
        final double maxTiltX = slim ? 0.8D : 0.6D;
        final double tailTiltX = Math.max(-maxTiltX, Math.min(maxTiltX, sway.tiltX * sway.amplitude)) * (slim ? -20D : -14D);
        final double phase = (ctx.player().tickCount + ctx.partialTicks()) * (slim ? 0.35D : 0.33D);
        final double angleZ = Math.sin(phase + sway.tiltX * 0.3D) * sway.amplitude * (slim ? 20D : 18D);
        final boolean chest = ctx.wearing(EquipmentSlot.CHEST);
        final PoseStack pose = ctx.pose();
        pose.pushPose();
        ctx.model().body.translateAndRotate(pose);
        pose.mulPose(Axis.YP.rotationDegrees((float) angleZ));
        pose.mulPose(Axis.XP.rotationDegrees((float) tailTiltX));
        pose.mulPose(Axis.XP.rotationDegrees(15F));
        pose.translate(0.0D, slim ? 1.15D : 1.27D, slim ? 0.3D : 0.34D);
        final float scale = slim ? 0.7F : 0.82F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0D, chest ? 0.12D : 0.1D, chest ? -0.04D : 0.04D);
        pose.mulPose(Axis.ZP.rotationDegrees(180F));
        pose.translate(-8F * SCALE, -8F * SCALE, 8F * SCALE);
        pose.translate(0.0D, 0.2D, -1.0D);
        TraitDrawing.model(ctx, FAELIS_TAIL, color, altColor, -1);
        pose.popPose();
    }

    public static void taurusBell(TraitRenderContext ctx, int color) {
        final Sway sway = SWAY.computeIfAbsent(ctx.player(), key -> new Sway());
        final Motion motion = Motion.of(ctx);
        sway.amplitude = Math.min(sway.amplitude + motion.horizontal() * 1.5D, 1.0D) * 0.97D;
        final double targetTilt = -motion.forward() * 6.0D * sway.amplitude;
        sway.tiltX = (sway.tiltX + (targetTilt - sway.tiltX) * 0.08D) * 0.92D;
        final double bellTiltX = Math.max(-1.0D, Math.min(1.0D, sway.tiltX * sway.amplitude)) * -30D;
        final double phase = (ctx.player().tickCount + ctx.partialTicks()) * 0.35D;
        final double angleZ = Math.sin(phase + sway.tiltX * 0.3D) * sway.amplitude * 15D;
        final boolean chest = ctx.wearing(EquipmentSlot.CHEST);
        final PoseStack pose = ctx.pose();
        pose.pushPose();
        ctx.model().body.translateAndRotate(pose);
        pose.mulPose(Axis.ZP.rotationDegrees((float) angleZ));
        pose.mulPose(Axis.XP.rotationDegrees((float) bellTiltX));
        pose.mulPose(Axis.XP.rotationDegrees(-15F));
        pose.translate(0.0D, 0.44D + (ctx.slim() ? 0 : 0.12D), -0.16D);
        final float scale = ctx.slim() ? 0.7F : 1.0F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0D, chest ? 0.12D : 0.1D, chest ? -0.04D : 0.04D);
        pose.mulPose(Axis.ZP.rotationDegrees(180F));
        pose.translate(-8F * SCALE, -8F * SCALE, 8F * SCALE);
        pose.translate(0.0D, 0.2D, -1.0D);
        TraitDrawing.model(ctx, TAURUS_BELL, color, -1, -1);
        pose.popPose();
    }

    /** 本帧水平移动量与朝视线方向的分量（1.12 ClientInfo#updateInfo） */
    private record Motion(double horizontal, double forward) {
        static Motion of(TraitRenderContext ctx) {
            final double dx = ctx.player().getX() - ctx.player().xo;
            final double dz = ctx.player().getZ() - ctx.player().zo;
            final Vec3 look = ctx.player().getViewVector(ctx.partialTicks());
            return new Motion(Math.sqrt(dx * dx + dz * dz), dx * look.x + dz * look.z);
        }
    }

    // ── 法埃利斯之爪 ──

    public static void faelisClaws(TraitRenderContext ctx, boolean left, boolean right) {
        final float offsetX = ctx.slim() ? -12.4F : -18.6F;
        if (left) {
            claw(ctx, ctx.model().leftArm, -offsetX);
        }
        if (right) {
            claw(ctx, ctx.model().rightArm, offsetX);
        }
    }

    private static void claw(TraitRenderContext ctx, ModelPart arm, float offsetX) {
        final PoseStack pose = ctx.pose();
        pose.pushPose();
        arm.translateAndRotate(pose);
        final float scale = SCALE * 0.16F;
        pose.scale(scale, scale, scale);
        pose.translate(offsetX, 61F, -21F);
        pose.mulPose(Axis.YP.rotationDegrees(-90F));
        TraitDrawing.quad(ctx, CLAWS, 0, 0, 0, 0, 0, 32, 32, 32, 32, 32, 32, 0xFFFFFF);
        pose.popPose();
    }

    private static ResourceLocation model(String path) {
        return new ResourceLocation(Reference.MODID, "race/" + path);
    }

    private ModelTraits() {
    }
}
