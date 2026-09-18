package xzeroair.trinkets.traits.abilities.archery;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.Trinkets;
import xzeroair.trinkets.util.config.abilities.SkilledArcherAbilityConfig;

/**
 * 弓重量表：把「物品;重量」配置解析为可查询的快照。对应 1.12 ConfigHelper.TrinketConfigStorage.BowWeights。
 *
 * 线程安全：配置可能在文件监听线程上重载，解析结果为不可变快照、volatile 发布，
 * 发现配置列表对象更换就整体重建（与 ManaRecovery 同一模式）。
 */
public final class BowWeightTable {

    /** 查询结果：未列出的武器类物品不享受加成 */
    public static final float UNLISTED = 0F;

    private record Entry(Pattern id, float weight) {
    }

    private record Snapshot(List<? extends String> bows, List<? extends String> blacklist,
            List<Entry> entries, List<Pattern> blocked) {
    }

    private static volatile Snapshot snapshot;

    /**
     * @return 弓的重量；≤ 1 的条目按默认重量处理；未列出的武器返回 {@link #UNLISTED}；
     *         非武器类物品（1.12 中识别不出类型的物品）返回默认重量
     */
    public static float weightOf(ItemStack stack, SkilledArcherAbilityConfig config) {
        final float defaultWeight = config.defaultWeight.get().floatValue();
        if (stack.isEmpty() || stack.getItem() instanceof ArmorItem) {
            return defaultWeight;
        }
        final ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        final String name = id != null ? id.toString() : "";
        final Snapshot current = currentSnapshot(config);
        for (Pattern blocked : current.blocked()) {
            if (blocked.matcher(name).matches()) {
                return UNLISTED;
            }
        }
        for (Entry entry : current.entries()) {
            if (entry.id().matcher(name).matches()) {
                return entry.weight() <= 1F ? defaultWeight : entry.weight();
            }
        }
        return isTypedWeapon(stack) ? UNLISTED : defaultWeight;
    }

    private static boolean isTypedWeapon(ItemStack stack) {
        return stack.getItem() instanceof ProjectileWeaponItem || stack.getItem() instanceof TieredItem
                || stack.getItem() instanceof ShieldItem;
    }

    private static Snapshot currentSnapshot(SkilledArcherAbilityConfig config) {
        final List<? extends String> bows = config.bows.get();
        final List<? extends String> blacklist = config.bowBlacklist.get();
        Snapshot current = snapshot;
        if (current == null || current.bows() != bows || current.blacklist() != blacklist) {
            current = build(bows, blacklist);
            snapshot = current;
        }
        return current;
    }

    private static Snapshot build(List<? extends String> bows, List<? extends String> blacklist) {
        final List<Entry> entries = new ArrayList<>();
        for (String raw : bows) {
            final String[] parts = raw.trim().split(";");
            if (parts.length < 2) {
                Trinkets.LOGGER.warn("Ignoring bow weight entry without a weight: {}", raw);
                continue;
            }
            try {
                entries.add(new Entry(glob(parts[0].trim()), Float.parseFloat(parts[parts.length - 1].trim())));
            } catch (NumberFormatException e) {
                Trinkets.LOGGER.warn("Ignoring bow weight entry with an invalid weight: {}", raw);
            }
        }
        final List<Pattern> blocked = new ArrayList<>();
        for (String raw : blacklist) {
            blocked.add(glob(raw.trim()));
        }
        return new Snapshot(bows, blacklist, List.copyOf(entries), List.copyOf(blocked));
    }

    private static Pattern glob(String glob) {
        final String id = glob.contains(":") ? glob : "minecraft:" + glob;
        final StringBuilder regex = new StringBuilder();
        for (String part : id.split("\\*", -1)) {
            if (!regex.isEmpty()) {
                regex.append(".*");
            }
            regex.append(Pattern.quote(part));
        }
        return Pattern.compile(regex.toString());
    }

    private BowWeightTable() {
    }
}
