package xzeroair.trinkets.traits.abilities.other;

import javax.annotation.Nonnull;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.IAttackAbility;
import xzeroair.trinkets.traits.abilities.interfaces.IFirstAidAbility;
import xzeroair.trinkets.util.compat.ModCompat;
import xzeroair.trinkets.util.compat.firstaid.FirstAidDamage;
import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 生命祝福：玩家受到致命伤害时有 1% 概率回满血并抵消这次伤害。
 *
 * 装有 First Aid 时另走分部位结算：头部或躯干被打空时同样按概率回满全部部位（{@link #firstAidHit}）。
 * 移植说明：1.12 为此单独派生 AbilityBlessingFirstAid 并按是否装有 First Aid 二选一；First Aid 钩子已与 First Aid 类解耦，合并为本类。
 */
public class AbilityBlessing extends Ability implements IAttackAbility, IFirstAidAbility {

    protected static final int CHANCE = 100;

    public AbilityBlessing() {
        super(AbilityNames.BLESSING_OF_LIFE);
    }

    @Override
    public float damaged(@Nonnull LivingEntity attacked, DamageSource source, float dmg) {
        if (!TrinketsConfig.SERVER.misc.disabledBlessings.get().isEmpty()) {
            return dmg;
        }
        if (attacked.getHealth() - dmg <= 0F && this.random.nextInt(CHANCE) == 0 && this.sendMessageToPlayer(attacked)) {
            attacked.heal(attacked.getMaxHealth());
            attacked.level().playSound(null, attacked.getX(), attacked.getY(), attacked.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.3F, 1F);
            return 0F;
        }
        return dmg;
    }

    @Override
    public boolean firstAidHit(Player player, DamageSource source, float undistributedDamage, FirstAidDamage damage) {
        if (!TrinketsConfig.SERVER.misc.disabledBlessings.get().isEmpty()) {
            return false;
        }
        if ((damage.headHealth() >= 1.0F && damage.bodyHealth() >= 1.0F) || this.random.nextInt(CHANCE) != 0 || !this.sendMessageToPlayer(player)) {
            return false;
        }
        damage.healAllParts();
        player.setHealth(player.getMaxHealth());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.3F, 1F);
        damage.scheduleResync();
        return true;
    }

    @Override
    public boolean sendMessageToPlayer(Entity entity) {
        return entity instanceof Player && !entity.level().isClientSide;
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final boolean active = TrinketsConfig.SERVER.misc.disabledBlessings.get().isEmpty();
        variables.oneIn("chance", active, CHANCE)
                .flag("firstaid", active && ModCompat.firstAid())
                .flag("off", !active);
    }
}
