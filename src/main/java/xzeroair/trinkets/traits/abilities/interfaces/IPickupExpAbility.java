package xzeroair.trinkets.traits.abilities.interfaces;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;

/** 拾取经验球时介入。 */
public interface IPickupExpAbility extends IAbilityInterface {

    void onPickup(Player player, ExperienceOrb orb);
}
