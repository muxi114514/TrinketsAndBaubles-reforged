package xzeroair.trinkets.entity.ai;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityTeleportEvent;

import xzeroair.trinkets.init.ModItems;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.util.helpers.EntityChecks;
import xzeroair.trinkets.util.helpers.TrinketHelper;

/**
 * 末影女王相关的共用判定：谁是女王、是否戴着王冠、末影人朝目标瞬移、骑士 AI 的女王定位。
 *
 * 移植说明：1.12 的 EnderQueensKnightAI 与 EnderMoveAI 各自复制了一整套「找召唤者 / 搜索附近女王 / 瞬移」代码，此处合并。
 * 末影人实体附加数据由 1.12 的 getEntityData 改为 1.20.1 的 getPersistentData，标签名不变。
 */
public final class EnderQueens {

    public static final String SUMMONED_TAG = "xat:summoned";
    public static final String QUEEN_UUID_TAG = "xat:ender_queen_owner_uuid";
    public static final String FOLLOWING_TAG = "xat:ender_queen_following";

    static final double DYNAMIC_QUEEN_RANGE_SQUARED = 256.0D;
    private static final int SEARCH_INTERVAL = 20;
    private static final String ABILITY = AbilityNames.key(AbilityNames.ENDER_QUEEN);

    public static boolean hasQueenAbility(@Nullable LivingEntity entity) {
        return EntityChecks.hasAbility(entity, ABILITY);
    }

    static boolean isActiveQueen(@Nullable Player player) {
        return player != null && player.isAlive() && hasQueenAbility(player);
    }

    /** 头盔位或饰品栏戴着末影王冠 */
    public static boolean wearsCrown(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ENDER_TIARA.get())
                || TrinketHelper.isEquipped(entity, stack -> stack.is(ModItems.ENDER_TIARA.get()));
    }

    /** 朝目标瞬移（照搬原版末影人 teleportTowards 的落点计算），经 Forge 瞬移事件可被取消 */
    static boolean teleportTowards(Mob mob, Entity target) {
        final Vec3 direction = new Vec3(mob.getX() - target.getX(), mob.getY(0.5D) - target.getEyeY(), mob.getZ() - target.getZ())
                .normalize();
        final double x = mob.getX() + (mob.getRandom().nextDouble() - 0.5D) * 8.0D - direction.x * 16.0D;
        final double y = mob.getY() + (mob.getRandom().nextInt(16) - 8) - direction.y * 16.0D;
        final double z = mob.getZ() + (mob.getRandom().nextDouble() - 0.5D) * 8.0D - direction.z * 16.0D;
        return teleport(mob, x, y, z);
    }

    /** randomTeleport 内部会检查目标区块已加载，不会同步加载区块 */
    public static boolean teleport(LivingEntity entity, double x, double y, double z) {
        final EntityTeleportEvent.EnderEntity event = ForgeEventFactory.onEnderTeleport(entity, x, y, z);
        if (event.isCanceled()) {
            return false;
        }
        final Vec3 from = entity.position();
        if (!entity.randomTeleport(event.getTargetX(), event.getTargetY(), event.getTargetZ(), true)) {
            return false;
        }
        entity.level().gameEvent(GameEvent.TELEPORT, from, GameEvent.Context.of(entity));
        entity.level().playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, entity.getSoundSource(), 1.0F, 1.0F);
        entity.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        return true;
    }

    /**
     * 为一只末影人定位它效忠的女王：有召唤者时只认召唤者（召唤者失去女王能力则骑士消失），
     * 否则每秒搜索一次 16 格内最近的女王。
     */
    static final class Locator {

        private final EnderMan knight;
        @Nullable
        private Player queen;
        private int nextSearchTick;

        Locator(EnderMan knight) {
            this.knight = knight;
        }

        boolean hasSummoner() {
            return this.knight.getPersistentData().contains(QUEEN_UUID_TAG);
        }

        @Nullable
        Player locate() {
            if (this.hasSummoner()) {
                this.queen = this.findSummoner();
                return this.queen;
            }
            if (this.isNearbyQueen(this.queen)) {
                return this.queen;
            }
            if (this.knight.tickCount < this.nextSearchTick) {
                return null;
            }
            this.nextSearchTick = this.knight.tickCount + SEARCH_INTERVAL + Math.floorMod(this.knight.getId(), SEARCH_INTERVAL);
            final List<Player> players = this.knight.level().getEntitiesOfClass(Player.class,
                    this.knight.getBoundingBox().inflate(16.0D, 4.0D, 16.0D), this::isNearbyQueen);
            Player closest = null;
            double closestDistance = Double.MAX_VALUE;
            for (Player player : players) {
                final double distance = this.knight.distanceToSqr(player);
                if (distance < closestDistance) {
                    closest = player;
                    closestDistance = distance;
                }
            }
            this.queen = closest;
            return closest;
        }

        @Nullable
        private Player findSummoner() {
            final MinecraftServer server = this.knight.level().getServer();
            if (server == null) {
                return null;
            }
            try {
                final UUID uuid = UUID.fromString(this.knight.getPersistentData().getString(QUEEN_UUID_TAG));
                final Player player = server.getPlayerList().getPlayer(uuid);
                if (player != null && !hasQueenAbility(player)) {
                    this.knight.discard();
                    return null;
                }
                return player != null && player.level() == this.knight.level() && isActiveQueen(player) ? player : null;
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        private boolean isNearbyQueen(@Nullable Player player) {
            return isActiveQueen(player) && this.knight.distanceToSqr(player) <= DYNAMIC_QUEEN_RANGE_SQUARED;
        }
    }

    private EnderQueens() {
    }
}
