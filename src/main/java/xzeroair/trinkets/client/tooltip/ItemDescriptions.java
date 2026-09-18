package xzeroair.trinkets.client.tooltip;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.init.ModItems;
import xzeroair.trinkets.items.base.RaceFood;
import xzeroair.trinkets.items.foods.ManaCrystal;
import xzeroair.trinkets.items.foods.ManaReagent;
import xzeroair.trinkets.items.foods.RestorationSerum;
import xzeroair.trinkets.traits.abilities.DescriptionVariables;
import xzeroair.trinkets.traits.elements.Element;
import xzeroair.trinkets.traits.elements.IElementProvider;
import xzeroair.trinkets.util.Reference;
import xzeroair.trinkets.util.config.TrinketsConfig;
import xzeroair.trinkets.util.config.server.ManaConfig;

/**
 * 物品自身的说明行（在能力与种族分组之前）：语言键 item.名称.tooltipN（方块物品为 xat.tile.名称.tooltipN，种族食物共用
 * xat.tooltip.race_food.tooltipN）与元素。
 * 魔力类食物与种族食物的数值取自配置与物品常量。
 */
@OnlyIn(Dist.CLIENT)
public final class ItemDescriptions {

    public static void append(ItemStack stack, ResourceLocation id, @Nullable Player player, List<Component> lines) {
        final DescriptionVariables variables = new DescriptionVariables();
        describe(stack, player, variables);
        final String prefix = stack.getItem() instanceof RaceFood ? Reference.MODID + ".tooltip.race_food"
                : stack.getItem() instanceof BlockItem ? Reference.MODID + ".tile." + id.getPath() : "item." + id.getPath();
        AbilityLines.appendKeyLines(prefix + ".tooltip", TooltipText.TEXT, "", variables, lines);
        if (stack.getItem() instanceof IElementProvider provider && TrinketsConfig.CLIENT.render.renderElements.get()) {
            final Element element = provider.getPrimaryElement(stack);
            if (!element.isNone()) {
                lines.add(Component.literal(TooltipText.translate("xat.tooltip.element", TooltipText.TEXT,
                        TooltipText.strip(element.getDisplayName().getString()))));
            }
        }
    }

    private static void describe(ItemStack stack, @Nullable Player player, DescriptionVariables variables) {
        final ManaConfig mana = TrinketsConfig.SERVER.magic;
        if (stack.getItem() instanceof ManaCrystal || stack.getItem() instanceof ManaReagent) {
            final MagicStats magic = player == null ? null : MagicStats.get(player);
            variables.number("mpmax", mana.bonusPerPoint.get())
                    .number("points", magic == null ? 0 : magic.getBonusMana())
                    .number("maxpoints", mana.bonusMax.get())
                    .oneIn("shatter", mana.crystalExplodes.get(), ManaCrystal.SHATTER_CHANCE)
                    .number("power", ManaCrystal.EXPLOSION_POWER)
                    .translated("reagent", true, ModItems.MANA_REAGENT.get().getDescriptionId())
                    .effects("harm", mana.reagentHarmful.get(), harm(ManaReagent.POISON_TICKS), true);
        } else if (stack.getItem() instanceof RestorationSerum) {
            variables.effects("harm", mana.reagentHarmful.get(), harm(RestorationSerum.POISON_TICKS), true);
        } else if (stack.getItem() instanceof RaceFood food) {
            variables.flag("transform", TrinketsConfig.SERVER.food.transformationEffects.get())
                    .option("race", TooltipText.strip(food.getRace().getDisplayName().getString()))
                    .translated("serum", true, ModItems.RESTORATION_SERUM.get().getDescriptionId())
                    .seconds("cooldown", true, RaceFood.COOLDOWN_TICKS);
        }
    }

    /** 魔力试剂与复原血清的副作用（ManaReagent#inflictHarm） */
    private static List<String> harm(int poisonTicks) {
        return List.of("minecraft:poison:" + poisonTicks + ":" + ManaReagent.HARM_AMPLIFIER,
                "minecraft:weakness:" + ManaReagent.WEAKNESS_TICKS + ":" + ManaReagent.HARM_AMPLIFIER);
    }

    private ItemDescriptions() {
    }
}
