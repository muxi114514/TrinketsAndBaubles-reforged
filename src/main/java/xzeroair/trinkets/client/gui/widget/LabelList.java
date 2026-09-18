package xzeroair.trinkets.client.gui.widget;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 单列文字选择列表：每行一个名称，点击选中并回调。对应 1.12 各界面里继承 GuiScrollingList 的种族/元素/能力列表。
 *
 * @param <T> 行对应的数据
 */
@OnlyIn(Dist.CLIENT)
public class LabelList<T> extends ObjectSelectionList<LabelList.Row<T>> {

    private final Consumer<T> onSelect;

    public LabelList(Minecraft minecraft, int left, int top, int bottom, int width, int rowHeight, List<T> values,
            Function<T, Component> label, Consumer<T> onSelect) {
        super(minecraft, width, bottom, top, bottom, rowHeight);
        this.onSelect = onSelect;
        this.setLeftPos(left);
        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
        for (T value : values) {
            this.addEntry(new Row<>(this, value, label.apply(value)));
        }
    }

    @Override
    public int getRowWidth() {
        return this.width - 8;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.x1 - 6;
    }

    @Override
    public void setSelected(@Nullable Row<T> row) {
        super.setSelected(row);
        if (row != null) {
            this.onSelect.accept(row.value);
        }
    }

    public static class Row<T> extends ObjectSelectionList.Entry<Row<T>> {

        private final LabelList<T> list;
        private final T value;
        private final Component label;

        Row(LabelList<T> list, T value, Component label) {
            this.list = list;
            this.value = value;
            this.label = label;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                boolean hovering, float partialTick) {
            final Minecraft minecraft = Minecraft.getInstance();
            graphics.drawString(minecraft.font, Language.getInstance().getVisualOrder(minecraft.font.substrByWidth(this.label, width - 6)),
                    left + 3, top + 2, 0xFFFFFF, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                this.list.setSelected(this);
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return this.label;
        }
    }
}
