package net.alex.guzhenren.item.gu;

import java.util.List;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuTypeTest {

    @Test
    @DisplayName("the four branches name their parents and no top-level type has one")
    void branchesNameTheirParents() {
        assertSame(GuType.DEFENSIVE, GuType.RETALIATION.getParent());
        assertSame(GuType.MOVEMENT, GuType.STEED.getParent());
        assertSame(GuType.MOVEMENT, GuType.FLYING.getParent());
        assertSame(GuType.INVESTIGATION, GuType.IDENTITY.getParent());
        for (GuType type : GuType.values()) {
            if (List.of(GuType.RETALIATION, GuType.STEED, GuType.FLYING, GuType.IDENTITY).contains(type)) {
                assertTrue(type.isBranch(), type.name());
                assertFalse(type.getParent().isBranch(), type.name());
            } else {
                assertFalse(type.isBranch(), type.name());
                assertNull(type.getParent(), type.name());
            }
        }
    }

    @Test
    @DisplayName("a GuProperties carries at most one type and none by default")
    void specCarriesOneTypeOrNone() {
        assertNull(GuProperties.of(net.alex.guzhenren.gameplay.aperture.Rank.ONE,
                net.alex.guzhenren.gameplay.path.GuPath.STRENGTH).getType());
        assertSame(GuType.DEFENSIVE, GuProperties.of(net.alex.guzhenren.gameplay.aperture.Rank.ONE,
                net.alex.guzhenren.gameplay.path.GuPath.STRENGTH).type(GuType.DEFENSIVE).getType());
    }

    @Test
    @DisplayName("a top-level type is one line, a branch is its parent then itself")
    void tooltipLineWordsTheParentBeforeTheBranch() {
        TranslatableContents plain = (TranslatableContents) MortalGuItem.formatType(GuType.HEALING).getContents();
        assertEquals("guzhenren.display.gu_type", plain.getKey());
        assertEquals(1, plain.getArgs().length);

        TranslatableContents branch = (TranslatableContents) MortalGuItem.formatType(GuType.FLYING).getContents();
        assertEquals("guzhenren.display.gu_type_branch", branch.getKey());
        assertEquals(2, branch.getArgs().length);
        assertEquals("guzhenren.enum.gu.gu_type.movement", keyOf(branch.getArgs()[0]));
        assertEquals("guzhenren.enum.gu.gu_type.flying", keyOf(branch.getArgs()[1]));
    }

    private static String keyOf(Object arg) {
        return ((TranslatableContents) ((net.minecraft.network.chat.Component) arg).getContents()).getKey();
    }
}
