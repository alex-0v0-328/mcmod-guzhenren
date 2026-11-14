package net.alex.guzhenren.command.root;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.command.CmdAperture;
import net.alex.guzhenren.command.CmdAwaken;
import net.alex.guzhenren.command.CmdBody;
import net.alex.guzhenren.command.CmdMind;
import net.alex.guzhenren.command.CmdPath;
import net.alex.guzhenren.command.CmdReset;
import net.alex.guzhenren.command.CmdSoul;
import net.alex.guzhenren.command.CmdTravel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /guzhenren}（别名 {@code /gzr}）的根，挂载全部子命令，需权限等级 2。
 *
 * <ul>
 *   <li>七个根分支都写附件数据；玩家状态在 B 面板读，无分支打印。</li>
 *   <li>{@code gzr} 是指向根的 {@code redirect}，其后输入都解析进子上下文。</li>
 *   <li>世界环境命令在独立的 {@code guworld} 根：{@link CmdTravel} 的 {@code enter}（等级 2）与 {@code exit}；
 *       任何等级的玩家可在宝黄天[Treasure Yellow Heaven]内对自己用 {@code exit}，别处或对他人限等级 2。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.command.ModEnumArgument
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ModCommand {

    private ModCommand() {}

    private static final int PERMISSION_LEVEL = 2;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(
                Commands.literal("guzhenren")
                        .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                        .then(CmdAwaken.buildNode())
                        .then(CmdReset.buildNode())
                        .then(CmdAperture.buildNode())
                        .then(CmdBody.buildNode())
                        .then(CmdSoul.buildNode())
                        .then(CmdPath.buildNode())
                        .then(CmdMind.buildNode()));

        dispatcher.register(Commands.literal("gzr")
                .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                .redirect(root));

        dispatcher.register(Commands.literal("guworld")
                .requires(source -> isAdmin(source) || CmdTravel.isInsideHeaven(source))
                .then(CmdTravel.enterNode().requires(ModCommand::isAdmin))
                .then(CmdTravel.exitNode(ModCommand::isAdmin)));
    }

    private static boolean isAdmin(CommandSourceStack source) {
        return source.hasPermission(PERMISSION_LEVEL);
    }
}
