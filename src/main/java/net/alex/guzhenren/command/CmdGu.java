package net.alex.guzhenren.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.alex.guzhenren.item.gu.stack.GuCondition;
import net.alex.guzhenren.item.gu.tending.GuHealth;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /gzr gu} 子命令：{@code condition set <normal|injured|dying>} 把目标玩家主手的需照顾蛊设为该伤势档并取满次数
 * （{@link GuHealth#set}）；主手不是持续照顾型蛊[Tended Gu]的目标被拒。目前濒死只能经此或炼蛊失败得到。
 */

public final class CmdGu {

    private static final String FAILED_NO_TENDED_GU = "guzhenren.command.failed.no_tended_gu";

    private CmdGu() {}

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode() {
        return Commands.literal("gu")
                .then(ModCommandSupport.buildEnumSetNode("condition", GuCondition.Stage.values(),
                        (player, stage) -> GuHealth.set(player.getMainHandItem(), stage),
                        CmdGu::holdsTendedGu, FAILED_NO_TENDED_GU));
    }

    private static boolean holdsTendedGu(ServerPlayer player) {
        return player.getMainHandItem().getItem() instanceof TendedGuItem;
    }
}
