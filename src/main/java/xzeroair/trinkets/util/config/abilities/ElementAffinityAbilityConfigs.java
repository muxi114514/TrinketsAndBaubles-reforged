package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/**
 * 暗/毒/水三系亲和与暗系免疫的配置段（对应 1.12 ConfigAbilityAffinityDark / AffinityPoison /
 * AffinityWater / ImmunityDark）。四者都很短，集中放在一个文件里避免碎文件。
 */
public final class ElementAffinityAbilityConfigs {

    /** 暗系亲和：命中概率附加凋零，对凋零目标吸血 */
    public static class Dark implements IAbilityConfig {

        public final BooleanValue enabled;
        public final BooleanValue trueLeech;
        public final DoubleValue leechAmount;
        public final IntValue chance;
        public final IntValue duration;

        public Dark(ForgeConfigSpec.Builder builder, String name) {
            builder.translation("xat.config.abilities." + name).push(name);
            this.enabled = IAbilityConfig.defineEnabled(builder, true);
            this.trueLeech = builder
                    .comment("Heal for the full damage dealt instead of a capped amount")
                    .translation("xat.config.abilities.affinity_dark.true_leech")
                    .define("trueLeech", false);
            this.leechAmount = builder
                    .comment("Maximum health leeched per hit when trueLeech is off")
                    .translation("xat.config.abilities.affinity_dark.leech_amount")
                    .defineInRange("leechAmount", 2.0D, 0.0D, 1000.0D);
            this.chance = defineChance(builder);
            this.duration = defineDuration(builder);
            builder.pop();
        }

        @Override
        public boolean isEnabled() {
            return this.enabled.get();
        }
    }

    /** 毒系亲和：非魔法/火焰/爆炸伤害概率附加中毒，对中毒目标的毒系伤害翻倍 */
    public static class Poison implements IAbilityConfig {

        public final BooleanValue enabled;
        public final DoubleValue damageMultiplier;
        public final IntValue chance;
        public final IntValue duration;

        public Poison(ForgeConfigSpec.Builder builder, String name) {
            builder.translation("xat.config.abilities." + name).push(name);
            this.enabled = IAbilityConfig.defineEnabled(builder, true);
            this.damageMultiplier = builder
                    .comment("Poison damage multiplier against poisoned targets")
                    .translation("xat.config.abilities.affinity_poison.multiplier")
                    .defineInRange("damageMultiplier", 2.0D, 0.0D, 100.0D);
            this.chance = defineChance(builder);
            this.duration = defineDuration(builder);
            builder.pop();
        }

        @Override
        public boolean isEnabled() {
            return this.enabled.get();
        }
    }

    /** 水系亲和：水下呼吸与水下挖掘加速 */
    public static class Water implements IAbilityConfig {

        public final BooleanValue enabled;
        public final IntValue bubbles;
        public final BooleanValue underwaterMining;
        public final BooleanValue vanilla;

        public Water(ForgeConfigSpec.Builder builder, String name) {
            builder.translation("xat.config.abilities." + name).push(name);
            this.enabled = IAbilityConfig.defineEnabled(builder, true);
            this.bubbles = builder
                    .comment("Air bubbles kept topped up while underwater; 0 disables")
                    .translation("xat.config.abilities.affinity_water.bubbles")
                    .defineInRange("bubbles", 1, 0, 10);
            this.underwaterMining = builder
                    .comment("Mine underwater as fast as on land")
                    .translation("xat.config.abilities.affinity_water.mining")
                    .define("underwaterMining", true);
            this.vanilla = builder
                    .comment("Use the vanilla Water Breathing effect instead of refilling air")
                    .translation("xat.config.abilities.affinity_water.vanilla")
                    .define("vanilla", false);
            builder.pop();
        }

        @Override
        public boolean isEnabled() {
            return this.enabled.get();
        }
    }

    /** 暗系免疫：免疫凋零伤害，可选择把暗系伤害转为治疗 */
    public static class DarkImmunity implements IAbilityConfig {

        public final BooleanValue enabled;
        public final BooleanValue healFromDark;
        public final DoubleValue healMultiplier;

        public DarkImmunity(ForgeConfigSpec.Builder builder, String name) {
            builder.translation("xat.config.abilities." + name).push(name);
            this.enabled = IAbilityConfig.defineEnabled(builder, true);
            this.healFromDark = builder
                    .comment("Dark damage heals instead of hurting")
                    .translation("xat.config.abilities.immunity_dark.heal")
                    .define("healFromDark", true);
            this.healMultiplier = builder
                    .comment("Portion of dark damage converted into healing")
                    .translation("xat.config.abilities.immunity_dark.heal_multiplier")
                    .defineInRange("healMultiplier", 1.0D, 0.0D, 100.0D);
            builder.pop();
        }

        @Override
        public boolean isEnabled() {
            return this.enabled.get();
        }
    }

    private static IntValue defineChance(ForgeConfigSpec.Builder builder) {
        return builder
                .comment("1 in N chance to inflict the effect on hit; 0 disables")
                .translation("xat.config.abilities.affinity.chance")
                .defineInRange("chance", 5, 0, 10000);
    }

    private static IntValue defineDuration(ForgeConfigSpec.Builder builder) {
        return builder
                .comment("Duration of the inflicted effect in ticks")
                .translation("xat.config.abilities.affinity.duration")
                .defineInRange("duration", 40, 1, 72000);
    }

    private ElementAffinityAbilityConfigs() {
    }
}
