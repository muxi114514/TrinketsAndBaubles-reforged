package xzeroair.trinkets.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.Trinkets;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.traits.AbilityHandler.AbilityHolder;
import xzeroair.trinkets.traits.abilities.interfaces.IAbilityInterface;

/**
 * 能力事件分发：把一次游戏事件依次交给实体身上实现了某接口的全部能力。
 *
 * 移植说明：1.12 的 CombatHandler / MovementHandler / BlockBreakEvents 里，
 * 「取能力表 → 遍历 → instanceof 接口 → 链式传值 → 满足条件中断 → 每个能力单独 try-catch」
 * 这一整段被复制了十余遍。收敛到此处后，事件处理器只剩一行分发调用，
 * 且异常隔离集中一处：单个能力出错只记日志、跳过它，不影响其余能力与事件本身。
 *
 * 线程安全：遍历的是能力表的快照，能力回调里移除/替换能力不会触发 ConcurrentModificationException。
 */
public final class AbilityDispatcher {

    /**
     * 浮点链：每个能力接收上一个的结果；结果 ≤ 0 即停止（对应 1.12 的伤害归零即中断）。
     */
    public static <T> float chainFloat(@Nullable LivingEntity entity, Class<T> type, float initial,
            BiFunction<T, Float, Float> call) {
        float value = initial;
        for (IAbilityInterface ability : snapshot(entity)) {
            if (!type.isInstance(ability)) {
                continue;
            }
            try {
                value = call.apply(type.cast(ability), value);
            } catch (Exception e) {
                logError(ability, e);
                continue;
            }
            if (value <= 0) {
                break;
            }
        }
        return value;
    }

    /**
     * 取消链：沿用 1.12 的「cancel 传入、cancel 传出」约定，任一能力返回 true 即停止并视为取消。
     */
    public static <T> boolean chainCancel(@Nullable LivingEntity entity, Class<T> type, boolean initial,
            BiFunction<T, Boolean, Boolean> call) {
        boolean cancel = initial;
        for (IAbilityInterface ability : snapshot(entity)) {
            if (!type.isInstance(ability)) {
                continue;
            }
            try {
                cancel = call.apply(type.cast(ability), cancel);
            } catch (Exception e) {
                logError(ability, e);
                continue;
            }
            if (cancel) {
                break;
            }
        }
        return cancel;
    }

    /**
     * 不中断的取消链：全部能力都会执行，最终结果为最后一次返回值。
     * 用于雷击这类「每个能力都要有机会做反应」的事件（与 1.12 行为一致）。
     */
    public static <T> boolean foldCancel(@Nullable LivingEntity entity, Class<T> type, boolean initial,
            BiFunction<T, Boolean, Boolean> call) {
        boolean cancel = initial;
        for (IAbilityInterface ability : snapshot(entity)) {
            if (!type.isInstance(ability)) {
                continue;
            }
            try {
                cancel = call.apply(type.cast(ability), cancel);
            } catch (Exception e) {
                logError(ability, e);
            }
        }
        return cancel;
    }

    /**
     * 时长链：只要当前时长仍 ≥ 0 就继续交给下一个能力，变为负数即停（对应 1.12 物品使用 Start/Tick 事件的 dur &gt;= 0 判断）。
     */
    public static <T> int chainDuration(@Nullable LivingEntity entity, Class<T> type, int initial,
            BiFunction<T, Integer, Integer> call) {
        int value = initial;
        for (IAbilityInterface ability : snapshot(entity)) {
            if (value < 0) {
                break;
            }
            if (!type.isInstance(ability)) {
                continue;
            }
            try {
                value = call.apply(type.cast(ability), value);
            } catch (Exception e) {
                logError(ability, e);
            }
        }
        return value;
    }

    /** 整数链：不中断 */
    public static <T> int chainInt(@Nullable LivingEntity entity, Class<T> type, int initial,
            BiFunction<T, Integer, Integer> call) {
        int value = initial;
        for (IAbilityInterface ability : snapshot(entity)) {
            if (!type.isInstance(ability)) {
                continue;
            }
            try {
                value = call.apply(type.cast(ability), value);
            } catch (Exception e) {
                logError(ability, e);
            }
        }
        return value;
    }

    public static <T> void forEach(@Nullable LivingEntity entity, Class<T> type, Consumer<T> call) {
        for (IAbilityInterface ability : snapshot(entity)) {
            if (!type.isInstance(ability)) {
                continue;
            }
            try {
                call.accept(type.cast(ability));
            } catch (Exception e) {
                logError(ability, e);
            }
        }
    }

    private static List<IAbilityInterface> snapshot(@Nullable LivingEntity entity) {
        final EntityProperties properties = EntityProperties.get(entity);
        if (properties == null) {
            return List.of();
        }
        final List<IAbilityInterface> abilities = new ArrayList<>();
        for (AbilityHolder holder : properties.getAbilityHandler().getActiveAbilities().values()) {
            abilities.add(holder.getAbility());
        }
        return abilities;
    }

    private static void logError(IAbilityInterface ability, Exception e) {
        Trinkets.LOGGER.error("Trinkets had an error with ability: {}", ability.getRegistryName(), e);
    }

    private AbilityDispatcher() {
    }
}
