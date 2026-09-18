package xzeroair.trinkets.items.trinkets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.items.base.AccessoryBase;
import xzeroair.trinkets.items.base.TrinketData;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.util.config.server.items.AccessoryConfig;

/**
 * 装饰品：佩戴时显示某个种族的外观特征（翅膀、耳朵、角等，渲染随 P6 补入），右键切换下一种、潜行右键切换上一种。
 * 同一种特征只能戴一件。对应 1.12 TrinketCosmetic。
 *
 * 移植说明：1.12 用物品 metadata 存特征序号；1.20.1 没有 metadata，改存 TrinketData 的 variant。
 * 1.12 本物品未设置属性 UUID（无属性），此处给一个固定值满足基类约束。
 */
public class TrinketCosmetic extends AccessoryBase {

    public TrinketCosmetic() {
        super("0b6e7f5a-3c2d-4e1f-9a8b-7c6d5e4f3a2b");
    }

    @Nullable
    @Override
    public AccessoryConfig<?> getAccessoryConfig() {
        return null;
    }

    @Override
    public Element getPrimaryElement() {
        return ModElements.NEUTRAL.get();
    }

    public static CosmeticFeature getFeature(ItemStack stack) {
        return CosmeticFeature.byId(TrinketData.getVariant(stack));
    }

    /** 右键循环切换特征（覆盖基类的能力开关切换） */
    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, Player player, @Nonnull InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            final boolean previous = player.isShiftKeyDown();
            final int count = CosmeticFeature.values().length;
            final int next = Math.floorMod(TrinketData.getVariant(stack) + (previous ? -1 : 1), count);
            TrinketData.setVariant(stack, next);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS,
                    0.3F, previous ? 0.6F : 0.3F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** 同一种特征只能戴一件，自身所在槽位除外 */
    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        final int variant = TrinketData.getVariant(stack);
        return CuriosApi.getCuriosInventory(slotContext.entity()).resolve()
                .map(inventory -> inventory.findCurios(worn -> worn.is(this) && TrinketData.getVariant(worn) == variant).stream()
                        .allMatch(result -> result.slotContext().identifier().equals(slotContext.identifier())
                                && result.slotContext().index() == slotContext.index()))
                .orElse(true);
    }

    @Nonnull
    @Override
    public String getDescriptionId(@Nonnull ItemStack stack) {
        return this.getDescriptionId() + "." + getFeature(stack).ordinal();
    }

    /** 可显示的特征，序号与 1.12 RenderCosmeticFeature 的 metadata 一致 */
    public enum CosmeticFeature {
        HUMAN, FAIRY, DWARF, ELF, GOBLIN, FAELIS, TITAN, DRAGON, TAURUS, TAURUS_BELL, TAURUS_F, TAURUS_F_BELL, TAURIAN_BELL,
        SUCCUBUS, GENERIC_HORNS, GENERIC_HORNS_INVERTED, DRAGON_HORNS, FAELIS_TAIL;

        public static CosmeticFeature byId(int id) {
            final CosmeticFeature[] values = values();
            return id >= 0 && id < values.length ? values[id] : HUMAN;
        }
    }
}
