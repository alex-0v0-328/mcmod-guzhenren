package net.alex.guzhenren.command.root;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.command.ApertureCommand;
import net.alex.guzhenren.command.AwakenCommand;
import net.alex.guzhenren.command.BodyCommand;
import net.alex.guzhenren.command.GuCommand;
import net.alex.guzhenren.command.MindCommand;
import net.alex.guzhenren.command.PathCommand;
import net.alex.guzhenren.command.ResetCommand;
import net.alex.guzhenren.command.SoulCommand;
import net.alex.guzhenren.command.TravelCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 模组指令树根入口：注册 {@code /guzhenren}（及重定向别名 {@code /gzr}）与维度传送指令 {@code /guworld}。
 * <ul>
 *   <li>{@code /guzhenren} 要求权限等级 2，下设 8 个子命令分支（7 个修改玩家附件数据，{@code gu} 修改主手蛊）；玩家状态
 *       统一通过客户端 B 面板查看，不设命令行查询打印分支。</li>
 *   <li>{@code /gzr} 通过 Brigadier {@code redirect} 指向 {@code /guzhenren} 根节点，重定向后的参数全部解析在子上下文中
 *       （参见 {@link net.alex.guzhenren.command.ModEnumArgument}）。</li>
 *   <li>维度进出命令挂载在独立的 {@code /guworld} 根节点下（{@link TravelCommand}）：{@code enter} 要求权限等级 2；处
 *       于宝黄天[Treasure Yellow Heaven]或狐仙福地[Fox Immortal Blissful Land]内的任何玩家均可对自己执行无参
 *       {@code exit} 离开，在四域或指定他人为目标时仍要求权限等级 2。</li>
 * </ul>
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
                        .then(AwakenCommand.buildNode())
                        .then(ResetCommand.buildNode())
                        .then(ApertureCommand.buildNode())
                        .then(BodyCommand.buildNode())
                        .then(SoulCommand.buildNode())
                        .then(PathCommand.buildNode())
                        .then(MindCommand.buildNode())
                        .then(GuCommand.buildNode()));

        dispatcher.register(Commands.literal("gzr")
                .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                .redirect(root));

        dispatcher.register(Commands.literal("guworld")
                .requires(source -> isAdmin(source) || TravelCommand.canExitSelf(source))
                .then(TravelCommand.enterNode().requires(ModCommand::isAdmin))
                .then(TravelCommand.exitNode(ModCommand::isAdmin)));
    }

    private static boolean isAdmin(CommandSourceStack source) {
        return source.hasPermission(PERMISSION_LEVEL);
    }
}
