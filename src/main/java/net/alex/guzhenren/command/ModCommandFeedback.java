package net.alex.guzhenren.command;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * 模组指令反馈消息的唯一发送出口：绿色表示状态已变更（{@link #sendSuccess}），红色表示操作被拒绝或未变更
 * （{@link #sendFailure}）。
 * <ul>
 *   <li>{@link #sendFailure} 底层同样调用 {@link CommandSourceStack#sendSuccess} 而非 {@code sendFailure}，以保留统一的
 *       {@code [GZR]} 前缀标签并支持多目标部分成功、部分拒绝时同时输出两条反馈。</li>
 *   <li>⚠ 颜色仅用于区分指令执行结果是否生效，严禁用于编码流派、转数或消息严重程度。</li>
 * </ul>
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
