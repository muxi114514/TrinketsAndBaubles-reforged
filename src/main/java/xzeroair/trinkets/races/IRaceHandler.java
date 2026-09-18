package xzeroair.trinkets.races;

import java.util.List;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.ArrowNockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;

/**
 * 种族行为契约：事件层把玩家身上发生的事转交给当前种族处理。
 *
 * 移植说明：
 * - 1.12 的 HarvestDropsEvent 在 1.20.1 已被 Forge 移除（掉落改由战利品表决定），
 *   故 blockDrops 改为直接收掉落列表，由事件层在合适时机调用。
 * - getRaceRenderer 是仅客户端的渲染入口，已从本接口移出，随 P6 客户端层单独定义，
 *   以免服务端加载到渲染类型。
 */
public interface IRaceHandler {

    /**
     * 从持久化的种族数据重建处理器时注册能力。
     * 此钩子只应定义能力，不得执行一次性的变身效果。
     */
    default void registerRaceAbilities() {
    }

    default void startTransformation() {
    }

    default void endTransformation() {
    }

    default void whileTranforming() {
    }

    default void whileTransformed() {
    }

    default void jump() {
    }

    default void fall(LivingFallEvent event) {
    }

    default boolean potionBeingApplied(MobEffectInstance effect) {
        return false;
    }

    default void breakingBlock(PlayerEvent.BreakSpeed event) {
    }

    default void blockBroken(BlockEvent.BreakEvent event) {
    }

    default void blockDrops(LivingEntity entity, List<ItemStack> drops) {
    }

    default void bowNocked(ArrowNockEvent event) {
    }

    default void bowDrawing(ItemStack stack, int charge) {
    }

    /** 本族实体吃完/喝完一个物品（如法埃利斯喝奶） */
    default void itemUseFinished(ItemStack stack) {
    }

    default void bowUsed(ArrowLooseEvent event) {
    }

    default void interact(PlayerInteractEvent event) {
    }

    default void interactWithEntity(PlayerInteractEvent.EntityInteract event) {
    }

    default boolean dismountedEntity(Entity mount) {
        return true;
    }

    default boolean mountEntity(Entity mount) {
        return true;
    }

    /** 被敌对生物选为目标时；返回 false 取消这次换目标 */
    default boolean targetedByEnemy(LivingEntity enemy) {
        return true;
    }

    default boolean isAttacked(DamageSource source, float dmg) {
        return true;
    }

    /** 护甲与附魔生效前 */
    default float isHurt(DamageSource source, float dmg) {
        return dmg;
    }

    /** 护甲与附魔生效后 */
    default float isDamaged(DamageSource source, float dmg) {
        return dmg;
    }

    default boolean attackedEntity(LivingEntity target, DamageSource source, float dmg) {
        return true;
    }

    default float hurtEntity(LivingEntity target, DamageSource source, float dmg) {
        return dmg;
    }

    default float damagedEntity(LivingEntity target, DamageSource source, float dmg) {
        return dmg;
    }

    default float onHeal(float healAmount) {
        return healAmount;
    }
}
