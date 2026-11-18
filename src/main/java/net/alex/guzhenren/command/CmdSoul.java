package net.alex.guzhenren.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /gzr soul} 子命令：独立于肉身状态修改目标玩家的魂魄上限与当前值（{@link SoulService}），结果由 B 面板读取；凡
 * 人同样具有魂魄，因此不设开窍门禁。
 */

public final class CmdSoul {

    private CmdSoul() {}

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode() {
        return Commands.literal("soul")
                .then(ModCommandSupport.buildCounter("max", SoulService::setMax, SoulService::addMax))
                .then(ModCommandSupport.buildCounter("current", SoulService::setCurrent, SoulService::addCurrent))
                .then(ModCommandSupport.withTargets(Commands.literal("refill"),
                        context -> ModCommandSupport.apply(context, SoulService::refill)));
    }
}
