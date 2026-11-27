package net.alex.guzhenren.gametest;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdRules;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdState;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdWork;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.ActionContext;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.EarthSpiritMenu;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.EarthSpiritPanel;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.FarmPart;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.GuPart;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.HerdPart;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.OverviewPart;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.Page;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelAction;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelActions;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelSection;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelSectionSource;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelSectionSources;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelSnapshot;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.SecretRealmPart;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.StoragePart;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.TerritoryPart;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLand;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.EarthSpiritEntity;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandMembership;
import net.alex.guzhenren.gameplay.immortalaperture.state.Territory;
import net.alex.guzhenren.gameplay.immortalaperture.storage.BlissfulStorage;
import net.alex.guzhenren.gameplay.immortalaperture.storage.StorageBrowser.Sort;
import net.alex.guzhenren.gameplay.immortalaperture.storage.StorageBrowser;
import net.alex.guzhenren.gameplay.immortalaperture.storage.StorageWork;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;

/**
 * 福地经营面板的面板段化外壳：每种操作与每页多段快照的线上编解码往返（每页恰好是归到该页的面板段各一段）、面板默认停在地图
 * 页、只算正在看的页并记住它、库藏「存同类」、库藏服务端分页（5,000 种条目下开面板附加数据不超 NeoForge 的 32,600 字节，
 * 翻页、搜索、排序、按流水号取物、查询参数解码校验）、按名字只加在线玩家、宰杀预估与实际宰杀一致。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlissfulPanelGameTests {

    private static final String GLOBAL_BATCH = "blissful_land_global";
    private static final int OPEN_DATA_LIMIT = 32_600;
    private static final int KINDS = 5_000;
    private static final int NEAR_CAP = 440;
    private static final int OVER_CAP = 2_000;

    private BlissfulPanelGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void everyActionAndSectionSurvivesTheWire(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UUID someone = UUID.randomUUID();
        ResourceLocation cow = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW);
        List<PanelAction> samples = List.of(new PanelActions.View(Page.INFO),
                new OverviewPart.Expel(someone), new OverviewPart.ExpelOutsiders(),
                new OverviewPart.SetWhitelisted(someone, true), new SecretRealmPart.MoveMobs(),
                new SecretRealmPart.DriveGutsGu(), new SecretRealmPart.AddWhitelist("Steve"), new HerdPart.Breed(),
                new HerdPart.Slaughter(), new HerdPart.SetHosting(cow, true), new HerdPart.SetKeep(cow, 7),
                new HerdPart.PlacePasture(3, -4), new HerdPart.MovePasture(1, -2, 5),
                new HerdPart.RemovePasture(2), new HerdPart.Gather(0), new GuPart.Capture(),
                new GuPart.SetHosting(cow, true), new GuPart.SetKeep(cow, 2),
                new FarmPart.Plan(7, -9), new FarmPart.Unplan(7, -9), new FarmPart.Remove(-1, 2),
                new FarmPart.Harvest(), new FarmPart.SetHosting(true),
                new FarmPart.SetCrop(2), new TerritoryPart.Cede(Territory.Side.EAST, 3),
                new StoragePart.Take(4L, true), new StoragePart.DepositCarried(),
                new StoragePart.DepositMatching(), new StoragePart.Browse("小麦", Sort.NAME, 2,
                        List.of(Items.WHEAT, Items.WHEAT_SEEDS)));
        Set<Class<?>> covered = new HashSet<>();
        for (PanelAction sample : samples) covered.add(sample.getClass());
        for (PanelAction.Type<?> type : PanelActions.types()) {
            helper.assertTrue(covered.contains(type.type()), type.type().getSimpleName() + " has no wire sample");
        }
        for (PanelAction sample : samples) {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            PanelActions.STREAM_CODEC.encode(buffer, sample);
            helper.assertValueEqual(PanelActions.STREAM_CODEC.decode(buffer), sample, "an action round trip");
        }

        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        BlissfulStorage.of(data).insert(new ItemStack(Items.WHEAT, 70));
        data.addWhitelist(someone, "Steve");
        HerdState.of(data).setKeep(cow, 3);
        List<PanelSectionSource<?>> placed = new ArrayList<>();
        for (Page page : Page.values()) {
            PanelSnapshot snapshot = EarthSpiritPanel.snapshot(level, data, someone, page, Component.literal("done"));
            helper.assertValueEqual(snapshot.page(), page, "a snapshot carries the asked page");
            List<PanelSectionSource<?>> sources = snapshot.sections().stream()
                    .<PanelSectionSource<?>>map(PanelSection::source).toList();
            helper.assertValueEqual(sources, PanelSectionSources.on(page),
                    page + " carries one section per source on it");
            helper.assertTrue(sources.stream().allMatch(source -> source.page() == page), page + " sections belong");
            placed.addAll(sources);
            byte[] first = encode(level, snapshot);
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(first),
                    level.registryAccess());
            PanelSnapshot decoded = PanelSnapshot.STREAM_CODEC.decode(buffer);
            helper.assertTrue(Arrays.equals(first, encode(level, decoded)), page + " section round trip");
        }
        helper.assertValueEqual(placed.size(), PanelSectionSources.ALL.size(), "every module sits on exactly one page");
        helper.assertValueEqual(PanelSectionSources.on(Page.INFO), List.of(OverviewPart.INSTANCE, HerdPart.INSTANCE,
                FarmPart.INSTANCE, GuPart.INSTANCE, TerritoryPart.INSTANCE), "the info sections in tab order");
        StorageBrowser.forget(someone);
        helper.succeed();
    }

    private static byte[] encode(ServerLevel level, PanelSnapshot snapshot) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        PanelSnapshot.STREAM_CODEC.encode(buffer, snapshot);
        return ByteBufUtil.getBytes(buffer);
    }

    @GameTest(template = "empty9x9x9", batch = GLOBAL_BATCH)
    public static void thePanelShowsAndRemembersTheChosenPage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlissfulLandData data = BlissfulLandData.get(level.getServer(), BlissfulLand.FOX_IMMORTAL);
        ServerPlayer owner = GameTestPlayers.survival(helper, "gzr-land-page-mock").onGround();
        EarthSpiritEntity spirit = helper.spawn(ModEntityTypes.EARTH_SPIRIT_BLISSFUL_LAND_FOX_IMMORTAL.get(),
                CENTER_POS.offset(1, 0, 1));
        try {
            LandMembership.claim(null, data, owner);
            EarthSpiritMenu menu = new EarthSpiritMenu(1, owner.getInventory(), spirit,
                    EarthSpiritPanel.snapshot(level, data, owner.getUUID(), EarthSpiritPanel.lastPage(owner),
                            Component.empty()));
            owner.containerMenu = menu;
            helper.assertValueEqual(menu.page(), Page.MAP, "the panel opens on the map");
            EarthSpiritPanel.handle(owner, new PanelActions.View(Page.STORAGE));
            helper.assertValueEqual(menu.page(), Page.STORAGE, "viewing a page switches what the server computes");
            helper.assertValueEqual(EarthSpiritPanel.lastPage(owner), Page.STORAGE, "the page is remembered");
            List<PanelSection> computed = EarthSpiritPanel.snapshot(level, data, owner.getUUID(), menu.page(),
                    Component.empty()).sections();
            helper.assertTrue(computed.size() == 1 && computed.getFirst() instanceof StoragePart.Section,
                    "only the storage section is computed");
            helper.succeed();
        } finally {
            EarthSpiritPanel.remember(owner, Page.MAP);
            StorageBrowser.forget(owner.getUUID());
            owner.containerMenu = owner.inventoryMenu;
            LandMembership.lose(level.getServer(), null, data);
        }
    }

    @GameTest(template = "empty9x9x9")
    public static void hiddenBagSlotsMoveOffScreenAndBack(GameTestHelper helper) {
        ServerPlayer owner = GameTestPlayers.survival(helper, "gzr-land-slots-mock").onGround();
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        EarthSpiritMenu menu = new EarthSpiritMenu(1, owner.getInventory(), null,
                EarthSpiritPanel.snapshot(helper.getLevel(), data, owner.getUUID(), Page.INFO, Component.empty()));
        Slot home = menu.slots.get(27);

        menu.setInventoryShown(false);
        helper.assertTrue(menu.slots.stream().allMatch(slot -> slot.x == EarthSpiritMenu.HIDDEN_XY
                && slot.y == EarthSpiritMenu.HIDDEN_XY && !slot.isActive()), "hidden bag slots sit off screen");
        Slot hidden = menu.slots.get(27);
        helper.assertValueEqual(hidden.index, 27, "a hidden slot keeps its menu index");
        helper.assertTrue(hidden.container == owner.getInventory() && hidden.getContainerSlot() == 0,
                "a hidden slot still points at the first hotbar slot");

        menu.setInventoryShown(true);
        helper.assertTrue(menu.slots.get(27) == home && home.isActive(), "showing puts the slot back at home");
        helper.assertValueEqual(home.y, EarthSpiritMenu.HOTBAR_Y, "the hotbar row is back at its height");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void depositingMatchesStoresOnlyKnownKindsOutsideTheHotbar(GameTestHelper helper) {
        ServerPlayer owner = GameTestPlayers.survival(helper, "gzr-land-deposit-mock").onGround();
        BlissfulStorage storage = BlissfulStorage.of(BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL));
        storage.insert(new ItemStack(Items.WHEAT, 1));
        owner.getInventory().setItem(0, new ItemStack(Items.WHEAT, 5));
        owner.getInventory().setItem(9, new ItemStack(Items.WHEAT, 7));
        owner.getInventory().setItem(10, new ItemStack(Items.DIRT, 3));

        helper.assertValueEqual(StorageWork.depositMatching(owner, storage), 7, "seven wheat go in");
        helper.assertTrue(owner.getInventory().getItem(9).isEmpty(), "the bag slot is emptied");
        helper.assertValueEqual(owner.getInventory().getItem(0).getCount(), 5, "the hotbar is left alone");
        helper.assertValueEqual(owner.getInventory().getItem(10).getCount(), 3, "a kind not in storage stays");
        helper.assertValueEqual(storage.count(stack -> stack.is(Items.WHEAT)), 8L, "storage counts eight");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void storageBrowsingPagesSearchesAndStaysUnderTheOpenLimit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = GameTestPlayers.survival(helper, "gzr-land-browse-mock").onGround();
        UUID viewer = owner.getUUID();
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        BlissfulStorage storage = BlissfulStorage.of(data);
        int page = StorageBrowser.PAGE_SIZE;
        for (int i = 0; i < KINDS; i++) storage.insert(named(i, i < page ? NEAR_CAP : i < 2 * page ? OVER_CAP : 0));
        storage.insert(new ItemStack(Items.WHEAT, 100));
        EarthSpiritMenu menu = new EarthSpiritMenu(1, owner.getInventory(), null,
                EarthSpiritPanel.snapshot(level, data, viewer, Page.INFO, Component.empty()));
        ActionContext context = new ActionContext(owner, menu, level, data);
        try {
            StoragePart.INSTANCE.opened(viewer);
            int open = encode(level, EarthSpiritPanel.snapshot(level, data, viewer, Page.STORAGE,
                    Component.empty())).length;
            helper.assertTrue(open < OPEN_DATA_LIMIT, "the open data is " + open + " bytes");
            StoragePart.Section first = storageSection(level, data, viewer);
            helper.assertValueEqual(first.entries().size(), page, "one page is one grid");
            helper.assertValueEqual(first.total(), KINDS + 1, "the total counts every kind");
            helper.assertTrue(first.entries().stream().noneMatch(StorageBrowser.Stored::trimmed),
                    "an item under the byte cap is sent whole");

            new StoragePart.Browse("", Sort.ORDER, 1, List.of()).perform(context);
            int trimmedPage = encode(level, EarthSpiritPanel.snapshot(level, data, viewer, Page.STORAGE,
                    Component.empty())).length;
            StoragePart.Section second = storageSection(level, data, viewer);
            helper.assertValueEqual(second.pageIndex(), 1, "the second page is shown");
            helper.assertValueEqual(second.entries().get(0).serial(), storage.entries().get(page).serial(),
                    "the second page starts right after the first");
            helper.assertTrue(second.entries().stream().allMatch(stored -> stored.trimmed()
                    && !stored.key().has(DataComponents.CUSTOM_NAME)), "an item over the byte cap is sent bare");

            new StoragePart.Browse("", Sort.ORDER, 9999, List.of()).perform(context);
            StoragePart.Section last = storageSection(level, data, viewer);
            helper.assertValueEqual(last.pageIndex(), last.pages() - 1, "a page past the end is the last page");
            helper.assertValueEqual(last.entries().size(), KINDS + 1 - (last.pages() - 1) * page,
                    "the last page holds the remainder");

            new StoragePart.Browse(" 条目4321号 ", Sort.NAME, 0, List.of()).perform(context);
            StoragePart.Section found = storageSection(level, data, viewer);
            helper.assertValueEqual(found.matched(), 1, "a name search finds one entry");
            long serial = found.entries().get(0).serial();
            new StoragePart.Browse("WHEAT", Sort.ORDER, -3, List.of()).perform(context);
            found = storageSection(level, data, viewer);
            helper.assertTrue(found.matched() == 1 && found.pageIndex() == 0 && found.entries().get(0).key()
                    .is(Items.WHEAT), "an ID search ignores case and a negative page is the first");
            new StoragePart.Browse("麦子", Sort.ORDER, 0, List.of(Items.WHEAT)).perform(context);
            helper.assertValueEqual(storageSection(level, data, viewer).matched(), 1,
                    "a client name hint finds what the server language cannot");
            new StoragePart.Browse("", Sort.COUNT, 0, List.of()).perform(context);
            helper.assertTrue(storageSection(level, data, viewer).entries().get(0).key().is(Items.WHEAT),
                    "the count sort puts the largest first");

            storage.extract(0, 64);
            StorageWork.take(owner, menu, storage, serial, false);
            helper.assertValueEqual(menu.getCarried().getHoverName().getString(), name(4321, 0),
                    "a shifted entry is still taken by its serial");
            menu.setCarried(ItemStack.EMPTY);
            StorageWork.take(owner, menu, storage, serial, false);
            helper.assertTrue(menu.getCarried().isEmpty(), "a serial whose entry is gone takes nothing");

            StoragePart.INSTANCE.opened(viewer);
            StoragePart.Section reopened = storageSection(level, data, viewer);
            helper.assertTrue(reopened.matched() == reopened.total() && reopened.pageIndex() == 0
                    && reopened.sort() == Sort.COUNT, "reopening clears the search and page and keeps the sort");
            StorageBrowser.forget(viewer);
            helper.assertValueEqual(storageSection(level, data, viewer).sort(), Sort.ORDER,
                    "logging out forgets the view");

            helper.assertTrue(refuses(level, buffer -> buffer.writeUtf("x".repeat(StorageBrowser.TEXT_LENGTH + 1))),
                    "an overlong search is refused on decode");
            helper.assertTrue(refuses(level, buffer -> {
                buffer.writeUtf("");
                buffer.writeVarInt(0);
                buffer.writeVarInt(0);
                buffer.writeVarInt(StorageBrowser.HINT_LIMIT + 1);
            }), "too many hints are refused on decode");
            Guzhenren.LOGGER.info("Storage browsing bytes: open {} (limit {}), trimmed page {}", open,
                    OPEN_DATA_LIMIT, trimmedPage);
            helper.succeed();
        } finally {
            StorageBrowser.forget(viewer);
        }
    }

    private static ItemStack named(int index, int padding) {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name(index, padding)));
        return stack;
    }

    private static String name(int index, int padding) {
        return "条目" + index + "号" + (padding > 0 ? " " + "x".repeat(padding) : "");
    }

    private static StoragePart.Section storageSection(ServerLevel level, BlissfulLandData data, UUID viewer) {
        return (StoragePart.Section) EarthSpiritPanel.snapshot(level, data, viewer, Page.STORAGE, Component.empty())
                .sections().getFirst();
    }

    private static boolean refuses(ServerLevel level, Consumer<RegistryFriendlyByteBuf> body) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        List<PanelAction.Type<?>> types = PanelActions.types();
        for (int i = 0; i < types.size(); i++) {
            if (types.get(i).type() == StoragePart.Browse.class) buffer.writeVarInt(i);
        }
        body.accept(buffer);
        try {
            PanelActions.STREAM_CODEC.decode(buffer);
            return false;
        } catch (DecoderException refused) {
            return true;
        }
    }

    @GameTest(template = "empty9x9x9")
    public static void onlyAnOnlineNameJoinsTheWhitelist(GameTestHelper helper) {
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        ServerPlayer guest = GameTestPlayers.survival(helper, "gzr-land-name-mock").onGround();

        helper.assertTrue(LandMembership.addWhitelist(helper.getLevel().getServer(), data, "gzr-land-name-mock"),
                "an online name is listed");
        helper.assertTrue(data.isWhitelisted(guest.getUUID()), "by that player's UUID");
        helper.assertFalse(LandMembership.addWhitelist(helper.getLevel().getServer(), data, "gzrNeverSeenMock"),
                "a name nobody online carries is refused, with no lookup");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theSlaughterPreviewMatchesTheSlaughter(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        List<Entity> herd = new ArrayList<>();
        for (int i = 0; i < 4; i++) herd.add(helper.spawn(EntityType.COW, CENTER_POS.offset(i - 2, 0, -2)));
        Cow calf = helper.spawn(EntityType.COW, CENTER_POS.offset(0, 0, 2));
        calf.setBaby(true);
        herd.add(calf);
        BlissfulStorage storage = BlissfulStorage.of(BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL));
        Map<ResourceLocation, Integer> keep = Map.of(HerdWork.typeId(calf), 2);

        int preview = HerdWork.slaughterCount(HerdWork.census(herd), keep, HerdRules.LIMIT);
        int killed = HerdWork.slaughter(helper.getLevel(), HerdWork.census(herd), keep, storage, null, HerdRules.LIMIT);
        helper.assertValueEqual(preview, 3, "five cows kept at two: three adults would go");
        helper.assertValueEqual(killed, preview, "the slaughter takes what the preview said");
        helper.succeed();
    }
}
