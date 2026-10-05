package net.alex.guzhenren.gameplay.path.strength;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.alex.guzhenren.command.ModCommandSupport;
import net.alex.guzhenren.command.ModEnumArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /gzr path strength}: writes the beast and human strengths a body holds; {@code /gzr info}
 * reads them.
 *
 * <p>Offers {@code grant}/{@code revoke}/{@code clear} for beast strengths and
 * {@code set}/{@code add}/{@code sub} for human strengths, read from an {@code int} argument so the
 * {@code (int)} narrowing is exact. All writes delegate to {@link PathStrengthService}, which is also the
 * one funnel that refreshes the attack modifier.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.command.ModEnumArgument
 * @since 1.0.0
 */

public final class CmdStrength {

    private CmdStrength() {}

    private static final String ARG_KIND = "kind";

    public static ArgumentBuilder<CommandSourceStack, ?> node() {
        return Commands.literal("strength")
                .then(beastNode("grant", PathStrengthService::grant))
                .then(beastNode("revoke", PathStrengthService::revoke))
                .then(humanStrength())
                .then(ModCommandSupport.withTargets(Commands.literal("clear"),
                        context -> ModCommandSupport.apply(context, PathStrengthService::clear)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> beastNode(
            String literal, ModCommandSupport.EnumOperation<BeastStrength> operation) {
        return Commands.literal(literal).then(ModCommandSupport.withTargets(
                ModEnumArgument.arg(ModCommandSupport.ARG_VALUE, BeastStrength.values()),
                context -> {
                    BeastStrength beast = ModEnumArgument.get(
                            context, ModCommandSupport.ARG_VALUE, BeastStrength.values());
                    return ModCommandSupport.apply(context, player -> operation.apply(player, beast));
                }));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> humanStrength() {
        return Commands.literal("human").then(ModCommandSupport.enumCounter(ARG_KIND, HumanStrength.values(),
                IntegerArgumentType.integer(),
                (player, kind, amount) -> PathStrengthService.setHumanStrength(player, kind, (int) amount),
                (player, kind, amount) -> PathStrengthService.addHumanStrength(player, kind, (int) amount)));
    }
}
