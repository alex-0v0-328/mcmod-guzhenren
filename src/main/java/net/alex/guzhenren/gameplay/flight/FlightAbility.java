package net.alex.guzhenren.gameplay.flight;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.jetbrains.annotations.NotNull;

/**
 * Flight granted by named sources: a Gu's flight, an anchored dimension's flight. Each source is granted and
 * revoked on its own; the player may fly while any source holds.
 *
 * <p>⚠ Callers grant, revoke and ask by source id. Each source is a transient {@code ADD_VALUE} modifier on
 * NeoForge's creative-flight attribute, named by the source id, so it is never saved and a respawn starts with none.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class FlightAbility {

    private FlightAbility() {}

    public static boolean has(@NotNull Player player, @NotNull ResourceLocation source) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        return flight != null && flight.hasModifier(source);
    }

    /** Whether the player may fly at all, from any grant: the game mode, another mod, or a source here. */
    public static boolean canFly(@NotNull Player player) { return player.mayFly(); }

    /** Grants flight from {@code source}; {@code false} when that source already held. */
    public static boolean grant(@NotNull ServerPlayer player, @NotNull ResourceLocation source) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight == null || flight.hasModifier(source)) return false;
        flight.addTransientModifier(new AttributeModifier(source, 1.0D, AttributeModifier.Operation.ADD_VALUE));
        return true;
    }

    /** Revokes flight from {@code source}; {@code false} when that source did not hold. */
    public static boolean revoke(@NotNull ServerPlayer player, @NotNull ResourceLocation source) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        return flight != null && flight.removeModifier(source);
    }
}
