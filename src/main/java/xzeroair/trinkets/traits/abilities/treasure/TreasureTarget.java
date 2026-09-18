package xzeroair.trinkets.traits.abilities.treasure;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.Trinkets;

/**
 * 贪婪之眼的一个寻宝目标：方块标签、单个方块或实体类型，附带高亮颜色。
 * 对应 1.12 ConfigHelper.ConfigTreasureObject（矿物词典 → 方块标签）。
 */
public record TreasureTarget(String entry, @Nullable TagKey<Block> blockTag, @Nullable Block block,
        @Nullable EntityType<?> entityType, int color) {

    private static final int DEFAULT_COLOR = 16766720;

    public boolean isEntity() {
        return this.entityType != null;
    }

    public boolean matches(BlockState state) {
        if (this.blockTag != null) {
            return state.is(this.blockTag);
        }
        return this.block != null && state.is(this.block);
    }

    /** 提示语中显示的目标名；方块与实体返回语言键（见 isDisplayNameTranslatable），标签返回可读文本 */
    public String displayName() {
        if (this.blockTag != null) {
            final String path = this.blockTag.location().getPath();
            final String name = path.substring(path.lastIndexOf('/') + 1).replace('_', ' ');
            return name.isEmpty() ? "" : name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
        }
        if (this.block != null) {
            return this.block.getDescriptionId();
        }
        return this.entityType != null ? this.entityType.getDescriptionId() : "";
    }

    public boolean isDisplayNameTranslatable() {
        return this.blockTag == null;
    }

    /** 解析配置列表；不存在的方块/实体/格式错误的条目跳过 */
    public static List<TreasureTarget> parseAll(List<? extends String> entries) {
        final List<TreasureTarget> targets = new ArrayList<>();
        for (String raw : entries) {
            final TreasureTarget target = parse(raw);
            if (target != null) {
                targets.add(target);
            }
        }
        return List.copyOf(targets);
    }

    @Nullable
    public static TreasureTarget parse(String raw) {
        final String[] parts = raw.trim().split(";");
        final String id = parts[0].trim();
        if (id.isEmpty()) {
            return null;
        }
        final int color = parts.length > 1 ? parseColor(parts[parts.length - 1].trim(), raw) : DEFAULT_COLOR;
        if (id.startsWith("#")) {
            final ResourceLocation tag = ResourceLocation.tryParse(id.substring(1));
            return tag == null ? null : new TreasureTarget(raw, TagKey.create(Registries.BLOCK, tag), null, null, color);
        }
        final ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return null;
        }
        if (ForgeRegistries.BLOCKS.containsKey(location)) {
            return new TreasureTarget(raw, null, ForgeRegistries.BLOCKS.getValue(location), null, color);
        }
        if (ForgeRegistries.ENTITY_TYPES.containsKey(location)) {
            return new TreasureTarget(raw, null, null, ForgeRegistries.ENTITY_TYPES.getValue(location), color);
        }
        return null;
    }

    private static int parseColor(String text, String raw) {
        try {
            if (text.startsWith("#")) {
                return Integer.parseInt(text.substring(1), 16);
            }
            if (text.startsWith("0x") || text.startsWith("0X")) {
                return Integer.parseInt(text.substring(2), 16);
            }
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            Trinkets.LOGGER.error("Invalid color in treasure entry: {}", raw);
            return DEFAULT_COLOR;
        }
    }
}
