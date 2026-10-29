package net.alex.guzhenren.particle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.alex.guzhenren.registry.particle.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The rings of second-stage flight [二段飞行], server side, so every player in particle range sees them: the sonic
 * boom ring [音爆环] at each entry and the sound barrier rings [音障环] while the flight stays fast.
 *
 * <p>{@link #sonicBoom} answers each {@code SecondStageStartedEvent} ({@link SecondStageRingEvents}): one
 * {@code sonic_boom_ring} blooming to ≈4 blocks, facing the look direction, at mid-body ({@link #RING_HEIGHT}) and
 * {@link #BOOM_BEHIND} blocks back along the look, so it opens behind the flier's own first-person camera, which gets
 * the screen ring ({@code net.alex.guzhenren.client.hud.SonicBoomHud}) instead; and the warden's sonic boom at
 * {@link #BOOM_VOLUME}, heard ≈32 blocks out, as far as the ring is sent. It also starts the player's
 * {@link BarrierRingTrail} there.
 *
 * <p>{@link #tick} steps that trail while {@link SecondStageService#isActive} holds and sends a {@code barrier_ring}
 * at mid-body for each ring it forms: none for the first two seconds, then one every ≈16 blocks while the flight
 * stays past three times plain flight. A ring blooms small to large where it formed, then widens to four times its
 * size and fades out ({@code RingParticle}'s spread exit). At full speed that is four rings a second, each living
 * 15 ticks, ≈3 alive at once behind each flier. A trail found missing while the stage is on (a dimension change
 * dropped it) picks up where the player stands, quiet again. The trail ends with the stage; logout and a dimension
 * change drop it ({@link #forget}); {@link SecondStageRingEvents} calls {@link #tick} at the end of each player
 * tick.
 *
 * <p>Pure feedback: no damage, no hitbox.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.particle.RingParticle
 * @see RingConeEmitter
 * @since 1.0.0
 */

public final class SecondStageRingEmitter {

    private static final double RING_HEIGHT = 1.0D;
    private static final double BOOM_BEHIND = 1.0D;
    private static final float BOOM_VOLUME = 2.0F;
    private static final float BOOM_PITCH = 1.0F;

    private static final Map<UUID, BarrierRingTrail> TRAILS = new HashMap<>();

    private SecondStageRingEmitter() {}

    public static void sonicBoom(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Vec3 look = player.getLookAngle();
        Vec3 ring = player.position().add(0.0D, RING_HEIGHT, 0.0D).subtract(look.scale(BOOM_BEHIND));
        Vec3 velocity = look.scale(RingConeEmitter.RING_NORMAL_DRIFT);
        level.sendParticles(ModParticles.SONIC_BOOM_RING.get(), ring.x, ring.y, ring.z,
                0, velocity.x, velocity.y, velocity.z, 1.0D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS, BOOM_VOLUME, BOOM_PITCH);
        TRAILS.put(player.getUUID(), BarrierRingTrail.start(player.position()));
    }

    public static void tick(ServerPlayer player) {
        if (!SecondStageService.isActive(player) || player.isRemoved() || player.isDeadOrDying()
                || !(player.level() instanceof ServerLevel level)) {
            TRAILS.remove(player.getUUID());
            return;
        }
        BarrierRingTrail trail = TRAILS.get(player.getUUID());
        if (trail == null) {
            TRAILS.put(player.getUUID(), BarrierRingTrail.start(player.position()));
            return;
        }
        BarrierRingTrail.Step step = trail.step(player.position());
        for (BarrierRingTrail.Ring ring : step.rings()) {
            Vec3 velocity = ring.facing().scale(RingConeEmitter.RING_NORMAL_DRIFT);
            level.sendParticles(ModParticles.BARRIER_RING.get(), ring.center().x, ring.center().y + RING_HEIGHT,
                    ring.center().z, 0, velocity.x, velocity.y, velocity.z, 1.0D);
        }
        TRAILS.put(player.getUUID(), step.next());
    }

    public static void forget(Player player) { TRAILS.remove(player.getUUID()); }
}
