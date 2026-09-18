package xzeroair.trinkets.client.gui.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.electronwill.nightconfig.core.UnmodifiableConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.common.ForgeConfigSpec.LongValue;
import net.minecraftforge.common.ForgeConfigSpec.ValueSpec;

import xzeroair.trinkets.client.gui.config.ConfigEntryList.Row;

/**
 * 配置行：左侧名称（悬停显示注释），右侧按值类型给出控件，最右是恢复默认按钮。
 * 修改先暂存在行内，确认时经 {@link Row#apply()} 校验后写入。
 */
@OnlyIn(Dist.CLIENT)
public final class ConfigRows {

    static final int CONTROL_WIDTH = 120;

    /** 按值类型建行；无法编辑的类型只显示当前值 */
    @SuppressWarnings("unchecked")
    public static Row create(ConfigValue<?> value, ValueSpec spec, String name, boolean editable) {
        final Component label = label(spec.getTranslationKey(), name);
        final String comment = spec.needsWorldRestart() ? spec.getComment() + "\n" + I18n.get("xat.config.gui.restart") : spec.getComment();
        final Row row;
        if (value instanceof BooleanValue bool) {
            row = new CycleRow<>(label, comment, bool, spec, current -> !current,
                    current -> current ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
        } else if (value instanceof EnumValue<?> enumValue) {
            row = enumRow(label, comment, enumValue, spec);
        } else if (value instanceof IntValue || value instanceof LongValue || value instanceof DoubleValue) {
            final Function<String, Object> parser = value instanceof IntValue ? Integer::valueOf
                    : value instanceof LongValue ? Long::valueOf : Double::valueOf;
            row = new TextRow<>(label, comment, (ConfigValue<Object>) value, spec, parser);
        } else if (value.get() instanceof String) {
            row = new TextRow<>(label, comment, (ConfigValue<Object>) value, spec, text -> text);
        } else if (value.get() instanceof List) {
            row = new ListRow(label, comment, (ConfigValue<List<? extends String>>) value, spec);
        } else {
            row = new LabeledRow(label, comment) {
            };
        }
        row.widgets.forEach(widget -> widget.active = editable);
        return row;
    }

    public static Component label(@Nullable String translationKey, String fallback) {
        return translationKey != null && I18n.exists(translationKey) ? Component.translatable(translationKey) : Component.literal(fallback);
    }

    private static <E extends Enum<E>> Row enumRow(Component label, String comment, EnumValue<E> value, ValueSpec spec) {
        return new CycleRow<>(label, comment, value, spec, current -> {
            final E[] constants = current.getDeclaringClass().getEnumConstants();
            return constants[(current.ordinal() + 1) % constants.length];
        }, current -> Component.literal(current.name()));
    }

    /** 带名称与注释提示的行 */
    abstract static class LabeledRow extends Row {

        private final Component label;
        private final List<FormattedCharSequence> tooltip;

        LabeledRow(Component label, @Nullable String comment) {
            this.label = label;
            this.tooltip = comment == null || comment.isBlank() ? List.of()
                    : Minecraft.getInstance().font.split(Component.literal(comment), 250);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                boolean hovering, float partialTick) {
            final Minecraft minecraft = Minecraft.getInstance();
            final Font font = minecraft.font;
            final int labelWidth = width - CONTROL_WIDTH - 28;
            graphics.drawString(font, Language.getInstance().getVisualOrder(font.substrByWidth(this.label, labelWidth)), left, top + 6, 0xFFFFFF, false);
            if (hovering && mouseX < left + labelWidth && !this.tooltip.isEmpty() && minecraft.screen != null) {
                minecraft.screen.setTooltipForNextRenderPass(this.tooltip);
            }
            final int controlLeft = left + width - CONTROL_WIDTH - 22;
            for (int i = 0; i < this.widgets.size(); i++) {
                final AbstractWidget widget = this.widgets.get(i);
                widget.setX(i == 0 ? controlLeft : left + width - 20);
                widget.setY(top);
                widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    /** 可暂存修改的值行：第 0 个控件为编辑控件，第 1 个为恢复默认 */
    abstract static class ValueRow<T> extends LabeledRow {

        protected final ConfigValue<T> value;
        protected final ValueSpec spec;
        @Nullable
        protected T pending;

        ValueRow(Component label, String comment, ConfigValue<T> value, ValueSpec spec) {
            super(label, comment);
            this.value = value;
            this.spec = spec;
            this.pending = value.get();
        }

        protected void addControl(AbstractWidget control) {
            this.widgets.add(control);
            this.widgets.add(Button.builder(Component.literal("R"), button -> this.setPending(this.value.getDefault()))
                    .bounds(0, 0, 20, 20).build());
        }

        protected void setPending(@Nullable T newValue) {
            this.pending = newValue;
            this.refresh();
        }

        protected abstract void refresh();

        @Override
        public void apply() {
            if (this.pending != null && this.spec.test(this.pending) && !Objects.equals(this.pending, this.value.get())) {
                this.value.set(this.pending);
            }
        }
    }

    /** 点击循环取值（布尔、枚举） */
    static class CycleRow<T> extends ValueRow<T> {

        private final Button button;
        private final Function<T, T> next;
        private final Function<T, Component> display;

        CycleRow(Component label, String comment, ConfigValue<T> value, ValueSpec spec, Function<T, T> next, Function<T, Component> display) {
            super(label, comment, value, spec);
            this.next = next;
            this.display = display;
            this.button = Button.builder(display.apply(value.get()), b -> this.setPending(this.next.apply(this.pending)))
                    .bounds(0, 0, CONTROL_WIDTH, 20).build();
            this.addControl(this.button);
        }

        @Override
        protected void refresh() {
            this.button.setMessage(this.display.apply(this.pending));
        }
    }

    /** 文本输入（数字、字符串）；不合法时文字变红且不写入 */
    static class TextRow<T> extends ValueRow<T> {

        private final EditBox box;
        private final Function<String, T> parser;
        private boolean syncing;

        TextRow(Component label, String comment, ConfigValue<T> value, ValueSpec spec, Function<String, T> parser) {
            super(label, comment, value, spec);
            this.parser = parser;
            this.box = new EditBox(Minecraft.getInstance().font, 0, 0, CONTROL_WIDTH, 20, label);
            this.box.setMaxLength(4096);
            this.box.setValue(String.valueOf(value.get()));
            this.box.setResponder(this::onEdited);
            this.addControl(this.box);
        }

        private void onEdited(String text) {
            if (this.syncing) {
                return;
            }
            try {
                this.pending = this.parser.apply(text.trim());
            } catch (RuntimeException e) {
                this.pending = null;
            }
            final boolean valid = this.pending != null && this.spec.test(this.pending);
            this.box.setTextColor(valid ? 0xE0E0E0 : 0xFF5555);
        }

        @Override
        protected void refresh() {
            this.syncing = true;
            this.box.setValue(String.valueOf(this.pending));
            this.box.setTextColor(0xE0E0E0);
            this.syncing = false;
        }
    }

    /** 字符串列表：按钮进入逐项编辑界面 */
    static class ListRow extends ValueRow<List<? extends String>> {

        private final Button button;

        ListRow(Component label, String comment, ConfigValue<List<? extends String>> value, ValueSpec spec) {
            super(label, comment, value, spec);
            this.button = Button.builder(Component.empty(), b -> {
                final Minecraft minecraft = Minecraft.getInstance();
                minecraft.setScreen(new ConfigListScreen(minecraft.screen, label, new ArrayList<>(this.pending), spec::test,
                        this::setPending, b.active));
            }).bounds(0, 0, CONTROL_WIDTH, 20).build();
            this.addControl(this.button);
            this.refresh();
        }

        @Override
        protected void refresh() {
            this.button.setMessage(Component.translatable("xat.config.gui.list", this.pending == null ? 0 : this.pending.size())
                    .withStyle(ChatFormatting.WHITE));
        }
    }

    /** 子分组：进入下一层配置界面 */
    public static Row section(Component label, @Nullable String comment, ForgeConfigSpec spec, List<String> path,
            UnmodifiableConfig values, boolean editable) {
        final LabeledRow row = new LabeledRow(label, comment) {
        };
        row.widgets.add(Button.builder(Component.literal(">"), button -> {
            final Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new ConfigSectionScreen(minecraft.screen, label, spec, path, values, editable));
        }).bounds(0, 0, CONTROL_WIDTH, 20).build());
        return row;
    }

    private ConfigRows() {
    }
}
