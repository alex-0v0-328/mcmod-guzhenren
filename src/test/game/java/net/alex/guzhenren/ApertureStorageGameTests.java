package net.alex.guzhenren;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorage;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotSame;
import static net.alex.guzhenren.gametest.UnitAsserts.assertSame;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ApertureStorageGameTests {

    private ApertureStorageGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void storageCopiesCallerInput(GameTestHelper helper) {
        ItemStack source = new ItemStack(Items.DIRT, 3);
        ApertureStorage storage = new ApertureStorage(List.of(List.of(source)), List.of(source));

        source.setCount(7);

        assertEquals(3, storage.get(0).getFirst().getCount());
        assertEquals(3, storage.getVital(0).getCount());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void storageReadersDoNotExposeState(GameTestHelper helper) {
        ApertureStorage storage = new ApertureStorage(
                List.of(List.of(new ItemStack(Items.DIRT, 3))),
                List.of(new ItemStack(Items.DIRT, 4)));

        storage.get(0).getFirst().setCount(7);
        storage.getVital(0).setCount(8);

        assertEquals(3, storage.get(0).getFirst().getCount());
        assertEquals(4, storage.getVital(0).getCount());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void withDoesNotAliasReplacement(GameTestHelper helper) {
        ItemStack replacement = new ItemStack(Items.DIRT, 3);
        ApertureStorage storage = ApertureStorage.DEFAULT.with(0, List.of(replacement));

        replacement.setCount(7);

        assertEquals(3, storage.get(0).getFirst().getCount());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void withSharesUnchangedSlots(GameTestHelper helper) {
        ApertureStorage storage = ApertureStorage.DEFAULT.with(0,
                List.of(new ItemStack(Items.DIRT, 3), new ItemStack(Items.STONE, 4)));
        List<ItemStack> next = new ArrayList<>(storage.getView(0));
        ItemStack replacement = new ItemStack(Items.APPLE, 5);
        next.set(1, replacement);

        ApertureStorage written = storage.with(0, next);

        assertSame(storage.getView(0).getFirst(), written.getView(0).getFirst());
        assertNotSame(replacement, written.getView(0).get(1));
        assertEquals(5, written.getView(0).get(1).getCount());
        assertEquals(4, storage.getView(0).get(1).getCount());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void pageOperationsUseOnlyTheirWindow(GameTestHelper helper) {
        ApertureStorage storage = ApertureStorage.DEFAULT.withPage(0, 2,
                List.of(new ItemStack(Items.DIRT, 3), ItemStack.EMPTY));

        List<ItemStack> page = storage.getPage(0, 2, 2);
        assertEquals(1, page.size());
        assertEquals(3, page.getFirst().getCount());
        assertTrue(storage.isSamePage(0, 2, page));

        page.getFirst().setCount(7);

        assertEquals(3, storage.getPage(0, 2, 1).getFirst().getCount());
        helper.succeed();
    }
}
