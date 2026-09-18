package xzeroair.trinkets.client.curios;

import java.util.function.BooleanSupplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.client.race.ModelTraits;
import xzeroair.trinkets.client.race.TraitDrawing;
import xzeroair.trinkets.client.race.TraitRenderContext;
import xzeroair.trinkets.init.ModItems;
import xzeroair.trinkets.items.trinkets.TrinketCosmetic;
import xzeroair.trinkets.items.trinkets.TrinketCosmetic.CosmeticFeature;
import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.helpers.TrinketHelper;

/**
 * 饰品佩戴时的外观（Curios 渲染器）。对应 1.12 各饰品的 playerRenderLayer 与 TrinketsRenderLayer 的饰品部分。
 *
 * 移植说明：1.12 潜行时额外上移 0.2（旧模型潜行整体平移）；1.20.1 潜行姿势体现在身体/头部骨骼上，不再平移。
 */
@OnlyIn(Dist.CLIENT)
public final class AccessoryRenderers {

    public static final ResourceLocation ENDER_CROWN_MODEL = new ResourceLocation(Reference.MODID, "item/ender_tiara_model");
    private static final float SCALE = 0.0625F;

    public static void register() {
        CuriosRendererRegistry.register(ModItems.SEA_STONE.get(),
                () -> new ChestItem(TrinketsConfig.CLIENT.render.seaStone::get, 0.0F, 0.16F, 0.14F, 0.0F));
        CuriosRendererRegistry.register(ModItems.DAMAGE_SHIELD.get(),
                () -> new ChestItem(TrinketsConfig.CLIENT.render.damageShield::get, 0.17F, 0.22F, 0.16F, 0.14F));
        CuriosRendererRegistry.register(ModItems.ENDER_TIARA.get(), EnderCrown::new);
        CuriosRendererRegistry.register(ModItems.FAELIS_CLAW.get(), FaelisClaw::new);
        CuriosRendererRegistry.register(ModItems.COSMETIC.get(), Cosmetic::new);
    }

    /**
     * 挂在胸前的物品（海洋之石、荣耀之盾）：以 3 像素比例绘制物品模型，穿胸甲时往外挪。
     *
     * @param armorShiftX 穿胸甲时的横向回调量（荣耀之盾 0.14，海洋之石 0）
     */
    private record ChestItem(BooleanSupplier enabled, float offsetX, float offsetY, float offsetZ, float armorShiftX)
            implements ICurioRenderer {

        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack pose,
                RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount,
                float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!this.enabled.getAsBoolean() || !(parent.getModel() instanceof HumanoidModel<?> model)) {
                return;
            }
            final LivingEntity entity = slotContext.entity();
            pose.pushPose();
            model.body.translateAndRotate(pose);
            pose.mulPose(Axis.XP.rotationDegrees(180F));
            pose.translate(this.offsetX, -this.offsetY, this.offsetZ);
            if (!entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
                pose.translate(this.offsetX - this.armorShiftX, 0F, -(this.offsetZ - 0.2F));
            }
            pose.scale(SCALE * 3F, SCALE * 3F, SCALE * 3F);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
                    pose, buffers, entity.level(), 0);
            pose.popPose();
        }
    }

    /** 饰品栏里的末影王冠：戴在头顶的 3D 冠冕（头盔位由物品模型覆盖 xat:worn 处理） */
    private static final class EnderCrown implements ICurioRenderer {

        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack pose,
                RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount,
                float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!TrinketsConfig.CLIENT.render.enderCrown.get() || !(parent.getModel() instanceof HumanoidModel<?> model)) {
                return;
            }
            final boolean helmet = !slotContext.entity().getItemBySlot(EquipmentSlot.HEAD).isEmpty();
            pose.pushPose();
            model.head.translateAndRotate(pose);
            pose.mulPose(Axis.ZP.rotationDegrees(180F));
            pose.translate(-8F * SCALE, 1.8F * SCALE, -4.8F * SCALE);
            pose.translate(0.0F, helmet ? 0.07F : 0.0F, helmet ? -0.04F : 0.0F);
            TraitDrawing.model(pose, buffers, light, ENDER_CROWN_MODEL, -1, -1, -1);
            pose.popPose();
        }
    }

    /** 法埃利斯之爪：法埃利斯本族（自带爪子）或戴着法埃利斯装饰品时不画；同名饰品只能戴一件，故只画左手（与 1.12 计数规则一致） */
    private static final class FaelisClaw implements ICurioRenderer {

        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack pose,
                RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount,
                float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!TrinketsConfig.CLIENT.render.faelisClaw.get()) {
                return;
            }
            final TraitRenderContext ctx = context(slotContext, pose, parent, buffers, light, limbSwing, limbSwingAmount, partialTicks);
            if (ctx == null || isRace(ctx.player(), "faelis") || TrinketHelper.isEquipped(ctx.player(),
                    worn -> worn.is(ModItems.COSMETIC.get()) && TrinketCosmetic.getFeature(worn) == CosmeticFeature.FAELIS)) {
                return;
            }
            ModelTraits.faelisClaws(ctx, true, false);
        }
    }

    /** 装饰品：按特征绘制种族外观；与自身种族相同的特征不重复绘制（1.12 TrinketCosmetic#playerRenderLayer） */
    private static final class Cosmetic implements ICurioRenderer {

        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack pose,
                RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount,
                float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!TrinketsConfig.CLIENT.render.rendering.get()) {
                return;
            }
            final TraitRenderContext ctx = context(slotContext, pose, parent, buffers, light, limbSwing, limbSwingAmount, partialTicks);
            if (ctx != null) {
                CosmeticTraits.render(TrinketCosmetic.getFeature(stack), ctx);
            }
        }
    }

    static boolean isRace(AbstractClientPlayer player, String race) {
        final EntityProperties properties = EntityProperties.get(player);
        return properties != null && race.equalsIgnoreCase(properties.getCurrentRaceCache().getRace().getName());
    }

    /** 装饰品不属于任何种族时的颜色取自玩家当前外观（1.12 装饰品调用的无颜色参数版本即如此） */
    @SuppressWarnings("unchecked")
    private static <T extends LivingEntity, M extends EntityModel<T>> TraitRenderContext context(SlotContext slotContext, PoseStack pose,
            RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount,
            float partialTicks) {
        if (!(slotContext.entity() instanceof AbstractClientPlayer player) || !(parent.getModel() instanceof PlayerModel<?> model)) {
            return null;
        }
        final EntityProperties properties = EntityProperties.get(player);
        final TraitRenderContext.Colors colors = properties == null ? TraitRenderContext.Colors.defaults()
                : TraitRenderContext.Colors.of(properties.getRaceHandler().getAppearance());
        return new TraitRenderContext(pose, buffers, light, player, (PlayerModel<AbstractClientPlayer>) model,
                "slim".equals(player.getModelName()), false, partialTicks, limbSwing, limbSwingAmount, colors);
    }

    private AccessoryRenderers() {
    }
}
