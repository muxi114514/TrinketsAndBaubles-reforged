package xzeroair.trinkets.traits.abilities.compat;

import javax.annotation.Nonnull;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.abilities.interfaces.IFirstAidAbility;
import xzeroair.trinkets.util.compat.ModCompat;
import xzeroair.trinkets.util.compat.firstaid.FirstAidDamage;
import xzeroair.trinkets.util.config.abilities.HardHeadAbilityConfig;
import xzeroair.trinkets.util.helpers.NumberText;

/**
 * 硬头（荣耀之盾，First Aid 联动）：头部血量被打到 0 而躯干仍在时，1/chance 概率保住头部血量并取消这次伤害。
 * 对应 1.12 AbilityHardHead。
 *
 * 移植说明：1.12 的 VIP 专属台词随 VIP 系统不移植，固定显示 "Ouch!"。
 */
public class AbilityHardHead extends Ability implements IFirstAidAbility {

    private final HardHeadAbilityConfig config;

    public AbilityHardHead(@Nonnull HardHeadAbilityConfig config) {
        super(AbilityNames.HARD_HEAD);
        this.config = config;
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public boolean firstAidHit(Player player, DamageSource source, float undistributedDamage, FirstAidDamage damage) {
        final int chance = this.config.chance.get();
        if (damage.headHealth() >= 1.0F || damage.bodyHealth() <= 0.0F || (chance > 0 && this.random.nextInt(chance) != 0)) {
            return false;
        }
        player.displayClientMessage(Component.literal("Ouch!").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD), true);
        damage.restoreHead();
        damage.scheduleResync();
        return true;
    }

    @Override
    public void describe(DescriptionVariables variables) {
        final int chance = this.config.chance.get();
        variables.option("chance", true, chance < 1 ? NumberText.percent(1) : NumberText.oneIn(chance));
    }

    @Override
    public String getCompatModId() {
        return ModCompat.FIRST_AID;
    }
}
