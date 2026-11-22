package xzeroair.trinkets.util.compat.survival;

import java.util.List;
import java.util.function.BooleanSupplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.FakePlayer;

import xzeroair.trinkets.Trinkets;
import xzeroair.trinkets.util.compat.ModCompat;

/**
 * 生存类模组联动门面（对应 1.12 util/compat/SurvivalCompat）。能力只调用本类，不接触具体模组的类。
 *
 * 移植说明：1.12 同时对接 Tough As Nails 与 Simple Difficulty（温度与口渴都在其中）。1.20.1 整合包里温度由冷汗负责，
 * 口渴/寄生虫效果由 Simple Difficulty Reforge 提供，水分数值由 Thirst Was Taken 保存，故按危害类型分别对接；
 * 效果只按注册名判断，不需要 Simple Difficulty 的 API。
 * <p>
 * Simple Difficulty Reforge can also run SimpleDifficulty's own temperature and thirst (when Cold Sweat / Thirst Was
 * Taken aren't installed). Then heat/cold immunity and Water Absorption use it through {@link SimpleDifficultyBridge},
 * like 1.12's SDCompat.
 */
public final class SurvivalCompat {

    /** 冷汗核心体温达到 ±100 开始受伤，免疫把体温限制在 ±本值以内 */
    public static final double TEMPERATURE_LIMIT = 99.0D;

    public enum Hazard {
        HEAT(SurvivalCompat::temperatureCompat, List.of(new ResourceLocation(ModCompat.SIMPLE_DIFFICULTY, "hyperthermia"))),
        COLD(SurvivalCompat::temperatureCompat, List.of(new ResourceLocation(ModCompat.SIMPLE_DIFFICULTY, "hypothermia"))),
        THIRST(ModCompat::simpleDifficulty, List.of(new ResourceLocation(ModCompat.SIMPLE_DIFFICULTY, "thirsty"))),
        PARASITES(ModCompat::simpleDifficulty, List.of(new ResourceLocation(ModCompat.SIMPLE_DIFFICULTY, "parasites")));

        private final BooleanSupplier active;
        private final List<ResourceLocation> effects;

        Hazard(BooleanSupplier active, List<ResourceLocation> effects) {
            this.active = active;
            this.effects = effects;
        }

        /** 负责该危害的模组是否已加载且联动开启 */
        public boolean isActive() {
            return this.active.getAsBoolean();
        }

        /** 阻断层：该效果是否属于本类危害 */
        public boolean isHazardEffect(MobEffectInstance instance) {
            return this.isActive() && this.effects.stream().anyMatch(id -> ModCompat.isEffect(instance, id));
        }
    }

    /** 清理层与体温修正：移除已有的危害效果，并把体温限制在伤害线以内 */
    public static void protect(LivingEntity entity, Hazard hazard) {
        if (entity.level().isClientSide || !hazard.isActive()) {
            return;
        }
        hazard.effects.forEach(id -> ModCompat.removeEffect(entity, id));
        if (!isRealPlayer(entity)) {
            return;
        }
        if (hazard != Hazard.HEAT && hazard != Hazard.COLD) {
            return;
        }
        if (ModCompat.coldSweat()) {
            try {
                if (hazard == Hazard.HEAT) {
                    ColdSweatBridge.limitHeat(entity);
                } else {
                    ColdSweatBridge.limitCold(entity);
                }
            } catch (RuntimeException | LinkageError e) {
                Trinkets.LOGGER.error("Trinkets had an error with Cold Sweat compat", e);
            }
        }
        if (simpleDifficultyTemperature()) {
            try {
                if (hazard == Hazard.HEAT) {
                    SimpleDifficultyBridge.limitHeat((Player) entity);
                } else {
                    SimpleDifficultyBridge.limitCold((Player) entity);
                }
            } catch (RuntimeException | LinkageError e) {
                Trinkets.LOGGER.error("Trinkets had an error with Simple Difficulty compat", e);
            }
        }
    }

    /** Heat and cold immunity work with Cold Sweat, or with Simple Difficulty Reforge's own temperature. */
    private static boolean temperatureCompat() {
        return ModCompat.coldSweat() || simpleDifficultyTemperature();
    }

    /** Simple Difficulty Reforge runs SimpleDifficulty's own temperature (when Cold Sweat isn't used). */
    public static boolean simpleDifficultyTemperature() {
        return ModCompat.simpleDifficulty() && SimpleDifficultyBridge.temperature();
    }

    /** Simple Difficulty Reforge runs SimpleDifficulty's own thirst (when Thirst Was Taken isn't used). */
    public static boolean simpleDifficultyThirst() {
        return ModCompat.simpleDifficulty() && SimpleDifficultyBridge.thirst();
    }

    /** The mod that heat and cold immunity work with, for tooltips: Cold Sweat first, as before. */
    public static String temperatureModId() {
        return !ModCompat.coldSweat() && simpleDifficultyTemperature() ? ModCompat.SIMPLE_DIFFICULTY : ModCompat.COLD_SWEAT;
    }

    /** The mod that Water Absorption fills, for tooltips. */
    public static String thirstModId() {
        return simpleDifficultyThirst() ? ModCompat.SIMPLE_DIFFICULTY : ModCompat.THIRST_WAS_TAKEN;
    }

    public static boolean canAbsorbWater() {
        return ModCompat.thirstWasTaken() || simpleDifficultyThirst();
    }

    /** 回复水分（1.12 addThirst：只加水分值，不加饱和） */
    public static void addThirst(LivingEntity entity, int amount) {
        if (entity.level().isClientSide || !canAbsorbWater() || !isRealPlayer(entity)) {
            return;
        }
        // Simple Difficulty Reforge's own thirst only runs when Thirst Was Taken isn't used (unless set to run anyway);
        // when it runs, that's the thirst bar the player sees
        if (simpleDifficultyThirst()) {
            try {
                SimpleDifficultyBridge.addThirst((Player) entity, amount);
            } catch (RuntimeException | LinkageError e) {
                Trinkets.LOGGER.error("Trinkets had an error with Simple Difficulty compat", e);
            }
            return;
        }
        try {
            ThirstWasTakenBridge.addThirst((Player) entity, amount, 0);
        } catch (RuntimeException | LinkageError e) {
            Trinkets.LOGGER.error("Trinkets had an error with Thirst Was Taken compat", e);
        }
    }

    private static boolean isRealPlayer(LivingEntity entity) {
        return entity instanceof Player && !(entity instanceof FakePlayer);
    }

    private SurvivalCompat() {
    }
}
