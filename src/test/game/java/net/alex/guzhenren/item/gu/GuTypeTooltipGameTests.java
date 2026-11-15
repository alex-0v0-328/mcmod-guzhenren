package net.alex.guzhenren.item.gu;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GuTypeTooltipGameTests {

    private GuTypeTooltipGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void everyTypedGuShowsItsTypeLine(GameTestHelper helper) {
        for (var entry : ModItems.ITEMS.getEntries()) {
            if (!(entry.get() instanceof MortalGuItem gu)) continue;

            GuType type = gu.guProperties.getType();
            List<String> keys = keys(gu);
            if (type == null) {
                assertFalse(keys.contains("guzhenren.display.gu_type"), entry.getId().toString());
                assertFalse(keys.contains("guzhenren.display.gu_type_branch"), entry.getId().toString());
            } else {
                assertTrue(keys.contains(type.isBranch() ? "guzhenren.display.gu_type_branch"
                        : "guzhenren.display.gu_type"), entry.getId().toString());
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theJadeSkinGuAreDefensiveAndTheLifespanGuHaveNoType(GameTestHelper helper) {
        assertEquals(GuType.DEFENSIVE, type(ModItems.JADE_SKIN_GU_1.get()));
        assertEquals(GuType.DEFENSIVE, type(ModItems.WHITE_JADE_GU_2.get()));
        assertEquals(GuType.PATH_MARK_CARVING, type(ModItems.WHITE_BOAR_GU_1.get()));
        assertEquals(GuType.MOVEMENT, type(ModItems.HORIZONTAL_CRASH_GU_3.get()));
        assertNull(type(ModItems.LIFESPAN_GU.get()));
        assertNull(type(ModItems.ROAMING_ZOMBIE_GU_2.get()));
        helper.succeed();
    }

    private static GuType type(Item item) { return ((MortalGuItem) item).guProperties.getType(); }

    private static List<String> keys(Item item) {
        List<Component> tooltip = new ArrayList<>();
        item.appendHoverText(new ItemStack(item), Item.TooltipContext.EMPTY, tooltip, TooltipFlag.NORMAL);
        return tooltip.stream()
                .map(Component::getContents)
                .filter(TranslatableContents.class::isInstance)
                .map(TranslatableContents.class::cast)
                .map(TranslatableContents::getKey)
                .toList();
    }
}
