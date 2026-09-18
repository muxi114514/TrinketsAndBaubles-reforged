package xzeroair.trinkets.util.helpers.damage;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import xzeroair.trinkets.init.ModDamageTypes;
import xzeroair.trinkets.traits.elements.Element;

/**
 * 伤害规则里单个词元的判定：伤害类型名、isXxx 类别、实体 id、eleXxx 元素。
 * 对应 1.12 DamageTypeConfigParser 的 parseDamageTypeMatchesType / parseEntityByRegName / compareElement。
 */
final class DamageTypeChecks {

    private static final Map<String, TagKey<DamageType>> TAGS = new ConcurrentHashMap<>();

    /**
     * 伤害类型名：* 任意；#命名空间:路径 为标签；命名空间:路径 为注册名；
     * 其余按 1.12 的旧名（即 message id，如 inFire、mob、arrow）或注册名路径比对。
     */
    static boolean matchesDamageName(DamageSource source, String name) {
        if ("*".equals(name)) {
            return true;
        }
        if (name.startsWith("#")) {
            final ResourceLocation id = ResourceLocation.tryParse(name.substring(1));
            return id != null && source.is(TAGS.computeIfAbsent(id.toString(),
                    key -> TagKey.create(Registries.DAMAGE_TYPE, id)));
        }
        if (name.contains(":")) {
            final ResourceLocation id = ResourceLocation.tryParse(name);
            return id != null && source.typeHolder().is(id);
        }
        return name.equals(source.getMsgId())
                || source.typeHolder().unwrapKey().map(key -> key.location().getPath().equals(name)).orElse(false);
    }

    /**
     * isXxx 类别判定。
     * 移植说明：1.12 的 isMagicDamage/isUnblockable/isDamageAbsolute 等布尔位在 1.20.1 均改为伤害类型标签；
     * 元素类别由 1.12 的「伤害名字符串表」改为 xat:is_* 标签。
     */
    static boolean matchesCategory(DamageSource source, String category) {
        return switch (category) {
            case "isFire" -> source.is(DamageTypeTags.IS_FIRE);
            case "isMagic" -> source.is(DamageTypeTags.WITCH_RESISTANT_TO);
            case "isExplosion" -> source.is(DamageTypeTags.IS_EXPLOSION);
            case "isProjectile" -> source.is(DamageTypeTags.IS_PROJECTILE);
            case "isAbsolute" -> source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS);
            case "isUnblockable" -> source.is(DamageTypeTags.BYPASSES_ARMOR);
            case "isCreativePlayer" -> source.getEntity() instanceof Player player && player.getAbilities().instabuild;
            case "isBleed" -> source.is(ModDamageTypes.BLEED);
            case "isPoison" -> source.is(ModDamageTypes.POISON) || source.is(ModDamageTypes.IS_POISON);
            case "isWater" -> source.is(ModDamageTypes.WATER) || source.is(ModDamageTypes.IS_WATER);
            case "isIce" -> source.is(ModDamageTypes.IS_ICE);
            case "isLightning" -> source.is(DamageTypeTags.IS_LIGHTNING);
            case "isLight" -> source.is(ModDamageTypes.IS_LIGHT);
            case "isDark" -> source.is(ModDamageTypes.IS_DARK);
            case "isEarth" -> source.is(ModDamageTypes.IS_EARTH);
            case "isAir" -> source.is(ModDamageTypes.IS_AIR);
            case "isVoid" -> source.is(ModDamageTypes.IS_VOID);
            default -> false;
        };
    }

    /** 实体词元：isTrue: 前缀取真正施害者，否则取直接来源；* 匹配任意（含无来源） */
    static boolean matchesEntity(DamageSource source, String token) {
        final boolean trueSource = token.startsWith("isTrue:");
        final String id = trueSource ? token.substring("isTrue:".length()) : token;
        if ("*".equals(id)) {
            return true;
        }
        final Entity entity = trueSource ? source.getEntity() : source.getDirectEntity();
        return entity != null && idEquals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()), id);
    }

    /** eleFire / eleIce …：与元素名忽略大小写比对 */
    static boolean matchesElement(String elementName, @Nullable Element element) {
        return element != null && element.getName().equalsIgnoreCase(elementName);
    }

    private static boolean idEquals(@Nullable ResourceLocation id, String expected) {
        return id != null && id.toString().equals(expected.toLowerCase(Locale.ROOT));
    }

    private DamageTypeChecks() {
    }
}
