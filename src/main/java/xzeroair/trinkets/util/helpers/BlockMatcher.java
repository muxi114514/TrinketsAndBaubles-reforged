package xzeroair.trinkets.util.helpers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.Trinkets;

/**
 * 方块匹配器：把配置里的方块条目列表编译一次，之后反复匹配。
 *
 * 条目语法：
 * <ul>
 *   <li>{@code minecraft:stone} —— 精确匹配注册名（省略命名空间时默认 minecraft）</li>
 *   <li>{@code minecraft:*planks*} —— 通配符 {@code *} 匹配任意字符</li>
 *   <li>{@code #minecraft:logs} —— 方块标签</li>
 * </ul>
 *
 * 移植说明：1.12 的 ConfigObject（ConfigHelper 的一部分）还支持矿物词典、{@code !mat:} 材质与 metadata。
 * 1.20.1 中矿物词典由标签取代（改写 {@code #命名空间:标签}），材质系统已于 1.20 移除，
 * metadata 不复存在（每个变种即独立方块），故只保留上述三种语法。
 * 精确条目放 HashSet，一次哈希即可命中；通配与标签条目通常很少，逐条检查。
 * 非法条目记日志后跳过，不影响其余条目。
 */
public final class BlockMatcher {

    public static final BlockMatcher EMPTY = new BlockMatcher(List.of());

    private final Set<ResourceLocation> exact = new HashSet<>();
    private final List<Pattern> wildcards = new ArrayList<>();
    private final List<TagKey<Block>> tags = new ArrayList<>();

    private BlockMatcher(List<? extends String> entries) {
        for (String entry : entries) {
            this.compile(entry);
        }
    }

    public static BlockMatcher of(List<? extends String> entries) {
        return entries == null || entries.isEmpty() ? EMPTY : new BlockMatcher(entries);
    }

    public boolean isEmpty() {
        return this.exact.isEmpty() && this.wildcards.isEmpty() && this.tags.isEmpty();
    }

    public boolean matches(BlockState state) {
        final ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id != null && this.exact.contains(id)) {
            return true;
        }
        for (TagKey<Block> tag : this.tags) {
            if (state.is(tag)) {
                return true;
            }
        }
        if (id != null && !this.wildcards.isEmpty()) {
            final String name = id.toString();
            for (Pattern pattern : this.wildcards) {
                if (pattern.matcher(name).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void compile(String raw) {
        if (raw == null) {
            return;
        }
        final String entry = raw.trim();
        if (entry.isEmpty()) {
            return;
        }
        try {
            if (entry.startsWith("#")) {
                this.tags.add(BlockTags.create(new ResourceLocation(withNamespace(entry.substring(1)))));
            } else if (entry.contains("*")) {
                this.wildcards.add(globToPattern(withNamespace(entry)));
            } else {
                this.exact.add(new ResourceLocation(withNamespace(entry)));
            }
        } catch (ResourceLocationException e) {
            Trinkets.LOGGER.warn("Ignoring invalid block entry in config: {}", raw);
        }
    }

    private static String withNamespace(String id) {
        return id.contains(":") ? id : "minecraft:" + id;
    }

    /** 仅 * 为通配，其余字符按字面匹配 */
    private static Pattern globToPattern(String glob) {
        final StringBuilder regex = new StringBuilder();
        for (String part : glob.split("\\*", -1)) {
            if (regex.length() > 0) {
                regex.append(".*");
            }
            regex.append(Pattern.quote(part));
        }
        return Pattern.compile(regex.toString());
    }
}
