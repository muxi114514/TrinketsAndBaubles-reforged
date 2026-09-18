package xzeroair.trinkets.traits.abilities.interfaces;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import xzeroair.trinkets.util.compat.firstaid.FirstAidDamage;

/**
 * First Aid 分部位伤害结算后的钩子（对应 1.12 util/compat/firstaid/IFirstAidAbility）。
 *
 * 移植说明：1.12 签名直接带 First Aid 的伤害模型类，靠 @Optional.Interface 在未装时剥离；
 * 1.20.1 无此机制，改为本模组自己的 {@link FirstAidDamage} 抽象，由联动接缝包装真实模型后传入。
 */
public interface IFirstAidAbility extends IAbilityInterface {

    /** @return true 取消本次伤害 */
    boolean firstAidHit(Player player, DamageSource source, float undistributedDamage, FirstAidDamage damage);
}
