package xzeroair.trinkets.client.race;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.races.EntityRacePropertiesHandler;
import xzeroair.trinkets.util.config.TrinketsConfig;

/** 相机距离按体型高度比例缩放，供 CameraMixin 调用；关闭「相机高度」选项时保持原版。 */
@OnlyIn(Dist.CLIENT)
public final class RaceCameraHooks {

    public static double scaleZoom(Entity entity, double distance) {
        if (!(entity instanceof Player player) || !TrinketsConfig.CLIENT.render.cameraHeight.get()) {
            return distance;
        }
        final EntityProperties properties = EntityProperties.get(player);
        if (properties == null) {
            return distance;
        }
        final EntityRacePropertiesHandler handler = properties.getRaceHandler();
        final float scale = handler.getHeightScale();
        return scale == 1.0F && !handler.isTransforming() ? distance : distance * scale;
    }

    private RaceCameraHooks() {
    }
}
