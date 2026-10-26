package net.alex.guzhenren.gameplay.flight;

import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Second-stage flight [二段飞行]: the stage above vanilla's sprinting flight, and its server half.
 *
 * <p>Flight has three speeds: plain flight, vanilla's sprinting flight, and the second stage, which flies at
 * {@link #SPEED_MULTIPLIER} times the plain flying speed and climbs and descends at {@link #CLIMB_MULTIPLIER} times.
 * {@link #canEnter} holds while the player's grant reaches the second stage ({@link FlightService#getGrant}) and the
 * player is flying and sprinting; both ends ask it, the client before sending a double tap of the sprint key
 * ({@code net.alex.guzhenren.client.input.SecondStageInput}), the server again in {@link #toggle}. The sprint
 * requirement keeps a hovering player out: vanilla only sprints while moving forward.
 *
 * <p>{@link #start} marks the stage ({@code PlayerData#setSecondStage}, synced to its owner) and posts a
 * {@link SecondStageStartedEvent} for the feedback above it (the sonic boom). Every start is a fresh entry, so every
 * entry booms. {@link #tick} runs every server tick ({@link FlightEvents}) and ends the stage once any condition of
 * {@link #canEnter} breaks: the sprint stopped (the player let go of forward or hit a wall), flying stopped (landed,
 * or vanilla's double jump), or the grant went. A second double tap ends it on purpose ({@link #toggle}). Ending it
 * leaves flight alone, so the player drops back to sprinting or plain flight. The server learns the sprint from the
 * client's sprint commands, so the stage ends a packet after the client stops sprinting.
 *
 * <p>⚠ The speed is never written to {@code Abilities}, which the game saves: only the client scales its own flight
 * ({@code net.alex.guzhenren.mixin.PlayerMixin} and {@code LocalPlayerMixin}), reading the synced mark; the server
 * needs no speed, since it does not simulate player movement. ⚠ {@link #SPEED_MULTIPLIER} stays below ≈19.8: past
 * it the second stage trips the vanilla server's "moved too quickly" check and rubber-bands. At 6 it covers ≈65
 * blocks a second, ≈4 chunks of new terrain, three times a sprinting flight and close to an elytra dive.
 *
 * <p>The mark is not saved and a death drops it, so a relog or a respawn starts outside the stage; a dimension change
 * keeps the same player, and the first tick there checks the conditions again.
 *
 * @author Alex
 * @version 1.0.0
 * @see FlightService
 * @since 1.0.0
 */

public final class SecondStageService {

    private SecondStageService() {}

    public static final float SPEED_MULTIPLIER = 6.0F;
    public static final float CLIMB_MULTIPLIER = 3.0F;

    public static boolean isActive(@NotNull Player player) { return PlayerData.get(player).isInSecondStage(); }

    public static boolean canEnter(@NotNull Player player) {
        return FlightService.getGrant(player).canFlySecondStage() && player.getAbilities().flying
                && player.isSprinting();
    }

    public static void toggle(@NotNull ServerPlayer player) {
        if (isActive(player)) {
            stop(player);
        } else {
            start(player);
        }
    }

    public static boolean start(@NotNull ServerPlayer player) {
        if (isActive(player) || !canEnter(player)) return false;
        PlayerData.get(player).setSecondStage(true);
        ModEvents.post(new SecondStageStartedEvent(player));
        return true;
    }

    public static void stop(@NotNull ServerPlayer player) { PlayerData.get(player).setSecondStage(false); }

    public static void tick(@NotNull ServerPlayer player) {
        if (isActive(player) && !canEnter(player)) stop(player);
    }
}
