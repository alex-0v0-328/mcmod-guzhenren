package net.alex.guzhenren;

import java.util.ArrayList;
import java.util.List;
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
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StrengthPathBranchTooltipGameTests {

    private StrengthPathBranchTooltipGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void beastStrengthPhantomBranchIsShown(GameTestHelper helper) {
        for (Item item : List.of(ModItems.WHITE_BOAR_GU_1.get(), ModItems.BLACK_BOAR_GU_1.get(),
                ModItems.FLOWER_BOAR_GU_1.get(), ModItems.BEAR_STRENGTH_GU_1.get(), ModItems.TIGER_STRENGTH_GU_2.get(),
                ModItems.STONE_TURTLE_STRENGTH_GU_3.get(), ModItems.GALLOPING_HORSE_STRENGTH_GU_3.get(),
                ModItems.GREEN_BULL_LABOR_GU_3.get(), ModItems.BRUTE_FORCE_LONGHORN_BEETLE_GU_1.get())) {
            assertBranch(item, "guzhenren.enum.strength.strength_path_branch.beast_strength_phantom");
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void humanJunStrengthBranchIsShown(GameTestHelper helper) {
        for (Item item : List.of(ModItems.JIN_STRENGTH_GU_1.get(), ModItems.TENS_JIN_STRENGTH_GU_2.get(),
                ModItems.JUN_STRENGTH_GU_3.get(), ModItems.TENS_JUN_STRENGTH_GU_4.get())) {
            assertBranch(item, "guzhenren.enum.strength.strength_path_branch.human_jun_strength");
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void normalStrengthPathBranchIsHidden(GameTestHelper helper) {
        assertFalse(hasStrengthPathBranch(ModItems.ALL_OUT_EFFORT_GU_3.get()));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void nonStrengthPathGuNeverShowStrengthPathBranch(GameTestHelper helper) {
        assertFalse(hasStrengthPathBranch(ModItems.SECOND_WATCH_GU_4.get()));
        helper.succeed();
    }

    private static boolean hasStrengthPathBranch(Item item) {
        return keys(item).stream().anyMatch(key -> key.startsWith(
                "guzhenren.enum.strength.strength_path_branch."));
    }

    private static void assertBranch(Item item, String key) {
        List<String> tooltipKeys = keys(item);
        assertTrue(tooltipKeys.size() > 1, item.toString());
        assertEquals(key, tooltipKeys.get(1), item.toString());
    }

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
