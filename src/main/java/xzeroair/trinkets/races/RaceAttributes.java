package xzeroair.trinkets.races;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;

import xzeroair.trinkets.attributes.ConfigAttributes;

/**
 * 种族属性修饰的施加与撤除。修饰器 UUID 取种族 UUID，名称为「种族名.属性名」，一律存盘（重进游戏不会先掉血再补回）。
 * 对应 1.12 EntityRacePropertiesHandler 的 addAttributes / removeOldAttributes。
 */
public final class RaceAttributes {

    /** 按比例施加一个种族的全部属性条目（比例为 0 时相当于撤除） */
    public static void apply(@Nullable LivingEntity entity, EntityRace race, List<? extends String> entries, double scale) {
        if (race.isNone()) {
            return;
        }
        ConfigAttributes.apply(entity, race.getName(), race.getUUID(), entries, scale, true, entry -> true);
    }

    /** 撤除所有属性上指定 UUID 的修饰器（种族切换时清理旧种族） */
    public static void removeAll(@Nullable LivingEntity entity, UUID... uuids) {
        ConfigAttributes.removeAll(entity, uuids);
    }

    private RaceAttributes() {
    }
}
