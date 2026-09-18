package xzeroair.trinkets.util.config.common;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

/**
 * 酿造药水的催化剂（对应 1.12 ConfigPotionMain 的 catalyst 字段）。
 *
 * 移植说明：
 * - 放在 COMMON 配置：酿造配方在 commonSetup 注册，此时 COMMON 配置已加载，而 SERVER 配置要进世界才加载。
 * - 1.12 的 Duration 不再可配：1.20.1 的酿造药水是注册表条目，效果时长在注册期就固化，
 *   早于任何配置文件加载，只能沿用 1.12 的默认时长。
 * - 1.12 条目的 metadata 与 1.13 前的旧 id 已迁移（snow → snow_block、leaves → #minecraft:leaves、magma → magma_block）。
 */
public class PotionsConfig {

    private final Map<String, ConfigValue<String>> catalysts = new LinkedHashMap<>();

    public PotionsConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Brewing catalysts. Format: item id or #item_tag. Requires a game restart")
                .translation("xat.config.potions")
                .push("potions");
        this.define(builder, "ice_resistance", "minecraft:snow_block");
        this.define(builder, "lightning_resistance", "xat:spark_powder");
        this.define(builder, "human", "minecraft:apple");
        this.define(builder, "dwarf", "minecraft:iron_block");
        this.define(builder, "elf", "#minecraft:leaves");
        this.define(builder, "fairy", "minecraft:ghast_tear");
        this.define(builder, "goblin", "minecraft:leather");
        this.define(builder, "titan", "minecraft:golden_apple");
        this.define(builder, "faelis", "xat:faelis_claw");
        this.define(builder, "dragon", "minecraft:dragon_breath");
        this.define(builder, "dragon_fire", "minecraft:magma_block");
        this.define(builder, "dragon_ice", "minecraft:packed_ice");
        this.define(builder, "dragon_lightning", "xat:spark_powder");
        this.define(builder, "taurus", "minecraft:milk_bucket");
        builder.pop();
    }

    private void define(ForgeConfigSpec.Builder builder, String name, String defaultCatalyst) {
        this.catalysts.put(name, builder
                .translation("xat.config.potions." + name)
                .define(name, defaultCatalyst));
    }

    /** 按药水名取催化剂配置；未定义返回 null */
    public String catalyst(String potionName) {
        final ConfigValue<String> value = this.catalysts.get(potionName);
        return value == null ? null : value.get();
    }
}
