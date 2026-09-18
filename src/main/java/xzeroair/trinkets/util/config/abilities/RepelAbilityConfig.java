package xzeroair.trinkets.util.config.abilities;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/**
 * 排斥（推开投射物）的配置段（对应 1.12 ConfigAbilityRepel）。
 * 1.12 的「反转白名单」带 @Config.Ignore（从未生效），不移植。
 */
public class RepelAbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;
    public final DoubleValue cost;
    public final IntValue frequency;
    public final DoubleValue force;
    public final ConfigValue<List<? extends String>> whitelist;
    public final IntValue rangeVertical;
    public final IntValue rangeHorizontal;

    public RepelAbilityConfig(ForgeConfigSpec.Builder builder, String name) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, true);
        this.cost = builder
                .comment("Mana spent every frequency ticks while active; 0 makes it free")
                .translation("xat.config.abilities.repel.cost")
                .defineInRange("cost", 10.0D, 0.0D, 10000.0D);
        this.frequency = builder
                .comment("Ticks between mana payments")
                .translation("xat.config.abilities.repel.frequency")
                .defineInRange("frequency", 20, 1, 12000);
        this.force = builder
                .comment("Push strength")
                .translation("xat.config.abilities.repel.force")
                .defineInRange("force", 0.1D, 0.1D, 1.0D);
        this.whitelist = builder
                .comment("Entity ids that get pushed away; \"modid:*\" matches a whole mod")
                .translation("xat.config.abilities.repel.whitelist")
                .defineListAllowEmpty("whitelist", List.of("minecraft:arrow", "minecraft:fireball"), o -> o instanceof String);
        this.rangeVertical = builder
                .comment("Vertical range in blocks")
                .translation("xat.config.abilities.repel.range_vertical")
                .defineInRange("rangeVertical", 6, 0, 32);
        this.rangeHorizontal = builder
                .comment("Horizontal range in blocks")
                .translation("xat.config.abilities.repel.range_horizontal")
                .defineInRange("rangeHorizontal", 12, 0, 32);
        builder.pop();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
