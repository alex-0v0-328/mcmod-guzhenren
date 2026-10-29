package net.alex.guzhenren.command;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * Every reply a command sends, in one voice: green for what changed, red for what did not.
 *
 * <p>The sole channel for command output. ⚠ Color carries exactly one meaning -- the category of the
 * reply -- and never encodes a domain, a rank [转数], or a severity. A red line still goes out through
 * {@code sendSuccess}, never {@code sendFailure}, because a partial refusal is a result, not a
 * failure, and {@code sendFailure} drops the {@code [GZR]} tag.
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
