package xzeroair.trinkets.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;

/**
 * 佩戴在饰品栏时接收事件的物品（对应 1.12 util/interfaces/IAccessoryInterface）。
 * 附属模组的饰品实现本接口即可由 events/AccessoryEventHandler 分发事件。
 *
 * 移植说明：1.12 接口还有跳跃、坠落、挖掘、掉落物等 20 余个钩子，但 0.33.4 的全部饰品只用到
 * 下面这几项（其余效果都经能力系统实现），按接口隔离原则只保留实际被使用的钩子。
 */
public interface IAccessoryInterface {

    /** 配置关闭时饰品不生效也不能佩戴 */
    default boolean isAccessoryEnabled() {
        return true;
    }

    /** 佩戴者即将受到攻击 */
    default void eventLivingAttacked(ItemStack stack, LivingEntity attacked, LivingAttackEvent event) {
    }

    /** 佩戴者即将攻击他人 */
    default void eventLivingAttacker(ItemStack stack, LivingEntity attacker, LivingAttackEvent event) {
    }

    /** 佩戴者受伤（护甲结算前） */
    default void eventLivingHurtAttacked(ItemStack stack, LivingEntity attacked, LivingHurtEvent event) {
    }

    /** 佩戴者伤害他人（护甲结算前） */
    default void eventLivingHurtAttacker(ItemStack stack, LivingEntity attacker, LivingHurtEvent event) {
    }

    /** 佩戴者受伤（护甲结算后） */
    default void eventLivingDamageAttacked(ItemStack stack, LivingEntity attacked, LivingDamageEvent event) {
    }

    /** 佩戴者伤害他人（护甲结算后） */
    default void eventLivingDamageAttacker(ItemStack stack, LivingEntity attacker, LivingDamageEvent event) {
    }

    /** 效果即将施加给佩戴者（免疫的阻断层） */
    default void eventPotionApplicable(ItemStack stack, LivingEntity entity, MobEffectEvent.Applicable event) {
    }
}
