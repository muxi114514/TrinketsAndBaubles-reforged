package xzeroair.trinkets.client.gui.config;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 字符串列表配置的逐项编辑：每项一个输入框与删除按钮，底部可新增。确认时整表校验，通过才交回上一层暂存。
 */
@OnlyIn(Dist.CLIENT)
public class ConfigListScreen extends Screen {

    @Nullable
    private final Screen parent;
    private final List<String> working;
    private final Predicate<Object> validator;
    private final Consumer<List<String>> onDone;
    private final boolean editable;
    private ConfigEntryList list;
    private double scroll;
    private boolean invalid;

    public ConfigListScreen(@Nullable Screen parent, Component title, List<String> working, Predicate<Object> validator,
            Consumer<List<String>> onDone, boolean editable) {
        super(title);
        this.parent = parent;
        this.working = working;
        this.validator = validator;
        this.onDone = onDone;
        this.editable = editable;
    }

    @Override
    protected void init() {
        final int rowWidth = Math.min(this.width - 40, 400);
        this.list = new ConfigEntryList(this.minecraft, this.width, this.height, 32, this.height - 58, rowWidth);
        for (int i = 0; i < this.working.size(); i++) {
            this.list.add(new ItemRow(i, rowWidth));
        }
        this.list.setScrollAmount(this.scroll);
        this.addRenderableWidget(this.list);
        this.addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            this.working.add("");
            this.scroll = Double.MAX_VALUE;
            this.rebuildWidgets();
        }).bounds(this.width / 2 - 154, this.height - 52, 150, 20).build()).active = this.editable;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 - 154, this.height - 26, 150, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.minecraft.setScreen(this.parent))
                .bounds(this.width / 2 + 4, this.height - 26, 150, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        if (this.invalid) {
            graphics.drawCenteredString(this.font, Component.translatable("xat.config.gui.invalid"), this.width / 2 + 79, this.height - 46, 0xFF5555);
        }
    }

    /** 确认（含 Esc）：整表合法才交回，否则提示并留在本界面 */
    @Override
    public void onClose() {
        if (!this.editable) {
            this.minecraft.setScreen(this.parent);
            return;
        }
        final List<String> result = this.working.stream().map(String::trim).filter(entry -> !entry.isEmpty()).toList();
        if (!this.validator.test(result)) {
            this.invalid = true;
            return;
        }
        this.onDone.accept(result);
        this.minecraft.setScreen(this.parent);
    }

    private class ItemRow extends ConfigEntryList.Row {

        private final EditBox box;

        ItemRow(int index, int rowWidth) {
            this.box = new EditBox(Minecraft.getInstance().font, 0, 0, rowWidth - 24, 20, Component.empty());
            this.box.setMaxLength(4096);
            this.box.setValue(ConfigListScreen.this.working.get(index));
            this.box.setResponder(text -> {
                ConfigListScreen.this.working.set(index, text);
                ConfigListScreen.this.invalid = false;
            });
            final Button remove = Button.builder(Component.literal("-"), button -> {
                ConfigListScreen.this.working.remove(index);
                ConfigListScreen.this.scroll = ConfigListScreen.this.list.getScrollAmount();
                ConfigListScreen.this.rebuildWidgets();
            }).bounds(0, 0, 20, 20).build();
            this.box.active = ConfigListScreen.this.editable;
            remove.active = ConfigListScreen.this.editable;
            this.widgets.add(this.box);
            this.widgets.add(remove);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                boolean hovering, float partialTick) {
            this.box.setX(left);
            this.box.setY(top);
            this.box.render(graphics, mouseX, mouseY, partialTick);
            this.widgets.get(1).setX(left + width - 20);
            this.widgets.get(1).setY(top);
            this.widgets.get(1).render(graphics, mouseX, mouseY, partialTick);
        }
    }
}
