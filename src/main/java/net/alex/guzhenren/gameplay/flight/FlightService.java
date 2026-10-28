package net.alex.guzhenren.gameplay.flight;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.dimension.DimensionTravelService;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Who may fly and how far up the ladder ({@link FlightGrant}), and the flight a Gu grants. The faster stage on top,
 * second-stage flight [二段飞行], is {@link SecondStageService}'s.
 *
 * <p>{@link #getGrant} decides what a player may do right now, for both ends (the client asks it before sending an
 * intent). A creative player and anyone inside the Treasure Yellow Heaven [宝黄天] may fly the second stage; a
 * survival player only through an active effect that implements {@link FlightContributor}, the strongest one winning
 * ({@link #getGuGrant}); a spectator, a rider, a sleeper and the dead get nothing. Flight is plain creative flight,
 * toggled with vanilla's double jump, which vanilla sprinting doubles.
 *
 * <p>Flight from a Gu is a {@link FlightAbility} source ({@link #GU_FLIGHT_MODIFIER_ID}), never a raw write to
 * {@code Abilities#mayfly}, which the game saves and game modes share: it never takes away flight the game mode or
 * the anchored dimension granted. {@link #tick} ({@link FlightEvents}, every server tick) grants it while a Gu grants
 * flight and, once none does, revokes it and turns flying off unless something else still lets the player fly, so a
 * survival player falls when the effect runs out. Creative and the Treasure Yellow Heaven bring their own flight.
 *
 * @author Alex
 * @version 1.0.0
 * @see FlightGrant
 * @see SecondStageService
 * @since 1.0.0
 */

public final class FlightService {

    private FlightService() {}

    private static final ResourceLocation GU_FLIGHT_MODIFIER_ID = Guzhenren.id("gu_flight");

    public static @NotNull FlightGrant getGrant(@NotNull Player player) {
        if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isSleeping()) {
            return FlightGrant.NONE;
        }
        if (player.isCreative() || DimensionTravelService.isInside(player, ModDimensions.TREASURE_YELLOW_HEAVEN)) {
            return FlightGrant.SECOND_STAGE;
        }
        return getGuGrant(player);
    }

    public static @NotNull FlightGrant getGuGrant(@NotNull Player player) {
        FlightGrant grant = FlightGrant.NONE;
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (instance.getEffect().value() instanceof FlightContributor contributor) {
                grant = FlightGrant.getStrongest(grant, contributor.getFlightGrant(instance.getAmplifier()));
            }
        }
        return grant;
    }

    public static void tick(@NotNull ServerPlayer player) {
        boolean granted = getGuGrant(player).canFly();
        if (granted == FlightAbility.has(player, GU_FLIGHT_MODIFIER_ID)) return;

        if (granted) {
            FlightAbility.grant(player, GU_FLIGHT_MODIFIER_ID);
            return;
        }
        FlightAbility.revoke(player, GU_FLIGHT_MODIFIER_ID);
        if (!FlightAbility.canFly(player)) player.getAbilities().flying = false;
        player.onUpdateAbilities();
    }
}
