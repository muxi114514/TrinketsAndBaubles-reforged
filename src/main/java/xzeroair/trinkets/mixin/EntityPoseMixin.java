package xzeroair.trinkets.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.races.EntityRacePropertiesHandler;

/**
 * 0.34.1: whether a player fits into a pose (standing up, crouching) is checked with their race size, so small races
 * can stand up under low ceilings and big races don't clip into them.
 */
@Mixin(value={Entity.class})
public abstract class EntityPoseMixin {
    @Inject(method={"canEnterPose"}, at={@At(value="HEAD")}, cancellable=true)
    private void xat$fixRacePoseCollision(Pose pose, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        EntityProperties properties = EntityProperties.get(player);
        if (properties == null) {
            return;
        }
        EntityRacePropertiesHandler handler = properties.getRaceHandler();
        if (handler == null) {
            return;
        }
        float widthScale = handler.getWidthScale();
        float heightScale = handler.getHeightScale();
        if (widthScale == 1.0f && heightScale == 1.0f) {
            return;
        }
        EntityDimensions base = player.getDimensions(pose);
        float width = handler.clampWidth(base.width * widthScale);
        float height = handler.clampHeight(base.height * heightScale);
        EntityDimensions scaled = base.fixed ? EntityDimensions.fixed(width, height) : EntityDimensions.scalable(width, height);
        cir.setReturnValue(player.level().noCollision((Entity)player, scaled.makeBoundingBox(player.position()).deflate(1.0E-7)));
    }
}

