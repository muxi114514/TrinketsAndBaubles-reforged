package xzeroair.trinkets.commands;

import javax.annotation.Nullable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import xzeroair.trinkets.capabilities.magic.MagicStats;
import xzeroair.trinkets.capabilities.race.EntityProperties;
import xzeroair.trinkets.capabilities.race.RaceCache;
import xzeroair.trinkets.init.ModElements;
import xzeroair.trinkets.init.ModRaces;
import xzeroair.trinkets.network.NetworkHandler;
import xzeroair.trinkets.network.OpenRaceSelectionPacket;
import xzeroair.trinkets.races.EntityRace;
import xzeroair.trinkets.traits.elements.Element;

/**
 * 管理指令 /xat &lt;玩家&gt; mana|race ...。对应 1.12 commands/CommandMain，子命令与权限等级（4）不变：
 * <ul>
 * <li>mana refill | set &lt;魔力&gt; | resetBonus</li>
 * <li>race setRace | setImbuedRace &lt;种族&gt; [元素]；reset | resetImbued；gui（授权 60 秒并打开种族选择界面）</li>
 * </ul>
 * 移植说明：1.12 手写参数解析且要求执行者必须是玩家（控制台无法使用）；改为 Brigadier 参数与补全，控制台同样可用，并给出执行反馈。
 */
public final class TrinketsCommand {

    private static final DynamicCommandExceptionType UNKNOWN_RACE = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.xat.race.unknown", String.valueOf(id)));
    private static final DynamicCommandExceptionType UNKNOWN_ELEMENT = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.xat.element.unknown", String.valueOf(id)));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("xat")
                .requires(source -> source.hasPermission(4))
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("/xat <player> <mana | race> <args>"), false);
                    return 0;
                })
                .then(Commands.argument("player", EntityArgument.player())
                        .then(mana())
                        .then(race())));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> mana() {
        return Commands.literal("mana")
                .then(Commands.literal("refill").executes(context -> withMana(context, "refill", MagicStats::refillMana)))
                .then(Commands.literal("set").then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                        .executes(context -> withMana(context, "set",
                                stats -> stats.setMana(FloatArgumentType.getFloat(context, "amount"))))))
                .then(Commands.literal("resetBonus").executes(context -> withMana(context, "reset_bonus", stats -> stats.setBonusMana(0))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> race() {
        return Commands.literal("race")
                .then(setRace("setRace", "set", false))
                .then(setRace("setImbuedRace", "set_imbued", true))
                .then(Commands.literal("reset").executes(context -> withRace(context, "reset", properties -> properties.setOriginalRaceCache(null))))
                .then(Commands.literal("resetImbued").executes(context -> withRace(context, "reset_imbued",
                        properties -> properties.setImbuedRaceCache(null))))
                .then(Commands.literal("gui").executes(context -> withRace(context, "gui", properties -> {
                    properties.authorizeRaceSelection();
                    NetworkHandler.sendTo(new OpenRaceSelectionPacket(false), EntityArgument.getPlayer(context, "player"));
                })));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> setRace(String name, String feedback, boolean imbued) {
        return Commands.literal(name).then(Commands.argument("race", ResourceLocationArgument.id())
                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(ModRaces.registry().getKeys(), builder))
                .executes(context -> applyRace(context, feedback, imbued, null))
                .then(Commands.argument("element", ResourceLocationArgument.id())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(ModElements.registry().getKeys(), builder))
                        .executes(context -> applyRace(context, feedback, imbued, ResourceLocationArgument.getId(context, "element")))));
    }

    private static int applyRace(CommandContext<CommandSourceStack> context, String feedback, boolean imbued,
            @Nullable ResourceLocation elementId) throws CommandSyntaxException {
        final ResourceLocation raceId = ResourceLocationArgument.getId(context, "race");
        final EntityRace race = ModRaces.registry().getValue(raceId);
        if (race == null) {
            throw UNKNOWN_RACE.create(raceId);
        }
        final Element element = elementId == null ? null : ModElements.registry().getValue(elementId);
        if (elementId != null && element == null) {
            throw UNKNOWN_ELEMENT.create(elementId);
        }
        final RaceCache cache = element == null ? new RaceCache(race) : new RaceCache(race, element);
        return withRace(context, feedback, properties -> {
            if (imbued) {
                properties.setImbuedRaceCache(cache);
            } else {
                properties.setOriginalRaceCache(cache);
            }
        });
    }

    private static int withMana(CommandContext<CommandSourceStack> context, String feedback, CommandAction<MagicStats> action)
            throws CommandSyntaxException {
        final ServerPlayer player = EntityArgument.getPlayer(context, "player");
        final MagicStats stats = MagicStats.get(player);
        if (stats == null) {
            return 0;
        }
        action.run(stats);
        context.getSource().sendSuccess(() -> Component.translatable("commands.xat.mana." + feedback, player.getDisplayName()), true);
        return 1;
    }

    private static int withRace(CommandContext<CommandSourceStack> context, String feedback, CommandAction<EntityProperties> action)
            throws CommandSyntaxException {
        final ServerPlayer player = EntityArgument.getPlayer(context, "player");
        final EntityProperties properties = EntityProperties.get(player);
        if (properties == null) {
            return 0;
        }
        action.run(properties);
        context.getSource().sendSuccess(() -> Component.translatable("commands.xat.race." + feedback, player.getDisplayName()), true);
        return 1;
    }

    @FunctionalInterface
    private interface CommandAction<T> {
        void run(T target) throws CommandSyntaxException;
    }

    private TrinketsCommand() {
    }
}
