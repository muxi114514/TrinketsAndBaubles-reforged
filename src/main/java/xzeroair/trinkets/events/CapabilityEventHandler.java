package xzeroair.trinkets.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.capabilities.race.EntityProperties;

/**
 * 驱动实体能力的生命周期（Forge 事件总线）。
 * 对应 1.12 的 PlayerEventMC / EventHandler 中与 capability 相关的部分。
 */
public class CapabilityEventHandler {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        final EntityProperties properties = EntityProperties.get(event.player);
        final MagicStats magic = MagicStats.get(event.player);
        if (event.phase == TickEvent.Phase.START) {
            if (properties != null) {
                properties.onUpdatePre();
            }
            return;
        }
        if (properties != null) {
            properties.onUpdate();
        }
        if (magic != null) {
            magic.onUpdate();
        }
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        final EntityProperties properties = EntityProperties.get(living);
        if (properties != null) {
            properties.onJoinWorld();
        }
        final MagicStats magic = MagicStats.get(living);
        if (magic != null) {
            magic.onJoinWorld();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        final EntityProperties properties = EntityProperties.get(event.getEntity());
        if (properties != null) {
            properties.onLogin();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        final EntityProperties properties = EntityProperties.get(event.getEntity());
        if (properties != null) {
            properties.onLogoff();
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        final EntityProperties properties = EntityProperties.get(event.getEntity());
        if (properties != null) {
            properties.onChangedDimension(event.getFrom(), event.getTo());
        }
    }

    /**
     * 死亡/进入末地返回时，新玩家实体需继承旧实例的数据。
     *
     * 移植说明（1.20.1 关键坑）：旧玩家实体的 capability 在此刻已被 invalidate，
     * 必须先 reviveCaps() 才读得到，读完再 invalidateCaps() 复原——1.12 无此要求，
     * 直接读旧实例即可。漏掉 revive 会静默拿到空数据，表现为「死亡后种族/能力/魔力清零」。
     */
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        final Player original = event.getOriginal();
        final Player clone = event.getEntity();
        final boolean keepInventory = clone.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        original.reviveCaps();
        try {
            final EntityProperties oldProperties = EntityProperties.get(original);
            final EntityProperties newProperties = EntityProperties.get(clone);
            if (oldProperties != null && newProperties != null) {
                newProperties.copyFrom(oldProperties, event.isWasDeath(), keepInventory);
            }
            final MagicStats oldMagic = MagicStats.get(original);
            final MagicStats newMagic = MagicStats.get(clone);
            if (oldMagic != null && newMagic != null) {
                newMagic.copyFrom(oldMagic, event.isWasDeath(), keepInventory);
            }
        } finally {
            original.invalidateCaps();
        }
    }
}
