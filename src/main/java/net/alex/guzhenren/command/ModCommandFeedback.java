package net.alex.guzhenren.command;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * 命令回复的唯一出口：绿色表示已改变，红色表示未改变。
 *
 * <ul>
 *   <li>⚠ 颜色只表示回复类别，不编码领域、转数或严重程度。</li>
 *   <li>红色也走 {@code sendSuccess}，不用 {@code sendFailure}：部分拒绝非失败；后者另丢 {@code [GZR]} 标签。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.command.ModCommandSupport
 * @since 1.0.0
 */

public final class ModCommandFeedback {

    private ModCommandFeedback() {}

    public static void sendSuccess(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> formatTagged(message, ChatFormatting.GREEN), false);
    }

    public static void sendFailure(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> formatTagged(message, ChatFormatting.RED), false);
    }

    private static Component formatTagged(Component message, ChatFormatting color) {
        return Component.translatable("guzhenren.command.tagged", message.copy().withStyle(color));
    }
}
