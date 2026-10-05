package net.alex.guzhenren.gameplay.dimension;

import java.util.Objects;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.dimension.DimensionReturnData.ReturnPoint;
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.jetbrains.annotations.NotNull;

/**
 * Generic anchored-dimension travel service. A single {@link DimensionReturnData} attachment stores the
 * one return point for the one anchored dimension a player may be inside at a time.
 *
 * <p>{@link #isInside} answers whether the player is currently in the given dimension; {@link #enter}
 * snapshots the player's original location and flying state before teleporting there, returning
 * {@code false} instead when they are already inside the dimension or the target level could not be
 * resolved. Death clears the record through {@link net.alex.guzhenren.gameplay.lifecycle.PlayerDataService}.
 *
 * <p>{@link #exit} restores the snapshot: if a return point exists, the player is teleported back to it
 * and resumes flying only if they were flying on entry and can still fly without the grant, returning
 * {@code true}; if no record exists, the player is sent to the Overworld shared spawn as a fallback
 * instead, returning {@code false}. Both paths revoke the flight grant and clear the record ({@link
 * #clear(Player)} is safe to call even when no record is present).
 *
 * <p>⚠ Flight inside is a transient {@code CREATIVE_FLIGHT} modifier, never the raw
 * {@code Abilities.mayfly}: the raw field is saved with the player and shared with game modes and other
 * mods, so writing it leaked survival flight whenever a player left without a matching restore.
 * Transient keeps the grant out of the save file. {@link #ensureFlight} re-grants the permission each
 * tick without forcing {@code flying} on, so the player may choose to hover or stop flying freely while
 * inside an anchored dimension; {@link #revokeFlight} drops the grant, and {@code flying} with it unless
 * something else still lets the player fly, each tick while the player is outside every anchored
 * dimension, so leaving by any route -- exit, vanilla {@code /tp}, another mod's teleport -- revokes it.
 *
 * <p>{@link #rescueIfBelowVoid} teleports the player back to the given spawn if they have fallen below
 * the current level's minimum build height, returning {@code true} if a rescue teleport happened.
 *
 * @author Alex
 * @version 1.0.0
 * @see DimensionReturnData
 * @since 1.0.0
 */

public final class DimensionTravelService {

    private DimensionTravelService() {}

    private static final ResourceLocation FLIGHT_MODIFIER_ID = Guzhenren.id("anchored_dimension_flight");

    public static boolean isInside(@NotNull Player player, @NotNull ResourceKey<Level> dimension) {
        return player.level().dimension().equals(Objects.requireNonNull(dimension, "dimension"));
    }

    public static boolean enter(@NotNull ServerPlayer player, @NotNull ResourceKey<Level> dimension,
            @NotNull Vec3 spawn) {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(spawn, "spawn");
        if (isInside(player, dimension)) {
            return false;
        }

        ServerLevel target = player.server.getLevel(dimension);
        if (target == null) {
            return false;
        }

        ReturnPoint point = new ReturnPoint(
                player.level().dimension(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                player.getAbilities().flying);
        player.setData(ModAttachments.DIMENSION_RETURN, DimensionReturnData.of(point));

        player.teleportTo(target, spawn.x, spawn.y, spawn.z, player.getYRot(), player.getXRot());
        ensureFlight(player);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        return true;
    }

    public static boolean exit(@NotNull ServerPlayer player) {
        DimensionReturnData data = player.getData(ModAttachments.DIMENSION_RETURN);
        if (data.isPresent()) {
            ReturnPoint point = data.point().orElseThrow();
            ServerLevel target = player.server.getLevel(point.level());
            boolean used = target != null;
            if (used) {
                player.teleportTo(target, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
                revokeFlight(player);
                player.getAbilities().flying = point.flying() && player.mayFly();
                player.fallDistance = 0.0F;
                player.onUpdateAbilities();
            } else {
                fallbackToOverworldSpawn(player);
            }
            clear(player);
            return used;
        }

        fallbackToOverworldSpawn(player);
        clear(player);
        return false;
    }

    private static void fallbackToOverworldSpawn(@NotNull ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        revokeFlight(player);
        player.fallDistance = 0.0F;
    }

    public static void clear(@NotNull Player player) {
        player.setData(ModAttachments.DIMENSION_RETURN, DimensionReturnData.DEFAULT);
    }

    public static void ensureFlight(@NotNull ServerPlayer player) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight != null && !flight.hasModifier(FLIGHT_MODIFIER_ID)) {
            flight.addTransientModifier(new AttributeModifier(FLIGHT_MODIFIER_ID, 1.0D,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    public static void revokeFlight(@NotNull ServerPlayer player) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight == null || !flight.removeModifier(FLIGHT_MODIFIER_ID)) return;
        if (player.getAbilities().flying && !player.mayFly()) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    public static boolean rescueIfBelowVoid(@NotNull ServerPlayer player, @NotNull Vec3 spawn) {
        if (player.getY() < player.level().getMinBuildHeight()) {
            player.teleportTo((ServerLevel) player.level(), spawn.x, spawn.y, spawn.z,
                    player.getYRot(), player.getXRot());
            player.fallDistance = 0.0F;
            return true;
        }
        return false;
    }
}
