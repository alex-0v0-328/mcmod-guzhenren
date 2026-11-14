package net.alex.guzhenren.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.alex.guzhenren.gameplay.mind.Brilliance;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /gzr mind}：写才情与念、意、情三池，B 面板读取；不设门槛，凡人也会思考。
 *
 * @author Alex
 * @version 1.0.0
 * @see CmdMindPool
 * @since 1.0.0
 */

public final class CmdMind {

    private CmdMind() {}

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode() {
        return Commands.literal("mind")
                .then(buildBrilliance())
                .then(CmdMindPool.buildNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildBrilliance() {
        return ModCommandSupport.buildEnumSetNode("brilliance", Brilliance.values(), MindService::setBrilliance,
                        ModCommandSupport.ANYONE, null)
                .then(shift("up", 1))
                .then(shift("down", -1));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> shift(String literal, int delta) {
        return ModCommandSupport.withTargets(Commands.literal(literal),
                context -> ModCommandSupport.apply(context, player -> MindService.shiftBrilliance(player, delta)));
    }
}
