package xzeroair.trinkets.events;

import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.races.EntityRacePropertiesHandler;
import xzeroair.trinkets.traits.AbilityDispatcher;
import xzeroair.trinkets.traits.abilities.interfaces.IInteractionAbility;

/**
 * 交互与骑乘事件 → 种族钩子 + 能力。对应 1.12 EventHandler 中的 playerInteract* / playerRightClickItem / onMount。
 *
 * 移植说明：
 * - 1.12 用一个监听「PlayerInteractEvent 基类」的方法接收全部交互子事件并交给种族的 interact；
 *   1.20.1 的事件总线不接受在抽象事件类上注册监听，故拆成对各个具体子事件分别订阅。
 * - 1.12 的 IInteractionAbility 五个方法中，只有 rightClickWithItem 真正被分发，其余四个的分发调用被原作者注释掉，
 *   这里保持一致（接口方法保留，供日后需要的能力使用）。
 * - 骑乘准入：P3b-1 只在变身 tick 里发现不合规坐骑后踢下来，这里补上 1.12 在骑乘事件层的拦截——
 *   不合规的坐骑从一开始就骑不上去；下坐骑同样可被种族拒绝。
 */
public class InteractionHandler {

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        final Player player = event.getEntity();
        final EntityRacePropertiesHandler race = raceOf(player);
        if (race != null) {
            race.interact(event);
        }
        AbilityDispatcher.forEach(player, IInteractionAbility.class, ability -> ability.rightClickWithItem(
                player, event.getLevel(), event.getItemStack(), event.getHand(), event.getFace(), event.getPos()));
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        final EntityRacePropertiesHandler race = raceOf(event.getEntity());
        if (race != null) {
            race.interact(event);
        }
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        final EntityRacePropertiesHandler race = raceOf(event.getEntity());
        if (race != null) {
            race.interact(event);
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        final EntityRacePropertiesHandler race = raceOf(event.getEntity());
        if (race != null) {
            race.interact(event);
            race.interactWithEntity(event);
        }
    }

    @SubscribeEvent
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        final EntityRacePropertiesHandler race = raceOf(event.getEntity());
        if (race != null) {
            race.interact(event);
        }
    }

    @SubscribeEvent
    public void onMount(EntityMountEvent event) {
        if (!(event.getEntityMounting() instanceof LivingEntity rider)) {
            return;
        }
        final EntityRacePropertiesHandler race = raceOf(rider);
        if (race == null) {
            return;
        }
        final boolean allowed = event.isMounting()
                ? race.mountEntity(event.getEntityBeingMounted())
                : race.dismountedEntity(event.getEntityBeingMounted());
        if (!allowed) {
            event.setCanceled(true);
        }
    }

    @Nullable
    private static EntityRacePropertiesHandler raceOf(LivingEntity entity) {
        final EntityProperties properties = EntityProperties.get(entity);
        return properties != null ? properties.getRaceHandler() : null;
    }
}
