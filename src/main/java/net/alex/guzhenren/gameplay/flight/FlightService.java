package net.alex.guzhenren.gameplay.flight;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.dimension.DimensionTravelService;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Flight [飞行] and burst [疾飞], the two stages of creative-style flight, and the server half of a burst.
 *
 * <p>{@link #getGrant} decides what a player may do right now, for both ends (the client asks it before sending a
 * toggle). A creative player and anyone inside the Treasure Yellow Heaven [宝黄天] may burst; a survival player only
 * through an active effect that implements {@link FlightContributor}, the strongest one winning
 * ({@link #getGuGrant}); a spectator, a rider, a sleeper and the dead get nothing. Flight is plain creative
 * flight, toggled with vanilla's double jump; a burst is that flight at {@link #BURST_MULTIPLIER} times the speed,
 * sprinting doubling it again as in creative, toggled by double-tapping sprint
 * ({@code net.alex.guzhenren.client.input.BurstInput}).
 *
 * <p>Flight from a Gu is a {@link FlightAbility} source ({@link #GU_FLIGHT_MODIFIER_ID}), never a raw write to
 * {@code Abilities#mayfly}, which the game saves and game modes share: it never takes away flight the game mode or
 * the anchored dimension granted. {@link #refreshGuFlight} grants it while a Gu grants flight and, once none does,
 * revokes it and turns flying off unless something else still lets the player fly, so a survival player falls when
 * the effect runs out. Creative and the Treasure Yellow Heaven bring their own flight, so a burst needs no source of
 * its own.
 *
 * <p>{@link #startBurst} marks the burst ({@code PlayerData#setBurst}, synced to its owner) before turning flying on,
 * so the client sees the mark first and a player standing on the ground jumps before its landing check can end the
 * flight ({@code net.alex.guzhenren.client.event.BurstEvents}). {@link #tick} runs every server tick ({@link
 * FlightEvents}): it refreshes the Gu flight, then ends a burst once the player stops flying (the client landed, or
 * vanilla's double jump turned flying off) or loses the burst grant. {@link #stopBurst} drops the mark; a player
 * something still lets fly keeps flying at normal speed, anyone else falls. It always resends the abilities: a client
 * that turned flying back on with vanilla's double jump in the tick before would otherwise keep flying unauthorized.
 *
 * <p>⚠ The speed is never written to {@code Abilities}, which the game saves: only the client scales its own flight
 * ({@code net.alex.guzhenren.mixin.PlayerMixin} and {@code LocalPlayerMixin}), reading the synced mark; the server
 * needs no speed, since it does not simulate player movement. ⚠ {@link #BURST_MULTIPLIER} stays below ≈9.9: past
 * it a sprinting burst trips the vanilla server's "moved too quickly" check and rubber-bands. At 3 a sprinting burst
 * covers ≈60 blocks a second, ≈3.75 chunks of new terrain, close to an elytra dive.
 *
 * <p>The mark is saved, so a player who logs out midair still bursts on return, and the player-data sync resends it on
 * login, respawn and dimension change; death drops it. Leaving the End through its exit portal keeps it but resets
 * the game mode, which turns a survival player's flying off, so the first tick ends the burst.
 *
 * @author Alex
 * @version 1.0.0
 * @see FlightGrant
 * @since 1.0.0
 */

public final class FlightService {

    private FlightService() {}

    public static final float BURST_MULTIPLIER = 3.0F;
    private static final ResourceLocation GU_FLIGHT_MODIFIER_ID = Guzhenren.id("gu_flight");

    public static @NotNull FlightGrant getGrant(@NotNull Player player) {
        if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isSleeping()) {
            return FlightGrant.NONE;
        }
        if (player.isCreative() || DimensionTravelService.isInside(player, ModDimensions.TREASURE_YELLOW_HEAVEN)) {
            return FlightGrant.BURST;
        }
        return getGuGrant(player);
    }

    public static @NotNull FlightGrant getGuGrant(@NotNull Player player) {
        FlightGrant grant = FlightGrant.NONE;
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (ModEffects.effectOf(instance) instanceof FlightContributor contributor) {
                grant = FlightGrant.getStrongest(grant, contributor.getFlightGrant(instance.getAmplifier()));
            }
        }
        return grant;
    }

    public static boolean isBursting(@NotNull Player player) {
        return PlayerData.get(player).burst();
    }

    public static void toggleBurst(@NotNull ServerPlayer player) {
        if (isBursting(player)) {
            stopBurst(player);
        } else {
            startBurst(player);
        }
    }

    public static boolean startBurst(@NotNull ServerPlayer player) {
        if (!getGrant(player).allowsBurst()) return false;

        refreshGuFlight(player);
        PlayerData.get(player).setBurst(true);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        return true;
    }

    public static void stopBurst(@NotNull ServerPlayer player) {
        PlayerData.get(player).setBurst(false);
        if (!FlightAbility.mayFly(player)) player.getAbilities().flying = false;
        player.onUpdateAbilities();
    }

    public static void tick(@NotNull ServerPlayer player) {
        refreshGuFlight(player);
        if (isBursting(player) && (!player.getAbilities().flying || !getGrant(player).allowsBurst())) {
            stopBurst(player);
        }
    }

    private static void refreshGuFlight(ServerPlayer player) {
        boolean granted = getGuGrant(player).allowsFlight();
        if (granted == FlightAbility.has(player, GU_FLIGHT_MODIFIER_ID)) return;

        if (granted) {
            FlightAbility.grant(player, GU_FLIGHT_MODIFIER_ID);
            return;
        }
        FlightAbility.revoke(player, GU_FLIGHT_MODIFIER_ID);
        if (!FlightAbility.mayFly(player)) player.getAbilities().flying = false;
        player.onUpdateAbilities();
    }
}
