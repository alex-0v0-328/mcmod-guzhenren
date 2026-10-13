package net.alex.guzhenren.gameplay.dimension;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.alex.guzhenren.command.ModCommandFeedback;
import net.alex.guzhenren.command.ModCommandSupport;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * {@code /guworld enter <dimension> [targets]} and {@code /guworld exit [targets]}: admin travel to
 * and from the mod's anchored dimensions.
 *
 * <p>{@code enter} only accepts dimensions listed in {@link ModDimensions#ANCHORED_DIMENSIONS} -- an
 * anchored dimension is one the player is meant to leave through us, with the return point recorded
 * by {@link DimensionTravelService}. Vanilla dimension hops stay with vanilla {@code /execute in}.
 * {@code exit} takes no dimension: the player leaves whichever anchored dimension they are inside.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.dimension.DimensionTravelService
 * @since 1.0.0
 */

public final class CmdTravel {

    private CmdTravel() {}

    private static final String ARG_DIMENSION = "dimension";

    public static ArgumentBuilder<CommandSourceStack, ?> enterNode() {
        return Commands.literal("enter")
                .then(ModCommandSupport.withTargets(
                        Commands.argument(ARG_DIMENSION, ResourceLocationArgument.id())
                                .suggests(CmdTravel::suggestAnchoredDimensions),
                        CmdTravel::enter));
    }

    public static ArgumentBuilder<CommandSourceStack, ?> exitNode() {
        return ModCommandSupport.withTargets(Commands.literal("exit"), CmdTravel::exit);
    }

    private static CompletableFuture<Suggestions> suggestAnchoredDimensions(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(
                ModDimensions.ANCHORED_DIMENSIONS.keySet().stream().map(ResourceKey::location), builder);
    }

    private static int enter(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ResourceLocation id = ResourceLocationArgument.getId(context, ARG_DIMENSION);
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, id);
        ModDimensions.AnchoredDimension destination = ModDimensions.ANCHORED_DIMENSIONS.get(dimension);
        if (destination == null) {
            ModCommandFeedback.failure(source,
                    Component.translatable("guzhenren.command.travel.unknown_dimension", id));
            return 0;
        }
        Component name = Component.translatable(Util.makeDescriptionId("dimension", id));

        List<ServerPlayer> refused = new ArrayList<>();
        List<ServerPlayer> sent = new ArrayList<>();
        for (ServerPlayer player : ModCommandSupport.targets(context)) {
            if (DimensionTravelService.isInside(player, dimension)
                    || !DimensionTravelService.enter(player, destination)) {
                refused.add(player);
            } else {
                sent.add(player);
            }
        }

        if (!refused.isEmpty()) {
            ModCommandFeedback.failure(source,
                    Component.translatable("guzhenren.command.travel.already_inside", names(refused), name));
        }
        if (!sent.isEmpty()) {
            ModCommandFeedback.success(source,
                    Component.translatable("guzhenren.command.travel.entered", names(sent), name));
        }
        return sent.size();
    }

    private static int exit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        List<ServerPlayer> refused = new ArrayList<>();
        List<ServerPlayer> returned = new ArrayList<>();

        for (ServerPlayer player : ModCommandSupport.targets(context)) {
            if (!ModDimensions.ANCHORED_DIMENSIONS.containsKey(player.level().dimension())) {
                refused.add(player);
                continue;
            }
            DimensionTravelService.exit(player);
            returned.add(player);
        }

        if (!refused.isEmpty()) {
            ModCommandFeedback.failure(source,
                    Component.translatable("guzhenren.command.travel.not_inside", names(refused)));
        }
        if (!returned.isEmpty()) {
            ModCommandFeedback.success(source,
                    Component.translatable("guzhenren.command.travel.exited", names(returned)));
        }
        return returned.size();
    }

    private static Component names(List<ServerPlayer> players) {
        return ComponentUtils.formatList(players, ServerPlayer::getDisplayName);
    }
}
