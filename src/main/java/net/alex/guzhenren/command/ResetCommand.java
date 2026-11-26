package net.alex.guzhenren.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.alex.guzhenren.lifecycle.PlayerDataService;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /gzr reset} 子命令：将目标玩家的全部模组数据重置回初始未开窍[Awakening]状态
 * （{@link PlayerDataService#resetAll}），并立即调用 {@link ModCommandSupport#refreshCommands} 向客户端重发命令树以隐藏
 * 需开窍的子命令。
 */

public final class ResetCommand {

    private ResetCommand() {}

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode() {
        return ModCommandSupport.withTargets(Commands.literal("reset"),
                context -> ModCommandSupport.apply(context, ResetCommand::reset));
    }

    private static void reset(ServerPlayer player) {
        ApertureStorageMenu.closeFor(player);
        PlayerDataService.resetAll(player);
        ModCommandSupport.refreshCommands(player);
    }
}
