package xzeroair.trinkets.events;

import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import xzeroair.trinkets.api.IAccessoryInterface;

/**
 * 把战斗与效果事件分发给饰品栏里的饰品（Forge 事件总线）。对应 1.12 events/TrinketEventHandler。
 *
 * 移植说明：
 * - 1.12 需同时遍历 Baubles 与自带饰品栏；1.20.1 只遍历 Curios（装饰槽不参与）。
 * - 1.12 的登录/切维度/克隆/死亡掉落等处理服务于自带饰品栏的存取与同步，由 Curios 承担，不移植；
 *   逐 tick 回调改由 ICurioItem#curioTick 驱动。
 */
public class AccessoryEventHandler {

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        final LivingEntity attacked = event.getEntity();
        forEachAccessory(attacked, (stack, item) -> item.eventLivingAttacked(stack, attacked, event));
        final LivingEntity attacker = attackerOf(event.getSource().getEntity());
        if (attacker != null) {
            forEachAccessory(attacker, (stack, item) -> item.eventLivingAttacker(stack, attacker, event));
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        final LivingEntity attacked = event.getEntity();
        forEachAccessory(attacked, (stack, item) -> item.eventLivingHurtAttacked(stack, attacked, event));
        final LivingEntity attacker = attackerOf(event.getSource().getEntity());
        if (attacker != null) {
            forEachAccessory(attacker, (stack, item) -> item.eventLivingHurtAttacker(stack, attacker, event));
        }
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        final LivingEntity attacked = event.getEntity();
        forEachAccessory(attacked, (stack, item) -> item.eventLivingDamageAttacked(stack, attacked, event));
        final LivingEntity attacker = attackerOf(event.getSource().getEntity());
        if (attacker != null) {
            forEachAccessory(attacker, (stack, item) -> item.eventLivingDamageAttacker(stack, attacker, event));
        }
    }

    @SubscribeEvent
    public void onEffectApplicable(MobEffectEvent.Applicable event) {
        final LivingEntity entity = event.getEntity();
        forEachAccessory(entity, (stack, item) -> item.eventPotionApplicable(stack, entity, event));
    }

    /** 遍历已启用的饰品；无 Curios 物品栏的实体直接跳过 */
    private static void forEachAccessory(LivingEntity entity, BiConsumer<ItemStack, IAccessoryInterface> consumer) {
        CuriosApi.getCuriosInventory(entity).ifPresent(inventory -> {
            for (SlotResult result : inventory.findCurios(stack -> stack.getItem() instanceof IAccessoryInterface)) {
                final ItemStack stack = result.stack();
                final IAccessoryInterface accessory = (IAccessoryInterface) stack.getItem();
                if (accessory.isAccessoryEnabled()) {
                    consumer.accept(stack, accessory);
                }
            }
        });
    }

    @Nullable
    private static LivingEntity attackerOf(@Nullable Object entity) {
        return entity instanceof LivingEntity living ? living : null;
    }
}
