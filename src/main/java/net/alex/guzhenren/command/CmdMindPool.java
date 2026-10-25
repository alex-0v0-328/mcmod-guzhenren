package net.alex.guzhenren.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.alex.guzhenren.gameplay.mind.MindPoolType;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * The mind pool cells under {@code /gzr mind wisdom}, spelled out one literal at a time.
 *
 * <p>Iterates {@link MindPoolType} and builds a cell per type,
 * each offering {@code current}/{@code max} with {@code set}/{@code add}/{@code sub}/{@code refill}.
 * All writes delegate to {@link MindService}.
 *
 * <p>⚠ Each pool keeps its own literal rather than becoming a value of one enum argument: a settled pool name is
 * never traded for a generic parameter.
 *
 * @author Alex
 * @version 1.0.0
 * @see CmdMind
 * @since 1.0.0
 */

public final class CmdMindPool {

    private CmdMindPool() {}

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode() {
        LiteralArgumentBuilder<CommandSourceStack> wisdom = Commands.literal("wisdom");
        for (MindPoolType type : MindPoolType.values()) {
            wisdom.then(buildCell(type));
        }
        return wisdom;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildCell(MindPoolType type) {
        return Commands.literal(type.getSerializedName())
                .then(ModCommandSupport.buildCounter("current",
                        (player, value) -> MindService.setCurrent(player, type, value),
                        (player, value) -> MindService.addCurrent(player, type, value)))
                .then(ModCommandSupport.buildCounter("max",
                        (player, value) -> MindService.setMax(player, type, value),
                        (player, value) -> MindService.addMax(player, type, value)))
                .then(ModCommandSupport.withTargets(Commands.literal("refill"),
                        context -> ModCommandSupport.apply(context, player -> MindService.refill(player, type))));
    }
}
