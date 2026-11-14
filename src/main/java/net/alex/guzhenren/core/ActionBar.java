package net.alex.guzhenren.core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 动作栏的一行翻译提示（催动、菜单点击、心跳步骤）：{@link #refuse} 红字报未发生，{@link #inform} 默认色报已发生。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
