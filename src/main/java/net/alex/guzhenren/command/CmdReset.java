package net.alex.guzhenren.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.alex.guzhenren.lifecycle.PlayerDataService;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /gzr reset}：把玩家恢复到出生时的状态。
 *
 * <ul>
 *   <li>{@link PlayerDataService#resetAll} 后调 {@link net.alex.guzhenren.command.ModCommandSupport#refreshCommands}：
 *       重置翻转开窍[Awakening]门槛，客户端须见新命令树。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see CmdAwaken
 * @since 1.0.0
 */

public final class CmdReset {

    private CmdReset() {}

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode() {
        return ModCommandSupport.withTargets(Commands.literal("reset"),
                context -> ModCommandSupport.apply(context, CmdReset::reset));
    }

    private static void reset(ServerPlayer player) {
        ApertureStorageMenu.closeFor(player);
        PlayerDataService.resetAll(player);
        ModCommandSupport.refreshCommands(player);
    }
}
