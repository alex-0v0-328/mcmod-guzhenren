package net.alex.guzhenren.gameplay.trade;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * {@link SoulTradeOffer} 在 36 格空背包上的结算契约，及测试商人价目。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SoulTradeOfferGameTests {

    private SoulTradeOfferGameTests() {}

    private static final int BAG = 36;

    @GameTest(template = "empty9x9x9")
    public static void testTraderPinsItsTwoOffers(GameTestHelper helper) {
        List<SoulTradeOffer> offers = SoulTrader.TEST_TRADE_GU_IMMORTAL.getOffers();
        assertEquals(2, offers.size());

        SoulTradeOffer pork = offers.getFirst();
        assertEquals(1, pork.costs().size());
        assertTrue(pork.costs().getFirst().createItemStack().is(ModItems.PRIMEVAL_STONE.get()));
        assertEquals(10, pork.costs().getFirst().count());
        assertTrue(pork.result().is(Items.PORKCHOP));
        assertEquals(64, pork.result().getCount());

        SoulTradeOffer gu = offers.get(1);
        assertEquals(1, gu.costs().size());
        assertTrue(gu.costs().getFirst().createItemStack().is(Items.DIRT));
        assertEquals(192, gu.costs().getFirst().count());
        assertTrue(gu.result().is(ModItems.WHITE_BOAR_GU_1.get()));
        assertEquals(1, gu.result().getCount());
        assertTrue(ItemStack.isSameItemSameComponents(gu.result(), new ItemStack(ModItems.WHITE_BOAR_GU_1.get())),
                "the Gu must arrive wild, like a caught one");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void settlePaysAcrossStacksAndDelivers(GameTestHelper helper) {
        NonNullList<ItemStack> bag = bag(
                new ItemStack(Items.DIRT, 64), new ItemStack(Items.DIRT, 64),
                new ItemStack(Items.DIRT, 64), new ItemStack(Items.DIRT, 10));

        assertEquals(SoulTradeOffer.Outcome.DONE, guOffer().settle(bag, List.of()));
        assertEquals(10, count(bag, Items.DIRT));
        assertEquals(1, count(bag, ModItems.WHITE_BOAR_GU_1.get()));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void shortPaymentLeavesTheBagUntouched(GameTestHelper helper) {
        NonNullList<ItemStack> bag = bag(new ItemStack(Items.DIRT, 64), new ItemStack(Items.DIRT, 64),
                new ItemStack(Items.DIRT, 63));
        List<ItemStack> before = snapshot(bag);

        assertFalse(guOffer().canAfford(bag));
        assertEquals(SoulTradeOffer.Outcome.SHORT, guOffer().settle(bag, List.of()));
        assertBagEquals(before, bag);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void paymentFreesTheSlotForTheResult(GameTestHelper helper) {
        NonNullList<ItemStack> bag = fullBagStartingWith(new ItemStack(ModItems.PRIMEVAL_STONE.get(), 10));

        assertEquals(SoulTradeOffer.Outcome.DONE, porkOffer().settle(bag, List.of()));
        assertEquals(64, count(bag, Items.PORKCHOP));
        assertEquals(0, count(bag, ModItems.PRIMEVAL_STONE.get()));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void resultWithoutRoomIsRefused(GameTestHelper helper) {
        NonNullList<ItemStack> bag = fullBagStartingWith(new ItemStack(ModItems.PRIMEVAL_STONE.get(), 11));
        List<ItemStack> before = snapshot(bag);

        assertEquals(SoulTradeOffer.Outcome.NO_ROOM, porkOffer().settle(bag, List.of()));
        assertBagEquals(before, bag);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void returningStacksMustStillFit(GameTestHelper helper) {
        NonNullList<ItemStack> bag = fullBagStartingWith(new ItemStack(ModItems.PRIMEVAL_STONE.get(), 10));
        List<ItemStack> before = snapshot(bag);

        List<ItemStack> returning = List.of(new ItemStack(Items.DIAMOND), ItemStack.EMPTY);
        assertEquals(SoulTradeOffer.Outcome.NO_ROOM, porkOffer().settle(bag, returning));
        assertBagEquals(before, bag);

        assertEquals(SoulTradeOffer.Outcome.DONE, porkOffer().settle(bag, List.of(ItemStack.EMPTY, ItemStack.EMPTY)));
        assertEquals(64, count(bag, Items.PORKCHOP));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void returningStacksAreOnlyChecked(GameTestHelper helper) {
        NonNullList<ItemStack> bag = bag(new ItemStack(ModItems.PRIMEVAL_STONE.get(), 10));

        assertEquals(SoulTradeOffer.Outcome.DONE,
                porkOffer().settle(bag, List.of(new ItemStack(Items.DIAMOND, 5))));
        assertEquals(0, count(bag, Items.DIAMOND));
        assertEquals(64, count(bag, Items.PORKCHOP));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void offersSurviveTheNetworkRoundTrip(GameTestHelper helper) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), ConnectionType.NEOFORGE);
        List<SoulTradeOffer> offers = SoulTrader.TEST_TRADE_GU_IMMORTAL.getOffers();

        SoulTradeMenu.writeOffers(buffer, offers);
        List<SoulTradeOffer> decoded = SoulTradeOffer.decodeList(buffer);

        assertEquals(offers.size(), decoded.size());
        for (int i = 0; i < offers.size(); i++) {
            List<TradeCost> sent = offers.get(i).costs();
            List<TradeCost> received = decoded.get(i).costs();
            assertEquals(sent.size(), received.size());
            for (int n = 0; n < sent.size(); n++) {
                assertEquals(sent.get(n).item(), received.get(n).item());
                assertEquals(sent.get(n).count(), received.get(n).count());
            }
            assertTrue(ItemStack.matches(offers.get(i).result(), decoded.get(i).result()));
        }
        assertEquals(192, decoded.get(1).costs().getFirst().count());
        helper.succeed();
    }

    private static SoulTradeOffer porkOffer() { return SoulTrader.TEST_TRADE_GU_IMMORTAL.getOffers().getFirst(); }

    private static SoulTradeOffer guOffer() { return SoulTrader.TEST_TRADE_GU_IMMORTAL.getOffers().get(1); }

    private static NonNullList<ItemStack> bag(ItemStack... stacks) {
        NonNullList<ItemStack> bag = NonNullList.withSize(BAG, ItemStack.EMPTY);
        for (int i = 0; i < stacks.length; i++) bag.set(i, stacks[i]);
        return bag;
    }

    private static NonNullList<ItemStack> fullBagStartingWith(ItemStack first) {
        NonNullList<ItemStack> bag = NonNullList.withSize(BAG, ItemStack.EMPTY);
        for (int i = 0; i < BAG; i++) bag.set(i, new ItemStack(Items.COBBLESTONE, 64));
        bag.set(0, first);
        return bag;
    }

    private static int count(List<ItemStack> bag, Item item) {
        return SoulTradeOffer.countHeld(bag, TradeCost.of(item, 1));
    }

    private static List<ItemStack> snapshot(List<ItemStack> bag) {
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack stack : bag) copies.add(stack.copy());
        return copies;
    }

    private static void assertBagEquals(List<ItemStack> expected, List<ItemStack> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertTrue(ItemStack.matches(expected.get(i), actual.get(i)), "slot " + i + " changed");
        }
    }
}
