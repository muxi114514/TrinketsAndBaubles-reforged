package xzeroair.trinkets.client.gui.widget;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 颜色选择组：十六进制输入框 + R/G/B 三条滑条 + 重置按钮（按钮上叠画当前色块，左上角小方块为默认色）。
 * 对应 1.12 client/gui/helpers/ColorSlider。任一控件改色后同步其余控件并回调新颜色。
 */
@OnlyIn(Dist.CLIENT)
public class ColorPicker {

    private static final String[] CHANNELS = {"R", "G", "B"};

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int defaultColor;
    private final IntConsumer onChange;
    private final EditBox field;
    private final IntSlider[] sliders = new IntSlider[3];
    private final Button reset;
    private int color;
    /** 代码同步控件时置位，屏蔽输入框的回调 */
    private boolean syncing;

    public ColorPicker(Font font, int x, int y, int width, int height, int color, int defaultColor, IntConsumer onChange) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color & 0xFFFFFF;
        this.defaultColor = defaultColor & 0xFFFFFF;
        this.onChange = onChange;
        this.field = new EditBox(font, x + 1, y, width, height, Component.empty());
        this.field.setMaxLength(7);
        this.field.setFilter(text -> text.matches("[#0-9a-fA-F]*"));
        this.field.setValue(toHex(this.color));
        this.field.setResponder(this::onTyped);
        for (int i = 0; i < 3; i++) {
            final int shift = 16 - i * 8;
            this.sliders[i] = new IntSlider(x, y + height * (i + 1) + 1, width + 22, height, Component.literal(CHANNELS[i] + ": "),
                    0, 255, (this.color >> shift) & 0xFF, value -> this.apply((this.color & ~(0xFF << shift)) | (value << shift), true));
        }
        this.reset = Button.builder(Component.literal("R"), button -> this.apply(this.defaultColor, true))
                .bounds(x + width + 2, y, 20, height).build();
    }

    public void addTo(Consumer<AbstractWidget> adder) {
        adder.accept(this.field);
        for (IntSlider slider : this.sliders) {
            adder.accept(slider);
        }
        adder.accept(this.reset);
    }

    public void tick() {
        this.field.tick();
    }

    public boolean isEditing() {
        return this.field.isFocused();
    }

    /** 在重置按钮上叠画当前色与默认色 */
    public void render(GuiGraphics graphics) {
        final int left = this.x + this.width + 3;
        graphics.fill(left, this.y + 1, left + 18, this.y + 19, 0xFF000000 | this.color);
        graphics.fill(left, this.y + 1, left + 4, this.y + 5, 0xFF000000 | this.defaultColor);
    }

    public int getNextY() {
        return this.y + this.height * 4 + 1;
    }

    private void onTyped(String text) {
        if (!this.syncing) {
            this.apply(parse(text), false);
        }
    }

    /** 更新颜色并同步控件；updateField 为 false 时不改输入框（用户正在输入） */
    private void apply(int newColor, boolean updateField) {
        this.color = newColor & 0xFFFFFF;
        this.syncing = true;
        if (updateField) {
            this.field.setValue(toHex(this.color));
        }
        for (int i = 0; i < 3; i++) {
            this.sliders[i].setValue((this.color >> (16 - i * 8)) & 0xFF);
        }
        this.syncing = false;
        this.onChange.accept(this.color);
    }

    private static String toHex(int color) {
        final String hex = Integer.toHexString(color & 0xFFFFFF);
        return "#" + "000000".substring(hex.length()) + hex;
    }

    /** 1.12 ColorHelper#getColorFromString 的十六进制分支：去掉 # 后按 16 进制解析，非法则为 0 */
    private static int parse(String text) {
        final String hex = text.replace("#", "");
        if (hex.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
