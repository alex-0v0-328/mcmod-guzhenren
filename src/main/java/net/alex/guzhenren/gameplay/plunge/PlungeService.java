package net.alex.guzhenren.gameplay.plunge;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.attribute.TransientModifiers;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.dimension.DimensionTravelService;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * The plunge [坠击]: a player in the air presses sneak plus Alt and slams straight down at
 * {@link #GRAVITY_MULTIPLIER} times gravity, cratering the landing and taking three times the fall damage. This is
 * the server half and the rule both ends share; the client sends the chord
 * ({@code net.alex.guzhenren.client.input.PlungeInput}).
 *
 * <p>{@link #canStart} holds for a living, awake, unmounted player off the ground, out of every fluid, off every
 * ladder and outside the Treasure Yellow Heaven [宝黄天], not plunging already: flying, falling, levitating and
 * gliding all count as in the air. The client asks it before sending, the server again in {@link #start}. Anyone
 * may plunge, creative included, with no cooldown; a drop too short to hurt leaves nothing behind.
 *
 * <p>{@link #start} turns flight and the second stage off, folds the elytra, clears levitation and slow falling,
 * zeroes the horizontal and the upward speed, and multiplies gravity through the synced gravity attribute
 * ({@code TransientModifiers#setGravityFactor}), so both ends fall that fast. It stores the top of the fall, the
 * player's height plus what they had already fallen, so starting low in a long fall saves nothing.
 *
 * <p>Each tick the server runs its own step of the player's movement before putting the player back where the
 * client last said, so its velocity keeps up with the fall. ⚠ That velocity is what vanilla's "moved too quickly"
 * check allows on top of 10 blocks a move, and the server's step meets the ground a tick before the client does,
 * zeroing it: the client's last move must stay under 10 blocks, so both ends cap the fall at
 * {@link #MAX_FALL_SPEED} blocks a tick (≈160 blocks a second, past ≈60 blocks of fall), which also holds at 10 TPS,
 * where one tick carries two moves.
 *
 * <p>{@link #tick} runs every server tick ({@link PlungeEvents}). Until the landing it holds flight and the elytra
 * shut, clears levitation and slow falling again if something re-applies them, and caps the speed; a plunge cannot
 * be called off. Death, mounting, a fluid, a ladder or the heaven end it early with no crater and no extra damage
 * ({@link #stop}), and so do a dimension change and a teleport. ⚠ It lands once the player is on the ground and
 * vanilla has reset the fall distance: the server's own step lands early, but only the client's moves run vanilla's
 * fall handling ({@code ServerPlayer#checkFallDamage} is empty), which resets the distance once the client is down.
 * So the crater sits where the client landed and the fall vanilla saw is the plunge's own, cancelled while
 * {@link #isActive} holds. The landing blasts the crater ({@link PlungeLanding#blast}) at the power
 * {@link PlungeImpact#getCraterPower} gives the height, then deals {@link PlungeLanding#getFallDamage} as plain fall
 * damage, which Feather Falling, Protection and Resistance reduce as usual. A player who may fly is hurt anyway:
 * vanilla spares them, the plunge does not. Block cushions (hay, slime, beds) do not apply. The crater digs out the
 * ground underfoot, and the drop into it is an ordinary fall.
 *
 * <p>{@link #tickLocal} is the local player's half: it holds flight and the elytra shut and caps the speed between
 * the server's corrections, and stops the landing dead, so a slime block cannot fling the player back up.
 *
 * <p>The top is synced to its owner and never saved, and a death drops it, so a relog or a respawn starts outside the
 * plunge. The gravity modifier is transient, and {@link #tick} takes it off any player who is not plunging.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlungeImpact
 * @since 1.0.0
 */

public final class PlungeService {

    private PlungeService() {}

    public static final double GRAVITY_MULTIPLIER = 8.0D;
    public static final double MAX_FALL_SPEED = 8.0D;
    private static final ResourceLocation GRAVITY_MODIFIER_ID = Guzhenren.id("plunge_gravity");

    public static boolean isActive(@NotNull Player player) { return PlayerData.get(player).getPlungeTopY() != null; }

    public static boolean canStart(@NotNull Player player) {
        return !isActive(player) && !player.onGround() && canContinue(player);
    }

    public static boolean start(@NotNull ServerPlayer player) {
        if (!canStart(player)) return false;
        PlayerData.get(player).setPlungeTopY(player.getY() + player.fallDistance);
        SecondStageService.stop(player);
        clearFloating(player);
        holdDown(player);
        player.setDeltaMovement(0.0D, Math.min(player.getDeltaMovement().y, 0.0D), 0.0D);
        player.hurtMarked = true;
        TransientModifiers.setGravityFactor(player, GRAVITY_MODIFIER_ID, GRAVITY_MULTIPLIER);
        return true;
    }

    public static void stop(@NotNull ServerPlayer player) {
        PlayerData.get(player).setPlungeTopY(null);
        TransientModifiers.setGravityFactor(player, GRAVITY_MODIFIER_ID, 1.0D);
    }

    public static void tick(@NotNull ServerPlayer player) {
        Double topY = PlayerData.get(player).getPlungeTopY();
        if (topY == null) {
            TransientModifiers.setGravityFactor(player, GRAVITY_MODIFIER_ID, 1.0D);
        } else if (!canContinue(player)) {
            stop(player);
        } else if (player.onGround() && player.fallDistance == 0.0F) {
            land(player, topY);
        } else {
            clearFloating(player);
            holdDown(player);
        }
    }

    public static void tickLocal(@NotNull Player player) {
        if (!isActive(player)) return;
        holdDown(player);
        if (player.onGround()) player.setDeltaMovement(Vec3.ZERO);
    }

    private static boolean canContinue(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isPassenger() && !player.isSleeping()
                && !player.isInFluidType() && !player.onClimbable()
                && !DimensionTravelService.isInside(player, ModDimensions.TREASURE_YELLOW_HEAVEN);
    }

    private static void clearFloating(ServerPlayer player) {
        ModEffects.remove(player, ModEffects.VANILLA_LEVITATION);
        ModEffects.remove(player, ModEffects.VANILLA_SLOW_FALLING);
    }

    private static void holdDown(Player player) {
        if (player.isFallFlying()) player.stopFallFlying();
        Vec3 motion = player.getDeltaMovement();
        if (motion.y < -MAX_FALL_SPEED) player.setDeltaMovement(motion.x, -MAX_FALL_SPEED, motion.z);
        if (!player.getAbilities().flying) return;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
    }

    private static void land(ServerPlayer player, double topY) {
        double height = PlungeImpact.getHeight(topY, player.getY());
        stop(player);
        player.setDeltaMovement(Vec3.ZERO);
        float power = PlungeImpact.getCraterPower(height);
        if (power > 0.0F) PlungeLanding.blast(player, power);
        int damage = PlungeLanding.getFallDamage(player, height);
        if (damage > 0) player.hurt(player.damageSources().fall(), damage);
    }
}
