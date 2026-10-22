package net.alex.guzhenren.gameplay.flight;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

/**
 * Flight granted by named sources: a Gu's flight, an anchored dimension's flight. Each source is granted and
 * revoked on its own; the player may fly while any source holds.
 *
 * <p>⚠ A version seam: callers grant, revoke and ask by source id on both sides. 1.20.1 has no creative-flight
 * attribute, so this keeps each player's sources in memory and turns {@link Abilities#mayfly} on when the first
 * source arrives and the player could not fly already, marking that the mod did it. When the last source goes it
 * turns {@code mayfly} back off only if the mod turned it on and the player is neither creative nor spectator, so
 * another mod's or the game mode's flight is never taken away.
 *
 * <p>⚠ Sources live with the player entity, like the mainline's transient attribute modifiers: a respawn or an End
 * return starts with none, and each source's own tick grants it again. The "the mod turned it on" mark is saved
 * with the player and carried across a clone, so a {@code mayfly} left on by a save or a clone with no source
 * behind it is turned off on the next tick.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@Mod.EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class FlightAbility {

    private FlightAbility() {}

    private static final String OWNED = Guzhenren.id("flight_owned").toString();
    private static final Map<Player, Set<ResourceLocation>> SOURCES = new WeakHashMap<>();

    public static boolean has(@NotNull Player player, @NotNull ResourceLocation source) {
        Set<ResourceLocation> held = SOURCES.get(player);
        return held != null && held.contains(source);
    }

    /** Whether the player may fly at all, from any grant: the game mode, another mod, or a source here. */
    public static boolean mayFly(@NotNull Player player) { return player.getAbilities().mayfly; }

    /** Grants flight from {@code source}; {@code false} when that source already held. */
    public static boolean grant(@NotNull ServerPlayer player, @NotNull ResourceLocation source) {
        if (!SOURCES.computeIfAbsent(player, p -> new HashSet<>()).add(source)) return false;
        Abilities abilities = player.getAbilities();
        if (!abilities.mayfly) {
            abilities.mayfly = true;
            player.getPersistentData().putBoolean(OWNED, true);
            player.onUpdateAbilities();
        }
        return true;
    }

    /** Revokes flight from {@code source}; {@code false} when that source did not hold. */
    public static boolean revoke(@NotNull ServerPlayer player, @NotNull ResourceLocation source) {
        Set<ResourceLocation> held = SOURCES.get(player);
        if (held == null || !held.remove(source)) return false;
        if (held.isEmpty()) release(player);
        return true;
    }

    private static void release(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(OWNED)) return;
        player.getPersistentData().remove(OWNED);
        if (player.isCreative() || player.isSpectator()) return;
        Abilities abilities = player.getAbilities();
        abilities.mayfly = false;
        abilities.flying = false;
        player.onUpdateAbilities();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Set<ResourceLocation> held = SOURCES.get(player);
        if (held == null || held.isEmpty()) release(player);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        if (original.getPersistentData().getBoolean(OWNED)) {
            event.getEntity().getPersistentData().putBoolean(OWNED, true);
        }
    }
}
