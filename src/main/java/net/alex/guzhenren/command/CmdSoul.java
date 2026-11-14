package net.alex.guzhenren.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /gzr soul}：独立于肉身写魂魄池，B 面板读取；整个分支不设门槛，凡人也有魂魄。
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
