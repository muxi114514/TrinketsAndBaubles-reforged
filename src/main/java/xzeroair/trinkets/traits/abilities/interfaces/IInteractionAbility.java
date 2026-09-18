package xzeroair.trinkets.traits.abilities.interfaces;

import org.apache.commons.lang3.tuple.ImmutablePair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event.Result;

/**
 * 交互相关。
 * 移植说明：1.12 EnumHand/EnumFacing/Vec3d -> 1.20.1 InteractionHand/Direction/Vec3。
 */
public interface IInteractionAbility extends IAbilityInterface {

    default void interact(LivingEntity entityLiving, Level level, ItemStack itemStack,
            InteractionHand hand, Direction face, BlockPos pos) {
    }

    default void interactEntity(LivingEntity entityLiving, Level level, ItemStack itemStack,
            InteractionHand hand, Direction face, BlockPos pos, Entity target) {
    }

    default void interactEntitySpecific(LivingEntity entityLiving, Level level, ItemStack itemStack,
            InteractionHand hand, Direction face, BlockPos pos, Entity target, Vec3 localPos) {
    }

    default void rightClickWithItem(LivingEntity entityLiving, Level level, ItemStack itemStack,
            InteractionHand hand, Direction face, BlockPos pos) {
    }

    default ImmutablePair<Result, Result> rightClickBlock(LivingEntity entityLiving, Level level, ItemStack itemStack,
            InteractionHand hand, Direction face, BlockPos pos, Vec3 hitVec, Result useBlock, Result useItem) {
        return null;
    }

    default ImmutablePair<Result, Result> leftClickBlock(LivingEntity entityLiving, Level level, ItemStack itemStack,
            InteractionHand hand, Direction face, BlockPos pos, Vec3 hitVec, Result useBlock, Result useItem) {
        return null;
    }
}
