package net.alex.guzhenren.gametest;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.blissful.BlissfulLand;
import net.alex.guzhenren.gameplay.blissful.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.EarthSpiritEntity;
import net.alex.guzhenren.gameplay.blissful.farm.FarmModule;
import net.alex.guzhenren.gameplay.blissful.herd.HerdModule;
import net.alex.guzhenren.gameplay.blissful.herd.HerdWork;
import net.alex.guzhenren.gameplay.blissful.member.MemberService;
import net.alex.guzhenren.gameplay.blissful.member.OverviewModule;
import net.alex.guzhenren.gameplay.blissful.mountain.MountainModule;
import net.alex.guzhenren.gameplay.blissful.panel.EarthSpiritMenu;
import net.alex.guzhenren.gameplay.blissful.panel.EarthSpiritPanel;
import net.alex.guzhenren.gameplay.blissful.panel.Page;
import net.alex.guzhenren.gameplay.blissful.panel.PanelAction;
import net.alex.guzhenren.gameplay.blissful.panel.PanelActions;
import net.alex.guzhenren.gameplay.blissful.panel.PanelSnapshot;
import net.alex.guzhenren.gameplay.blissful.storage.BlissfulStorage;
import net.alex.guzhenren.gameplay.blissful.storage.StorageModule;
import net.alex.guzhenren.gameplay.blissful.storage.StorageWork;
import net.alex.guzhenren.gameplay.blissful.territory.Territory;
import net.alex.guzhenren.gameplay.blissful.territory.TerritoryModule;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;

/**
 * 福地经营面板的模块化外壳：每种操作与每页快照段的线上编解码往返、面板只算正在看的页并记住它、库藏「存同类」、按名字
 * 只加在线玩家、宰杀预估与实际宰杀一致。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlissfulPanelGameTests {

    private static final String GLOBAL_BATCH = "blissful_land_global";

    private BlissfulPanelGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void everyActionAndSectionSurvivesTheWire(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UUID someone = UUID.randomUUID();
        ResourceLocation cow = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW);
        List<PanelAction> samples = List.of(new PanelActions.View(Page.FARMING),
                new OverviewModule.Expel(someone), new OverviewModule.ExpelOutsiders(),
                new OverviewModule.SetWhitelisted(someone, true), new MountainModule.MoveMobs(),
                new MountainModule.DriveGutsGu(), new MountainModule.AddWhitelist("Steve"), new HerdModule.Breed(),
                new HerdModule.Slaughter(), new HerdModule.SetHosting(cow, true), new HerdModule.SetKeep(cow, 7),
                new FarmModule.Reclaim(), new FarmModule.Harvest(), new FarmModule.SetHosting(true),
                new FarmModule.SetCrop(2), new TerritoryModule.Cede(Territory.Side.EAST, 3),
                new StorageModule.Take(4, "minecraft:wheat", true), new StorageModule.DepositCarried(),
                new StorageModule.DepositMatching());
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
        data.storage().insert(new ItemStack(Items.WHEAT, 70));
        data.addWhitelist(someone, "Steve");
        data.herd().setKeep(cow, 3);
        for (Page page : Page.values()) {
            PanelSnapshot snapshot = EarthSpiritPanel.snapshot(level, data, page, Component.literal("done"));
            helper.assertValueEqual(snapshot.section().page(), page, "a snapshot carries the asked page");
            byte[] first = encode(level, snapshot);
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(first),
                    level.registryAccess());
            PanelSnapshot decoded = PanelSnapshot.STREAM_CODEC.decode(buffer);
            helper.assertTrue(Arrays.equals(first, encode(level, decoded)), page + " section round trip");
        }
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
        EarthSpiritEntity spirit = helper.spawn(ModEntityTypes.EARTH_SPIRIT_FOX_IMMORTAL_BLISSFUL_LAND.get(),
                CENTER_POS.offset(1, 0, 1));
        try {
            MemberService.claim(null, data, owner);
            EarthSpiritMenu menu = new EarthSpiritMenu(1, owner.getInventory(), spirit,
                    EarthSpiritPanel.snapshot(level, data, Page.OVERVIEW, Component.empty()));
            owner.containerMenu = menu;
            helper.assertValueEqual(menu.page(), Page.OVERVIEW, "the panel opens on the overview");
            EarthSpiritPanel.handle(owner, new PanelActions.View(Page.STORAGE));
            helper.assertValueEqual(menu.page(), Page.STORAGE, "viewing a page switches what the server computes");
            helper.assertValueEqual(EarthSpiritPanel.lastPage(owner), Page.STORAGE, "the page is remembered");
            helper.assertTrue(EarthSpiritPanel.snapshot(level, data, menu.page(), Component.empty()).section()
                    instanceof StorageModule.Section, "only the storage section is computed");
            helper.succeed();
        } finally {
            EarthSpiritPanel.remember(owner, Page.OVERVIEW);
            owner.containerMenu = owner.inventoryMenu;
            MemberService.lose(level.getServer(), null, data);
        }
    }

    @GameTest(template = "empty9x9x9")
    public static void depositingMatchesStoresOnlyKnownKindsOutsideTheHotbar(GameTestHelper helper) {
        ServerPlayer owner = GameTestPlayers.survival(helper, "gzr-land-deposit-mock").onGround();
        BlissfulStorage storage = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL).storage();
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
    public static void onlyAnOnlineNameJoinsTheWhitelist(GameTestHelper helper) {
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        ServerPlayer guest = GameTestPlayers.survival(helper, "gzr-land-name-mock").onGround();

        helper.assertTrue(MemberService.addWhitelist(helper.getLevel().getServer(), data, "gzr-land-name-mock"),
                "an online name is listed");
        helper.assertTrue(data.isWhitelisted(guest.getUUID()), "by that player's UUID");
        helper.assertFalse(MemberService.addWhitelist(helper.getLevel().getServer(), data, "gzrNeverSeenMock"),
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
        BlissfulStorage storage = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL).storage();
        Map<ResourceLocation, Integer> keep = Map.of(HerdWork.typeId(calf), 2);

        int preview = HerdWork.slaughterCount(HerdWork.census(herd), keep, HerdWork.LIMIT);
        int killed = HerdWork.slaughter(helper.getLevel(), HerdWork.census(herd), keep, storage, null, HerdWork.LIMIT);
        helper.assertValueEqual(preview, 3, "five cows kept at two: three adults would go");
        helper.assertValueEqual(killed, preview, "the slaughter takes what the preview said");
        helper.succeed();
    }
}
