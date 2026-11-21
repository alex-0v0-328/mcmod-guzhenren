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
 * {@code /guworld enter <dimension> [targets]} 与 {@code /guworld exit [targets]} 子命令：进出本模组登记的锚定维度。
 * <ul>
 *   <li>权限规则：{@code enter} 仅限管理员使用；处于 {@link ModDimensions#allowsSelfExit} 维度（宝黄天[Treasure Yellow
 *       Heaven]、狐仙福地[Fox Immortal Blissful Land]）内的任何玩家均可对自己执行无参 {@code exit} 离开（宝黄天的守卫禁
 *       止其他离开方式，福地没有门户），而带目标参数的 {@code exit <targets>} 或从四域退出仍仅限管理员；原版跨维度传送（{@code ServerPlayer#changeDimension}）会自动重发权限与命令树，确保 {@code exit}
 *       可见性实时更新。</li>
 *   <li>{@code enter} 仅接受 {@link ModDimensions#ANCHORED_DIMENSIONS} 中登记的锚定维度，由
 *       {@link DimensionTravelService} 记录进入前的返回锚点；若目标维度已登记但服务端未加载对应世界（如未安装 Gu
 *       World），则返回专属缺失维度提示。</li>
 *   <li>⚠ 构造翻译组件时参数只能传入 {@link Component}、数字或字符串，因此维度 {@link ResourceLocation} 必须先转为字符
 *       串传入。</li>
 * </ul>
 */

public final class CmdTravel {

    private CmdTravel() {}

    private static final String ARG_DIMENSION = "dimension";

    public static ArgumentBuilder<CommandSourceStack, ?> enterNode() {
        return Commands.literal("enter")
                .then(ModCommandSupport.withTargets(
                        Commands.argument(ARG_DIMENSION, ResourceLocationArgument.id())
                                .suggests((context, builder) -> suggestAnchoredDimensions(builder)),
                        CmdTravel::enter));
    }

    public static ArgumentBuilder<CommandSourceStack, ?> exitNode(Predicate<CommandSourceStack> admin) {
        return Commands.literal("exit")
                .requires(source -> admin.test(source) || canExitSelf(source))
                .executes(CmdTravel::exit)
                .then(Commands.argument(ModCommandSupport.ARG_TARGETS, EntityArgument.players())
                        .requires(admin)
                        .executes(CmdTravel::exit));
    }

    public static boolean canExitSelf(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player
                && ModDimensions.allowsSelfExit(player.level().dimension());
    }

    private static CompletableFuture<Suggestions> suggestAnchoredDimensions(SuggestionsBuilder builder) {
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
