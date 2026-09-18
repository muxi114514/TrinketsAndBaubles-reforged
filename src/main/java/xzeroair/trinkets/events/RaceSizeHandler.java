package xzeroair.trinkets.events;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.races.EntityRacePropertiesHandler;
import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 种族体型缩放（Forge 事件总线）。
 *
 * 移植说明：这是 1.12 → 1.20.1 改动最大的一处。
 * 1.12 在种族处理器里直接写 entity.width / entity.height 字段、手工 setEntityBoundingBox，
 * 还要自行判断睡觉 / 潜行 / 鞘翅三种姿势分别算 eyeHeight。
 * 1.20.1 的实体尺寸不可变，唯一正确入口是 EntityEvent.Size——而且事件已按 Pose 给出该姿势下的
 * 基准尺寸与眼高，姿势判断由原版完成，这里只需按种族的体型百分比等比缩放，
 * 原版 1.12 那套姿势分支因此整体消失。
 *
 * 触发时机：实体尺寸需要重算时（EntityRacePropertiesHandler.updateSize 里调 refreshDimensions 主动触发）。
 */
public class RaceSizeHandler {

    @SubscribeEvent
    public void onEntitySize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        final EntityProperties properties = EntityProperties.get(living);
        if (properties == null) {
            return;
        }
        final EntityRacePropertiesHandler handler = properties.getRaceHandler();
        if (handler == null) {
            return;
        }
        final float widthScale = handler.getWidthScale();
        final float heightScale = handler.getHeightScale();
        if (widthScale == 1.0F && heightScale == 1.0F) {
            return;
        }
        // 幼体不缩放，避免与原版幼体尺寸叠加
        if (living.isBaby()) {
            return;
        }

        final EntityDimensions base = event.getNewSize();
        final float width = handler.clampWidth(base.width * widthScale);
        final float height = handler.clampHeight(base.height * heightScale);
        event.setNewSize(base.fixed ? EntityDimensions.fixed(width, height) : EntityDimensions.scalable(width, height));

        // 眼高必须两端等比缩放，否则服务端的视线判定与客户端画面对不上；
        // 「相机是否跟随」只是客户端选项，故仅客户端读该配置——
        // CLIENT 配置在专用服务器上根本不加载，服务端读它会抛异常。
        final boolean adjustEyeHeight = !living.level().isClientSide
                || TrinketsConfig.CLIENT.render.cameraHeight.get();
        if (adjustEyeHeight) {
            event.setNewEyeHeight(Math.max(event.getNewEyeHeight() * heightScale, 0.2F));
        }
    }
}
