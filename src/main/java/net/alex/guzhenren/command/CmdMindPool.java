package net.alex.guzhenren.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.alex.guzhenren.gameplay.mind.MindPoolType;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /gzr mind wisdom} 下各池节点：每种 {@link MindPoolType} 一个，写入全交给 {@link MindService}。
 *
 * <ul>
 *   <li>⚠ 每池保留自己的字面量，不并成一个枚举参数：已定的池名不换成泛化参数。</li>
 * </ul>
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
