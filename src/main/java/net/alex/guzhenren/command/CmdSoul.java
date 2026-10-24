package net.alex.guzhenren.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /gzr soul}: writes the soul [魂魄] pool independently of the body [肉身]; {@code /gzr info} reads it.
 *
 * <p>Offers set/add/sub for the current and maximum soul, plus a refill action. A mortal has a soul,
 * so the entire branch is ungated.
 *
 * @author Alex
 * @version 1.0.0
 * @see SoulService
 * @since 1.0.0
 */

public final class CmdSoul {

    private CmdSoul() {}

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode() {
        return Commands.literal("soul")
                .then(ModCommandSupport.buildCounter("max", SoulService::setMax, SoulService::addMax))
                .then(ModCommandSupport.buildCounter("current", SoulService::setCurrent, SoulService::addCurrent))
                .then(ModCommandSupport.withTargets(Commands.literal("refill"),
                        context -> ModCommandSupport.apply(context, SoulService::refill)));
    }
}
