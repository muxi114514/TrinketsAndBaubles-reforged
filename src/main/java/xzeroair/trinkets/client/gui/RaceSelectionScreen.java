package xzeroair.trinkets.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.client.gui.widget.LabelList;
import xzeroair.trinkets.client.gui.widget.TextPanel;
import xzeroair.trinkets.client.tooltip.AbilityLines;
import xzeroair.trinkets.client.tooltip.RaceTooltips;
import xzeroair.trinkets.client.tooltip.TooltipSections;
import xzeroair.trinkets.client.tooltip.TooltipText;
import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.init.ModRaces;
import xzeroair.trinkets.network.NetworkHandler;
import xzeroair.trinkets.network.SelectRacePacket;
import xzeroair.trinkets.races.EntityRace;
import xzeroair.trinkets.races.RaceSelection;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.util.Reference;

/**
 * 种族选择界面：左列可选种族（排除 None 与黑名单），右列元素，中间是种族图标与说明；确认后由服务端校验并写入出生种族。
 * 对应 1.12 GuiRaceSelectionScreen。
 *
 * 移植说明：1.12 未选元素时发送空元素，服务端判定无效、确认按钮看似无反应；这里未选元素按中性元素提交（说明图标本就按中性显示）。
 */
@OnlyIn(Dist.CLIENT)
public class RaceSelectionScreen extends Screen {

    private static final int ICON_TEXTURE_SIZE = 18;

    private final LocalPlayer player;
    private final boolean firstLogin;
    private int raceListWidth;
    @Nullable
    private EntityRace race;
    @Nullable
    private Element element;
    @Nullable
    private TextPanel description;

    public RaceSelectionScreen(LocalPlayer player, boolean firstLogin) {
        super(Component.translatable("gui.xat.race.selection"));
        this.player = player;
        this.firstLogin = firstLogin;
    }

    @Override
    protected void init() {
        this.description = null;
        if (!this.firstLogin) {
            this.addRenderableWidget(Button.builder(Component.literal("<--"), button -> this.minecraft.setScreen(new RacePropertiesScreen(this.player)))
                    .bounds(2, 2, 50, 20).build());
        }
        this.addRenderableWidget(Button.builder(Component.literal("X").withStyle(ChatFormatting.RED), button -> this.onClose())
                .bounds(this.width - 54, 2, 40, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.xat.confirm").withStyle(ChatFormatting.GREEN), button -> this.confirm())
                .bounds(this.width - 80, this.height - 32, 60, 20).build());

        final List<EntityRace> races = ModRaces.registry().getValues().stream().filter(RaceSelection::isSelectable).toList();
        this.raceListWidth = listWidth(races, EntityRace::getDisplayName);
        this.addRenderableWidget(new LabelList<>(this.minecraft, 10, 32, this.height - 60, this.raceListWidth, 18, races,
                entry -> entry.getDisplayName().copy().withStyle(ChatFormatting.GOLD), this::selectRace));

        final List<Element> elements = new ArrayList<>(ModElements.registry().getValues());
        final int elementListWidth = listWidth(elements, Element::getDisplayName);
        this.addRenderableWidget(new LabelList<>(this.minecraft, this.width - 160, 32, this.height - 60, elementListWidth, 18, elements,
                entry -> entry.getDisplayName().copy().withStyle(ChatFormatting.GOLD), this::selectElement));
        this.updateDescription();
    }

    private <T> int listWidth(List<T> values, Function<T, Component> name) {
        int width = 0;
        for (T value : values) {
            width = Math.max(width, this.font.width(name.apply(value)));
        }
        return Math.min(width + 12, 150);
    }

    private void selectRace(EntityRace selected) {
        this.race = selected;
        this.updateDescription();
    }

    private void selectElement(Element selected) {
        this.element = selected;
        this.updateDescription();
    }

    private Element elementOrNeutral() {
        return this.element == null ? ModElements.NEUTRAL.get() : this.element;
    }

    private void updateDescription() {
        if (this.description != null) {
            this.removeWidget(this.description);
            this.description = null;
        }
        if (this.race == null) {
            return;
        }
        final List<Component> lines = new ArrayList<>();
        final DescriptionVariables variables = new DescriptionVariables()
                .option("element", TooltipText.strip(elementOrNeutral().getDisplayName().getString()));
        AbilityLines.appendKeyLines(this.race.getTranslationKey() + ".tooltip", TooltipText.TEXT, "", variables, lines);
        // 与物品说明相同的种族详情（全部展开）
        final TooltipSections sections = new TooltipSections();
        RaceTooltips.append(sections, this.race, elementOrNeutral(), this.player);
        sections.render(lines, false);
        final int left = this.raceListWidth + 12;
        final int width = this.width - (160 + this.raceListWidth + 14);
        this.description = this.addRenderableWidget(new TextPanel(this.minecraft, width, 32, this.height - 60, left, lines,
                raceIcon(this.race, elementOrNeutral()), ICON_TEXTURE_SIZE));
    }

    /** 种族药水图标；巨龙按火/冰/雷元素区分（1.12 ConstantsTextureResourceLocation#getPotionIconForRace） */
    private static ResourceLocation raceIcon(EntityRace race, Element element) {
        String name = race.getName().toLowerCase();
        if ("dragon".equals(name) && (element.getName().equalsIgnoreCase("fire") || element.getName().equalsIgnoreCase("ice")
                || element.getName().equalsIgnoreCase("lightning"))) {
            name += "_" + element.getName().toLowerCase();
        }
        return new ResourceLocation(Reference.MODID, "textures/mob_effect/" + name + ".png");
    }

    private void confirm() {
        final ResourceLocation raceId = this.race == null ? null : ModRaces.registry().getKey(this.race);
        final ResourceLocation elementId = ModElements.registry().getKey(elementOrNeutral());
        if (raceId != null && elementId != null) {
            NetworkHandler.sendToServer(new SelectRacePacket(raceId, elementId));
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        final int x = this.width - 76;
        if (this.race != null) {
            this.selectedLabel(graphics, this.race.getDisplayName(), x, this.height - 50);
        }
        if (this.element != null) {
            this.selectedLabel(graphics, this.element.getDisplayName(), x, this.height - 64);
        }
    }

    private void selectedLabel(GuiGraphics graphics, Component text, int x, int y) {
        final int textWidth = this.font.width(text);
        graphics.fill(x, y, x + textWidth + (textWidth % 2) + 10, y + 14, 0x80000000);
        graphics.drawString(this.font, text, x + 6, y + 3, 0xFFAA00, true);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.minecraft.setScreen(new InventoryScreen(this.player));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
