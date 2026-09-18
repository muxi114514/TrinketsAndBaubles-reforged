package xzeroair.trinkets.util.helpers;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * NBT 辅助工具。
 * 移植说明：1.12 的 entity.getEntityData() 在 1.20.1 为 entity.getPersistentData()；
 * 玩家取 PERSISTED_NBT_TAG 子标签，其内容可跨死亡保留。
 * has* 系列是原版的「键存在才消费」写法，保留以让能力读档代码形状一致。
 */
public class NBTHelper {

    public static CompoundTag getEntityTag(@Nonnull Entity entity) {
        return getEntityTag(entity, new CompoundTag());
    }

    public static CompoundTag getEntityTag(@Nonnull Entity entity, CompoundTag fallback) {
        final CompoundTag tag = entity.getPersistentData();
        if (tag == null) {
            return fallback;
        }
        if (entity instanceof Player) {
            if (!tag.contains(Player.PERSISTED_NBT_TAG)) {
                tag.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
            }
            return tag.getCompound(Player.PERSISTED_NBT_TAG);
        }
        return tag;
    }

    public static boolean hasTagCompound(CompoundTag compound, String key) {
        return compound != null && compound.contains(key, Tag.TAG_COMPOUND);
    }

    public static void hasTag(CompoundTag compound, String key, Consumer<CompoundTag> consumer) {
        if (hasTagCompound(compound, key)) {
            consumer.accept(compound.getCompound(key));
        }
    }

    public static void hasBoolean(CompoundTag compound, String key, Consumer<Boolean> consumer) {
        if (compound != null && compound.contains(key)) {
            consumer.accept(compound.getBoolean(key));
        }
    }

    public static void hasInteger(CompoundTag compound, String key, Consumer<Integer> consumer) {
        if (compound != null && compound.contains(key)) {
            consumer.accept(compound.getInt(key));
        }
    }

    public static void hasFloat(CompoundTag compound, String key, Consumer<Float> consumer) {
        if (compound != null && compound.contains(key)) {
            consumer.accept(compound.getFloat(key));
        }
    }

    public static void hasString(CompoundTag compound, String key, Consumer<String> consumer) {
        if (compound != null && compound.contains(key)) {
            consumer.accept(compound.getString(key));
        }
    }

    private NBTHelper() {
    }
}
