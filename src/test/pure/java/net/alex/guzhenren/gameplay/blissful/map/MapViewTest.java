package net.alex.guzhenren.gameplay.blissful.map;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapViewTest {

    @Test
    @DisplayName("a view is at most 64 chunks a side; min above max on either axis is an unsubscribe")
    void validation() {
        assertTrue(new MapView(-32, -32, 31, 31).isValid());
        assertFalse(new MapView(-32, -32, 32, 31).isValid(), "65 chunks wide is ignored whole");
        assertFalse(new MapView(0, -32, 0, 32).isValid(), "65 chunks deep is ignored whole");
        assertTrue(new MapView(5, 5, 5, 5).isValid());
        assertTrue(new MapView(1, 0, 0, 0).isEmpty());
        assertTrue(new MapView(0, 1, 0, 0).isEmpty());
        assertTrue(MapView.NONE.isEmpty());
        assertFalse(MapView.NONE.isValid());
        assertFalse(new MapView(Integer.MIN_VALUE, 0, Integer.MAX_VALUE, 0).isValid(), "no overflow");
    }

    @Test
    @DisplayName("chunks queue nearest the view centre first, ties broken by z then x")
    void queueOrder() {
        long[] order = new MapView(0, 0, 2, 2).byDistance();
        assertEquals(9, order.length);
        assertEquals(ChunkPos.asLong(1, 1), order[0], "the centre chunk goes first");
        assertEquals(ChunkPos.asLong(1, 0), order[1]);
        assertEquals(ChunkPos.asLong(0, 1), order[2]);
        assertEquals(ChunkPos.asLong(2, 1), order[3]);
        assertEquals(ChunkPos.asLong(1, 2), order[4]);
        assertEquals(ChunkPos.asLong(0, 0), order[5], "corners come last");
        assertEquals(ChunkPos.asLong(2, 2), order[8]);
    }

    @Test
    @DisplayName("an even-sided view centres on the chunk corner, so the four middle chunks lead")
    void evenViewOrder() {
        long[] order = new MapView(-2, -2, 1, 1).byDistance();
        assertEquals(16, order.length);
        for (int i = 0; i < 4; i++) {
            int x = ChunkPos.getX(order[i]);
            int z = ChunkPos.getZ(order[i]);
            assertTrue(x >= -1 && x <= 0 && z >= -1 && z <= 0, "middle four first");
        }
        long lastDistance = -1;
        for (long chunk : order) {
            long distance = new MapView(-2, -2, 1, 1).distanceSq(ChunkPos.getX(chunk), ChunkPos.getZ(chunk));
            assertTrue(distance >= lastDistance, "never moves back toward the centre");
            lastDistance = distance;
        }
    }

    @Test
    @DisplayName("tiles are kept up to 32 chunks outside the view and dropped beyond")
    void keepMargin() {
        MapView view = new MapView(0, 0, 9, 9);
        assertTrue(view.keeps(-32, 41));
        assertFalse(view.keeps(-33, 0));
        assertFalse(view.keeps(0, 42));
        assertFalse(MapView.NONE.keeps(0, 0), "an empty view keeps nothing");
        assertTrue(view.contains(ChunkPos.asLong(9, 9)));
        assertFalse(view.contains(ChunkPos.asLong(10, 9)));
    }
}
