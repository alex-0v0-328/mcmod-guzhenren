package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.item.Booklet;
import net.alex.guzhenren.item.BookletItem;
import net.alex.guzhenren.item.Chronicle;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveBookItem;
import net.alex.guzhenren.refinement.GuRecipeItem;
import net.alex.guzhenren.registry.item.ModCreativeTabs;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertSame;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BookletGameTests {

    private BookletGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void readingInEitherHandKeepsTheBookletUntouched(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-booklet-gametest").onGround();
        Item booklet = ModItems.GU_ATLAS_HOPE.get();
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = new ItemStack(booklet);
            player.setItemInHand(hand, stack);

            InteractionResultHolder<ItemStack> result = booklet.use(player.level(), player, hand);

            assertTrue(result.getResult().consumesAction(), hand + " read was refused");
            assertSame(stack, result.getObject());
            assertSame(stack, player.getItemInHand(hand));
            assertEquals(1, stack.getCount());
            assertTrue(ItemStack.isSameItemSameComponents(stack, new ItemStack(booklet)),
                    hand + " read wrote the book");
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void guAtlasHopeIsOneBookletOfItsText(GameTestHelper helper) {
        Item item = ModItems.GU_ATLAS_HOPE.get();
        assertTrue(item instanceof BookletItem booklet && booklet.getText() == Booklet.GU_ATLAS_HOPE,
                "Gu Atlas: Hope carries another text");
        assertFalse(item instanceof GuItem, "a booklet took on Gu properties");
        assertEquals(1, new ItemStack(item).getMaxStackSize());
        assertSame(ModItems.HOPE_GU.get(), Booklet.GU_ATLAS_HOPE.getFigure().orElseThrow().getItem());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void humanAncestorLegendShelvesItsSectionsInOrder(GameTestHelper helper) {
        List<Chronicle> sections = Chronicle.sections(Chronicle.Book.HUMAN_ANCESTOR_LEGEND);
        assertEquals(sections.size(), ModItems.HUMAN_ANCESTOR_LEGEND.size());
        List<Item> shelf = getShown(helper, ModCreativeTabs.BOOKS.get());
        for (int at = 0; at < sections.size(); at++) {
            Item item = ModItems.HUMAN_ANCESTOR_LEGEND.get(at).get();
            assertEquals(Guzhenren.id("human_ancestor_legend_" + (at + 1)),
                    ModItems.HUMAN_ANCESTOR_LEGEND.get(at).getId());
            assertTrue(item instanceof BookletItem booklet && sections.get(at).equals(booklet.getText()),
                    item + " carries another text");
            assertEquals(1, new ItemStack(item).getMaxStackSize());
            assertEquals(shelf.indexOf(ModItems.GU_ATLAS_HOPE.get()) + 1 + at, shelf.indexOf(item),
                    item + " is out of order on the book tab");
        }
        for (boolean traditional : new boolean[] { false, true }) {
            assertEquals("困境和希望", sections.getFirst().title(traditional), "the mod loader hides the section texts");
            assertTrue(sections.getLast().read(traditional).size() > 10, "the last section reads short");
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void figuresMatchTheGuTheyShow(GameTestHelper helper) {
        for (Booklet booklet : Booklet.values()) {
            booklet.getFigure().ifPresent(figure -> {
                Item item = figure.getItem();
                assertTrue(item instanceof MortalGuItem, booklet + " shows " + figure.id() + ", no mortal Gu");
                MortalGuItem gu = (MortalGuItem) item;
                assertSame(figure.rank(), gu.getRank());
                assertSame(figure.path(), gu.getPath());
            });
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void bookTabListsRecipesThenMovesThenBooklets(GameTestHelper helper) {
        Item booklet = ModItems.GU_ATLAS_HOPE.get();
        CreativeModeTab tab = ModCreativeTabs.BOOKS.get();
        assertSame(ModItems.GU_RECIPE.get(), tab.getIconItem().getItem());
        List<Item> shelf = getShown(helper, tab);
        assertTrue(shelf.contains(booklet), "Gu Atlas: Hope missing from the book tab");
        assertTrue(shelf.contains(ModItems.KM_MORTAL_LITTLE_LIGHT_AMPLIFY.get()), "no killing move on the book tab");
        List<Class<?>> rounds = List.of(GuRecipeItem.class, KillingMoveBookItem.class, BookletItem.class);
        int last = 0;
        for (Item item : shelf) {
            int round = -1;
            for (int at = 0; at < rounds.size(); at++) if (rounds.get(at).isInstance(item)) round = at;
            assertTrue(round >= 0, "the book tab shows " + item + ", no book");
            assertTrue(round >= last, item + " comes before an earlier kind of book");
            last = round;
        }
        for (CreativeModeTab other : List.of(ModCreativeTabs.MORTAL_GU.get(), ModCreativeTabs.GU_MATERIAL.get(),
                ModCreativeTabs.STRENGTH_MORTAL_GU.get(), ModCreativeTabs.WOOD_MORTAL_GU.get())) {
            assertFalse(getShown(helper, other).contains(booklet), "the booklet shows in " + other.getDisplayName());
        }
        helper.succeed();
    }

    private static List<Item> getShown(GameTestHelper helper, CreativeModeTab tab) {
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), false,
                helper.getLevel().registryAccess()));
        return tab.getDisplayItems().stream().map(ItemStack::getItem).toList();
    }
}
