package net.alex.guzhenren.gameplay.aperture;

import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ApertureTalentMarksTest {

    @Test
    void extremePhysiqueMarksSplitAcrossTalentPaths() {
        assertEquals(1_200L, ApertureService.getTalentMarksPerPath(ExtremePhysique.VERDANT_GREAT_SUN));
        assertEquals(600L, ApertureService.getTalentMarksPerPath(ExtremePhysique.NORTHERN_DARK_ICE_SOUL));
    }

    @Test
    void beastStrengthsAggregateByFamily() {
        PathStrengthData data = PathStrengthData.DEFAULT.with(BeastStrength.WHITE_BOAR)
                .with(BeastStrength.BLACK_BOAR).with(BeastStrength.BEAR);

        assertEquals(2, data.getBeastReadings().get(BeastStrengthFamily.BOAR));
        assertEquals(1, data.getBeastReadings().get(BeastStrengthFamily.BEAR));
    }
}
