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
import net.minecraftforge.fml.ModList;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.traits.AbilityHandler;
import xzeroair.trinkets.traits.AbilityHandler.AbilityHolder;
import xzeroair.trinkets.traits.abilities.interfaces.IAbilityInterface;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.util.config.TrinketsConfig;

/**
 * 把一组能力按「被动 / 主动 / 联动·模组名」分组加入说明。
 * 实时状态取自玩家身上「同一来源」正在生效的能力实例（未佩戴时不显示状态）。
 */
@OnlyIn(Dist.CLIENT)
public final class AbilityTooltips {

    public static String passiveTitle() {
        return I18n.get("xat.tooltip.section.passive");
    }

    public static String activeTitle() {
        return I18n.get("xat.tooltip.section.active");
    }

    /**
     * @param sourceId 能力来源（物品注册名或种族注册名），用于匹配玩家身上的实例与服务端禁用令
     * @param element  来源当前的元素形态；限定了其它元素的能力跳过
     */
    public static void append(TooltipSections sections, List<IAbilityInterface> abilities, String sourceId, ItemStack source,
            @Nullable Player player, @Nullable Element element) {
        final EntityProperties properties = player == null ? null : EntityProperties.get(player);
        final AbilityHandler handler = properties == null ? null : properties.getAbilityHandler();
        final MagicStats magic = player == null ? null : MagicStats.get(player);
        sections.group(passiveTitle()).group(activeTitle());
        for (IAbilityInterface ability : abilities) {
            final Element required = ability.getRequiredElement();
            if (required != null && element != null && required != element) {
                continue;
            }
            final String key = ability.getRegistryName().toString();
            final boolean disabled = !ability.isAbilityEnabled() || (handler != null && handler.hasKillOrder(sourceId, key));
            final List<Component> lines = new ArrayList<>();
            lines.add(AbilityLines.header(ability, disabled, magic));
            if (!disabled) {
                AbilityLines.appendDetails(ability, lines);
                final IAbilityInterface live = liveInstance(handler, key, sourceId);
                if (live != null && TrinketsConfig.CLIENT.tooltip.showStatus.get()) {
                    AbilityLines.appendStatus(live, source, lines);
                }
            }
            sections.addEntry(groupTitle(ability), AbilityLines.name(ability), disabled, lines);
        }
    }

    /** 能力界面等只需单个能力的完整说明时使用 */
    public static List<Component> describe(IAbilityInterface ability, @Nullable IAbilityInterface live, ItemStack source,
            @Nullable MagicStats magic) {
        final List<Component> lines = new ArrayList<>();
        lines.add(AbilityLines.header(ability, !ability.isAbilityEnabled(), magic));
        AbilityLines.appendDetails(ability, lines);
        if (live != null && TrinketsConfig.CLIENT.tooltip.showStatus.get()) {
            AbilityLines.appendStatus(live, source, lines);
        }
        return lines;
    }

    private static String groupTitle(IAbilityInterface ability) {
        final String modId = ability.getCompatModId();
        if (modId != null) {
            final String modName = ModList.get().getModContainerById(modId)
                    .map(container -> container.getModInfo().getDisplayName()).orElse(modId);
            return I18n.get("xat.tooltip.section.compat", modName);
        }
        return ability.isActiveAbility() ? activeTitle() : passiveTitle();
    }

    @Nullable
    private static IAbilityInterface liveInstance(@Nullable AbilityHandler handler, String key, String sourceId) {
        if (handler == null) {
            return null;
        }
        final AbilityHolder holder = handler.getActiveAbilities().get(key);
        return holder != null && sourceId.equals(holder.getSourceID()) ? holder.getAbility() : null;
    }

    private AbilityTooltips() {
    }
}
