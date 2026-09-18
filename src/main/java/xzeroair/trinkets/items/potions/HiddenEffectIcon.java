package xzeroair.trinkets.items.potions;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

/**
 * 不显示图标的效果（对应 1.12 Potion#shouldRender / shouldRenderHUD / shouldRenderInvText 返回 false）。
 * 本接口的方法只在客户端被调用，实例本身不引用客户端类，放在通用代码中安全。
 */
public final class HiddenEffectIcon implements IClientMobEffectExtensions {

    public static final HiddenEffectIcon INSTANCE = new HiddenEffectIcon();

    @Override
    public boolean isVisibleInInventory(MobEffectInstance instance) {
        return false;
    }

    @Override
    public boolean isVisibleInGui(MobEffectInstance instance) {
        return false;
    }

    private HiddenEffectIcon() {
    }
}
