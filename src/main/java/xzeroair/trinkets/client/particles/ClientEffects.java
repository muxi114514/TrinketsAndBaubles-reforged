package xzeroair.trinkets.client.particles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.network.EffectsRenderPacket;

/**
 * 按特效编号在客户端生成粒子与音效。对应 1.12 ClientProxy#renderEffect。
 *
 * 移植说明：1.12 的 8 号护盾光环特效在全部源码中无任何发送方，不移植。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientEffects {

    public static void play(int effectID, double x, double y, double z, double x2, double y2, double z2,
            int color, float alpha, float intensity) {
        final Minecraft mc = Minecraft.getInstance();
        final ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        switch (effectID) {
            case EffectsRenderPacket.LIGHTNING -> {
                mc.particleEngine.add(new LightningParticle(level, x, y, z, x2, y2, z2, color, alpha, intensity));
                playImpactSound(level, x, y, z);
            }
            case EffectsRenderPacket.LIGHTNING_ORB -> {
                LightningOrbParticle.spawn(level, x, y, z, color, alpha);
                playImpactSound(level, x, y, z);
            }
            case EffectsRenderPacket.LIGHTNING_ORB_SILENT -> LightningOrbParticle.spawn(level, x, y, z, color, alpha);
            case EffectsRenderPacket.SWEEP -> level.addParticle(ParticleTypes.SWEEP_ATTACK, x, y, z, 0, 0, 0);
            case EffectsRenderPacket.FIRE_BREATH -> mc.particleEngine.add(new FireBreathParticle(level, x, y, z, 0, 0, 0, color, alpha));
            case EffectsRenderPacket.EXPLOSION -> {
                level.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 0, 0, 0);
                level.playLocalSound(x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F,
                        (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F, true);
            }
            case EffectsRenderPacket.GREED -> mc.particleEngine.add(new GreedParticle(level,
                    x + level.random.nextDouble(), y + level.random.nextDouble(), z + level.random.nextDouble(), color));
            default -> {
            }
        }
    }

    public static void breathWisp(double x, double y, double z, int color) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.particleEngine.add(new BreathWispParticle(mc.level, x, y, z, color));
        }
    }

    private static void playImpactSound(ClientLevel level, double x, double y, double z) {
        level.playLocalSound(x, y, z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 0.2F, 0.6F, true);
    }

    private ClientEffects() {
    }
}
