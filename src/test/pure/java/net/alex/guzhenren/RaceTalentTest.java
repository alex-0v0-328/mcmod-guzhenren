package net.alex.guzhenren;

import java.util.EnumSet;
import java.util.Set;
import net.alex.guzhenren.gameplay.body.Race;
import net.alex.guzhenren.gameplay.path.GuAttainment;
import net.alex.guzhenren.gameplay.path.GuPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaceTalentTest {

    @Test
    @DisplayName("Alex's own walkthrough: 宗师 -> 毛民 -> cultivate one -> 人族 lands on 准大宗师")
    void theWalkthrough() {
        GuAttainment asHuman = GuAttainment.GRANDMASTER;
        GuAttainment asHairy = asHuman.shift(Race.TALENT_SHIFT);
        assertSame(GuAttainment.QUASI_GREAT_GRANDMASTER, asHairy);

        GuAttainment cultivated = asHairy.shift(1);
        assertSame(GuAttainment.GREAT_GRANDMASTER, cultivated);

        assertSame(GuAttainment.QUASI_GREAT_GRANDMASTER, cultivated.shift(-Race.TALENT_SHIFT));
    }

    @Test
    @DisplayName("a shift is reversible everywhere the ladder has room -- earned grades are never stolen")
    void reversibleOffTheCeiling() {
        for (GuAttainment start : GuAttainment.values()) {
            if (start == GuAttainment.SUPREME_GRANDMASTER) continue;
            GuAttainment there = start.shift(Race.TALENT_SHIFT);
            assertSame(start, there.shift(-Race.TALENT_SHIFT), "from " + start);
        }
    }

    @Test
    @DisplayName("⚠ AT the ceiling the round trip LOSES one grade -- shift clamps, so the grant is a no-op")
    void ceilingLosesOneGrade() {
        GuAttainment top = GuAttainment.SUPREME_GRANDMASTER;
        assertSame(top, top.shift(Race.TALENT_SHIFT));
        assertSame(GuAttainment.QUASI_SUPREME_GRANDMASTER, top.shift(Race.TALENT_SHIFT).shift(-Race.TALENT_SHIFT));
        assertNotSame(top, top.shift(Race.TALENT_SHIFT).shift(-Race.TALENT_SHIFT));
    }

    @Test
    @DisplayName("the floor cannot be drained -- a revoke at 无 [NONE] stays there")
    void floorIsStable() {
        assertSame(GuAttainment.NONE, GuAttainment.NONE.shift(-Race.TALENT_SHIFT));
        assertSame(GuAttainment.ORDINARY, GuAttainment.NONE.shift(Race.TALENT_SHIFT));
    }

    @Test
    @DisplayName("no two races share a talent path, so a race change can never grant and revoke the same one")
    void everyVariantOwnsItsOwnPath() {
        Set<GuPath> taken = EnumSet.noneOf(GuPath.class);
        int variants = 0;
        for (Race race : Race.values()) {
            if (!race.isVariant()) continue;
            variants++;
            assertTrue(taken.add(race.getTalentPath()), "duplicate path on " + race);
        }
        assertEquals(variants, taken.size());
        assertSame(null, Race.HUMAN.getTalentPath());
    }
}
