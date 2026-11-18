package net.alex.guzhenren.core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家动作栏单行翻译提示工具（用于蛊虫催动、菜单点击与修炼心跳反馈）：{@link #refuse} 以红色字体提示操作被拒绝或未发
 * 生，{@link #inform} 以默认颜色提示状态已更新。
 */

public final class ActionBar {

    private ActionBar() {}

    public static void refuse(@NotNull Player player, @NotNull String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED), true);
    }

    public static void inform(@NotNull Player player, @NotNull String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }
}
