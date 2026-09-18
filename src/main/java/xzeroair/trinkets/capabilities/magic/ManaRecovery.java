package xzeroair.trinkets.capabilities.magic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.Trinkets;
import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 魔力回复名单：吃下/喝下名单里的物品后回复魔力。对应 1.12 ConfigHelper.MPRecoveryItem 与 TrinketConfigStorage 的回复表。
 *
 * 条目格式：{@code 物品id;数值} 或 {@code #物品标签;数值}；数值带 % 表示按最大魔力比例，否则为固定点数。
 * 数值按 0.05 取整（与 1.12 一致）。为兼容从 1.12 照搬的旧条目，三段式 {@code 物品;meta;数值} 只取首尾两段。
 * 未安装模组的物品静默跳过（默认名单含 botania 物品）。
 *
 * 线程安全：配置可能在文件监听线程上重载，故把解析结果做成不可变快照、以 volatile 发布；
 * 发现配置列表对象换了就整体重建，读取方永远看到一份完整的快照。
 */
public final class ManaRecovery {

    private static final float AMOUNT_STEP = 0.05F;

    /** 一条回复规则 */
    public record Entry(float amount, boolean percent) {

        public float resolve(float maxMana) {
            return this.percent ? maxMana * (this.amount * 0.01F) : this.amount;
        }
    }

    private record Snapshot(List<? extends String> source, Map<Item, Entry> items, List<Map.Entry<TagKey<Item>, Entry>> tags) {
    }

    private static volatile Snapshot snapshot;

    @Nullable
    public static Entry find(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        final Snapshot current = currentSnapshot();
        final Entry exact = current.items().get(stack.getItem());
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<TagKey<Item>, Entry> tagged : current.tags()) {
            if (stack.is(tagged.getKey())) {
                return tagged.getValue();
            }
        }
        return null;
    }

    private static Snapshot currentSnapshot() {
        final List<? extends String> source = TrinketsConfig.SERVER.magic.recovery.get();
        Snapshot current = snapshot;
        if (current == null || current.source() != source) {
            current = build(source);
            snapshot = current;
        }
        return current;
    }

    private static Snapshot build(List<? extends String> source) {
        final Map<Item, Entry> items = new HashMap<>();
        final List<Map.Entry<TagKey<Item>, Entry>> tags = new ArrayList<>();
        for (String raw : source) {
            final String[] parts = raw.trim().split(";");
            if (parts.length < 2) {
                Trinkets.LOGGER.warn("Ignoring mana recovery entry without an amount: {}", raw);
                continue;
            }
            final Entry entry = parseAmount(parts[parts.length - 1].trim());
            if (entry == null) {
                Trinkets.LOGGER.warn("Ignoring mana recovery entry with an invalid amount: {}", raw);
                continue;
            }
            final String id = parts[0].trim();
            try {
                if (id.startsWith("#")) {
                    tags.add(Map.entry(ItemTags.create(new ResourceLocation(id.substring(1))), entry));
                    continue;
                }
                final ResourceLocation key = new ResourceLocation(id);
                if (!ForgeRegistries.ITEMS.containsKey(key)) {
                    Trinkets.LOGGER.debug("Skipping mana recovery entry for missing item: {}", raw);
                    continue;
                }
                if (items.put(ForgeRegistries.ITEMS.getValue(key), entry) != null) {
                    Trinkets.LOGGER.warn("Duplicate mana recovery rule for {}, the later one wins", id);
                }
            } catch (ResourceLocationException e) {
                Trinkets.LOGGER.warn("Ignoring mana recovery entry with an invalid id: {}", raw);
            }
        }
        return new Snapshot(source, Map.copyOf(items), List.copyOf(tags));
    }

    @Nullable
    private static Entry parseAmount(String text) {
        final boolean percent = text.endsWith("%");
        try {
            final float value = Float.parseFloat(percent ? text.substring(0, text.length() - 1) : text);
            if (!Float.isFinite(value)) {
                return null;
            }
            final float magnitude = Math.round(Math.abs(value) / AMOUNT_STEP) * AMOUNT_STEP;
            return new Entry(value < 0 ? -magnitude : magnitude, percent);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ManaRecovery() {
    }
}
