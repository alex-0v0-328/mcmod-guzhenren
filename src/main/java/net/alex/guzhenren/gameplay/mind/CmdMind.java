package net.alex.guzhenren.gameplay.mind;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.alex.guzhenren.command.ModCommandSupport;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /gzr mind}: writes brilliance [才情] and the three mind pools; {@code /gzr info} reads them.
 *
 * <p>Offers graded set/up/down for brilliance [才情] and delegates to
 * {@link CmdMindPool} for the three wisdom pools. All of
 * {@code /gzr mind} is ungated -- a mortal thinks too.
 *
 * @author Alex
 * @version 1.0.0
 * @see CmdMindPool
 * @since 1.0.0
 */

public final class CmdMind {

    private CmdMind() {}

    public static ArgumentBuilder<CommandSourceStack, ?> node() {
        return Commands.literal("mind")
                .then(brilliance())
                .then(CmdMindPool.node());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> brilliance() {
        return ModCommandSupport.enumSetNode("brilliance", Brilliance.values(), MindService::setBrilliance,
                        ModCommandSupport.ANYONE, null)
                .then(shift("up", 1))
                .then(shift("down", -1));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> shift(String literal, int delta) {
        return ModCommandSupport.withTargets(Commands.literal(literal),
                context -> ModCommandSupport.apply(context, player -> MindService.shiftBrilliance(player, delta)));
    }
}
