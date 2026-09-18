package xzeroair.trinkets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import xzeroair.trinkets.entity.ai.EnderQueens;
import xzeroair.trinkets.util.Reference;

/** 女王听不到末影人的凝视尖啸（客户端）。对应 1.12 EnderQueenHandler#soundEvent。 */
@Mod.EventBusSubscriber(modid = Reference.MODID, value = Dist.CLIENT)
public final class EnderQueenSoundHandler {

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (event.getSound() == null || !SoundEvents.ENDERMAN_STARE.getLocation().equals(event.getSound().getLocation())) {
            return;
        }
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && (EnderQueens.hasQueenAbility(player) || EnderQueens.wearsCrown(player))) {
            event.setSound(null);
        }
    }

    private EnderQueenSoundHandler() {
    }
}
