package net.alex.guzhenren.gameplay.aperture;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Predicate;
import net.alex.guzhenren.command.ModCommandSupport;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * The awakening [开窍] gate every command that needs an aperture shares: {@link #AWAKENED} refuses a target
 * per player (data protection), {@link #sourceAwakened} hides a subtree in {@code requires()} (presentation
 * only), and {@link #applyOnAwakened} runs an operation on the awakened targets alone.
 *
 * <p>It lives with the aperture, not in the command framework, so the framework stays below every feature.
 * ⚠ Anything that flips {@code sourceAwakened} for a player has to call
 * {@code ModCommandSupport.refreshCommands}, or the client keeps the tree it was last sent.
 *
 * @author Alex
 * @version 1.0.0
 * @see ModCommandSupport
 * @since 1.0.0
 */

public final class AwakenedGate {

    private AwakenedGate() {}

    public static final Predicate<ServerPlayer> AWAKENED = ApertureService::isAwakened;

    public static boolean sourceAwakened(CommandSourceStack source) {
        return !(source.getEntity() instanceof ServerPlayer player) || ApertureService.isAwakened(player);
    }

    public static int applyOnAwakened(CommandContext<CommandSourceStack> context,
                                      ModCommandSupport.PlayerOperation operation) throws CommandSyntaxException {
        return ModCommandSupport.applyIf(context, AWAKENED, ModCommandSupport.FAILED_UNAWAKENED, operation);
    }
}
