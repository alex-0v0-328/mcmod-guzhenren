package net.alex.guzhenren.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import net.alex.guzhenren.gameplay.dimension.DimensionTravelService;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * {@code /guworld enter <dimension> [targets]} 与 {@code /guworld exit [targets]}：进出本模组的锚定维度。
 *
 * <ul>
 *   <li>{@code enter} 仅限管理员；宝黄天[Treasure Yellow Heaven]内任何玩家可用无目标 {@code exit}，守卫不留别的出路；
 *       {@code exit <targets>} 与从其他锚定维度退出仍限管理员；{@code exit} 不带维度，离开当前锚定维度。</li>
 *   <li>门槛读所在维度，命令树须随跨维度更新：原版换世界即重发（{@code ServerPlayer#changeDimension} 发送权限等级，
 *       进而重发命令）。</li>
 *   <li>{@code enter} 只收 {@link ModDimensions#ANCHORED_DIMENSIONS} 所列维度，即玩家应经本模组离开的维度，返回点由
 *       {@link DimensionTravelService} 记录；原版维度间跳转仍用 {@code /execute in}。</li>
 *   <li>已列出但服务端无其世界的维度（未装 Gu World）用专门消息拒绝。</li>
 *   <li>⚠ 翻译参数只能是组件、数字或字符串：id 以文本传入。</li>
 * </ul>
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

    public static ArgumentBuilder<CommandSourceStack, ?> exitNode(Predicate<CommandSourceStack> admin) {
        return Commands.literal("exit")
                .requires(source -> admin.test(source) || isInsideHeaven(source))
                .executes(CmdTravel::exit)
                .then(Commands.argument(ModCommandSupport.ARG_TARGETS, EntityArgument.players())
                        .requires(admin)
                        .executes(CmdTravel::exit));
    }

    public static boolean isInsideHeaven(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player
                && DimensionTravelService.isInside(player, ModDimensions.TREASURE_YELLOW_HEAVEN);
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
            ModCommandFeedback.sendFailure(source,
                    Component.translatable("guzhenren.command.travel.unknown_dimension", id.toString()));
            return 0;
        }
        if (source.getServer().getLevel(dimension) == null) {
            ModCommandFeedback.sendFailure(source,
                    Component.translatable("guzhenren.command.travel.missing_dimension", id.toString()));
            return 0;
        }
        Component name = Component.translatable(Util.makeDescriptionId("dimension", id));

        ModCommandSupport.Outcome outcome = ModCommandSupport.run(context,
                player -> !DimensionTravelService.isInside(player, dimension)
                        && DimensionTravelService.enter(player, destination));
        if (!outcome.refused().isEmpty()) {
            ModCommandFeedback.sendFailure(source,
                    Component.translatable("guzhenren.command.travel.already_inside", outcome.refusedNames(), name));
        }
        if (!outcome.done().isEmpty()) {
            ModCommandFeedback.sendSuccess(source,
                    Component.translatable("guzhenren.command.travel.entered", outcome.doneNames(), name));
        }
        return outcome.done().size();
    }

    private static int exit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ModCommandSupport.Outcome outcome = ModCommandSupport.run(context, player -> {
            if (!ModDimensions.ANCHORED_DIMENSIONS.containsKey(player.level().dimension())) return false;
            DimensionTravelService.exit(player);
            return true;
        });

        if (!outcome.refused().isEmpty()) {
            ModCommandFeedback.sendFailure(source,
                    Component.translatable("guzhenren.command.travel.not_inside", outcome.refusedNames()));
        }
        if (!outcome.done().isEmpty()) {
            ModCommandFeedback.sendSuccess(source,
                    Component.translatable("guzhenren.command.travel.exited", outcome.doneNames()));
        }
        return outcome.done().size();
    }
}
