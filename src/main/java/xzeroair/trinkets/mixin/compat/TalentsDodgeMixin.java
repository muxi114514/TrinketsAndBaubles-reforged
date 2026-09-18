package xzeroair.trinkets.mixin.compat;

import java.util.function.Supplier;

import net.minecraftforge.network.NetworkEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xzeroair.trinkets.util.compat.DodgeCompat;
import xzeroair.trinkets.util.compat.ModCompat;

/**
 * Talents「侧步」闪避包的服务端处理：只有通过全部检查（技能开启、在地面、不在冷却）才会写入冷却表，
 * 故挂在写冷却表（HashMap#put）之后，确保只在闪避真正生效时通知本模组。目标类不存在时 @Pseudo 静默跳过。
 */
@Pseudo
@Mixin(targets = "com.seniors.talents.network.packet.common.DodgePacket", remap = false)
public abstract class TalentsDodgeMixin {

    @Inject(method = "lambda$handle$0", require = 0,
            at = @At(value = "INVOKE", target = "Ljava/util/HashMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER))
    private void xat$onSidestep(Supplier<NetworkEvent.Context> context, CallbackInfo ci) {
        if (ModCompat.talents()) {
            DodgeCompat.onDodge(context.get().getSender());
        }
    }
}
