package xzeroair.trinkets.client.gui.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 配置界面的滚动行列表。每行持有自己的控件，界面重建（如窗口缩放）时行对象复用，暂存的修改不会丢失。
 */
@OnlyIn(Dist.CLIENT)
public class ConfigEntryList extends ContainerObjectSelectionList<ConfigEntryList.Row> {

    public static final int ROW_HEIGHT = 24;

    private final int rowWidth;

    public ConfigEntryList(Minecraft minecraft, int width, int height, int top, int bottom, int rowWidth) {
        super(minecraft, width, height, top, bottom, ROW_HEIGHT);
        this.rowWidth = rowWidth;
    }

    public void add(Row row) {
        this.addEntry(row);
    }

    @Override
    public int getRowWidth() {
        return this.rowWidth;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.width / 2 + this.rowWidth / 2 + 6;
    }

    public abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {

        protected final List<AbstractWidget> widgets = new ArrayList<>();

        /** 把暂存的修改写入配置（Forge 配置每次写入即存盘，故只在确认时调用） */
        public void apply() {
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.widgets;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.widgets;
        }
    }
}
