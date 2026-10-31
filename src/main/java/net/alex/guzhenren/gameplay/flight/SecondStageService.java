package net.alex.guzhenren.gameplay.flight;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.entity.gu.WildGuEntity;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.impact.ImpactCrater;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Second-stage flight [二段飞行]: the stage above vanilla's sprinting flight, the rules both ends share, and its
 * server half.
 *
 * <p>Flight has three speeds: plain flight, vanilla's sprinting flight, and the second stage, a cruise along the
 * look that tops out at {@link #SPEED_MULTIPLIER} times plain flight ({@link SecondStageCruise}). {@link #canEnter}
 * holds while the player's grant reaches the second stage ({@link FlightService#getGrant}) and the player is flying
 * and sprinting; both ends ask it, the client before sending a double tap of the sprint key
 * ({@code net.alex.guzhenren.client.input.SecondStageInput}), the server again in {@link #toggle}. The sprint
 * requirement keeps a hovering player out: vanilla only sprints while moving forward.
 *
 * <p>{@link #start} marks the stage ({@code PlayerData#setSecondStage}, synced to every client that sees the player),
 * sets the throttle to {@link SecondStageCruise#ENTRY_THROTTLE} and posts a {@link SecondStageStartedEvent} for the
 * feedback above it (the sonic boom). Every start is a fresh entry, so every entry booms. {@link #tick} runs every
 * server tick ({@link FlightEvents}): it steps the throttle ({@link #stepThrottle}), which fills while the player
 * sprints and drains once the sprint stops (the player let go of forward or brushed a wall), and ends the stage once
 * {@link #canStay} breaks: the throttle ran dry without a sprint, flying stopped (landed, or vanilla's double jump),
 * or the grant went. A second double tap ends it on purpose ({@link #toggle}). Ending it leaves flight alone, so the
 * player drops back to sprinting or plain flight. The server learns the sprint from the client's sprint commands, a
 * packet after the client, which steps its own throttle ({@code net.alex.guzhenren.client.event.SecondStageEvents}).
 *
 * <p>{@link #travel} replaces vanilla's flying travel while the mark is on
 * ({@code net.alex.guzhenren.mixin.PlayerMixin}) on whichever end moves the player: the client's own physics, and the
 * step the server runs each tick before it puts the player back where the client said, the step vanilla's elytra wall
 * damage rides on. The velocity follows the throttled look ({@link SecondStageCruise#nextVelocity}); jump and sneak
 * climb nothing ({@code LocalPlayerMixin}). Ahead of the move the server rams every living body in the swept box while
 * the flight is past {@link SecondStageCruise#BARRIER_SPEED}: player-attack damage by speed and a shove along the
 * flight, each body once per {@link SecondStageCruise#RAM_COOLDOWN_TICKS}, the flier unslowed. Only a hit the body
 * takes shoves it, scaled down by its knockback resistance; a refused one (no PvP, invulnerable, still flinching)
 * leaves it in place and free to be rammed the next tick. Wild Gu, allies either way round (a team, the flier's own
 * pets), anything the flier owns, spectators and armor stands are spared. Also ahead of the move, past the barrier
 * speed and where {@code ModEvents#canGrief} allows, the server shatters every fragile block
 * ({@link SecondStageCruise#isFragile}: leaves, glass, snow and the like) with a collision shape in the box swept over
 * the next {@link SecondStageCruise#BREAK_LOOKAHEAD_TICKS} ticks ({@link #shatter}: {@code Level#destroyBlock} with
 * drops, so the break particles and sound are vanilla's), and the flight passes through. A collision on any axis, a
 * floor and a ceiling included, costs the elytra's wall damage on the server by the speed lost in three dimensions,
 * and losing at least the barrier speed in it is a crash: the throttle empties, the velocity stops, a
 * {@link SecondStageCrashEvent} goes up on that end (the server's ring and nearby shakes, the client's own shake), and
 * the server ends the stage and blasts a crater into the wall like a plunge's ({@link ImpactCrater#blast} at
 * {@link SecondStageCruise#getCrashPower}, {@link #CRASH_AHEAD} blocks ahead of mid-body along the flight). A
 * collision whose one-tick sweep holds nothing but fragile blocks is no hit at all ({@link #isFragileOnly}): the
 * client reaches glass the server's break has not arrived at yet, and with griefing refused the flier merely stops
 * against it, so no damage, no crash and the throttle keeps draining or filling as the sprint says. Each end judges the
 * crash on its own velocity, the server's a packet behind, so near the barrier one end may crash while the other only
 * pays the wall; the throttle drains the difference away within {@link SecondStageCruise#COAST_TICKS}. A level flight
 * brushing the ground collides vertically too, but loses next to no speed, so it costs nothing. Landing is no wall:
 * vanilla ends flight on the ground, and the stage with it.
 *
 * <p>⚠ The speed is never written to {@code Abilities}, which the game saves. ⚠ {@link #SPEED_MULTIPLIER} stays below
 * ≈18: vanilla's "moved too quickly" check allows 10 blocks a move beyond the server's own velocity, and at an entry
 * the server's step has not caught up with the client's speed yet, so past it the entry rubber-bands. At 6 it covers
 * ≈65 blocks a second, ≈4 chunks of new terrain, three times a sprinting flight and close to an elytra dive.
 *
 * <p>The mark and the throttle are not saved and a death drops them, so a relog or a respawn starts outside the
 * stage; a dimension change keeps the same player, and the first tick there checks the conditions again.
 *
 * @author Alex
 * @version 1.0.0
 * @see FlightService
 * @since 1.0.0
 */

public final class SecondStageService {

    private SecondStageService() {}

    public static final float SPEED_MULTIPLIER = 6.0F;
    private static final float WALL_BIG_SOUND_DAMAGE = 4.0F;
    private static final double CRASH_AHEAD = 0.8D;
    private static final float RAM_VOLUME = 1.0F;
    private static final Map<UUID, Map<Integer, Long>> RAMMED = new HashMap<>();

    public static boolean isActive(@NotNull Player player) { return PlayerData.get(player).isInSecondStage(); }

    public static boolean canEnter(@NotNull Player player) {
        return FlightService.getGrant(player).canFlySecondStage() && player.getAbilities().flying
                && player.isSprinting();
    }

    public static boolean canStay(@NotNull Player player, double throttle) {
        return FlightService.getGrant(player).canFlySecondStage() && player.getAbilities().flying
                && (player.isSprinting() || throttle > 0.0D);
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
        PlayerData data = PlayerData.get(player);
        data.setSecondStage(true);
        data.setSecondStageThrottle(SecondStageCruise.ENTRY_THROTTLE);
        ModEvents.post(new SecondStageStartedEvent(player));
        return true;
    }

    public static void stop(@NotNull ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        data.setSecondStage(false);
        data.setSecondStageThrottle(0.0D);
        RAMMED.remove(player.getUUID());
    }

    public static void forget(@NotNull Player player) { RAMMED.remove(player.getUUID()); }

    public static void tick(@NotNull ServerPlayer player) {
        if (isActive(player) && !canStay(player, stepThrottle(player))) stop(player);
    }

    public static double stepThrottle(@NotNull Player player) {
        PlayerData data = PlayerData.get(player);
        double throttle = SecondStageCruise.nextThrottle(data.getSecondStageThrottle(), player.isSprinting());
        data.setSecondStageThrottle(throttle);
        return throttle;
    }

    public static boolean travel(@NotNull Player player) {
        if (!player.getAbilities().flying || player.isPassenger() || !player.isControlledByLocalInstance()
                || !isActive(player)) {
            return false;
        }
        Vec3 velocity = SecondStageCruise.nextVelocity(player.getDeltaMovement(), player.getLookAngle(),
                PlayerData.get(player).getSecondStageThrottle(),
                SecondStageCruise.getTopSpeed(player.getAbilities().getFlyingSpeed()));
        if (player instanceof ServerPlayer server) {
            ram(server, velocity);
            shatter(server, velocity);
        }
        if (player.isFallFlying()) player.stopFallFlying();
        player.setDeltaMovement(velocity);
        AABB swept = player.getBoundingBox().expandTowards(velocity);
        player.move(MoverType.SELF, velocity);
        if ((player.horizontalCollision || player.verticalCollision) && !isFragileOnly(player.level(), swept)) {
            hitWall(player, velocity);
        }
        player.resetFallDistance();
        player.calculateEntityAnimation(false);
        return true;
    }

    private static void shatter(ServerPlayer player, Vec3 velocity) {
        if (velocity.length() < SecondStageCruise.BARRIER_SPEED || !ModEvents.canGrief(player.level(), player)) return;
        AABB swept = player.getBoundingBox().expandTowards(velocity.scale(SecondStageCruise.BREAK_LOOKAHEAD_TICKS));
        for (BlockPos pos : blocksIn(swept)) {
            if (isFragile(player.level(), pos, player.level().getBlockState(pos))) {
                player.level().destroyBlock(pos, true, player);
            }
        }
    }

    private static boolean isFragileOnly(Level level, AABB swept) {
        boolean found = false;
        for (BlockPos pos : blocksIn(swept)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getCollisionShape(level, pos).isEmpty()) continue;
            if (!isFragile(level, pos, state)) return false;
            found = true;
        }
        return found;
    }

    private static Iterable<BlockPos> blocksIn(AABB box) {
        return BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                Mth.ceil(box.maxX) - 1, Mth.ceil(box.maxY) - 1, Mth.ceil(box.maxZ) - 1);
    }

    private static boolean isFragile(Level level, BlockPos pos, BlockState state) {
        return !state.isAir() && SecondStageCruise.isFragile(state.getBlock().getExplosionResistance())
                && !state.getCollisionShape(level, pos).isEmpty();
    }

    private static void hitWall(Player player, Vec3 velocity) {
        double lost = velocity.length() - player.getDeltaMovement().length();
        if (!player.level().isClientSide()) {
            float damage = SecondStageCruise.getWallDamage(lost);
            if (damage > 0.0F) {
                LivingEntity.Fallsounds sounds = player.getFallSounds();
                player.playSound((int) damage > WALL_BIG_SOUND_DAMAGE ? sounds.big() : sounds.small(), 1.0F, 1.0F);
                player.hurt(player.damageSources().flyIntoWall(), damage);
            }
        }
        if (!SecondStageCruise.isCrash(lost)) return;

        PlayerData.get(player).setSecondStageThrottle(0.0D);
        player.setDeltaMovement(Vec3.ZERO);
        Vec3 heading = velocity.normalize();
        ModEvents.post(new SecondStageCrashEvent(player, heading));
        if (!(player instanceof ServerPlayer server)) return;
        stop(server);
        Vec3 impact = player.position().add(0.0D, player.getBbHeight() / 2.0D, 0.0D).add(heading.scale(CRASH_AHEAD));
        ImpactCrater.blast(server, impact, SecondStageCruise.getCrashPower(lost));
    }

    private static void ram(ServerPlayer player, Vec3 velocity) {
        double speed = velocity.length();
        if (speed < SecondStageCruise.BARRIER_SPEED) return;
        long now = player.level().getGameTime();
        Map<Integer, Long> rammed = RAMMED.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        rammed.values().removeIf(at -> now - at >= SecondStageCruise.RAM_COOLDOWN_TICKS);

        AABB swept = player.getBoundingBox().expandTowards(velocity);
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, swept,
                target -> canRam(player, target))) {
            if (rammed.putIfAbsent(target.getId(), now) != null) continue;
            if (!target.hurt(player.damageSources().playerAttack(player), SecondStageCruise.getRamDamage(speed))) {
                rammed.remove(target.getId());
                continue;
            }
            double resistance = Math.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
            target.setDeltaMovement(SecondStageCruise.getRamPush(velocity).scale(1.0D - resistance));
            target.hasImpulse = true;
            target.hurtMarked = true;
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, RAM_VOLUME, 1.0F);
        }
    }

    private static boolean canRam(ServerPlayer player, LivingEntity target) {
        return target != player && target.isAlive() && !target.isSpectator() && target.isPickable()
                && !player.isAlliedTo(target) && !target.isAlliedTo(player)
                && !(target instanceof OwnableEntity owned && player.equals(owned.getOwner()))
                && !(target instanceof WildGuEntity) && !(target instanceof ArmorStand);
    }
}
