package xzeroair.trinkets.client.tooltip;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.capabilities.race.RaceCache;
import xzeroair.trinkets.races.EntityRace;
import xzeroair.trinkets.races.EntityRacePropertiesHandler;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.util.config.server.RaceConfig;

/**
 * 种族说明（变身戒指、种族食物、种族选择界面共用）：体型与魔力亲和、属性、效果、伤害规则、坐骑限制、本族特性，以及本族能力。
 */
@OnlyIn(Dist.CLIENT)
public final class RaceTooltips {

    public static void append(TooltipSections sections, EntityRace race, Element element, @Nullable Player player) {
        final EntityRacePropertiesHandler handler = race.getRaceHandler(null, null, new RaceCache(race, element));
        handler.registerAllRaceAbilities();
        final String title = I18n.get("xat.tooltip.section.race", TooltipText.strip(race.getDisplayName().getString()));
        sections.addSummary(title, summary(race, handler));
        sections.addDetails(title, details(race, handler));
        AbilityTooltips.append(sections, List.copyOf(handler.getRaceAbilities().values()), String.valueOf(race.getRegistryName()),
                ItemStack.EMPTY, player, element);
    }

    private static List<Component> summary(EntityRace race, EntityRacePropertiesHandler handler) {
        final List<Component> lines = new ArrayList<>();
        lines.add(line(TooltipText.translate("xat.tooltip.race.size", TooltipText.TEXT, race.getRaceHeight() + "%", race.getRaceWidth() + "%")));
        lines.add(line(TooltipText.translate("xat.tooltip.race.affinity", TooltipText.TEXT, race.getMagicAffinity() + "%")));
        lines.addAll(ConfigLines.attributes(handler.getAttributes()));
        return lines;
    }

    private static List<Component> details(EntityRace race, EntityRacePropertiesHandler handler) {
        final List<Component> lines = new ArrayList<>();
        lines.addAll(ConfigLines.effects(handler.getEffectsToAdd(), handler.getEffectsToRemove()));
        lines.addAll(ConfigLines.damageRules(handler.getDamageTypesToIgnore()));
        final RaceConfig<?> config = handler.getConfig();
        if (config != null) {
            mountLines(config, lines);
        }
        final DescriptionVariables traits = new DescriptionVariables();
        handler.describeTraits(traits);
        AbilityLines.appendKeyLines("xat.race." + race.getRegistryName().getPath() + ".trait", TooltipText.TEXT, TooltipText.DETAIL_INDENT, traits, lines);
        return lines;
    }

    private static void mountLines(RaceConfig<?> config, List<Component> lines) {
        if (!config.canMount.get()) {
            lines.add(line(TooltipText.translate("xat.tooltip.race.mount.none", TooltipText.TEXT)));
            return;
        }
        final List<String> listed = List.copyOf(config.mountBlacklist.get());
        if (!listed.isEmpty()) {
            final String key = config.mountWhitelist.get() ? "xat.tooltip.race.mount.only" : "xat.tooltip.race.mount.except";
            lines.add(line(TooltipText.TEXT + TooltipText.strip(I18n.get(key)) + TooltipText.entities(listed, TooltipText.TEXT)));
        }
        if (!config.canControlBoats.get()) {
            lines.add(line(TooltipText.translate("xat.tooltip.race.mount.boat", TooltipText.TEXT)));
        }
    }

    private static Component line(String text) {
        return Component.literal(TooltipText.DETAIL_INDENT + text);
    }

    private RaceTooltips() {
    }
}
