package xzeroair.trinkets.util.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.Trinkets;

/**
 * 装备重量表：`物品id[;槽位];数值` 或 `材质名[;槽位];数值`，物品 id 可用 * 通配。
 * 槽位为 head/chest/legs/feet/mainhand/offhand/hand，省略表示任意槽位。
 * 对应 1.12 ConfigHelper.ConfigEquipmentObject 在法埃利斯重甲惩罚、徒手格斗名单中的用法。
 *
 * 移植说明：
 * - 1.12 的 metadata 段（如 ;*;）已无意义，解析时忽略非数值、非槽位的中间段，兼容旧写法。
 * - 1.12 的材质名取 ArmorMaterial/ToolMaterial 的枚举名；1.20.1 取护甲材质名（iron/gold/chainmail/diamond…）
 *   与工具等级注册名的路径（wood/stone/iron/diamond/netherite…）。
 * - 同一物品优先匹配物品条目，其次才是材质条目（与 1.12 查找顺序一致）。
 */
public final class EquipmentWeights {

    private record Entry(@Nullable Pattern item, @Nullable String material, @Nullable String slot, double weight) {
    }

    private static final Map<List<? extends String>, List<Entry>> CACHE = new ConcurrentHashMap<>();

    /**
     * @param slot 物品所在槽位；手持物品传 MAINHAND/OFFHAND，其余位置传 null
     * @return 命中条目的数值；未命中返回空
     */
    public static OptionalDouble weightOf(List<? extends String> table, ItemStack stack, @Nullable EquipmentSlot slot) {
        if (stack.isEmpty() || table.isEmpty()) {
            return OptionalDouble.empty();
        }
        final List<Entry> entries = CACHE.computeIfAbsent(table, EquipmentWeights::parse);
        final ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        final String name = id == null ? "" : id.toString();
        final String slotName = slotName(slot);
        for (Entry entry : entries) {
            if (entry.item() != null && entry.item().matcher(name).matches() && slotMatches(entry, slotName)) {
                return OptionalDouble.of(entry.weight());
            }
        }
        final String material = materialOf(stack);
        if (material == null) {
            return OptionalDouble.empty();
        }
        for (Entry entry : entries) {
            if (material.equals(entry.material()) && slotMatches(entry, slotName)) {
                return OptionalDouble.of(entry.weight());
            }
        }
        return OptionalDouble.empty();
    }

    /** 配置重载后丢弃旧列表的解析结果 */
    public static void clearCache() {
        CACHE.clear();
    }

    private static boolean slotMatches(Entry entry, @Nullable String slotName) {
        if (entry.slot() == null) {
            return true;
        }
        if (slotName == null) {
            return false;
        }
        return entry.slot().equals(slotName)
                || ("hand".equals(entry.slot()) && ("mainhand".equals(slotName) || "offhand".equals(slotName)));
    }

    @Nullable
    private static String slotName(@Nullable EquipmentSlot slot) {
        return slot == null ? null : slot.getName();
    }

    @Nullable
    private static String materialOf(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem armor) {
            final String name = armor.getMaterial().getName();
            return name.substring(name.indexOf(':') + 1).toLowerCase(Locale.ROOT);
        }
        if (stack.getItem() instanceof TieredItem tiered) {
            final ResourceLocation tier = TierSortingRegistry.getName(tiered.getTier());
            return tier == null ? null : tier.getPath().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private static List<Entry> parse(List<? extends String> table) {
        final List<Entry> entries = new ArrayList<>();
        for (String raw : table) {
            final String[] parts = raw.trim().split(";");
            if (parts.length < 2) {
                Trinkets.LOGGER.warn("Ignoring equipment weight entry without a value: {}", raw);
                continue;
            }
            final double weight;
            try {
                weight = Double.parseDouble(parts[parts.length - 1].trim());
            } catch (NumberFormatException e) {
                Trinkets.LOGGER.warn("Ignoring equipment weight entry with an invalid value: {}", raw);
                continue;
            }
            String slot = null;
            for (int i = 1; i < parts.length - 1; i++) {
                final String token = parts[i].trim().toLowerCase(Locale.ROOT);
                if (isSlot(token)) {
                    slot = token;
                }
            }
            final String key = parts[0].trim();
            if (key.contains(":")) {
                entries.add(new Entry(glob(key.toLowerCase(Locale.ROOT)), null, slot, weight));
            } else {
                entries.add(new Entry(null, key.toLowerCase(Locale.ROOT), slot, weight));
            }
        }
        return List.copyOf(entries);
    }

    private static boolean isSlot(String token) {
        return switch (token) {
            case "head", "chest", "legs", "feet", "mainhand", "offhand", "hand" -> true;
            default -> false;
        };
    }

    private static Pattern glob(String glob) {
        final StringBuilder regex = new StringBuilder();
        for (String part : glob.split("\\*", -1)) {
            if (!regex.isEmpty()) {
                regex.append(".*");
            }
            regex.append(Pattern.quote(part));
        }
        return Pattern.compile(regex.toString());
    }

    private EquipmentWeights() {
    }
}
