package net.alex.guzhenren.item.gu.mortal.path.strength;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HostedBeastGuTest {

    @Test
    @DisplayName("a dropped Gu keeps at least half its health, rounded up")
    void theFloorIsHalfRoundedUp() {
        assertEquals(4, HostedBeastGu.getMinDropHealth(8));
        assertEquals(5, HostedBeastGu.getMinDropHealth(9));
        assertEquals(6, HostedBeastGu.getMinDropHealth(12));
    }

    @Test
    @DisplayName("a dropped rank-two Gu's health spans six to twelve and reaches both ends")
    void healthSpansHalfToFull() {
        RandomSource random = RandomSource.create(20261114L);
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) seen.add(HostedBeastGu.rollHealth(12, random));
        assertEquals(Set.of(6, 7, 8, 9, 10, 11, 12), seen);
    }
}
