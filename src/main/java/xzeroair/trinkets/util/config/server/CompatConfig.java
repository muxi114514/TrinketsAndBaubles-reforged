package xzeroair.trinkets.util.config.server;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;

/**
 * 模组联动开关（对应 1.12 CompatibilityConfigs）。每项只在对应模组已加载时才生效。
 *
 * 移植说明：
 * - 1.12 的默认值是「该模组是否已加载」，1.20.1 配置在模组加载前就要给出默认值，改为默认开启、运行时再与加载状态相与，效果相同。
 * - 生存类：1.12 的 Tough As Nails / Simple Difficulty 在 1.20.1 由冷汗（温度）、Simple Difficulty Reforge（口渴/寄生虫效果、
 *   原药水核心效果）与 Thirst Was Taken（水分）承担。
 * - 闪避：1.12 对接 Elenai Dodge；1.20.1 另对接 Talents 的「侧步」技能。
 * - FireResistanceTiers / Better Diving / MoBends / ArtemisLib / Ido / Tropicraft / Patchouli 手册不再提供联动。
 */
public class CompatConfig {

    public final BooleanValue firstAid;
    public final BooleanValue elenaiDodge;
    public final BooleanValue talents;
    public final BooleanValue coldSweat;
    public final BooleanValue simpleDifficulty;
    public final BooleanValue thirstWasTaken;
    public final BooleanValue enhancedVisuals;
    public final BooleanValue lycanitesMobs;
    public final BooleanValue defiledLands;

    public CompatConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Mod compatibility toggles; each only applies when that mod is installed")
                .translation("xat.config.compat")
                .push("compat");
        this.firstAid = define(builder, "firstAid", "first_aid", "First Aid: head-shot protection and per-part Blessing of Life");
        this.elenaiDodge = define(builder, "elenaiDodge", "elenai_dodge",
                "Elenai Dodge 2: its dodges trigger the Arcing Orb dodge effects instead of the double-tap dodge");
        this.talents = define(builder, "talents", "talents",
                "Talents: the Sidestep skill triggers the Arcing Orb dodge effects instead of the double-tap dodge");
        this.coldSweat = define(builder, "coldSweat", "cold_sweat", "Cold Sweat: heat and cold immunities");
        this.simpleDifficulty = define(builder, "simpleDifficulty", "simple_difficulty",
                "Simple Difficulty: thirst and parasite immunities; its Flight effect takes over creative flight");
        this.thirstWasTaken = define(builder, "thirstWasTaken", "thirst_was_taken", "Thirst Was Taken: Water Absorption");
        this.enhancedVisuals = define(builder, "enhancedVisuals", "enhanced_visuals", "Enhanced Visuals: clear vision abilities");
        this.lycanitesMobs = define(builder, "lycanitesMobs", "lycanites_mobs", "Lycanites Mobs: Weightless blocks the Weight effect");
        this.defiledLands = define(builder, "defiledLands", "defiled_lands", "Defiled Lands: Vicious Strike can use its Bleeding effect");
        builder.pop();
    }

    private static BooleanValue define(ForgeConfigSpec.Builder builder, String name, String key, String comment) {
        return builder.comment(comment).translation("xat.config.compat." + key).define(name, true);
    }
}
