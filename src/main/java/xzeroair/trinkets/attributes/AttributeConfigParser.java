package xzeroair.trinkets.attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.Trinkets;
import xzeroair.trinkets.init.ModAttributes;

/**
 * 属性配置条目解析：`Name:属性, Amount:数值, Operation:0~2[, Saved:true]`，分隔符可为逗号/分号/竖线/空格。
 * 对应 1.12 ConfigHelper#getAttributeEntry。
 *
 * 属性名同时接受 1.20.1 注册名（如 minecraft:generic.max_health）与 1.12 旧名（如 generic.maxHealth），
 * 旧名经下表映射；解析结果按原字符串缓存（ConcurrentHashMap，配置重载时条目字符串不变则直接复用）。
 */
public final class AttributeConfigParser {

    private static final Pattern SEPARATORS = Pattern.compile("[\\[\\]|,;\\s]+");
    private static final Optional<AttributeEntry> INVALID = Optional.empty();
    private static final Map<String, Optional<AttributeEntry>> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<Supplier<Attribute>>> LEGACY_NAMES = new ConcurrentHashMap<>();

    static {
        legacy("generic.maxhealth", () -> Attributes.MAX_HEALTH);
        legacy("generic.followrange", () -> Attributes.FOLLOW_RANGE);
        legacy("generic.knockbackresistance", () -> Attributes.KNOCKBACK_RESISTANCE);
        legacy("generic.movementspeed", () -> Attributes.MOVEMENT_SPEED);
        legacy("generic.flyingspeed", () -> Attributes.FLYING_SPEED);
        legacy("generic.attackdamage", () -> Attributes.ATTACK_DAMAGE);
        legacy("generic.attackspeed", () -> Attributes.ATTACK_SPEED);
        legacy("generic.armor", () -> Attributes.ARMOR);
        legacy("generic.armortoughness", () -> Attributes.ARMOR_TOUGHNESS);
        legacy("generic.luck", () -> Attributes.LUCK);
        // 1.12 的触及距离同时影响方块与实体，1.20.1 拆成两个属性
        legacy("generic.reachdistance", ForgeMod.BLOCK_REACH, ForgeMod.ENTITY_REACH);
        legacy("forge.swimspeed", ForgeMod.SWIM_SPEED);
        legacy("forge.nametagdistance", ForgeMod.NAMETAG_DISTANCE);
        legacy("forge.entitygravity", ForgeMod.ENTITY_GRAVITY);
        // 1.12 自定义步高基础值 0.6 的加法修饰器，等价于 1.20.1 步高增量属性
        legacy("xat.stepheight", ForgeMod.STEP_HEIGHT_ADDITION);
        legacy("xat.jump", ModAttributes.JUMP);
        legacy("xat.flyspeed", ModAttributes.FLY_SPEED);
        legacy("magic.maxmana", ModAttributes.MAX_MANA);
        legacy("xat.entitymagic.maxmana", ModAttributes.MAX_MANA);
        legacy("magic.regen", ModAttributes.MANA_REGEN);
        legacy("xat.entitymagic.regen", ModAttributes.MANA_REGEN);
        legacy("magic.regen.cooldown", ModAttributes.MANA_REGEN_COOLDOWN);
        legacy("xat.entitymagic.regen.cooldown", ModAttributes.MANA_REGEN_COOLDOWN);
        legacy("magic.affinity", ModAttributes.MAGIC_AFFINITY);
        legacy("xat.entitymagic.affinity", ModAttributes.MAGIC_AFFINITY);
    }

    @SafeVarargs
    private static void legacy(String name, Supplier<Attribute>... attributes) {
        LEGACY_NAMES.put(name, List.of(attributes));
    }

    /** 解析一条配置；无效条目（属性不存在、数值为 0 或格式错误）返回 null */
    @Nullable
    public static AttributeEntry parse(String entry) {
        if (entry == null || entry.isBlank()) {
            return null;
        }
        return CACHE.computeIfAbsent(entry, AttributeConfigParser::doParse).orElse(null);
    }

    /** 批量解析，跳过无效条目 */
    public static List<AttributeEntry> parseAll(List<? extends String> entries) {
        final List<AttributeEntry> result = new ArrayList<>();
        for (String entry : entries) {
            final AttributeEntry parsed = parse(entry);
            if (parsed != null) {
                result.add(parsed);
            }
        }
        return result;
    }

    private static Optional<AttributeEntry> doParse(String raw) {
        final String cleaned = raw.trim()
                .replaceFirst("(?i)name:", "")
                .replaceFirst("(?i),?\\s*amount:", ";")
                .replaceFirst("(?i),?\\s*operation:", ";")
                .replaceFirst("(?i),?\\s*saved:", ";");
        final String[] parts = SEPARATORS.split(cleaned.trim());
        if (parts.length < 2) {
            return warn(raw);
        }
        final List<Attribute> attributes = resolve(parts[0]);
        if (attributes.isEmpty()) {
            return warn(raw);
        }
        try {
            final double amount = Double.parseDouble(parts[1].replace("+", ""));
            final int operation = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            final boolean saved = parts.length > 3 && Boolean.parseBoolean(parts[3]);
            if (amount == 0 || operation < 0 || operation > 2) {
                return INVALID;
            }
            return Optional.of(new AttributeEntry(parts[0], attributes, amount, operation, saved));
        } catch (NumberFormatException e) {
            return warn(raw);
        }
    }

    private static List<Attribute> resolve(String name) {
        final List<Supplier<Attribute>> legacy = LEGACY_NAMES.get(name.toLowerCase(Locale.ROOT));
        if (legacy != null) {
            final List<Attribute> attributes = new ArrayList<>();
            legacy.forEach(supplier -> attributes.add(supplier.get()));
            return attributes;
        }
        final ResourceLocation id = ResourceLocation.tryParse(name.toLowerCase(Locale.ROOT));
        final Attribute attribute = id == null ? null : ForgeRegistries.ATTRIBUTES.getValue(id);
        return attribute == null ? List.of() : List.of(attribute);
    }

    private static Optional<AttributeEntry> warn(String raw) {
        Trinkets.LOGGER.warn("Ignoring invalid attribute entry: {}", raw);
        return INVALID;
    }

    private AttributeConfigParser() {
    }
}
