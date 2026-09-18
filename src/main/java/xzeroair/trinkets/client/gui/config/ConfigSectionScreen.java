package xzeroair.trinkets.client.gui.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.electronwill.nightconfig.core.UnmodifiableConfig;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.ValueSpec;

/**
 * 一层配置分组：列出子分组与配置项。「完成」（或 Esc）把本层暂存的修改写入配置，「取消」放弃。
 * 对应 1.12 由 Forge 自动生成的 GuiConfig（TrinketsGuiFactory）。
 */
@OnlyIn(Dist.CLIENT)
public class ConfigSectionScreen extends Screen {

    @Nullable
    private final Screen parent;
    private final ForgeConfigSpec spec;
    private final List<String> path;
    private final UnmodifiableConfig values;
    private final boolean editable;
    /** 行在首次 init 时建立，之后重建界面复用，保留暂存的修改 */
    private final List<ConfigEntryList.Row> rows = new ArrayList<>();

    public ConfigSectionScreen(@Nullable Screen parent, Component title, ForgeConfigSpec spec, List<String> path,
            UnmodifiableConfig values, boolean editable) {
        super(title);
        this.parent = parent;
        this.spec = spec;
        this.path = List.copyOf(path);
        this.values = values;
        this.editable = editable;
    }

    @Override
    protected void init() {
        if (this.rows.isEmpty()) {
            this.buildRows();
        }
        final ConfigEntryList list = new ConfigEntryList(this.minecraft, this.width, this.height, 32, this.height - 32,
                Math.min(this.width - 40, 360));
        this.rows.forEach(list::add);
        this.addRenderableWidget(list);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 - 154, this.height - 26, 150, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.minecraft.setScreen(this.parent))
                .bounds(this.width / 2 + 4, this.height - 26, 150, 20).build());
    }

    private void buildRows() {
        for (Map.Entry<String, Object> entry : this.values.valueMap().entrySet()) {
            final List<String> childPath = new ArrayList<>(this.path);
            childPath.add(entry.getKey());
            if (entry.getValue() instanceof UnmodifiableConfig section) {
                final Component label = ConfigRows.label(this.spec.getLevelTranslationKey(childPath), entry.getKey());
                this.rows.add(ConfigRows.section(label, this.spec.getLevelComment(childPath), this.spec, childPath, section, this.editable));
            } else if (entry.getValue() instanceof ConfigValue<?> value && this.spec.getSpec().get(childPath) instanceof ValueSpec valueSpec) {
                this.rows.add(ConfigRows.create(value, valueSpec, entry.getKey(), this.editable));
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.editable && this.spec.isLoaded()) {
            this.rows.forEach(ConfigEntryList.Row::apply);
        }
        this.minecraft.setScreen(this.parent);
    }
}
