package xzeroair.trinkets.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.Camera;

import xzeroair.trinkets.client.race.RaceCameraHooks;

/**
 * 第三人称相机与玩家的距离随种族体型缩放（1.12 PlayerCameraSetupEvents#CameraSetup）。
 * Forge 1.20.1 没有调整相机距离的事件，故修改 Camera#setup 中传给 getMaxZoom 的初始距离（原版恒为 4）。
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D"), index = 0)
    private double xat$scaleZoom(double distance) {
        // 用公开的 getEntity 而非 @Shadow 字段，避免生产环境字段名混淆映射问题
        return RaceCameraHooks.scaleZoom(((Camera) (Object) this).getEntity(), distance);
    }
}
