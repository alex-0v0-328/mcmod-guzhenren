package net.alex.guzhenren.gameplay.soul;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.alex.guzhenren.command.ModCommandSupport;
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

    public static ArgumentBuilder<CommandSourceStack, ?> node() {
        return Commands.literal("soul")
                .then(ModCommandSupport.counter("max", SoulService::setMax, SoulService::addMax))
                .then(ModCommandSupport.counter("current", SoulService::setCurrent, SoulService::addCurrent))
                .then(ModCommandSupport.withTargets(Commands.literal("refill"),
                        context -> ModCommandSupport.apply(context, SoulService::refill)));
    }
}
