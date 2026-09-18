package xzeroair.trinkets.client.gui.widget;

import java.util.function.IntConsumer;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ForgeSlider;

/**
 * 整数滑条：拖动时回调新值，代码里 setValue 不回调（避免与联动控件互相触发）。对应 1.12 GuiPropertiesSlider。
 * 取值范围为空（max <= min）时禁用，避免 ForgeSlider 以 0 作除数。
 */
@OnlyIn(Dist.CLIENT)
public class IntSlider extends ForgeSlider {

    private final IntConsumer onChange;

    public IntSlider(int x, int y, int width, int height, Component prefix, int min, int max, int value, IntConsumer onChange) {
        super(x, y, width, height, prefix, Component.empty(), min, Math.max(max, min + 1), Math.max(min, Math.min(value, max)), 1.0D, 0, true);
        this.onChange = onChange;
        this.active = max > min;
    }

    @Override
    protected void applyValue() {
        this.onChange.accept(this.getValueInt());
    }
}
