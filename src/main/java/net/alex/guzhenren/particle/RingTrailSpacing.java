package net.alex.guzhenren.particle;

/**
 * The even-spacing accumulator behind the planted dash trail and the sound barrier rings [音障环]: how far
 * into this tick's movement segment the next rings drop. Distance-based rather than tick-based because the
 * server-side dodge movement arrives in uneven chunks (Epic Fight locks movement and lets the animation drive it),
 * so a per-tick drop clusters the rings at the start and end of the path;
 * spacing by measured travel keeps the trail uniform across windups, jumps and wall truncations.
 * {@link #SPACING} is the distance between two neighboring rings on the dash path, in blocks; the sound barrier
 * rings pass their own spacing ({@code BarrierRingTrail}).
 *
 * <p>{@link #getDrops} takes the spacing, the distance traveled since the last drop ({@code carry}, below the
 * spacing, or exactly the spacing for a ring that is due at the segment's start), this tick's movement segment
 * length ({@code segLen}), and the rings the burst
 * may still drop ({@code ringsLeft}), and returns a {@link Drops} of where on the segment the rings
 * drop, plus the new carry and remaining ring budget; {@link Drops#offsets} are the ascending
 * offsets along this tick's segment where rings drop.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

final class RingTrailSpacing {

    private RingTrailSpacing() {}

    static final double SPACING = 2.0D;

    static Drops getDrops(double carry, double segLen, int ringsLeft) {
        return getDrops(SPACING, carry, segLen, ringsLeft);
    }

    static Drops getDrops(double spacing, double carry, double segLen, int ringsLeft) {
        double along = spacing - carry;
        int count = 0;
        while (count < ringsLeft && along + count * spacing <= segLen) count++;
        double[] offsets = new double[count];
        for (int i = 0; i < count; i++) offsets[i] = along + i * spacing;
        double newCarry = count == 0 ? carry + segLen : segLen - (along + (count - 1) * spacing);
        return new Drops(offsets, newCarry, ringsLeft - count);
    }

    record Drops(double[] offsets, double carry, int ringsLeft) {}
}
