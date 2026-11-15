package net.alex.guzhenren.gameplay.aperture.pressure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 空窍[Aperture]压力爆炸坑列哈希：同种子挖同坑，故抖动、三种散布、雪环边缘钉到固定值与范围，不只是「某个哈希」。 */

class CraterShapeTest {

    private static final long SEED = 20261109L;
    private static final double EPSILON = 1.0E-12D;
    private static final String LAVA = String.join("",
            "#..#.....#......",
            "..#...#.........",
            "......#.....#...",
            ".#...#..........",
            "#.....#.........",
            "..#.....#.#...#.",
            ".###..#...##..#.",
            "...........#....",
            "##............#.",
            "#.....#.....#...",
            ".###.##.....#...",
            "...#.#....#...#.",
            ".#.##..#........",
            "..#........##.#.",
            "...#....#...###.",
            ".....##.#..#...#");
    private static final String GOLD = String.join("",
            ".#......###....#",
            "..........#..#..",
            ".......#........",
            "...#............",
            "#...............",
            "..#.............",
            "..#.......#...#.",
            "#......#..#.....",
            "................",
            "#..........#....",
            ".#....#......#..",
            ".....#..##...#..",
            ".#........#...#.",
            "................",
            "................",
            "#...............");
    private static final String POWDER_SNOW = String.join("",
            ".#..#.#..###..#.",
            "##.##..#..#..#.#",
            ".#....#.##.###.#",
            "##...###..##....",
            "#.##..##.#..#...",
            ".#.#..#####...#.",
            "#.#...##.#.#.#.#",
            "##.....#..#...##",
            "...####...#####.",
            "...#...##.#..##.",
            "..#....#.####...",
            "...#.#.##.#..###",
            "..##.#..#...#.##",
            "####..#..###.#.#",
            ".....#.#.##.##.#",
            "#..##.#.#..#.###");

    private static String mask(ColumnTest scatter) {
        StringBuilder mask = new StringBuilder();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) mask.append(scatter.at(x, z) ? '#' : '.');
        }
        return mask.toString();
    }

    private interface ColumnTest {

        boolean at(int blockX, int blockZ);
    }

    @Test
    @DisplayName("a column's jitter is fixed by its place and the seed, and sits in [0, 0.1)")
    void theJitterIsAFunctionOfThePlaceAndTheSeed() {
        assertEquals(0.026638889312744142D, CraterShape.getColumnJitter(-90, 51, SEED), EPSILON);
        assertEquals(0.09539422988891602D, CraterShape.getColumnJitter(-53, 22, SEED), EPSILON);
        assertEquals(0.019304561614990237D, CraterShape.getColumnJitter(58, -65, SEED), EPSILON);
        assertNotEquals(CraterShape.getColumnJitter(-90, 51, SEED), CraterShape.getColumnJitter(-90, 51, SEED + 1L));
        double lowest = 1.0D;
        double highest = 0.0D;
        for (int z = -60; z < 60; z++) {
            for (int x = -60; x < 60; x++) {
                double jitter = CraterShape.getColumnJitter(x, z, SEED);
                lowest = Math.min(lowest, jitter);
                highest = Math.max(highest, jitter);
            }
        }
        assertTrue(lowest >= 0.0D && lowest < 0.001D, "the low end is reached: " + lowest);
        assertTrue(highest < 0.1D && highest > 0.099D, "the high end stays under a tenth: " + highest);
    }

    @Test
    @DisplayName("lava, gold and powder snow scatter over the same columns the same way for a seed")
    void theScattersAreStablePerSeed() {
        assertEquals(LAVA, mask((x, z) -> CraterShape.isLavaColumn(x, z, SEED)));
        assertEquals(GOLD, mask((x, z) -> CraterShape.isGoldColumn(x, z, SEED)));
        assertEquals(POWDER_SNOW, mask((x, z) -> CraterShape.isPowderSnowColumn(x, z, SEED)));
    }

    @Test
    @DisplayName("each scatter hashes under its own salt, so lava and gold never line up")
    void theScattersDoNotLineUp() {
        int lava = 0;
        int gold = 0;
        int both = 0;
        for (int z = -100; z < 100; z++) {
            for (int x = -100; x < 100; x++) {
                boolean isLava = CraterShape.isLavaColumn(x, z, SEED);
                boolean isGold = CraterShape.isGoldColumn(x, z, SEED);
                if (isLava) lava++;
                if (isGold) gold++;
                if (isLava && isGold) both++;
            }
        }
        assertTrue(both < gold / 2, "lava and gold overlap on " + both + " of " + gold + " gold columns");
        assertTrue(both < lava / 2);
    }

    @Test
    @DisplayName("the snow ring ends 8 to 16 blocks past the crater, at most its reach, and every part of it is used")
    void theRingEdgeStaysInItsBand() {
        int radius = 20;
        double nearest = Double.MAX_VALUE;
        double farthest = 0.0D;
        for (int z = -100; z < 100; z++) {
            for (int x = -100; x < 100; x++) {
                double edge = CraterShape.getRingOuterRadius(radius, x, z, SEED);
                nearest = Math.min(nearest, edge);
                farthest = Math.max(farthest, edge);
            }
        }
        assertTrue(nearest >= radius + 8 && nearest < radius + 8.01D, "nearest " + nearest);
        assertTrue(farthest < radius + 16 && farthest > radius + 15.99D, "farthest " + farthest);
        assertEquals(radius + 16, CraterShape.getRingReach(radius));
        assertEquals(32.63392639160156D, CraterShape.getRingOuterRadius(radius, -90, 51, SEED), 1.0E-4D);
    }
}
