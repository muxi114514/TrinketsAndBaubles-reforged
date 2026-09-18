package xzeroair.trinkets.util.helpers;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.capabilities.race.EntityProperties;

/** 实体判定（对应 1.12 TrinketHelper 的 isEntityBoss / entityHasAbility 与各处的注册名名单比对）。 */
public final class EntityChecks {

    /**
     * 是否为 Boss。移植说明：1.12 的 isNonBoss() 在 1.20.1 由 Forge 标签 forge:bosses 表示，凋灵与末影龙原版已在其中。
     */
    public static boolean isBoss(@Nullable Entity entity) {
        if (entity == null || (entity instanceof Player && !(entity instanceof FakePlayer))) {
            return false;
        }
        return entity.getType().is(Tags.EntityTypes.BOSSES) || entity instanceof WitherBoss || entity instanceof EnderDragon;
    }

    /** 实体当前是否拥有指定能力（能力键如 xat:ender_queen） */
    public static boolean hasAbility(@Nullable LivingEntity entity, String abilityKey) {
        final EntityProperties properties = EntityProperties.get(entity);
        return properties != null && properties.getAbilityHandler().getAbility(abilityKey) != null;
    }

    /** 实体注册名是否在名单中；「modid:*」匹配整个模组 */
    public static boolean isListed(Entity entity, List<? extends String> list) {
        if (list.isEmpty()) {
            return false;
        }
        final ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null && (list.contains(id.toString()) || list.contains(id.getNamespace() + ":*"));
    }

    private EntityChecks() {
    }
}
