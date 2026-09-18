package xzeroair.trinkets.traits.abilities.other;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;

import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.traits.abilities.AbilityNames;
import xzeroair.trinkets.traits.abilities.interfaces.IPickupExpAbility;

/**
 * 升级增益：1.12 中是未完成的占位能力（方法体全空），仅供经验装置物品挂载注册名。忠实保留为空实现。
 */
public class AbilityLevelingBuff extends Ability implements IPickupExpAbility {

    public AbilityLevelingBuff() {
        super(AbilityNames.LEVELING);
    }

    @Override
    public void onPickup(Player player, ExperienceOrb orb) {
    }
}
