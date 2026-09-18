package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/**
 * 末影女王的配置段（对应 1.12 ConfigAbilityEnderQueen）。几率字段为「1/N」的 N，0 表示关闭。
 */
public class EnderQueenAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final BooleanValue enderChest;
    public final BooleanValue waterHurts;
    public final IntValue ignoreChance;
    public final IntValue spawnChance;
    public final IntValue teleportChance;
    public final BooleanValue endermanFollow;
    public final BooleanValue endermanRetaliate;
    public final BooleanValue endermanDropExp;
    public final BooleanValue endermanDropItems;
    public final BooleanValue blockTeleportation;

    public EnderQueenAbilityConfig(ForgeConfigSpec.Builder builder, String name) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.enderChest = builder
                .comment("The auxiliary key opens the ender chest, costing all mana")
                .translation("xat.config.abilities.ender_queen.chest")
                .define("enderChest", true);
        this.waterHurts = builder
                .comment("Water drains mana, or hurts once mana runs out")
                .translation("xat.config.abilities.ender_queen.water_hurts")
                .define("waterHurts", false);
        this.ignoreChance = builder
                .comment("1 in N chance to ignore an attack from a living attacker")
                .translation("xat.config.abilities.ender_queen.ignore_chance")
                .defineInRange("ignoreChance", 10, 0, 10000);
        this.spawnChance = builder
                .comment("1 in N chance to summon an enderman knight when attacked")
                .translation("xat.config.abilities.ender_queen.spawn_chance")
                .defineInRange("spawnChance", 10, 0, 10000);
        this.teleportChance = builder
                .comment("1 in N chance to teleport away from projectiles")
                .translation("xat.config.abilities.ender_queen.teleport_chance")
                .defineInRange("teleportChance", 1, 0, 10000);
        this.endermanFollow = builder
                .comment("Endermen follow the queen around")
                .translation("xat.config.abilities.ender_queen.follow")
                .define("endermanFollow", true);
        this.endermanRetaliate = builder
                .comment("Endermen may still fight back against the queen")
                .translation("xat.config.abilities.ender_queen.retaliate")
                .define("endermanRetaliate", false);
        this.endermanDropExp = builder
                .comment("Endermen killed by the queen drop experience")
                .translation("xat.config.abilities.ender_queen.drop_exp")
                .define("endermanDropExp", false);
        this.endermanDropItems = builder
                .comment("Endermen killed by the queen drop items")
                .translation("xat.config.abilities.ender_queen.drop_items")
                .define("endermanDropItems", false);
        this.blockTeleportation = builder
                .comment("Mobs near the queen or a boss cannot teleport")
                .translation("xat.config.abilities.ender_queen.block_teleport")
                .define("blockTeleportation", false);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
