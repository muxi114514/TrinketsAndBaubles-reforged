package xzeroair.trinkets.util.config.abilities;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;

/**
 * 只有启用开关的能力配置段。
 *
 * 移植说明：1.12 的能力配置是可随处 new 的普通对象（如 new ConfigAbilityImmunityFire(3600)），
 * 种族在自己的配置里各持一份，于是「妖精的飞行」与「巨龙的飞行」可以分别调参。
 * ForgeConfigSpec 的值只能在构建期定义，故改为「构造时接收 builder 与名称、在当前层级下 push 一段」，
 * 由各族的能力配置类在自己的分组里调用，保留按种族分别调参的能力。
 *
 * 有额外字段的配置段不继承本类而是各自实现 {@link IAbilityConfig}——若在父类构造器里回调子类
 * 追加字段，子类的 final 字段此时尚未初始化，无法赋值。
 */
public class AbilityConfig implements IAbilityConfig {

    public final BooleanValue enabled;

    public AbilityConfig(ForgeConfigSpec.Builder builder, String name, boolean defaultEnabled) {
        builder.translation("xat.config.abilities." + name).push(name);
        this.enabled = IAbilityConfig.defineEnabled(builder, defaultEnabled);
        builder.pop();
    }

    public AbilityConfig(ForgeConfigSpec.Builder builder, String name) {
        this(builder, name, true);
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }
}
