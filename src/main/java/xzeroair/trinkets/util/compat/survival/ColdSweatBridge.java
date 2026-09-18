package xzeroair.trinkets.util.compat.survival;

import com.momosoftworks.coldsweat.api.util.Temperature;

import net.minecraft.world.entity.LivingEntity;

/**
 * 冷汗接缝：唯一直接引用冷汗类的地方，只经 {@link SurvivalCompat} 在冷汗已加载时调用。
 *
 * 冷汗的核心体温达到 ±100 开始受伤（超出范围 ±150）。免疫只把核心体温限制在伤害线以内，
 * 对应 1.12 Tough As Nails 联动「炎热区 → 温暖上限、冰冷区 → 凉爽下限」的做法：仍会感到冷热，但不会受伤。
 */
final class ColdSweatBridge {

    private static final double HEAT_LIMIT = SurvivalCompat.TEMPERATURE_LIMIT;
    private static final double COLD_LIMIT = -SurvivalCompat.TEMPERATURE_LIMIT;

    static void limitHeat(LivingEntity entity) {
        if (Temperature.get(entity, Temperature.Trait.CORE) > HEAT_LIMIT) {
            Temperature.set(entity, Temperature.Trait.CORE, HEAT_LIMIT);
        }
    }

    static void limitCold(LivingEntity entity) {
        if (Temperature.get(entity, Temperature.Trait.CORE) < COLD_LIMIT) {
            Temperature.set(entity, Temperature.Trait.CORE, COLD_LIMIT);
        }
    }

    private ColdSweatBridge() {
    }
}
