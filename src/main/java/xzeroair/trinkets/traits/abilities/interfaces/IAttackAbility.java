package xzeroair.trinkets.traits.abilities.interfaces;

import java.util.Collection;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;

import xzeroair.trinkets.util.helpers.DamageHelper;

/**
 * 战斗相关：受击侧与攻击侧各三段（取消 / 护甲前 / 护甲后）。
 * 移植说明：1.12 靠 EntityDamageSourceIndirect 与 source.isExplosion/isMagicDamage 判断间接伤害；
 * 1.20.1 伤害类型是数据驱动的，改用 DamageTypeTags 与「直接来源与来源不同」来判定。
 */
public interface IAttackAbility extends IAbilityInterface {

    /** 被敌对生物选为目标时；返回 true 取消这次换目标 */
    default boolean targetedByEnemy(LivingEntity enemy, boolean cancel) {
        return cancel;
    }

    /** 返回 false 可完全取消这次攻击 */
    default boolean attacked(LivingEntity attacked, DamageSource source, float dmg, boolean cancel) {
        return cancel;
    }

    /** 护甲与附魔尚未生效；返回 0 即取消 */
    default float hurt(LivingEntity attacked, DamageSource source, float dmg) {
        return dmg;
    }

    /** 护甲与附魔已生效；返回 0 即取消 */
    default float damaged(LivingEntity attacked, DamageSource source, float dmg) {
        return dmg;
    }

    default boolean died(LivingEntity attacked, DamageSource source, boolean cancel) {
        return cancel;
    }

    default boolean attackEntity(LivingEntity target, DamageSource source, float dmg, boolean cancel) {
        return cancel;
    }

    /** 攻击侧，附魔尚未生效 */
    default float hurtEntity(LivingEntity target, DamageSource source, float dmg) {
        return dmg;
    }

    /** 攻击侧，护甲与附魔已生效 */
    default float damageEntity(LivingEntity target, DamageSource source, float dmg) {
        return dmg;
    }

    default void killedEntity(LivingEntity target, DamageSource source) {
    }

    default int killedEntityExpDrop(LivingEntity target, int originalExp, int droppedExp) {
        return droppedExp;
    }

    /** drops 即事件的原始集合，增删会直接生效（1.20.1 的 LivingDropsEvent.getDrops() 为 Collection） */
    default void killedEntityItemDrops(LivingEntity target, DamageSource source, int lootingLevel, Collection<ItemEntity> drops) {
    }

    default boolean isIndirectDamage(DamageSource source) {
        return DamageHelper.isIndirect(source);
    }
}
