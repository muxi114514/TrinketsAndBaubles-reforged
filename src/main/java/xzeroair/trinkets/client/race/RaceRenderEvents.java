package xzeroair.trinkets.client.race;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.client.curios.AccessoryRenderers;
import xzeroair.trinkets.entity.AlphaWolf;
import xzeroair.trinkets.races.EntityRacePropertiesHandler;
import xzeroair.trinkets.traits.abilities.AbilityElytraFlight;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.util.Reference;

/**
 * 种族的整体渲染调整：体型缩放、自定义滑翔姿势、名牌高度补偿；以及特征渲染层与模型的注册。
 * 对应 1.12 client/events/RenderEntitiesEvent 与 RaceDefaultRenderer 的 doRenderPlayerPre/Post、Specials。
 *
 * 移植说明：
 * - 实体碰撞尺寸已由 RaceSizeHandler 缩放，这里只缩放画面；名牌高度取自缩放后的碰撞箱，会被画面缩放再缩一次，故单独抵消。
 * - 1.12 通过改写 limbSwing 让泰坦迈步变慢、滑翔时停摆腿；1.20.1 的步态状态不可外部写入，这两项表现未移植。
 * - 在最低优先级且未被取消时才压栈，保证 Pre/Post 成对。
 */
@Mod.EventBusSubscriber(modid = Reference.MODID, value = Dist.CLIENT)
public final class RaceRenderEvents {

    /** 渲染线程内当前这次玩家渲染是否由本类压过栈 */
    private static boolean scaled;
    private static boolean nameTagCompensated;
    private static float lastWidthScale = 1.0F;
    private static float lastHeightScale = 1.0F;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        final Player player = event.getEntity();
        final EntityProperties properties = EntityProperties.get(player);
        if (properties == null) {
            return;
        }
        final EntityRacePropertiesHandler handler = properties.getRaceHandler();
        final float width = handler.getWidthScale();
        final float height = handler.getHeightScale();
        final boolean gliding = properties.getAbilityHandler().getAbility(AbilityNames.key(AbilityNames.ELYTRA_FLIGHT))
                instanceof AbilityElytraFlight flight && flight.isGliding();
        final boolean resize = (handler.isTransforming() || handler.isTransformed()) && (width != 1.0F || height != 1.0F) && !player.isBaby();
        if (!resize && !gliding) {
            return;
        }
        final PoseStack pose = event.getPoseStack();
        pose.pushPose();
        scaled = true;
        lastWidthScale = resize ? width : 1.0F;
        lastHeightScale = resize ? height : 1.0F;
        if (resize) {
            // 骑乘时以座位点为缩放中心，避免小体型沉进坐骑里（狼王自带座位偏移，不处理）
            final double pivot = player.isPassenger() && !(player.getVehicle() instanceof AlphaWolf) ? -player.getMyRidingOffset() : 0.0D;
            pose.translate(0.0D, pivot, 0.0D);
            pose.scale(width, height, width);
            pose.translate(0.0D, -pivot, 0.0D);
        }
        if (gliding) {
            applyGlidePose(pose, player, event.getPartialTick());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (nameTagCompensated) {
            event.getPoseStack().popPose();
            nameTagCompensated = false;
        }
        if (scaled) {
            event.getPoseStack().popPose();
            scaled = false;
        }
    }

    /** 名牌位置 = 缩放后碰撞箱高度 + 0.5，外层画面缩放会再乘一次，这里反向抵消 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!scaled || nameTagCompensated || !(event.getEntity() instanceof AbstractClientPlayer)) {
            return;
        }
        if (lastWidthScale == 1.0F && lastHeightScale == 1.0F) {
            return;
        }
        event.getPoseStack().pushPose();
        event.getPoseStack().scale(1.0F / lastWidthScale, 1.0F / lastHeightScale, 1.0F / lastWidthScale);
        nameTagCompensated = true;
    }

    /** 1.12 RaceDefaultRenderer#applyCustomGlidePose：身体沿视线平躺，并按侧向速度转向 */
    private static void applyGlidePose(PoseStack pose, Player player, float partialTick) {
        final float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        final float vanillaRotation = 180.0F - bodyYaw;
        pose.mulPose(Axis.YP.rotationDegrees(vanillaRotation));
        pose.mulPose(Axis.XP.rotationDegrees(-90.0F - player.getViewXRot(partialTick)));
        final Vec3 look = player.getViewVector(partialTick);
        final double mx = player.getX() - player.xo;
        final double mz = player.getZ() - player.zo;
        final double speed = mx * mx + mz * mz;
        final double lookLength = look.x * look.x + look.z * look.z;
        if (speed > 0.0D && lookLength > 0.0D) {
            final double facing = Mth.clamp((mx * look.x + mz * look.z) / (Math.sqrt(speed) * Math.sqrt(lookLength)), -1.0D, 1.0D);
            final double strafe = mx * look.z - mz * look.x;
            pose.mulPose(Axis.YP.rotation((float) (Math.signum(strafe) * Math.acos(facing))));
        }
        pose.mulPose(Axis.YP.rotationDegrees(-vanillaRotation));
    }

    /** mod 事件总线：渲染层、模型层定义与附加模型 */
    @Mod.EventBusSubscriber(modid = Reference.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(WingTraits.FAIRY_WINGS_LAYER, WingTraits::createFakeWingsLayer);
        }

        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            WingTraits.bakeLayers(event.getEntityModels().bakeLayer(WingTraits.FAIRY_WINGS_LAYER));
            for (String skin : event.getSkins()) {
                final PlayerRenderer renderer = event.getSkin(skin);
                if (renderer != null) {
                    renderer.addLayer(new RaceTraitLayer(renderer));
                }
            }
        }

        @SubscribeEvent
        public static void onRegisterModels(ModelEvent.RegisterAdditional event) {
            ModelTraits.ALL.forEach(event::register);
            event.register(AccessoryRenderers.ENDER_CROWN_MODEL);
        }

        private ModBus() {
        }
    }

    private RaceRenderEvents() {
    }
}
