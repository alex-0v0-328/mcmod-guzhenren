package net.alex.guzhenren.client.radial;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link RadialLayout} 测试：扇区 0 朝上，其余在 GUI 坐标（y 向下）顺时针；边界居两中心正中；死区；单条目铺满整环。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

class RadialLayoutTest {

    private static final double DEAD_ZONE = 30.0D;
    private static final double EPS = 1.0E-9D;

    @Test
    @DisplayName("four sectors run up, right, down, left")
    void fourSectorsRunClockwiseFromUp() {
        assertEquals(0, RadialLayout.sectorAt(0.0D, -50.0D, 4, DEAD_ZONE));
        assertEquals(1, RadialLayout.sectorAt(50.0D, 0.0D, 4, DEAD_ZONE));
        assertEquals(2, RadialLayout.sectorAt(0.0D, 50.0D, 4, DEAD_ZONE));
        assertEquals(3, RadialLayout.sectorAt(-50.0D, 0.0D, 4, DEAD_ZONE));
    }

    @Test
    @DisplayName("sector 0 straddles straight up, so just left of up is still sector 0")
    void sectorZeroStraddlesUp() {
        assertEquals(0, RadialLayout.sectorAt(-10.0D, -50.0D, 4, DEAD_ZONE));
        assertEquals(0, RadialLayout.sectorAt(10.0D, -50.0D, 4, DEAD_ZONE));
        assertEquals(3, RadialLayout.sectorAt(-50.0D, -40.0D, 4, DEAD_ZONE));
        assertEquals(1, RadialLayout.sectorAt(50.0D, -40.0D, 4, DEAD_ZONE));
    }

    @Test
    @DisplayName("inside the dead zone, or with no entries, nothing is pointed at")
    void deadZoneAndEmptyPointAtNothing() {
        assertEquals(RadialLayout.NONE, RadialLayout.sectorAt(10.0D, -20.0D, 4, DEAD_ZONE));
        assertEquals(RadialLayout.NONE, RadialLayout.sectorAt(0.0D, -50.0D, 0, DEAD_ZONE));
        assertEquals(0, RadialLayout.sectorAt(0.0D, -DEAD_ZONE, 4, DEAD_ZONE));
    }

    @Test
    @DisplayName("one entry owns the whole ring")
    void oneEntryOwnsTheRing() {
        assertEquals(0, RadialLayout.sectorAt(0.0D, 50.0D, 1, DEAD_ZONE));
        assertEquals(0, RadialLayout.sectorAt(-50.0D, 1.0D, 1, DEAD_ZONE));
    }

    @Test
    @DisplayName("a sector's center angle lands back in that sector at any radius past the dead zone")
    void centreAnglesRoundTrip() {
        for (int count = 1; count <= 9; count++) {
            for (int i = 0; i < count; i++) {
                double angle = RadialLayout.centerAngle(i, count);
                double x = RadialLayout.x(55.0D, angle);
                double y = RadialLayout.y(55.0D, angle);
                assertEquals(i, RadialLayout.sectorAt(x, y, count, DEAD_ZONE), count + " sectors, sector " + i);
                assertEquals(55.0D, Math.hypot(x, y), EPS);
            }
        }
    }
}
