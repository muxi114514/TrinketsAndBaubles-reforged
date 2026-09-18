package xzeroair.trinkets.init;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import xzeroair.trinkets.util.Reference;

/**
 * 自定义伤害类型。
 *
 * 移植说明：1.12 的伤害类型是代码里 new 出来的 EntityDamageSource 常量；1.20.1 改为**数据驱动注册表**——
 * 类型定义在 data/xat/damage_type/*.json，代码侧只持 ResourceKey，用时从世界的 registryAccess 取 Holder。
 * 死亡消息键也随之改变：1.12 自行拼 xat.damagetype.&lt;type&gt;.attack，
 * 1.20.1 由 json 里的 message_id 决定，最终键为 death.attack.&lt;message_id&gt;。
 */
public class ModDamageTypes {

    public static final ResourceKey<DamageType> BLEED = create("damage_bleed");
    public static final ResourceKey<DamageType> POISON = create("damage_poison");
    public static final ResourceKey<DamageType> WATER = create("damage_water");
    /** 龙息：火焰/中性、冰霜、雷电三种（数据里分别挂 is_fire / is_lightning 与 bypasses_armor 标签） */
    public static final ResourceKey<DamageType> DRAGON_FIRE = create("dragon_fire");
    public static final ResourceKey<DamageType> DRAGON_ICE = create("dragon_ice");
    public static final ResourceKey<DamageType> DRAGON_LIGHTNING = create("dragon_lightning");

    /**
     * 元素伤害标签（data/xat/tags/damage_type/）。对应 1.12 TrinketElementsConfig 的 Xxx_Damage 字符串表：
     * 1.20.1 伤害类型已数据化，改用标签，整合包可用数据包追加其他模组的伤害类型。火焰/雷电直接用原版标签。
     */
    public static final TagKey<DamageType> IS_DARK = tag("is_dark");
    public static final TagKey<DamageType> IS_POISON = tag("is_poison");
    public static final TagKey<DamageType> IS_WATER = tag("is_water");
    public static final TagKey<DamageType> IS_ICE = tag("is_ice");
    public static final TagKey<DamageType> IS_LIGHT = tag("is_light");
    public static final TagKey<DamageType> IS_EARTH = tag("is_earth");
    public static final TagKey<DamageType> IS_AIR = tag("is_air");
    public static final TagKey<DamageType> IS_VOID = tag("is_void");

    private static TagKey<DamageType> tag(String name) {
        return TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(Reference.MODID, name));
    }

    private static ResourceKey<DamageType> create(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(Reference.MODID, name));
    }

    /** 带直接/间接来源实体的伤害源（如雷击的施法者） */
    public static DamageSource source(Level level, ResourceKey<DamageType> type, @Nullable Entity attacker) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), attacker);
    }

    /** 区分直接来源（如投射物、坐骑）与真正施害者的伤害源 */
    public static DamageSource source(Level level, ResourceKey<DamageType> type, @Nullable Entity direct, @Nullable Entity causing) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), direct, causing);
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type));
    }

    private ModDamageTypes() {
    }
}
