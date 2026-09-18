package xzeroair.trinkets.util.helpers;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 按配置字符串增删状态效果。
 *
 * 配置语法（沿用 1.12）：{@code [modid:]effect[:duration[:amplifier]]}
 * 省略 modid 时默认 minecraft，duration 默认 300 tick，amplifier 默认 0。
 *
 * 移植说明：1.12 的 Potion / PotionEffect 在 1.20.1 分别是 MobEffect / MobEffectInstance
 * （注意 1.20.1 的 Potion 指的是酿造药水，与 1.12 的 Potion 语义互换，勿搞反）。
 */
public class PotionHelper {

    private static final int DEFAULT_DURATION = 300;

    /** 一条配置解析出的效果 */
    public record ParsedEffect(MobEffect effect, int duration, int amplifier) {

        public MobEffectInstance toInstance() {
            return new MobEffectInstance(this.effect, this.duration, this.amplifier, false, false);
        }
    }

    @Nullable
    public static ParsedEffect parse(String config) {
        if (config == null) {
            return null;
        }
        final String cleaned = config.trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        final String[] parts = cleaned.split(":");
        final String id = parts.length > 1 ? parts[0] + ":" + parts[1] : "minecraft:" + parts[0];
        final MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(id));
        if (effect == null) {
            return null;
        }
        int duration = DEFAULT_DURATION;
        int amplifier = 0;
        if (parts.length > 2) {
            duration = parseInt(parts[2], DEFAULT_DURATION);
            if (parts.length > 3) {
                amplifier = parseInt(parts[3], 0);
            }
        }
        return new ParsedEffect(effect, duration, amplifier);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** 该效果实例是否匹配配置列表中的任意一条 */
    public static boolean isEffect(@Nullable MobEffectInstance instance, List<? extends String> configs) {
        if (instance == null || configs == null) {
            return false;
        }
        final ResourceLocation actual = ForgeRegistries.MOB_EFFECTS.getKey(instance.getEffect());
        if (actual == null) {
            return false;
        }
        for (String config : configs) {
            final ParsedEffect parsed = parse(config);
            if (parsed != null && parsed.effect() == instance.getEffect()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按配置施加效果。
     * permanent 为真时表示「该效果应当常驻」——每 tick 调用本方法，效果将要过期时自动续上，
     * 从而在 1.20.1 不需要 1.12 那种超长时长的写法。
     */
    public static void addAllFromConfig(LivingEntity entity, boolean permanent, List<? extends String> configs) {
        if (entity == null || configs == null || configs.isEmpty()) {
            return;
        }
        for (String config : configs) {
            final ParsedEffect parsed = parse(config);
            if (parsed == null) {
                continue;
            }
            final MobEffectInstance active = entity.getEffect(parsed.effect());
            if (active == null) {
                entity.addEffect(parsed.toInstance());
            } else if (permanent && active.getDuration() < parsed.duration() / 2) {
                entity.addEffect(parsed.toInstance());
            }
        }
    }

    /** 按配置无条件施加（已有同种效果时由原版规则合并），用于命中类一次性效果 */
    public static void applyAllFromConfig(LivingEntity entity, List<? extends String> configs) {
        if (entity == null || configs == null) {
            return;
        }
        for (String config : configs) {
            final ParsedEffect parsed = parse(config);
            if (parsed != null) {
                entity.addEffect(parsed.toInstance());
            }
        }
    }

    public static void removeAllFromConfig(LivingEntity entity, List<? extends String> configs) {
        if (entity == null || configs == null || configs.isEmpty()) {
            return;
        }
        for (String config : configs) {
            final ParsedEffect parsed = parse(config);
            if (parsed != null && entity.hasEffect(parsed.effect())) {
                entity.removeEffect(parsed.effect());
            }
        }
    }

    private PotionHelper() {
    }
}
