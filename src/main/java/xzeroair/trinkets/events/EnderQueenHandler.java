package xzeroair.trinkets.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.EnderManAngerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.entity.ai.EnderQueenFollowGoal;
import xzeroair.trinkets.entity.ai.EnderQueenKnightGoal;
import xzeroair.trinkets.entity.ai.EnderQueens;
import xzeroair.trinkets.init.ModItems;
import xzeroair.trinkets.init.ModRaces;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.abilities.EnderQueenAbilityConfig;
import xzeroair.trinkets.util.helpers.EntityChecks;
import xzeroair.trinkets.util.helpers.TrinketHelper;

/**
 * 末影女王的世界事件（Forge 事件总线）。对应 1.12 events/EnderQueenHandler 与 entity/ai/EnderAiEdit。
 *
 * 移植说明：
 * - 1.12 用反射式字符串比对删掉原版 AIFindPlayer，再塞入改写的 EnderAiEdit 只为「不被女王的注视激怒」；
 *   1.20.1 的 Forge 提供 EnderManAngerEvent，取消即可，原版 AI 保持不动。
 * - EnderTeleportEvent 在 1.20.1 拆为 EntityTeleportEvent 的多个子类，指令传送不受影响。
 */
public class EnderQueenHandler {

    /** 女王 / Boss 阻止瞬移的范围（水平 ±格、垂直 ±格） */
    public static final double BLOCK_RADIUS = 16.0D;
    public static final double BLOCK_HEIGHT = 4.0D;

    @SubscribeEvent
    public void onEnderManAnger(EnderManAngerEvent event) {
        if (!isEnabled()) {
            return;
        }
        final Player player = event.getPlayer();
        final EnderMan enderMan = event.getEntity();
        final boolean ownQueen = player.getStringUUID().equals(enderMan.getPersistentData().getString(EnderQueens.QUEEN_UUID_TAG));
        if (ownQueen || EnderQueens.wearsCrown(player) || EnderQueens.hasQueenAbility(player)
                || TrinketHelper.isEquipped(player, stack -> stack.is(ModItems.DRAGONS_EYE.get()))
                || isDragon(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onEnderManJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof EnderMan enderMan)) {
            return;
        }
        enderMan.getPersistentData().remove(EnderQueens.FOLLOWING_TAG);
        if (!isEnabled()) {
            return;
        }
        if (!hasGoal(enderMan.targetSelector.getAvailableGoals(), EnderQueenKnightGoal.class)) {
            enderMan.targetSelector.addGoal(2, new EnderQueenKnightGoal(enderMan));
        }
        if (config().endermanFollow.get() && !hasGoal(enderMan.goalSelector.getAvailableGoals(), EnderQueenFollowGoal.class)) {
            enderMan.goalSelector.addGoal(3, new EnderQueenFollowGoal(enderMan));
        }
    }

    /** 女王或 Boss 附近 16 格内，生物（及开启 PVP 时的玩家）无法瞬移；水中不受限 */
    @SubscribeEvent
    public void onTeleport(EntityTeleportEvent event) {
        if (!(event instanceof EntityTeleportEvent.EnderEntity || event instanceof EntityTeleportEvent.EnderPearl
                || event instanceof EntityTeleportEvent.ChorusFruit) || !config().blockTeleportation.get()) {
            return;
        }
        final Entity entity = event.getEntity();
        if (entity.level().isClientSide || entity.isInWater()) {
            return;
        }
        final boolean restricted = entity instanceof Player
                ? entity instanceof ServerPlayer player && player.server.isPvpAllowed()
                : entity instanceof LivingEntity;
        if (!restricted) {
            return;
        }
        final AABB area = entity.getBoundingBox().inflate(BLOCK_RADIUS, BLOCK_HEIGHT, BLOCK_RADIUS);
        final boolean blocked = !entity.level().getEntitiesOfClass(LivingEntity.class, area,
                other -> other != entity && !other.isSpectator()
                        && (EntityChecks.isBoss(other) || EnderQueens.wearsCrown(other) || EnderQueens.hasQueenAbility(other)))
                .isEmpty();
        if (blocked) {
            event.setCanceled(true);
        }
    }

    private static boolean isDragon(Player player) {
        final EntityProperties properties = EntityProperties.get(player);
        return properties != null && properties.getCurrentRaceCache().getRace() == ModRaces.DRAGON.get();
    }

    private static boolean hasGoal(Iterable<WrappedGoal> goals, Class<?> type) {
        for (WrappedGoal goal : goals) {
            if (type.isInstance(goal.getGoal())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEnabled() {
        return TrinketsConfig.SERVER.items.enderCrown.enabled.get() && config().isEnabled();
    }

    private static EnderQueenAbilityConfig config() {
        return TrinketsConfig.SERVER.items.enderCrown.abilities.enderQueen;
    }
}
