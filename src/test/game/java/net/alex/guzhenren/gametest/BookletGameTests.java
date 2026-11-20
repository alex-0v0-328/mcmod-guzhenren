package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.item.Booklet;
import net.alex.guzhenren.item.BookletItem;
import net.alex.guzhenren.item.Chronicle;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.MortalGuItem;
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
    public static void renzuZhuanShelvesItsSectionsInOrder(GameTestHelper helper) {
        List<Chronicle> sections = Chronicle.sections(Chronicle.Book.RENZU_ZHUAN);
        assertEquals(sections.size(), ModItems.RENZU_ZHUAN.size());
        List<Item> shelf = getShown(helper, ModCreativeTabs.BOOKLET.get());
        for (int at = 0; at < sections.size(); at++) {
            Item item = ModItems.RENZU_ZHUAN.get(at).get();
            assertEquals(Guzhenren.id("renzu_zhuan_" + (at + 1)), ModItems.RENZU_ZHUAN.get(at).getId());
            assertTrue(item instanceof BookletItem booklet && sections.get(at).equals(booklet.getText()),
                    item + " carries another text");
            assertEquals(1, new ItemStack(item).getMaxStackSize());
            assertEquals(shelf.indexOf(ModItems.GU_ATLAS_HOPE.get()) + 1 + at, shelf.indexOf(item),
                    item + " is out of order on the booklet tab");
        }
        for (boolean traditional : new boolean[] { false, true }) {
            assertEquals("困境和希望", sections.getFirst().title(traditional), "the mod loader hides the section texts");
            assertTrue(sections.getLast().read(traditional).size() > 1000, "the last section reads short");
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
    public static void bookletsFillTheirOwnTabAlone(GameTestHelper helper) {
        Item booklet = ModItems.GU_ATLAS_HOPE.get();
        CreativeModeTab tab = ModCreativeTabs.BOOKLET.get();
        assertSame(booklet, tab.getIconItem().getItem());
        List<Item> shelf = getShown(helper, tab);
        assertTrue(shelf.contains(booklet), "Gu Atlas: Hope missing from the booklet tab");
        assertTrue(shelf.stream().allMatch(BookletItem.class::isInstance), "the booklet tab shows a non-booklet");
        for (CreativeModeTab other : List.of(ModCreativeTabs.MORTAL_GU.get(), ModCreativeTabs.GU_MATERIAL.get(),
                ModCreativeTabs.STRENGTH_MORTAL_GU.get(), ModCreativeTabs.WOOD_MORTAL_GU.get(),
                ModCreativeTabs.KM_MORTAL.get())) {
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
