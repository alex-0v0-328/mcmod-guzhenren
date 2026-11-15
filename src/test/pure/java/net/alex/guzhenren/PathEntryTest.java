package net.alex.guzhenren;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.alex.guzhenren.gameplay.path.GuAttainment;
import net.alex.guzhenren.gameplay.path.PathEntry;
import net.alex.guzhenren.gameplay.path.PathMarks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathEntryTest {

    @Test
    @DisplayName("an entry with no attainment and no bare marks of either kind is the default")
    void defaultNeedsBothCountersEmpty() {
        assertTrue(PathEntry.DEFAULT.withMarks(new PathMarks(-4L, 0L)).isDefault());
        assertFalse(PathEntry.DEFAULT.withMarks(PathMarks.ofPoints(1L)).isDefault());
        assertFalse(PathEntry.DEFAULT.withMarks(PathMarks.ofMarks(1L)).isDefault());
        assertFalse(PathEntry.DEFAULT.withAttainment(GuAttainment.ORDINARY).isDefault());
    }

    @Test
    @DisplayName("bare marks and points save inline as two fields, and the old tagged marks map reads as none")
    void countersRoundTripAndOldMarksReadAsNone() {
        JsonObject saved = JsonParser.parseString("""
                {"attainment":"grandmaster","marks":7,"points":42}
                """).getAsJsonObject();
        PathEntry decoded = PathEntry.CODEC.parse(JsonOps.INSTANCE, saved).result().orElseThrow();
        assertSame(GuAttainment.GRANDMASTER, decoded.attainment());
        assertEquals(new PathMarks(7L, 42L), decoded.marks());
        JsonObject written = PathEntry.CODEC.encodeStart(JsonOps.INSTANCE, decoded).result().orElseThrow()
                .getAsJsonObject();
        assertEquals(7L, written.get("marks").getAsLong());
        assertEquals(42L, written.get("points").getAsLong());

        JsonObject old = JsonParser.parseString("""
                {"attainment":"master","marks":{"natural":99}}
                """).getAsJsonObject();
        PathEntry legacy = PathEntry.CODEC.parse(JsonOps.INSTANCE, old).result().orElseThrow();
        assertSame(GuAttainment.MASTER, legacy.attainment());
        assertTrue(legacy.marks().isEmpty());
    }
}
