package xzeroair.trinkets.items.base;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 饰品栈上的持久数据：主/副能力开关、外观变种、存储经验与魔力、制作者。
 *
 * 移植说明：1.12 存在物品能力 TrinketProperties 的 NBT 里，靠自定义包同步到客户端；
 * 1.20.1 直接写进物品栈 NBT 的 xat 子标签，由原版容器同步与 Curios 的槽位同步自动下发，无需自定义包。
 * 键名沿用 1.12（main.ability 等）。
 */
public final class TrinketData {

    private static final String ROOT = "xat";
    private static final String MAIN = "main.ability";
    private static final String ALT = "alt.ability";
    private static final String VARIANT = "variant";
    private static final String EXP = "exp";
    private static final String MANA = "mana";
    private static final String CRAFTER_NAME = "crafter.name";
    private static final String CRAFTER_UUID = "crafter.uuid";

    public static boolean isMainAbility(ItemStack stack) {
        final CompoundTag tag = read(stack);
        return tag != null && tag.getBoolean(MAIN);
    }

    public static void setMainAbility(ItemStack stack, boolean enabled) {
        if (isMainAbility(stack) != enabled) {
            write(stack).putBoolean(MAIN, enabled);
        }
    }

    public static boolean isAltAbility(ItemStack stack) {
        final CompoundTag tag = read(stack);
        return tag != null && tag.getBoolean(ALT);
    }

    public static void setAltAbility(ItemStack stack, boolean enabled) {
        if (isAltAbility(stack) != enabled) {
            write(stack).putBoolean(ALT, enabled);
        }
    }

    public static int getVariant(ItemStack stack) {
        final CompoundTag tag = read(stack);
        return tag == null ? 0 : tag.getInt(VARIANT);
    }

    public static void setVariant(ItemStack stack, int variant) {
        if (getVariant(stack) != variant) {
            write(stack).putInt(VARIANT, variant);
        }
    }

    public static int getStoredExp(ItemStack stack) {
        final CompoundTag tag = read(stack);
        return tag == null ? 0 : tag.getInt(EXP);
    }

    public static void setStoredExp(ItemStack stack, int exp) {
        if (getStoredExp(stack) != exp) {
            write(stack).putInt(EXP, exp);
        }
    }

    public static float getStoredMana(ItemStack stack) {
        final CompoundTag tag = read(stack);
        return tag == null ? 0 : tag.getFloat(MANA);
    }

    public static void setStoredMana(ItemStack stack, float mana) {
        if (getStoredMana(stack) != mana) {
            write(stack).putFloat(MANA, mana);
        }
    }

    public static String getCrafter(ItemStack stack) {
        final CompoundTag tag = read(stack);
        return tag == null ? "" : tag.getString(CRAFTER_NAME);
    }

    public static String getCrafterUUID(ItemStack stack) {
        final CompoundTag tag = read(stack);
        return tag == null ? "" : tag.getString(CRAFTER_UUID);
    }

    /** 制作时记录制作者（对应 1.12 TrinketProperties#onCrafted） */
    public static void setCrafter(ItemStack stack, @Nullable Player player) {
        if (player == null) {
            return;
        }
        final CompoundTag tag = write(stack);
        tag.putString(CRAFTER_NAME, player.getGameProfile().getName());
        tag.putString(CRAFTER_UUID, player.getStringUUID());
    }

    @Nullable
    private static CompoundTag read(ItemStack stack) {
        final CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(ROOT, CompoundTag.TAG_COMPOUND) ? tag.getCompound(ROOT) : null;
    }

    private static CompoundTag write(ItemStack stack) {
        return stack.getOrCreateTagElement(ROOT);
    }

    private TrinketData() {
    }
}
