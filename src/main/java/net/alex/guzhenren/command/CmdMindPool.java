package net.alex.guzhenren.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.alex.guzhenren.gameplay.mind.MindPoolType;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /gzr mind wisdom} 子命令分支：为每种思绪池类型（{@link MindPoolType}）挂载独立的字面量节点（{@code current}、
 * {@code max}、{@code refill}），并将所有数值修改委托给 {@link MindService}。
 * <ul>
 *   <li>⚠ 每种思绪池固定注册为独立的字面量节点而非合并为单个枚举参数，以保持既有命令路径稳定。</li>
 * </ul>
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
