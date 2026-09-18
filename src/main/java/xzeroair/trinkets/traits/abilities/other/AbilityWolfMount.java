package xzeroair.trinkets.traits.abilities.other;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import xzeroair.trinkets.entity.AlphaWolf;
import xzeroair.trinkets.init.ModEntities;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.base.AbilityRaceSpecific;
import xzeroair.trinkets.traits.abilities.interfaces.ITickableAbility;
import xzeroair.trinkets.util.KeyNames;
import xzeroair.trinkets.util.config.abilities.IAbilityConfig;
import xzeroair.trinkets.util.handlers.Counter;
import xzeroair.trinkets.util.helpers.RayTraceHelper;

/**
 * 骑狼（哥布林）：对着自己驯服的成年狼按种族技能键，把它换成可骑乘的狼王并骑上；
 * 骑乘时再按技能键发动冲撞撕咬（2 秒冷却）。对应 1.12 traits/abilities/other/AbilityWolfMount。
 */
public class AbilityWolfMount extends AbilityRaceSpecific implements ITickableAbility {

    private static final String ATTACK_COOLDOWN = "mountAtkCooldown";
    private static final int ATTACK_COOLDOWN_TICKS = 40;
    private static final double DEFAULT_REACH = 5.0D;

    public AbilityWolfMount(@Nonnull IAbilityConfig config) {
        super(AbilityNames.WOLF_RIDER);
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public boolean onKeyPress(Entity entity, boolean aux) {
        if (!(entity instanceof Player player)) {
            return true;
        }
        final double reach = player.getBlockReach() > 0 ? player.getBlockReach() : DEFAULT_REACH;
        if (player.getVehicle() instanceof AlphaWolf wolf) {
            final Counter counter = this.cooldown();
            if (counter.Tick()) {
                if (!player.level().isClientSide) {
                    wolf.mountedAttack(player, reach);
                }
                counter.resetTick();
            }
        } else if (!player.isPassenger()) {
            final HitResult result = RayTraceHelper.rayTrace(player, reach * 0.5D);
            if (result instanceof EntityHitResult hit && hit.getEntity() instanceof Wolf wolf && !(wolf instanceof AlphaWolf)) {
                this.mountWolf(player, wolf);
            }
        }
        return true;
    }

    /** 冷却计时：倒数到 0 才允许下一次攻击 */
    private Counter cooldown() {
        return this.tickHandler.getCounter(ATTACK_COOLDOWN, ATTACK_COOLDOWN_TICKS, true, true, false, true, false);
    }

    public boolean mountWolf(LivingEntity rider, Wolf wolf) {
        final Level level = rider.level();
        if (!wolf.isTame() || !wolf.isOwnedBy(rider) || wolf.isBaby() || !wolf.isAlive() || level.isClientSide || rider.isPassenger()
                || !(rider instanceof Player player)) {
            return false;
        }
        final AlphaWolf alpha = ModEntities.ALPHA_WOLF.get().create(level);
        if (alpha == null) {
            return false;
        }
        alpha.setCustomName(wolf.getCustomName());
        alpha.moveTo(wolf.getX(), wolf.getY(), wolf.getZ(), wolf.getYRot(), 0.0F);
        alpha.tame(player);
        alpha.setHealth(wolf.getHealth());
        if (!level.addFreshEntity(alpha)) {
            return false;
        }
        if (rider.startRiding(alpha)) {
            // 骑上之后才暂存并移除原狼：骑乘失败时狼王被移除也不会凭空还原出第二只狼
            alpha.storeOldWolf(wolf);
            wolf.discard();
            return true;
        }
        alpha.discard();
        return false;
    }

    @Override
    public void tickAbility(LivingEntity entity) {
        final Counter counter = this.cooldown();
        if (counter.getTick() > 0) {
            counter.Tick();
        }
    }

    /** 骑乘增益与 AlphaWolf#shareBuffs 一致 */
    @Override
    public void describe(DescriptionVariables variables) {
        variables.keybind("key", KeyNames.RACE_ABILITY)
                .seconds("cooldown", true, ATTACK_COOLDOWN_TICKS)
                .effects("rider", true, List.of("minecraft:strength", "minecraft:regeneration"), false)
                .effects("wolf", true, List.of("minecraft:regeneration:100:1"), false);
    }
}
