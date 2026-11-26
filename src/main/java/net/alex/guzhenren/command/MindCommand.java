package net.alex.guzhenren.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.alex.guzhenren.gameplay.mind.Brilliance;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /gzr mind} 子命令：修改玩家的才情等级与念、意、情三类思绪池（{@link MindPoolCommand}），结果由 B 面板读取；凡
 * 人同样具备才情与思绪，因此不设开窍门禁。
 */

public final class MindCommand {

    private MindCommand() {}

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode() {
        return Commands.literal("mind")
                .then(buildBrilliance())
                .then(MindPoolCommand.buildNode());
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
