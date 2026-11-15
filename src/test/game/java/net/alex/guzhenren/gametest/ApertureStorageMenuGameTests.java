package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorage;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.item.gu.mortal.path.space.PrimevalElderGuItem;
import net.alex.guzhenren.item.gu.stack.RefinedGuState;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.alex.guzhenren.registry.item.ModItems;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.alex.guzhenren.vault.ApertureStorageTick;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestApertures.awakenedMortal;
import static net.alex.guzhenren.gametest.GameTestApertures.openStorage;
import static net.alex.guzhenren.gametest.GameTestPlayers.serverTicks;

/**
 * 空窍存储界面：放置与 Shift 移动受负载上限约束；本命蛊在超负载与负载内的交换；界面须保留的存储蛊维护写入。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ApertureStorageMenuGameTests {
    private static final int WORM_FULL = 8;
    private static final int FIFTH_RANK_FULL = 8;

    private ApertureStorageMenuGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void storageMousePlacementStopsAtTheLoad(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        PlayerData.get(player).setApertureStorage(ApertureStorage.DEFAULT.with(ApertureData.PRIMARY,
                fifthRank(FIFTH_RANK_FULL - 1)));
        ApertureStorageMenu menu = new ApertureStorageMenu(1, player.getInventory(), ApertureData.PRIMARY, 0);

        menu.setCarried(new ItemStack(ModItems.SECOND_APERTURE_GU_5.get()));
        menu.clicked(FIFTH_RANK_FULL - 1, 0, ClickType.PICKUP, player);
        helper.assertValueEqual(storedCount(player, ApertureData.PRIMARY), FIFTH_RANK_FULL,
                "mouse placement under the load");
        helper.assertTrue(menu.getCarried().isEmpty(), "mouse placement under the load left the gu on the cursor");

        menu.setCarried(new ItemStack(ModItems.SECOND_APERTURE_GU_5.get()));
        menu.clicked(FIFTH_RANK_FULL, 0, ClickType.PICKUP, player);
        helper.assertValueEqual(storedCount(player, ApertureData.PRIMARY), FIFTH_RANK_FULL,
                "mouse placement over the load");
        helper.assertValueEqual(menu.getCarried().getCount(), 1, "mouse placement over the load took the gu");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void storageShiftMoveStopsAtTheLoad(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        PlayerData.get(player).setApertureStorage(ApertureStorage.DEFAULT.with(ApertureData.PRIMARY,
                fifthRank(FIFTH_RANK_FULL - 1)));
        player.getInventory().setItem(0, new ItemStack(ModItems.SECOND_APERTURE_GU_5.get()));
        player.getInventory().setItem(1, new ItemStack(ModItems.SECOND_APERTURE_GU_5.get()));
        ApertureStorageMenu menu = new ApertureStorageMenu(1, player.getInventory(), ApertureData.PRIMARY, 0);

        menu.clicked(ApertureStorageMenu.PAGE_SIZE + 27, 0, ClickType.QUICK_MOVE, player);
        menu.clicked(ApertureStorageMenu.PAGE_SIZE + 28, 0, ClickType.QUICK_MOVE, player);

        helper.assertValueEqual(storedCount(player, ApertureData.PRIMARY), FIFTH_RANK_FULL,
                "shift move accepted count");
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "shift move under the load left the gu behind");
        helper.assertValueEqual(player.getInventory().getItem(1).getCount(), 1,
                "shift move over the load took the gu");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void storageSyncedLoadLimitsSlot(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        ApertureStorageMenu menu = new ApertureStorageMenu(1, player.getInventory(), ApertureData.PRIMARY, 0);
        ItemStack incoming = new ItemStack(ModItems.SECOND_APERTURE_GU_5.get());

        menu.setData(2, 224);
        helper.assertValueEqual(menu.getSlot(0).getMaxStackSize(incoming), 1,
                "synced load slot limit");
        menu.setData(2, ApertureStorageService.MAX_LOAD);
        helper.assertValueEqual(menu.getSlot(0).getMaxStackSize(incoming), 0,
                "synced full load slot limit");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void storageLegacyOverloadCanBeReducedBySwap(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        PlayerData.get(player).setApertureStorage(ApertureStorage.DEFAULT.with(ApertureData.PRIMARY,
                fifthRank(FIFTH_RANK_FULL + 2)));
        ApertureStorageMenu menu = new ApertureStorageMenu(1, player.getInventory(), ApertureData.PRIMARY, 0);
        menu.setCarried(new ItemStack(ModItems.SECOND_APERTURE_GU_1.get()));

        menu.clicked(1, 0, ClickType.PICKUP, player);

        helper.assertTrue(ApertureStorageService.getItems(player, ApertureData.PRIMARY).get(1)
                .is(ModItems.SECOND_APERTURE_GU_1.get()), "legacy overload replacement was blocked");
        helper.assertValueEqual(ApertureStorageService.load(player, ApertureData.PRIMARY), 290,
                "legacy overload reduced load");
        helper.assertTrue(menu.getCarried().is(ModItems.SECOND_APERTURE_GU_5.get()),
                "legacy overload kept replaced gu on cursor");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void vitalMouseSwapOverLoadKeepsBothGu(GameTestHelper helper) {
        ItemStack vital = GameTestGu.refined(ModItems.LIQUOR_WORM_1.get(), 3);
        ServerPlayer player = fullVitalPlayer(helper, vital);
        ItemStack heavier = GameTestGu.refined(ModItems.FOUR_FLAVORS_LIQUOR_WORM_2.get(), 3);
        ApertureStorageMenu menu = new ApertureStorageMenu(1, player.getInventory(), ApertureData.PRIMARY, 0);
        menu.setCarried(heavier.copy());

        menu.clicked(ApertureStorageMenu.VITAL_SLOT, 0, ClickType.PICKUP, player);

        assertVitalKept(helper, player, menu, vital);
        helper.assertTrue(ItemStack.matches(menu.getCarried(), heavier), "over-load mouse swap took the heavier gu");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void vitalKeySwapOverLoadKeepsBothGu(GameTestHelper helper) {
        ItemStack vital = GameTestGu.refined(ModItems.LIQUOR_WORM_1.get(), 3);
        ServerPlayer player = fullVitalPlayer(helper, vital);
        ItemStack heavier = GameTestGu.refined(ModItems.FOUR_FLAVORS_LIQUOR_WORM_2.get(), 3);
        player.getInventory().setItem(0, heavier.copy());
        ApertureStorageMenu menu = new ApertureStorageMenu(1, player.getInventory(), ApertureData.PRIMARY, 0);

        menu.clicked(ApertureStorageMenu.VITAL_SLOT, 0, ClickType.SWAP, player);

        assertVitalKept(helper, player, menu, vital);
        helper.assertTrue(ItemStack.matches(player.getInventory().getItem(0), heavier),
                "over-load key swap took the heavier gu");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void vitalMouseSwapWithinLoadReplacesVital(GameTestHelper helper) {
        ItemStack vital = GameTestGu.refined(ModItems.LIQUOR_WORM_1.get(), 3);
        ServerPlayer player = fullVitalPlayer(helper, vital);
        ApertureStorageMenu menu = new ApertureStorageMenu(1, player.getInventory(), ApertureData.PRIMARY, 0);
        menu.setCarried(GameTestGu.refined(ModItems.WHITE_BOAR_GU_1.get(), 3));

        menu.clicked(ApertureStorageMenu.VITAL_SLOT, 0, ClickType.PICKUP, player);

        helper.assertTrue(ApertureStorageService.getVital(player, ApertureData.PRIMARY).is(ModItems.WHITE_BOAR_GU_1.get()),
                "equal-load vital swap was refused");
        helper.assertTrue(menu.getCarried().is(ModItems.LIQUOR_WORM_1.get()) && menu.getCarried().getCount() == 1,
                "replaced vital gu missing from the cursor");
        helper.assertTrue(!VitalGuState.isVital(menu.getCarried()), "the replaced vital gu kept its mark");
        helper.assertValueEqual(ApertureStorageService.load(player, ApertureData.PRIMARY),
                ApertureStorageService.MAX_LOAD, "equal-load vital swap load");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void storageCloseKeepsHeartbeatUpkeep(GameTestHelper helper) {
        ServerPlayer player = hurtElderPlayer(helper);
        ApertureStorageMenu menu = openStorage(player);

        serverTicks(player, Ticks.SECOND);
        helper.assertValueEqual(storedElderDamage(player), 0, "heartbeat did not mend the stored elder");
        menu.removed(player);

        helper.assertValueEqual(storedElderDamage(player), 0, "closing the menu undid the heartbeat upkeep");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void storageClickKeepsHeartbeatUpkeep(GameTestHelper helper) {
        ServerPlayer player = hurtElderPlayer(helper);
        ApertureStorageMenu menu = openStorage(player);

        serverTicks(player, Ticks.SECOND);
        menu.setCarried(new ItemStack(ModItems.SECOND_APERTURE_GU_1.get()));
        menu.clicked(1, 0, ClickType.PICKUP, player);

        helper.assertValueEqual(storedElderDamage(player), 0, "a slot click undid the heartbeat upkeep");
        helper.assertTrue(ApertureStorageService.getItems(player, ApertureData.PRIMARY).get(1)
                .is(ModItems.SECOND_APERTURE_GU_1.get()), "the click after the heartbeat was not stored");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void storedLotusStonesSurviveTheUpkeepWalk(GameTestHelper helper) {
        ServerPlayer player = wormPlayer(helper);
        List<ItemStack> stored = new ArrayList<>(ApertureStorageService.getItems(player, ApertureData.PRIMARY));
        stored.add(GameTestGu.refined(ModItems.HEAVENLY_ESSENCE_TREASURE_LOTUS_GU_3.get(), 3));
        stored.add(GameTestGu.refined(ModItems.PRIMEVAL_ELDER_GU_1.get(), 3));
        PlayerData.get(player).setApertureStorage(ApertureStorage.DEFAULT.with(ApertureData.PRIMARY, stored));

        ApertureStorageTick.tickStored(player, 1L);

        List<ItemStack> after = ApertureStorageService.getItems(player, ApertureData.PRIMARY);
        helper.assertValueEqual(TendedGuItem.getState(after.get(0)).hunger(), WORM_FULL, "the worm's feeding was lost");
        helper.assertTrue(PrimevalElderGuItem.getStored(after.get(2)) > 0L,
                "stones the lotus minted into the stored elder were written over");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void storedLotusStonesSurviveAnElderThatHealedFirst(GameTestHelper helper) {
        ServerPlayer player = wormPlayer(helper);
        ItemStack elder = new ItemStack(ModItems.PRIMEVAL_ELDER_GU_1.get());
        elder.set(ModDataComponents.REFINED_GU_STATE, new RefinedGuState(true, 0, 0, 3, 2));
        ((PrimevalElderGuItem) elder.getItem()).storeStones(elder, 10);
        List<ItemStack> stored = new ArrayList<>(ApertureStorageService.getItems(player, ApertureData.PRIMARY));
        stored.add(elder);
        stored.add(GameTestGu.refined(ModItems.HEAVENLY_ESSENCE_TREASURE_LOTUS_GU_3.get(), 3));
        PlayerData.get(player).setApertureStorage(ApertureStorage.DEFAULT.with(ApertureData.PRIMARY, stored));

        ApertureStorageTick.tickStored(player, 0L);

        ItemStack after = ApertureStorageService.getItems(player, ApertureData.PRIMARY).get(1);
        helper.assertTrue(TendedGuItem.getState(after).damageTaken() == 0, "the elder's own upkeep did not heal it");
        helper.assertTrue(PrimevalElderGuItem.getStored(after) > 8L,
                "stones the lotus minted after the elder's own write were written over");
        helper.succeed();
    }

    private static ServerPlayer fullVitalPlayer(GameTestHelper helper, ItemStack vital) {
        ServerPlayer player = awakenedMortal(helper);
        VitalGuState.bind(vital, player, ApertureData.PRIMARY);
        List<ItemStack> stored = fifthRank(FIFTH_RANK_FULL - 1);
        stored.add(new ItemStack(ModItems.SECOND_APERTURE_GU_4.get()));
        stored.add(new ItemStack(ModItems.SECOND_APERTURE_GU_3.get()));
        stored.add(new ItemStack(ModItems.SECOND_APERTURE_GU_2.get()));
        stored.add(new ItemStack(ModItems.SECOND_APERTURE_GU_1.get()));
        PlayerData.get(player).setApertureStorage(ApertureStorage.DEFAULT.with(ApertureData.PRIMARY, stored)
                .withVital(ApertureData.PRIMARY, vital.copy()));
        ApertureStorageService.adoptVital(player, ApertureData.PRIMARY, vital);
        helper.assertValueEqual(ApertureStorageService.load(player, ApertureData.PRIMARY),
                ApertureStorageService.MAX_LOAD, "full aperture setup load");
        return player;
    }

    private static void assertVitalKept(GameTestHelper helper, ServerPlayer player, ApertureStorageMenu menu,
                                        ItemStack vital) {
        helper.assertTrue(ItemStack.matches(ApertureStorageService.getVital(player, ApertureData.PRIMARY), vital),
                "over-load swap replaced the vital gu");
        helper.assertTrue(ItemStack.matches(menu.getSlot(ApertureStorageMenu.VITAL_SLOT).getItem(), vital),
                "over-load swap left the vital slot out of sync");
        helper.assertValueEqual(ApertureStorageService.load(player, ApertureData.PRIMARY),
                ApertureStorageService.MAX_LOAD, "over-load swap changed the load");
        helper.assertTrue(!menu.getCarried().is(vital.getItem())
                        && player.getInventory().countItem(vital.getItem()) == 0,
                "over-load swap copied the vital gu");
    }

    // A Liquor Worm one day's drain can leave short of its full 8 but never starve, with food in the holder's bag.
    private static ServerPlayer wormPlayer(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        ItemStack worm = new ItemStack(ModItems.LIQUOR_WORM_1.get());
        worm.set(ModDataComponents.REFINED_GU_STATE, new RefinedGuState(true, 0, 0, 4, 0));
        PlayerData.get(player).setApertureStorage(ApertureStorage.DEFAULT.with(ApertureData.PRIMARY, List.of(worm)));
        player.getInventory().setItem(9, new ItemStack(ModItems.LIQUOR.get(), 16));
        return player;
    }

    // A stored Elder Gu mends from its own stones on every heartbeat, day rollover or not: a write the menu must keep.
    private static ServerPlayer hurtElderPlayer(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        ItemStack elder = new ItemStack(ModItems.PRIMEVAL_ELDER_GU_1.get());
        elder.set(ModDataComponents.REFINED_GU_STATE, new RefinedGuState(true, 0, 0, 3, 2));
        ((PrimevalElderGuItem) elder.getItem()).storeStones(elder, 10);
        PlayerData.get(player).setApertureStorage(ApertureStorage.DEFAULT.with(ApertureData.PRIMARY, List.of(elder)));
        return player;
    }

    private static int storedElderDamage(ServerPlayer player) {
        return TendedGuItem.getState(ApertureStorageService.getItems(player, ApertureData.PRIMARY).getFirst())
                .damageTaken();
    }

    private static List<ItemStack> fifthRank(int count) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < count; i++) stacks.add(new ItemStack(ModItems.SECOND_APERTURE_GU_5.get()));
        return stacks;
    }

    private static int storedCount(ServerPlayer player, int aperture) {
        return ApertureStorageService.getItems(player, aperture).stream().mapToInt(ItemStack::getCount).sum();
    }
}
