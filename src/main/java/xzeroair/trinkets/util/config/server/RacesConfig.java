package xzeroair.trinkets.util.config.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import xzeroair.trinkets.util.config.server.race.DragonAbilitiesConfig;
import xzeroair.trinkets.util.config.server.race.DwarfAbilitiesConfig;
import xzeroair.trinkets.util.config.server.race.ElfAbilitiesConfig;
import xzeroair.trinkets.util.config.server.race.FaelisAbilitiesConfig;
import xzeroair.trinkets.util.config.server.race.FairyAbilitiesConfig;
import xzeroair.trinkets.util.config.server.race.GoblinAbilitiesConfig;
import xzeroair.trinkets.util.config.server.race.TaurusAbilitiesConfig;
import xzeroair.trinkets.util.config.server.race.TitanAbilitiesConfig;

/**
 * 全部种族的配置容器。默认体型、魔力亲和与属性修饰取自 1.12 各族配置类的默认值
 * （属性名已换成 1.20.1 注册名；1.12 的触及距离沿用旧名，由解析器映射到方块/实体两个触及属性）。
 * 尚未移植能力的种族暂以 Void 占位，能力移植时换成对应的能力配置类。
 */
public class RacesConfig {

    private final Map<String, RaceConfig<?>> byName = new LinkedHashMap<>();

    public final BooleanValue selectionMenu;
    public final ConfigValue<List<? extends String>> selectionBlacklist;

    public final RaceConfig<Void> human;
    public final RaceConfig<FairyAbilitiesConfig> fairy;
    public final RaceConfig<DwarfAbilitiesConfig> dwarf;
    public final RaceConfig<TitanAbilitiesConfig> titan;
    public final RaceConfig<GoblinAbilitiesConfig> goblin;
    public final RaceConfig<ElfAbilitiesConfig> elf;
    public final RaceConfig<FaelisAbilitiesConfig> faelis;
    public final RaceConfig<DragonAbilitiesConfig> dragon;
    public final RaceConfig<TaurusAbilitiesConfig> taurus;

    public RacesConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Per-race settings")
                .translation("xat.config.races")
                .push("races");

        this.selectionMenu = builder
                .comment("Prompts the player to choose a Race when joining a world for the first time.")
                .translation("xat.config.races.menu")
                .worldRestart()
                .define("selectionMenu", false);
        this.selectionBlacklist = builder
                .comment("These are races which are blacklisted from the Race Selection Menu if Enabled")
                .translation("xat.config.races.menu.blacklist")
                .worldRestart()
                .defineListAllowEmpty("selectionBlacklist", List.of("Dragon", "Taurus"), o -> o instanceof String);

        this.human = this.define(builder, "human", 100, 100, 100, List.of(), null);
        this.fairy = this.define(builder, "fairy", 25, 25, 500, List.of(
                "Name:minecraft:generic.max_health, Amount:-0.6, Operation:2",
                "Name:minecraft:generic.movement_speed, Amount:-0.25, Operation:2",
                "Name:minecraft:generic.attack_damage, Amount:-0.75, Operation:2",
                "Name:minecraft:generic.armor, Amount:-0.5, Operation:2",
                "Name:minecraft:generic.armor_toughness, Amount:-0.25, Operation:2",
                "Name:generic.reachDistance, Amount:-0.35, Operation:2",
                "Name:forge:swim_speed, Amount:-0.25, Operation:2",
                "Name:xat:jump, Amount:-0.25, Operation:2",
                "Name:forge:step_height_addition, Amount:-0.35, Operation:0",
                "Name:xat:fly_speed, Amount:-0.4, Operation:2"), FairyAbilitiesConfig::new);
        this.dwarf = this.define(builder, "dwarf", 75, 75, 100, List.of(
                "Name:minecraft:generic.max_health, Amount:-0.3, Operation:1",
                "Name:minecraft:generic.knockback_resistance, Amount:0.2, Operation:1",
                "Name:minecraft:generic.movement_speed, Amount:-0.25, Operation:1",
                "Name:minecraft:generic.attack_damage, Amount:0.25, Operation:1",
                "Name:minecraft:generic.attack_speed, Amount:-0.25, Operation:2",
                "Name:minecraft:generic.armor_toughness, Amount:0.25, Operation:2"), DwarfAbilitiesConfig::new);
        this.titan = this.define(builder, "titan", 300, 300, 50, List.of(
                "Name:minecraft:generic.max_health, Amount:2, Operation:1",
                "Name:minecraft:generic.knockback_resistance, Amount:1, Operation:0",
                "Name:minecraft:generic.attack_damage, Amount:0.5, Operation:2",
                "Name:minecraft:generic.attack_speed, Amount:-0.5, Operation:2",
                "Name:generic.reachDistance, Amount:1, Operation:1",
                "Name:xat:jump, Amount:0.75, Operation:1",
                "Name:forge:step_height_addition, Amount:1.4, Operation:0"), TitanAbilitiesConfig::new);
        this.goblin = this.define(builder, "goblin", 50, 50, 75, List.of(
                "Name:minecraft:generic.max_health, Amount:-0.4, Operation:2",
                "Name:minecraft:generic.movement_speed, Amount:0.2, Operation:1",
                "Name:minecraft:generic.attack_damage, Amount:0.5, Operation:1",
                "Name:minecraft:generic.luck, Amount:1, Operation:0",
                "Name:forge:swim_speed, Amount:0.1, Operation:1"), GoblinAbilitiesConfig::new);
        this.elf = this.define(builder, "elf", 100, 100, 200, List.of(
                "Name:minecraft:generic.movement_speed, Amount:0.1, Operation:1",
                "Name:minecraft:generic.attack_speed, Amount:0.3, Operation:1"), ElfAbilitiesConfig::new);
        this.faelis = this.define(builder, "faelis", 85, 85, 125, List.of(
                "Name:minecraft:generic.max_health, Amount:-0.25, Operation:1",
                "Name:minecraft:generic.movement_speed, Amount:0.15, Operation:1",
                "Name:minecraft:generic.attack_damage, Amount:-0.25, Operation:1",
                "Name:minecraft:generic.attack_speed, Amount:0.15, Operation:2",
                "Name:minecraft:generic.armor_toughness, Amount:-0.15, Operation:2",
                "Name:minecraft:generic.luck, Amount:1, Operation:0",
                "Name:generic.reachDistance, Amount:-0.1, Operation:1",
                "Name:forge:swim_speed, Amount:0.3, Operation:1",
                "Name:xat:jump, Amount:0.6, Operation:1",
                "Name:forge:step_height_addition, Amount:0.6, Operation:0"), FaelisAbilitiesConfig::new);
        this.dragon = this.define(builder, "dragon", 120, 120, 400, List.of(
                "Name:minecraft:generic.max_health, Amount:0.25, Operation:1",
                "Name:minecraft:generic.attack_damage, Amount:0.5, Operation:1",
                "Name:minecraft:generic.armor_toughness, Amount:0.5, Operation:1",
                "Name:xat:fly_speed, Amount:-0.4, Operation:2"), DragonAbilitiesConfig::new);
        this.taurus = this.define(builder, "taurus", 120, 120, 100, List.of(
                "Name:minecraft:generic.max_health, Amount:0.5, Operation:1",
                "Name:minecraft:generic.knockback_resistance, Amount:0.5, Operation:0",
                "Name:minecraft:generic.armor, Amount:0.2, Operation:1",
                "Name:minecraft:generic.armor_toughness, Amount:0.3, Operation:1",
                "Name:minecraft:generic.attack_damage, Amount:0.2, Operation:1",
                "Name:minecraft:generic.movement_speed, Amount:-0.1, Operation:1",
                "Name:forge:swim_speed, Amount:-0.15, Operation:2"), TaurusAbilitiesConfig::new);

        builder.pop();
    }

    private <A> RaceConfig<A> define(ForgeConfigSpec.Builder builder, String name, int height, int width,
            int affinity, List<String> attributes, @Nullable Function<ForgeConfigSpec.Builder, A> abilities) {
        final RaceConfig<A> config = new RaceConfig<>(builder, name, height, width, affinity, attributes, abilities);
        this.byName.put(name, config);
        return config;
    }

    /** 按种族注册名（小写）取配置；未知种族返回 null */
    @Nullable
    public RaceConfig<?> get(String raceName) {
        return raceName == null ? null : this.byName.get(raceName.toLowerCase());
    }

    public Map<String, RaceConfig<?>> all() {
        return this.byName;
    }
}
