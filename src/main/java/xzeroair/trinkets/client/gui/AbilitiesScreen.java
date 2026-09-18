package xzeroair.trinkets.client.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.client.gui.widget.LabelList;
import xzeroair.trinkets.client.gui.widget.TextPanel;
import xzeroair.trinkets.client.tooltip.AbilityTooltips;
import xzeroair.trinkets.client.tooltip.AttributeLines;
import xzeroair.trinkets.traits.AbilityHandler.AbilityHolder;

/**
 * 能力与属性界面：左列当前生效的能力，中间是选中能力的来源与说明，右侧是当前种族的属性修饰。
 * 对应 1.12 GuiAttributesScreen（其排序按钮与颜色滑条是未接线的残留，不移植）。
 */
@OnlyIn(Dist.CLIENT)
public class AbilitiesScreen extends Screen {

    private final LocalPlayer player;
    private int listWidth;
    @Nullable
    private TextPanel description;

    public AbilitiesScreen(LocalPlayer player) {
        super(Component.translatable("gui.xat.race.stats"));
        this.player = player;
    }

    @Override
    protected void init() {
        this.description = null;
        this.addRenderableWidget(Button.builder(Component.literal("<--"), button -> this.minecraft.setScreen(new RacePropertiesScreen(this.player)))
                .bounds(2, 2, 50, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("X").withStyle(ChatFormatting.RED),
                button -> this.minecraft.setScreen(new InventoryScreen(this.player))).bounds(this.width - 16, 2, 14, 20).build());
        final EntityProperties properties = EntityProperties.get(this.player);
        if (properties == null) {
            return;
        }
        final List<AbilityHolder> abilities = new ArrayList<>(properties.getAbilityHandler().getActiveAbilities().values());
        this.listWidth = 0;
        for (AbilityHolder holder : abilities) {
            this.listWidth = Math.max(this.listWidth, this.font.width(holder.getAbility().getDisplayName()) + 10);
        }
        this.listWidth = Math.min(Math.max(this.listWidth, 40), 150);
        if (!abilities.isEmpty()) {
            this.addRenderableWidget(new LabelList<>(this.minecraft, 10, 32, this.height - 84, this.listWidth, 20, abilities,
                    holder -> holder.getAbility().getDisplayName(), this::select));
        }
        final int attributesLeft = this.width - this.listWidth - 50;
        final List<? extends String> attributeConfig = properties.getCurrentRaceCache().getRace().getInformation().getAttributes();
        this.addRenderableWidget(new TextPanel(this.minecraft, this.listWidth + 40, 32, this.height - 14, attributesLeft,
                AttributeLines.format(attributeConfig)));
    }

    private void select(AbilityHolder holder) {
        if (this.description != null) {
            this.removeWidget(this.description);
        }
        final List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(holder.getSourceID()).withStyle(ChatFormatting.DARK_AQUA));
        lines.addAll(AbilityTooltips.describe(holder.getAbility(), holder.getAbility(),
                holder.getInfo().getStackFromHandler(this.player), MagicStats.get(this.player)));
        final int left = this.listWidth + 20;
        final int width = (this.width - this.listWidth - 50) - this.listWidth - 30;
        this.description = this.addRenderableWidget(new TextPanel(this.minecraft, width, 32, this.height - 14, left, lines));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.minecraft.setScreen(new InventoryScreen(this.player));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
