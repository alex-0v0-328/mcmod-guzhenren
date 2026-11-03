package net.alex.guzhenren.particle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.registry.particle.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The server-side cone trail behind the shockwave ring [激波环]: each ring blooms in place
 * from smallest to largest where it was born.
 *
 * <p>{@link #dashCone} plants the dash trail along the path, one ring every
 * {@link RingTrailSpacing#SPACING} blocks of ACTUAL travel -- the server-side dodge movement
 * arrives in uneven chunks (Epic Fight locks movement and lets the animation drive it), so a
 * per-tick drop clusters the rings at the start and end of the path, while
 * spacing by measured travel stays uniform across windups, jumps and wall truncations. The ring at
 * the dash start appears first and the one near the end last, up to {@link #DASH_MAX_RINGS} rings
 * (~16 blocks of path). The ring velocity is only a facing-normal carrier
 * ({@link #RING_NORMAL_DRIFT}, blocks per tick): a zero vector would trip {@code RingGeometry}'s
 * no-direction fallback and lay the ring flat, and over the ring's whole life the drift moves it
 * under a quarter block, which reads as stationary. Every ring of this package goes out through {@link #sendRing},
 * with count 0 and speed 1.0, handing the particle the exact velocity, which the ring also reads as its facing
 * normal ({@code FACING_MOTION}).
 *
 * <p>{@link #DASH_BURSTS} holds one active burst per player: a
 * retrigger while a dash burst is alive (a second payload inside the window) would replay the
 * whole trail, so a dash inside its own window is skipped instead of replacing it. The
 * {@link Burst} interface's {@code tick} drops one tick's ring(s) and returns the next state, or
 * null once the burst is spent. {@link DashBurst} positions each ring along the tick's movement
 * segment so even a chunked dodge spreads them evenly, and ends when the ring budget is spent, the
 * player stands still for {@link #DASH_IDLE_LIMIT} ticks (a dodge windup is shorter) below
 * {@link #DASH_IDLE_SPEED_SQR} squared horizontal speed (0.05 blocks/tick counts as still), or the
 * {@link #DASH_MAX_TICKS} safety cap runs out. {@link #DASH_RING_HEIGHT} is the dash ring's spawn
 * height above the feet, the standing ring's mid-body (max radius 1.1). A player's burst is dropped on logout and on
 * a dimension change ({@link #forget}): a dash burst measures travel from a position in one level, and carried into
 * another it would lay its rings along the line between the two. The per-player {@link #tick}, which
 * {@code RingConeEvents} calls at the end of each player tick, returns at once while the map is empty.
 *
 * <p>Future speed- or force-feel actions call the {@code dashCone} entry point
 * (or add a sibling) rather than spawning rings directly. Pure visual feedback: no damage, no
 * hitbox.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.particle.RingParticle
 * @since 1.0.0
 */

public final class RingConeEmitter {

    private static final double MIN_DIRECTION_LENGTH_SQR = 1.0E-6D;
    private static final int DASH_MAX_RINGS = 8;
    private static final int DASH_MAX_TICKS = 25;
    private static final int DASH_IDLE_LIMIT = 3;
    private static final double DASH_IDLE_SPEED_SQR = 0.0025D;
    private static final double DASH_RING_HEIGHT = 1.0D;
    private static final double RING_NORMAL_DRIFT = 0.03D;

    private static final Map<UUID, Burst> DASH_BURSTS = new HashMap<>();

    private RingConeEmitter() {}

    public static void dashCone(ServerPlayer player, Vec3 motionDirection) {
        if (DASH_BURSTS.containsKey(player.getUUID())) return;
        if (motionDirection.lengthSqr() < MIN_DIRECTION_LENGTH_SQR) return;
        DASH_BURSTS.put(player.getUUID(), new DashBurst(motionDirection.normalize(),
                DASH_MAX_TICKS, player.position(), 0.0D, DASH_MAX_RINGS, 0));
    }

    public static void tick(ServerPlayer player) {
        if (DASH_BURSTS.isEmpty()) return;
        if (player.isRemoved() || player.isDeadOrDying()
                || !(player.level() instanceof ServerLevel level)) {
            forget(player);
            return;
        }
        tickBurst(DASH_BURSTS, player, level);
    }

    public static void forget(Player player) {
        DASH_BURSTS.remove(player.getUUID());
    }

    /** Sends one ring at the point given, facing along {@code facing}, which its drift velocity carries. */
    static void sendRing(ServerLevel level, ParticleOptions ring, double x, double y, double z, Vec3 facing) {
        Vec3 velocity = facing.scale(RING_NORMAL_DRIFT);
        level.sendParticles(ring, x, y, z, 0, velocity.x, velocity.y, velocity.z, 1.0D);
    }

    private static void tickBurst(Map<UUID, Burst> bursts, ServerPlayer player, ServerLevel level) {
        Burst burst = bursts.get(player.getUUID());
        if (burst == null) return;
        Burst next = burst.tick(player, level);
        if (next == null) bursts.remove(player.getUUID());
        else bursts.put(player.getUUID(), next);
    }

    private interface Burst {

        Burst tick(ServerPlayer player, ServerLevel level);
    }

    private record DashBurst(Vec3 direction, int ticksLeft, Vec3 lastPos, double carry,
                             int ringsLeft, int idleTicks) implements Burst {

        @Override
        public Burst tick(ServerPlayer player, ServerLevel level) {
            if (ticksLeft <= 0 || ringsLeft <= 0 || idleTicks >= DASH_IDLE_LIMIT) return null;
            Vec3 position = player.position();
            Vec3 segment = position.subtract(lastPos);
            double segLen = Math.sqrt(segment.x * segment.x + segment.z * segment.z);
            int idle = segLen * segLen < DASH_IDLE_SPEED_SQR ? idleTicks + 1 : 0;

            double newCarry = carry;
            int newRingsLeft = ringsLeft;
            if (segLen > 0.0D) {
                RingTrailSpacing.Drops drops = RingTrailSpacing.getDrops(carry, segLen, ringsLeft);
                if (drops.offsets().length > 0) {
                    Vec3 segDir = new Vec3(segment.x, 0.0D, segment.z).normalize();
                    for (double offset : drops.offsets()) {
                        Vec3 anchor = lastPos.add(segDir.scale(offset));
                        sendRing(level, ModParticles.SHOCKWAVE_RING.get(), anchor.x, player.getY() + DASH_RING_HEIGHT,
                                anchor.z, direction);
                    }
                }
                newCarry = drops.carry();
                newRingsLeft = drops.ringsLeft();
            }
            return new DashBurst(direction, ticksLeft - 1, position, newCarry, newRingsLeft, idle);
        }
    }
}
