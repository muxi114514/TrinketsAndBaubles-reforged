package xzeroair.trinkets.events;

import javax.annotation.Nullable;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.races.EntityRacePropertiesHandler;
import xzeroair.trinkets.traits.AbilityDispatcher;
import xzeroair.trinkets.traits.abilities.interfaces.IAttackAbility;
import xzeroair.trinkets.traits.abilities.interfaces.IHealAbility;
import xzeroair.trinkets.traits.abilities.interfaces.ILightningStrikeAbility;

/**
 * 战斗事件 → 种族钩子 + 能力（Forge 事件总线）。对应 1.12 events/CombatHandler。
 *
 * 顺序约定（与 1.12 一致）：每个事件先处理攻击方、再处理受击方；
 * 每一方先问种族处理器、再依次问能力。
 *
 * 移植说明：
 * - 1.12 的 source.canHarmInCreative() → 1.20.1 的 DamageTypeTags.BYPASSES_INVULNERABILITY。
 * - LivingSetAttackTargetEvent 在 1.20.1 更名为 LivingChangeTargetEvent。
 */
public class CombatHandler {

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        final DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        final float damage = event.getAmount();
        final LivingEntity attacked = event.getEntity();

        final LivingEntity attacker = attackerOf(source);
        if (attacker != null) {
            final EntityRacePropertiesHandler race = raceOf(attacker);
            boolean cancel = race != null && !race.attackedEntity(attacked, source, damage);
            if (!cancel) {
                cancel = AbilityDispatcher.chainCancel(attacker, IAttackAbility.class, false,
                        (ability, c) -> ability.attackEntity(attacked, source, damage, c));
            }
            if (cancel) {
                event.setCanceled(true);
                return;
            }
        }

        final EntityRacePropertiesHandler race = raceOf(attacked);
        boolean cancel = race != null && !race.isAttacked(source, damage);
        if (!cancel) {
            cancel = AbilityDispatcher.chainCancel(attacked, IAttackAbility.class, false,
                    (ability, c) -> ability.attacked(attacked, source, damage, c));
        }
        if (cancel) {
            event.setCanceled(true);
        }
    }

    /** 护甲与附魔结算前 */
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        final DamageSource source = event.getSource();
        float damage = event.getAmount();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || damage == 0) {
            return;
        }
        final LivingEntity attacked = event.getEntity();

        final LivingEntity attacker = attackerOf(source);
        if (attacker != null) {
            final EntityRacePropertiesHandler race = raceOf(attacker);
            if (race != null) {
                damage = race.hurtEntity(attacked, source, damage);
            }
            if (damage > 0) {
                damage = AbilityDispatcher.chainFloat(attacker, IAttackAbility.class, damage,
                        (ability, d) -> ability.hurtEntity(attacked, source, d));
            }
        }

        if (damage > 0) {
            final EntityRacePropertiesHandler race = raceOf(attacked);
            if (race != null) {
                damage = race.isHurt(source, damage);
            }
            if (damage > 0) {
                damage = AbilityDispatcher.chainFloat(attacked, IAttackAbility.class, damage,
                        (ability, d) -> ability.hurt(attacked, source, d));
            }
        }
        applyDamage(event, damage);
    }

    /** 护甲与附魔结算后 */
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        final DamageSource source = event.getSource();
        float damage = event.getAmount();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || damage == 0) {
            return;
        }
        final LivingEntity attacked = event.getEntity();

        final LivingEntity attacker = attackerOf(source);
        if (attacker != null) {
            final EntityRacePropertiesHandler race = raceOf(attacker);
            if (race != null) {
                damage = race.damagedEntity(attacked, source, damage);
            }
            if (damage > 0) {
                damage = AbilityDispatcher.chainFloat(attacker, IAttackAbility.class, damage,
                        (ability, d) -> ability.damageEntity(attacked, source, d));
            }
        }

        if (damage > 0) {
            final EntityRacePropertiesHandler race = raceOf(attacked);
            if (race != null) {
                damage = race.isDamaged(source, damage);
            }
            if (damage > 0) {
                damage = AbilityDispatcher.chainFloat(attacked, IAttackAbility.class, damage,
                        (ability, d) -> ability.damaged(attacked, source, d));
            }
        }
        if (damage <= 0) {
            event.setCanceled(true);
        } else {
            event.setAmount(damage);
        }
    }

    @SubscribeEvent
    public void onLivingHeal(LivingHealEvent event) {
        final LivingEntity entity = event.getEntity();
        float amount = event.getAmount();
        final EntityRacePropertiesHandler race = raceOf(entity);
        if (race != null) {
            amount = race.onHeal(amount);
        }
        final float healed = AbilityDispatcher.chainFloat(entity, IHealAbility.class, amount,
                (ability, a) -> ability.onHeal(entity, a));
        if (healed <= 0) {
            event.setCanceled(true);
        } else {
            event.setAmount(healed);
        }
    }

    /** 每个能力都要有机会反应（比如雷电免疫给火焰抗性），故不中断 */
    @SubscribeEvent
    public void onStruckByLightning(EntityStruckByLightningEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        final boolean cancel = AbilityDispatcher.foldCancel(living, ILightningStrikeAbility.class, false,
                (ability, c) -> ability.onStruckByLightning(living, c));
        if (cancel) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onChangeTarget(LivingChangeTargetEvent event) {
        final LivingEntity target = event.getNewTarget();
        if (target == null) {
            return;
        }
        final LivingEntity enemy = event.getEntity();
        final EntityRacePropertiesHandler race = raceOf(target);
        boolean cancel = race != null && !race.targetedByEnemy(enemy);
        if (!cancel) {
            cancel = AbilityDispatcher.chainCancel(target, IAttackAbility.class, false,
                    (ability, c) -> ability.targetedByEnemy(enemy, c));
        }
        if (cancel) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        final DamageSource source = event.getSource();
        final LivingEntity dead = event.getEntity();
        final boolean cancel = AbilityDispatcher.chainCancel(dead, IAttackAbility.class, false,
                (ability, c) -> ability.died(dead, source, c));
        if (cancel) {
            event.setCanceled(true);
            return;
        }
        final LivingEntity killer = attackerOf(source);
        if (killer != null) {
            AbilityDispatcher.forEach(killer, IAttackAbility.class, ability -> ability.killedEntity(dead, source));
        }
    }

    @SubscribeEvent
    public void onExperienceDrop(LivingExperienceDropEvent event) {
        final LivingEntity killer = event.getAttackingPlayer();
        if (killer == null) {
            return;
        }
        final LivingEntity dead = event.getEntity();
        final int original = event.getOriginalExperience();
        final int dropped = AbilityDispatcher.chainInt(killer, IAttackAbility.class, event.getDroppedExperience(),
                (ability, exp) -> ability.killedEntityExpDrop(dead, original, exp));
        event.setDroppedExperience(dropped);
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        final LivingEntity killer = attackerOf(event.getSource());
        if (killer == null) {
            return;
        }
        final LivingEntity dead = event.getEntity();
        AbilityDispatcher.forEach(killer, IAttackAbility.class,
                ability -> ability.killedEntityItemDrops(dead, event.getSource(), event.getLootingLevel(), event.getDrops()));
    }

    // ── 工具 ──

    private static void applyDamage(LivingHurtEvent event, float damage) {
        if (damage <= 0) {
            event.setCanceled(true);
        } else {
            event.setAmount(damage);
        }
    }

    @Nullable
    private static LivingEntity attackerOf(DamageSource source) {
        return source.getEntity() instanceof LivingEntity living ? living : null;
    }

    @Nullable
    private static EntityRacePropertiesHandler raceOf(LivingEntity entity) {
        final EntityProperties properties = EntityProperties.get(entity);
        return properties != null ? properties.getRaceHandler() : null;
    }
}
