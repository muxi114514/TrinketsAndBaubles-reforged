package xzeroair.trinkets.util.compat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 联动模组是否生效 = 已加载 且 对应联动开关开启。对应 1.12 compatibility/ModCompat 与各 XxxCompat#isModEnabled。
 *
 * 加载状态在首次查询时缓存（运行期不变）；开关每次实时读取，配置重载后立即生效。
 * 只在游戏运行期调用（SERVER 配置加载之后），启动期注册事件时请用 {@link #isLoaded(String)}。
 */
public final class ModCompat {

    public static final String FIRST_AID = "firstaid";
    public static final String ELENAI_DODGE = "elenaidodge2";
    public static final String TALENTS = "talents";
    public static final String COLD_SWEAT = "cold_sweat";
    public static final String SIMPLE_DIFFICULTY = "simple_difficulty";
    public static final String THIRST_WAS_TAKEN = "thirst";
    public static final String ENHANCED_VISUALS = "enhancedvisuals";
    public static final String LYCANITES_MOBS = "lycanitesmobs";
    public static final String DEFILED_LANDS = "defiledlands";

    private static final Map<String, Boolean> LOADED = new ConcurrentHashMap<>();

    public static boolean isLoaded(String modId) {
        return LOADED.computeIfAbsent(modId, id -> ModList.get() != null && ModList.get().isLoaded(id));
    }

    public static boolean firstAid() {
        return isLoaded(FIRST_AID) && TrinketsConfig.SERVER.compat.firstAid.get();
    }

    public static boolean elenaiDodge() {
        return isLoaded(ELENAI_DODGE) && TrinketsConfig.SERVER.compat.elenaiDodge.get();
    }

    public static boolean talents() {
        return isLoaded(TALENTS) && TrinketsConfig.SERVER.compat.talents.get();
    }

    public static boolean coldSweat() {
        return isLoaded(COLD_SWEAT) && TrinketsConfig.SERVER.compat.coldSweat.get();
    }

    public static boolean simpleDifficulty() {
        return isLoaded(SIMPLE_DIFFICULTY) && TrinketsConfig.SERVER.compat.simpleDifficulty.get();
    }

    public static boolean thirstWasTaken() {
        return isLoaded(THIRST_WAS_TAKEN) && TrinketsConfig.SERVER.compat.thirstWasTaken.get();
    }

    public static boolean enhancedVisuals() {
        return isLoaded(ENHANCED_VISUALS) && TrinketsConfig.SERVER.compat.enhancedVisuals.get();
    }

    public static boolean lycanitesMobs() {
        return isLoaded(LYCANITES_MOBS) && TrinketsConfig.SERVER.compat.lycanitesMobs.get();
    }

    public static boolean defiledLands() {
        return isLoaded(DEFILED_LANDS) && TrinketsConfig.SERVER.compat.defiledLands.get();
    }

    /** 按注册名取其它模组的效果；未安装或不存在时为 null */
    @Nullable
    public static MobEffect effect(ResourceLocation id) {
        return ForgeRegistries.MOB_EFFECTS.containsKey(id) ? ForgeRegistries.MOB_EFFECTS.getValue(id) : null;
    }

    public static boolean isEffect(MobEffectInstance instance, ResourceLocation id) {
        return id.equals(ForgeRegistries.MOB_EFFECTS.getKey(instance.getEffect()));
    }

    /** 清理层：移除实体身上已有的该效果 */
    public static void removeEffect(LivingEntity entity, ResourceLocation id) {
        final MobEffect effect = effect(id);
        if (effect != null && entity.hasEffect(effect)) {
            entity.removeEffect(effect);
        }
    }

    public static boolean hasEffect(LivingEntity entity, ResourceLocation id) {
        final MobEffect effect = effect(id);
        return effect != null && entity.hasEffect(effect);
    }

    private ModCompat() {
    }
}
