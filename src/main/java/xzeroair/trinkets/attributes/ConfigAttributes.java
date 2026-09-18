package xzeroair.trinkets.attributes;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 把「属性配置条目」施加到实体上（种族、饰品共用）。修饰器 UUID 由来源决定，名称为「前缀.属性名」。
 * 对应 1.12 EntityRacePropertiesHandler#addAttributes、AccessoryBase#initAttributes 与 AttributeHelper#removeAttributesByUUID。
 */
public final class ConfigAttributes {

    /**
     * 按比例施加全部条目。
     *
     * @param alwaysSaved 为 true 时无视条目自身的存盘标记一律存盘
     * @param active      返回 false 的条目本 tick 改为撤除（如与深海探索者冲突的游泳速度）
     */
    public static void apply(@Nullable LivingEntity entity, String prefix, UUID uuid, List<? extends String> entries,
            double scale, boolean alwaysSaved, Predicate<AttributeEntry> active) {
        if (entity == null || entries.isEmpty()) {
            return;
        }
        for (AttributeEntry entry : AttributeConfigParser.parseAll(entries)) {
            final boolean enabled = active.test(entry);
            for (Attribute attribute : entry.attributes()) {
                final UpdatingAttribute modifier = new UpdatingAttribute(prefix + "." + entry.name(), uuid, () -> attribute)
                        .setSavedInNBT(alwaysSaved || entry.saved());
                if (enabled) {
                    modifier.addModifier(entity, entry.amount() * scale, entry.operation());
                } else {
                    modifier.removeModifier(entity);
                }
            }
        }
    }

    /** 撤除所有属性上指定 UUID 的修饰器 */
    public static void removeAll(@Nullable LivingEntity entity, UUID... uuids) {
        if (entity == null) {
            return;
        }
        for (Attribute attribute : ForgeRegistries.ATTRIBUTES.getValues()) {
            final AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            for (UUID uuid : uuids) {
                if (instance.getModifier(uuid) != null) {
                    new UpdatingAttribute("", uuid, () -> attribute).removeModifier(entity);
                }
            }
        }
    }

    private ConfigAttributes() {
    }
}
