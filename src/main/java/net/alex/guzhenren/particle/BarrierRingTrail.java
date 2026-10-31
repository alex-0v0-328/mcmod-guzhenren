package net.alex.guzhenren.particle;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.flight.SecondStageCruise;
import net.minecraft.world.phys.Vec3;

/**
 * Where one second-stage flight [二段飞行] forms its sound barrier rings [音障环], one tick at a time: pure state,
 * so the pure JVM tests can pin it; {@link SecondStageRingEmitter} sends what {@link #step} returns.
 *
 * <p>The entry already booms (the sonic boom ring [音爆环]), so the barrier rings keep quiet for
 * {@link #QUIET_TICKS} after it. From then on a ring forms at once, since a fresh trail ({@link #start}) carries a
 * full spacing, if the flight is past {@link #BARRIER_SPEED}, three times plain creative flight at full speed
 * ({@link SecondStageCruise#PLAIN_FLIGHT_SPEED}), and one more every {@link #SPACING} blocks of actual 3D travel
 * past it, through {@link RingTrailSpacing} like the dash trail. Below the barrier nothing forms and no distance
 * counts. The speed is the per-tick movement smoothed by {@link #SMOOTHING}, since the server sees a player's moves
 * as they arrive, sometimes two to a tick.
 *
 * <p>Each {@link Ring} forms on the path at foot level, {@link #HEAD_LEAD} blocks ahead along the movement at the
 * leading face, and faces it; the flier passes through it and leaves it behind. A tick's movement past
 * {@link #TELEPORT_DISTANCE} is a teleport, not flight: the trail picks up again there, its speed reset, without
 * laying rings along the jump.
 *
 * <p>At 6 times flight speed (≈3.3 blocks a tick) a ring forms every ≈5 ticks, four a second.
 *
 * @author Alex
 * @version 1.0.0
 * @see SecondStageRingEmitter
 * @since 1.0.0
 */

record BarrierRingTrail(Vec3 lastPos, double carry, double speed, int age) {

    static final int QUIET_TICKS = 40;
    static final double SPACING = 16.0D;
    static final double BARRIER_SPEED = SecondStageCruise.BARRIER_SPEED;
    static final double HEAD_LEAD = 1.0D;
    static final double TELEPORT_DISTANCE = 16.0D;
    private static final double SMOOTHING = 0.5D;
    private static final double MIN_LENGTH = 1.0E-6D;

    static BarrierRingTrail start(Vec3 position) { return new BarrierRingTrail(position, SPACING, 0.0D, 0); }

    Step step(Vec3 position) {
        int nextAge = Math.min(age + 1, QUIET_TICKS + 1);
        Vec3 segment = position.subtract(lastPos);
        double length = segment.length();
        if (length > TELEPORT_DISTANCE) {
            return new Step(new BarrierRingTrail(position, carry, 0.0D, nextAge), List.of());
        }

        double smoothed = SMOOTHING * speed + (1.0D - SMOOTHING) * length;
        if (nextAge <= QUIET_TICKS || smoothed < BARRIER_SPEED || length < MIN_LENGTH) {
            return new Step(new BarrierRingTrail(position, carry, smoothed, nextAge), List.of());
        }

        Vec3 direction = segment.scale(1.0D / length);
        RingTrailSpacing.Drops drops = RingTrailSpacing.getDrops(SPACING, carry, length, Integer.MAX_VALUE);
        List<Ring> rings = new ArrayList<>();
        for (double offset : drops.offsets()) {
            rings.add(new Ring(lastPos.add(direction.scale(offset + HEAD_LEAD)), direction));
        }
        return new Step(new BarrierRingTrail(position, drops.carry(), smoothed, nextAge), rings);
    }

    record Ring(Vec3 center, Vec3 facing) {}

    record Step(BarrierRingTrail next, List<Ring> rings) {}
}
