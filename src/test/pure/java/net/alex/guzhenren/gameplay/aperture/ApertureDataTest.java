package net.alex.guzhenren.gameplay.aperture;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 双空窍[Aperture]列表：容量、第一与第二空窍各是哪个。 */

class ApertureDataTest {

    private static final Aperture FIRST = Aperture.createOpenedAt(80);
    private static final Aperture SECOND = Aperture.createSecond(Rank.ONE);

    @Test
    @DisplayName("an empty list holds no aperture; one aperture is not full, two are")
    void theListFillsAtTwo() {
        assertFalse(ApertureData.DEFAULT.hasAperture());
        assertFalse(ApertureData.DEFAULT.isFull());
        ApertureData one = new ApertureData(List.of(FIRST));
        assertTrue(one.hasAperture());
        assertFalse(one.isFull());
        assertTrue(new ApertureData(List.of(FIRST, SECOND)).isFull());
    }

    @Test
    @DisplayName("a list longer than two keeps its first two, and a list of two is left whole")
    void theListIsCappedAtTwo() {
        assertEquals(List.of(FIRST, SECOND), new ApertureData(List.of(FIRST, SECOND)).apertures());
        assertEquals(List.of(FIRST, SECOND), new ApertureData(List.of(FIRST, SECOND, FIRST)).apertures());
    }

    @Test
    @DisplayName("an index past either end reads as no aperture, and the ends themselves read")
    void indexesOutsideTheListReadNone() {
        ApertureData both = new ApertureData(List.of(FIRST, SECOND));
        assertSame(Aperture.NONE, both.get(-1));
        assertSame(Aperture.NONE, both.get(2));
        assertSame(FIRST, both.get(0));
        assertSame(SECOND, both.get(1));
        assertSame(FIRST, both.getPrimary());
    }

    @Test
    @DisplayName("the first aperture is the one not marked second, wherever the list keeps it")
    void theFirstIndexIsTheUnmarkedOne() {
        ApertureData secondAhead = new ApertureData(List.of(SECOND, FIRST));
        assertEquals(1, secondAhead.getFirstIndex());
        assertEquals(0, secondAhead.getSecondIndex());
        assertEquals(1, secondAhead.indexOf(ApertureData.PRIMARY));
        assertEquals(0, secondAhead.indexOf(ApertureData.SECOND));
        assertEquals(ApertureData.SECOND, secondAhead.getKindAt(0));
        assertEquals(ApertureData.PRIMARY, secondAhead.getKindAt(1));
        ApertureData firstAhead = new ApertureData(List.of(FIRST, SECOND));
        assertEquals(0, firstAhead.getFirstIndex());
        assertEquals(1, firstAhead.getSecondIndex());
    }

    @Test
    @DisplayName("a lone second aperture is not awakened, and neither is an empty list")
    void onlyAFirstApertureAwakens() {
        assertFalse(ApertureData.DEFAULT.isAwakened());
        assertEquals(-1, ApertureData.DEFAULT.getFirstIndex());
        ApertureData lone = new ApertureData(List.of(SECOND));
        assertFalse(lone.isAwakened());
        assertEquals(-1, lone.getFirstIndex());
        assertEquals(-1, new ApertureData(List.of(FIRST)).getSecondIndex());
        assertTrue(new ApertureData(List.of(FIRST)).isAwakened());
    }

    @Test
    @DisplayName("a full list opens nothing more")
    void aFullListOpensNothing() {
        ApertureData both = new ApertureData(List.of(FIRST, SECOND));
        assertSame(both, both.withOpened(FIRST));
        assertEquals(List.of(FIRST), ApertureData.DEFAULT.withOpened(FIRST).apertures());
    }
}
