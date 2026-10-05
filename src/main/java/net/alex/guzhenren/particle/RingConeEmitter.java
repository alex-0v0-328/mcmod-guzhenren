package net.alex.guzhenren.particle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.particle.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The server-side cone trail behind the shockwave ring [激波环] (Alex, 2026-09-20 planted-trail
 * spec): each ring blooms in place from smallest to largest where it was born.
 *
 * <p>{@link #dashCone} plants the dash trail along the path, one ring every
 * {@link RingTrailSpacing#SPACING} blocks of ACTUAL travel -- the server-side dodge movement
 * arrives in uneven chunks (Epic Fight locks movement and lets the animation drive it), so a
 * per-tick drop clusters the rings at the start and end of the path (Alex, 2026-09-20), while
 * spacing by measured travel stays uniform across windups, jumps and wall truncations. The ring at
 * the dash start appears first and the one near the end last, up to {@link #DASH_MAX_RINGS} rings
 * (~16 blocks of path). {@link #punchCone} replays the same planted logic on a fixed ray: the ring
 * at the strike point (the victim's hitbox center) appears first, then one per tick every
 * {@link RingTrailSpacing#SPACING} blocks behind the target -- up to {@link #PUNCH_MAX_RINGS}
 * rings, a long tail running ~16 blocks past the strike point (Alex, 2026-09-20: "the rings behind
 * spread over a very long distance"). The ring velocity is only a facing-normal carrier
 * ({@link #RING_NORMAL_DRIFT}, blocks per tick): a zero vector would trip {@code RingGeometry}'s
 * no-direction fallback and lay the ring flat, and over the ring's whole life the drift moves it
 * under a quarter block, which reads as stationary. Both {@link DashBurst#tick} and
 * {@link PunchBurst#tick} send the particle with count 0 and speed 1.0, handing it the exact
 * velocity, which the ring also reads as its facing normal ({@code FACING_MOTION}).
 *
 * <p>{@link #DASH_BURSTS} and {@link #PUNCH_BURSTS} hold one active burst per effect per player, on
 * separate maps so a dash trail and a punch trail coexist (charging-crash play mixes the two): a
 * retrigger while a dash burst is alive (a second payload inside the window) used to replay the
 * whole trail -- Alex's 2026-09-19 "the cone plays twice" report -- so a dash inside its own window
 * is now skipped instead of replacing it, while a punch retrigger restarts the punch trail. The
 * {@link Burst} interface's {@code tick} drops one tick's ring(s) and returns the next state, or
 * null once the burst is spent. {@link DashBurst} positions each ring along the tick's movement
 * segment so even a chunked dodge spreads them evenly, and ends when the ring budget is spent, the
 * player stands still for {@link #DASH_IDLE_LIMIT} ticks (a dodge windup is shorter) below
 * {@link #DASH_IDLE_SPEED_SQR} squared horizontal speed (0.05 blocks/tick counts as still), or the
 * {@link #DASH_MAX_TICKS} safety cap runs out. {@link #DASH_RING_HEIGHT} is the dash ring's spawn
 * height above the feet, the standing ring's mid-body (max radius 1.1). {@link PunchBurst} plants
 * the same logic on the fixed ray described above.
 *
 * <p>Future speed- or force-feel actions call the {@code dashCone}/{@code punchCone} entry points
 * (or add a sibling) rather than spawning rings directly. Pure visual feedback: no damage, no
 * hitbox.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.particle.RingParticle
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class RingConeEmitter {

    private static final double MIN_DIRECTION_LENGTH_SQR = 1.0E-6D;
    private static final int DASH_MAX_RINGS = 8;
    private static final int DASH_MAX_TICKS = 25;
    private static final int DASH_IDLE_LIMIT = 3;
    private static final double DASH_IDLE_SPEED_SQR = 0.0025D;
    private static final double DASH_RING_HEIGHT = 1.0D;
    private static final double RING_NORMAL_DRIFT = 0.03D;
    private static final int PUNCH_MAX_RINGS = 8;

    private static final Map<UUID, Burst> DASH_BURSTS = new HashMap<>();
    private static final Map<UUID, Burst> PUNCH_BURSTS = new HashMap<>();

    private RingConeEmitter() {}

    public static void dashCone(ServerPlayer player, Vec3 motionDirection) {
        if (DASH_BURSTS.containsKey(player.getUUID())) return;
        if (motionDirection.lengthSqr() < MIN_DIRECTION_LENGTH_SQR) return;
        DASH_BURSTS.put(player.getUUID(), new DashBurst(motionDirection.normalize(),
                DASH_MAX_TICKS, player.position(), 0.0D, DASH_MAX_RINGS, 0));
    }

    public static void punchCone(ServerPlayer player, Vec3 look, Vec3 strikePoint) {
        if (look.lengthSqr() < MIN_DIRECTION_LENGTH_SQR) return;
        PUNCH_BURSTS.put(player.getUUID(), new PunchBurst(look.normalize(), PUNCH_MAX_RINGS, strikePoint));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isRemoved() || player.isDeadOrDying()
                || !(player.level() instanceof ServerLevel level)) {
            DASH_BURSTS.remove(player.getUUID());
            PUNCH_BURSTS.remove(player.getUUID());
            return;
        }
        tickBurst(DASH_BURSTS, player, level);
        tickBurst(PUNCH_BURSTS, player, level);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DASH_BURSTS.remove(event.getEntity().getUUID());
        PUNCH_BURSTS.remove(event.getEntity().getUUID());
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
                RingTrailSpacing.Drops drops = RingTrailSpacing.drops(carry, segLen, ringsLeft);
                if (drops.offsets().length > 0) {
                    Vec3 segDir = new Vec3(segment.x, 0.0D, segment.z).normalize();
                    Vec3 velocity = direction.scale(RING_NORMAL_DRIFT);
                    for (double offset : drops.offsets()) {
                        Vec3 anchor = lastPos.add(segDir.scale(offset));
                        level.sendParticles(ModParticles.SHOCKWAVE_RING.get(),
                                anchor.x, player.getY() + DASH_RING_HEIGHT, anchor.z,
                                0, velocity.x, velocity.y, velocity.z, 1.0D);
                    }
                }
                newCarry = drops.carry();
                newRingsLeft = drops.ringsLeft();
            }
            return new DashBurst(direction, ticksLeft - 1, position, newCarry, newRingsLeft, idle);
        }
    }

    private record PunchBurst(Vec3 direction, int ringsLeft, Vec3 anchor) implements Burst {

        @Override
        public Burst tick(ServerPlayer player, ServerLevel level) {
            if (ringsLeft <= 0) return null;
            Vec3 velocity = direction.scale(RING_NORMAL_DRIFT);
            int index = PUNCH_MAX_RINGS - ringsLeft;
            Vec3 ring = anchor.add(direction.scale(RingTrailSpacing.SPACING * index));
            level.sendParticles(ModParticles.IMPACT_RING.get(), ring.x, ring.y, ring.z,
                    0, velocity.x, velocity.y, velocity.z, 1.0D);
            return new PunchBurst(direction, ringsLeft - 1, anchor);
        }
    }
}
