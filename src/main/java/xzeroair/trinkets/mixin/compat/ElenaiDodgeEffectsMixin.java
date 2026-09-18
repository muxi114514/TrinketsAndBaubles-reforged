package xzeroair.trinkets.mixin.compat;

import net.minecraftforge.network.NetworkEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xzeroair.trinkets.util.compat.DodgeCompat;
import xzeroair.trinkets.util.compat.ModCompat;

/**
 * Elenai Dodge 2 的「闪避生效」包在服务端主线程处理时（enqueueWork 内的 lambda），通知本模组触发闪避能力效果。
 * 目标类不存在（未装 Elenai Dodge 2）时 @Pseudo 让本 mixin 静默跳过。
 */
@Pseudo
@Mixin(targets = "com.elenai.elenaidodge2.networking.messages.DodgeEffectsCTSPacket", remap = false)
public abstract class ElenaiDodgeEffectsMixin {

    @Inject(method = "lambda$handle$1", at = @At("HEAD"), require = 0)
    private void xat$onDodgeEffects(NetworkEvent.Context context, CallbackInfo ci) {
        if (ModCompat.elenaiDodge()) {
            DodgeCompat.onDodge(context.getSender());
        }
    }
}
