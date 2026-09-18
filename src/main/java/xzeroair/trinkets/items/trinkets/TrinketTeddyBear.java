package xzeroair.trinkets.items.trinkets;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import xzeroair.trinkets.blocks.TeddyBearType;
import xzeroair.trinkets.blocks.entity.TeddyBearBlockEntity;
import xzeroair.trinkets.init.ModBlocks;
import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.items.base.TrinketData;
import xzeroair.trinkets.traits.abilities.AbilityWellRested;
import xzeroair.trinkets.traits.abilities.interfaces.IAbilityInterface;
import xzeroair.trinkets.traits.abilities.other.AbilityBlessing;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.server.items.AccessoryAbilitiesConfigs.TeddyBear;

/**
 * 泰迪熊：佩戴获得安睡；对方块右键可放下（保留物品本身）。名称带制作者；按名称/制作者显示彩蛋外观。
 * 「Stephanie's Snowie」额外带生命祝福（隐藏彩蛋，misc.disabledBlessings 非空时关闭）。对应 1.12 TrinketTeddyBear。
 */
public class TrinketTeddyBear extends SimpleAccessory<TeddyBear> {

    public TrinketTeddyBear() {
        super("33b34669-715d-4caa-a31e-9c643c52ba66", ModElements.LIGHT, () -> TrinketsConfig.SERVER.items.teddyBear,
                (config, abilities) -> abilities.add(new AbilityWellRested(config.wellRested)));
    }

    @Override
    public void initAbilities(ItemStack stack, List<IAbilityInterface> list) {
        super.initAbilities(stack, list);
        if (TeddyBearType.of(stack) == TeddyBearType.SNOWIE && TrinketsConfig.SERVER.misc.disabledBlessings.get().isEmpty()) {
            list.add(new AbilityBlessing());
        }
    }

    /** 放下泰迪熊（对应 1.12 onItemUse + placeBlockAt），落点规则同原版方块物品，但不允许点方块底面 */
    @Nonnull
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() == Direction.DOWN) {
            return InteractionResult.FAIL;
        }
        final BlockPlaceContext placeContext = new BlockPlaceContext(context);
        final Level level = context.getLevel();
        final BlockPos pos = placeContext.getClickedPos();
        final Player player = context.getPlayer();
        final ItemStack stack = context.getItemInHand();
        if (!placeContext.canPlace() || (player != null && !player.mayUseItemAt(pos, context.getClickedFace(), stack))) {
            return InteractionResult.FAIL;
        }
        final BlockState state = ModBlocks.TEDDY_BEAR.get().getStateForPlacement(placeContext);
        if (state == null || !state.canSurvive(level, pos)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!level.setBlock(pos, state, 11)) {
            return InteractionResult.FAIL;
        }
        if (level.getBlockEntity(pos) instanceof TeddyBearBlockEntity teddy) {
            teddy.setTeddyBear(stack);
        }
        final SoundType sound = state.getSoundType(level, pos, player);
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, stack);
        }
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    /** 未改名时显示为「制作者的泰迪熊」 */
    @Nonnull
    @Override
    public Component getName(@Nonnull ItemStack stack) {
        final String crafter = TrinketData.getCrafter(stack);
        return crafter.isEmpty() ? super.getName(stack) : Component.translatable("item.xat.teddy_bear.owned", crafter, super.getName(stack));
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        if (TeddyBearType.of(stack) == TeddyBearType.SNOWIE) {
            tooltip.add(Component.translatable("item.xat.teddy_bear.snowie"));
            tooltip.add(Component.empty());
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
