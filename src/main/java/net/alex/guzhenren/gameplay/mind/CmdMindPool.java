package net.alex.guzhenren.gameplay.mind;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.alex.guzhenren.command.ModCommandSupport;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * The mind pool cells under {@code /gzr mind wisdom}, spelled out one literal at a time.
 *
 * <p>Iterates {@link MindPoolType} and builds a cell per type,
 * each offering {@code current}/{@code max} with {@code set}/{@code add}/{@code sub}/{@code refill}.
 * All writes delegate to {@link MindService}.
 *
 * <p>⚠ They are literals rather than a single enum argument so that brilliance [才情] can sit beside
 * them as a sibling. Tidying this into one enum argument takes that away.
 *
 * @author Alex
 * @version 1.0.0
 * @see CmdMind
 * @since 1.0.0
 */

public final class CmdMindPool {

    private CmdMindPool() {}

    public static ArgumentBuilder<CommandSourceStack, ?> node() {
        LiteralArgumentBuilder<CommandSourceStack> wisdom = Commands.literal("wisdom");
        for (MindPoolType type : MindPoolType.values()) {
            wisdom.then(cell(type));
        }
        return wisdom;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> cell(MindPoolType type) {
        return Commands.literal(type.getSerializedName())
                .then(Commands.literal("current")
                        .then(ModCommandSupport.longNode("set",
                                (player, value) -> MindService.setCurrent(player, type, value)))
                        .then(ModCommandSupport.longNode("add",
                                (player, value) -> MindService.addCurrent(player, type, value)))
                        .then(ModCommandSupport.longNode("sub",
                                (player, value) -> MindService.addCurrent(player, type, -value))))
                .then(Commands.literal("max")
                        .then(ModCommandSupport.longNode("set",
                                (player, value) -> MindService.setMax(player, type, value)))
                        .then(ModCommandSupport.longNode("add",
                                (player, value) -> MindService.addMax(player, type, value)))
                        .then(ModCommandSupport.longNode("sub",
                                (player, value) -> MindService.addMax(player, type, -value))))
                .then(ModCommandSupport.withTargets(Commands.literal("refill"),
                        context -> ModCommandSupport.apply(context, player -> MindService.refill(player, type))));
    }
}
