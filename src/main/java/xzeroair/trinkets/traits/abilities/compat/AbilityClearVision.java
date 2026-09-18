package xzeroair.trinkets.traits.abilities.compat;

import javax.annotation.Nonnull;

import xzeroair.trinkets.traits.abilities.Ability;
import xzeroair.trinkets.util.compat.ModCompat;
import xzeroair.trinkets.util.config.abilities.IAbilityConfig;

/**
 * Enhanced Visuals 联动的标记能力：拥有者不显示对应的画面干扰，实际拦截在客户端 client/compat/EnhancedVisualsHandler。
 * 注册名决定拦截哪一种：clear_vision（爆炸模糊，荣耀之盾）/ clear_splash（水花，海洋之石）/ ender_eyes（末影人雪花屏，末影王冠）。
 *
 * 移植说明：1.12 为此写了 AbilityEnhancedVisualsBlur / Splash / Static 三个空子类，合并为本类。
 */
public class AbilityClearVision extends Ability {

    public AbilityClearVision(String name, @Nonnull IAbilityConfig config) {
        super(name);
        this.setAbilityEnabled(config.isEnabled());
    }

    @Override
    public String getCompatModId() {
        return ModCompat.ENHANCED_VISUALS;
    }
}
