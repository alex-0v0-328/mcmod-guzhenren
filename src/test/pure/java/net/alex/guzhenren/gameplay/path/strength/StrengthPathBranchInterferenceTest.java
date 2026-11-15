package net.alex.guzhenren.gameplay.path.strength;

import java.util.EnumSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 分支声明的道痕互斥：对称；仅兽力虚影流与人力钧力流互斥；活玩家拒绝见 {@code MarkInterferenceGameTests}。 */

class StrengthPathBranchInterferenceTest {

    @Test
    @DisplayName("interference is symmetric")
    void interferenceIsSymmetric() {
        for (StrengthPathBranch a : StrengthPathBranch.values()) {
            for (StrengthPathBranch b : StrengthPathBranch.values()) {
                assertEquals(a.interferesWith(b), b.interferesWith(a), a + " / " + b);
            }
        }
    }

    @Test
    @DisplayName("only beast and human strength interfere")
    void onlyBeastAndHumanStrengthInterfere() {
        assertTrue(StrengthPathBranch.BEAST_STRENGTH_PHANTOM.interferesWith(StrengthPathBranch.HUMAN_JUN_STRENGTH));
        for (StrengthPathBranch a : StrengthPathBranch.values()) {
            assertFalse(a.interferesWith(a), a + " interferes with itself");
            for (StrengthPathBranch b : StrengthPathBranch.values()) {
                boolean pair = EnumSet.of(a, b).equals(EnumSet.of(StrengthPathBranch.BEAST_STRENGTH_PHANTOM,
                        StrengthPathBranch.HUMAN_JUN_STRENGTH));
                assertEquals(pair, a.interferesWith(b), a + " / " + b);
            }
        }
    }
}
