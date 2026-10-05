package net.alex.guzhenren.particle;

/**
 * The even-spacing accumulator behind the planted dash trail: how far into this tick's movement
 * segment the next rings drop. Distance-based rather than tick-based because the server-side dodge
 * movement arrives in uneven chunks (Epic Fight locks movement and lets the animation drive it),
 * so a per-tick drop clusters the rings at the start and end of the path;
 * spacing by measured travel keeps the trail uniform across windups, jumps and wall truncations.
 * {@link #SPACING} is the distance between two neighboring rings on the dash path, in blocks.
 *
 * <p>{@link #drops} takes the distance traveled since the last drop ({@code carry}, always
 * {@code < SPACING}), this tick's movement segment length ({@code segLen}), and the rings the burst
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

    static Drops drops(double carry, double segLen, int ringsLeft) {
        double along = SPACING - carry;
        int count = 0;
        while (count < ringsLeft && along + count * SPACING <= segLen) count++;
        double[] offsets = new double[count];
        for (int i = 0; i < count; i++) offsets[i] = along + i * SPACING;
        double newCarry = count == 0 ? carry + segLen : segLen - (along + (count - 1) * SPACING);
        return new Drops(offsets, newCarry, ringsLeft - count);
    }

    record Drops(double[] offsets, double carry, int ringsLeft) {}
}
