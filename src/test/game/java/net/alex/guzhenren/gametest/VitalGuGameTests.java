package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.gameplay.mind.MindPoolType;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.lifecycle.PendingVitalPenalties;
import net.alex.guzhenren.lifecycle.VitalLossService;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.alex.guzhenren.registry.item.ModItems;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestApertures.awakenedMortal;
import static net.alex.guzhenren.gametest.GameTestApertures.mortal;
import static net.alex.guzhenren.gametest.GameTestApertures.openStorage;
import static net.alex.guzhenren.gametest.GameTestPlayers.messages;
import static net.alex.guzhenren.gametest.GameTestPlayers.serverTicks;

/**
 * 本命蛊绑定、继承、丧失测试，覆盖在线与离线惩罚账本两条路径。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VitalGuGameTests {

    private VitalGuGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void vitalBindingSurvivesTheFirstApertureInsert(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        ApertureService.openSecond(player, Rank.THREE);
        ApertureService.setPrimaryPath(player, 0, GuPath.STRENGTH);
        ItemStack vital = GameTestGu.refined(ModItems.WHITE_BOAR_GU_1.get(), 3);
        VitalGuState.bind(vital, player, 0);
        ApertureStorageService.adoptVital(player, 0, vital);
        player.getInventory().setItem(9, vital);

        ApertureService.awaken(player, 80);
        ApertureService.setPrimaryPath(player, ApertureData.PRIMARY, GuPath.WOOD);
        TendedGuItem.starve(player, player.getInventory().getItem(9));
        serverTicks(player, VitalLossService.OFFLINE_VITAL_SETTLE_AFTER_TICKS + Ticks.SECOND);

        helper.assertTrue(ApertureService.getAperture(player, 0).primaryPath() == GuPath.WOOD,
                "losing the second aperture's vital cleared the first aperture's path");
        helper.assertTrue(ApertureService.getAperture(player, 1).primaryPath() == null,
                "the second aperture kept the lost vital's path");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void refinedVitalInheritsTheFirstEatenAperture(GameTestHelper helper) {
        ServerPlayer player = twoPathPlayer(helper);

        int heir = ApertureService.inheritVital(player, List.of(ApertureData.SECOND, ApertureData.PRIMARY),
                GuPath.FOOD);

        helper.assertValueEqual(heir, 1, "the first eaten vital's aperture inherits");
        helper.assertTrue(ApertureService.getAperture(player, 1).primaryPath() == GuPath.FOOD, "heir path");
        helper.assertTrue(ApertureService.getAperture(player, 0).primaryPath() == null,
                "the other eaten vital's aperture kept its path");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void refinedVitalReturnsToItsOwnAperture(GameTestHelper helper) {
        ServerPlayer player = twoPathPlayer(helper);

        int heir = ApertureService.inheritVital(player, List.of(ApertureData.SECOND), GuPath.FOOD);

        helper.assertValueEqual(heir, 1, "a second-aperture vital's product binds back to the second");
        helper.assertTrue(ApertureService.getAperture(player, 1).primaryPath() == GuPath.FOOD, "heir path");
        helper.assertTrue(ApertureService.getAperture(player, 0).primaryPath() == GuPath.WOOD,
                "an aperture whose vital was not eaten lost its path");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void inheritVitalSkipsAMissingAperture(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        ApertureService.setPrimaryPath(player, ApertureData.PRIMARY, GuPath.WOOD);

        int heir = ApertureService.inheritVital(player, List.of(ApertureData.SECOND, ApertureData.PRIMARY),
                GuPath.FOOD);

        helper.assertValueEqual(heir, 0, "the first eaten vital whose aperture exists inherits");
        helper.assertTrue(ApertureService.getAperture(player, 0).primaryPath() == GuPath.FOOD, "heir path");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 200)
    public static void replacedVitalDiesHarmlessly(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        SoulService.setMax(player, 1_000L);
        SoulService.setCurrent(player, 1_000L);
        ApertureStorageMenu menu = openStorage(player);
        menu.setCarried(GameTestGu.refined(ModItems.LIQUOR_WORM_1.get(), 3));
        menu.clicked(ApertureStorageMenu.VITAL_SLOT, 0, ClickType.PICKUP, player);
        ItemStack former = ApertureStorageService.getVital(player, ApertureData.PRIMARY);
        menu.setCarried(GameTestGu.refined(ModItems.WHITE_BOAR_GU_1.get(), 3));
        menu.clicked(ApertureStorageMenu.VITAL_SLOT, 0, ClickType.PICKUP, player);

        helper.assertTrue(!VitalGuState.isVital(menu.getCarried()), "the replaced vital kept its mark");
        TendedGuItem.starve(player, former);
        serverTicks(player, VitalLossService.OFFLINE_VITAL_SETTLE_AFTER_TICKS + Ticks.SECOND);

        helper.assertTrue(ApertureService.getAperture(player, 0).primaryPath() == GuPath.STRENGTH,
                "a replaced vital's death cleared the new vital's path");
        helper.assertValueEqual(SoulService.get(player).currentSoul(), 1_000L,
                "a replaced vital's death was charged");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void offlineVitalGuDeathIsRecordedAndSurvivesSave(GameTestHelper helper) {
        ServerPlayer holder = mortal(helper);
        UUID offlineOwner = UUID.randomUUID();
        PendingVitalPenalties ledger = PendingVitalPenalties.get(helper.getLevel().getServer());

        TendedGuItem.starve(holder, vitalOwnedBy(offlineOwner, ApertureData.PRIMARY));
        helper.assertValueEqual(ledger.count(offlineOwner), 1, "offline owner's vital loss recorded");

        PendingVitalPenalties reloaded = PendingVitalPenalties.load(
                GameTestWorld.save(helper.getLevel(), ledger), GameTestWorld.stackIo(helper.getLevel()));
        ItemStack restored = reloaded.poll(offlineOwner);
        helper.assertTrue(restored != null && restored.is(ModItems.WHITE_BOAR_GU_1.get())
                && offlineOwner.equals(VitalGuState.getOwner(restored)), "pending loss survives a save/load round trip");
        ledger.poll(offlineOwner);
        helper.assertValueEqual(ledger.count(offlineOwner), 0, "ledger drained");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void offlineVitalPenaltyWaitsOutSpawnInvulnerability(GameTestHelper helper) {
        ServerPlayer owner = mortal(helper);
        ApertureService.awaken(owner, 80);
        PendingVitalPenalties ledger = PendingVitalPenalties.get(helper.getLevel().getServer());
        ledger.recordPenalty(owner.getUUID(), vitalOwnedBy(owner.getUUID(), ApertureData.PRIMARY));

        VitalLossService.settleOfflineVitalLoss(owner);
        serverTicks(owner, VitalLossService.OFFLINE_VITAL_SETTLE_AFTER_TICKS);
        helper.assertValueEqual(owner.tickCount, VitalLossService.OFFLINE_VITAL_SETTLE_AFTER_TICKS,
                "tick count");
        helper.assertValueEqual(ledger.count(owner.getUUID()), 1,
                "nothing settles at login or on the heartbeats inside spawn invulnerability");
        ledger.poll(owner.getUUID());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void offlineVitalPenaltiesSettleOnePerHeartbeat(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer owner = GameTestPlayers.survival(helper, "gzr-gametest-mock").inbox(inbox).build();
        ApertureService.awaken(owner, 80);
        serverTicks(owner, VitalLossService.OFFLINE_VITAL_SETTLE_AFTER_TICKS);
        SoulService.setMax(owner, 1_000L);
        SoulService.setCurrent(owner, 1_000L);
        for (MindPoolType type : MindPoolType.values()) {
            MindService.setMax(owner, type, 1_000_000L);
            MindService.setCurrent(owner, type, 1_000_000L);
        }
        ApertureService.setPrimaryPath(owner, ApertureData.PRIMARY, GuPath.TIME);
        PendingVitalPenalties ledger = PendingVitalPenalties.get(helper.getLevel().getServer());
        ItemStack vital = boundVital(owner);
        ledger.recordPenalty(owner.getUUID(), vital.copy());
        ledger.recordPenalty(owner.getUUID(), vital.copy());
        float healthBefore = owner.getHealth();

        serverTicks(owner, Ticks.SECOND);
        helper.assertValueEqual(owner.tickCount, 80, "first heartbeat past spawn invulnerability");
        helper.assertValueEqual(ledger.count(owner.getUUID()), 1,
                "one loss settles on the first heartbeat");
        helper.assertValueEqual(SoulService.get(owner).currentSoul(), 500L,
                "first settle halves the soul");
        for (MindPoolType type : MindPoolType.values()) {
            helper.assertTrue(MindService.getCurrent(owner, type) < 1_000_000L, "first settle halves " + type);
        }
        helper.assertTrue(owner.getHealth() < healthBefore * 0.5F, "first settle's hurt landed");
        helper.assertTrue(ApertureService.getAperture(owner, ApertureData.PRIMARY).primaryPath() == null,
                "first settle clears the bound aperture's primary path");
        helper.assertValueEqual(messages(inbox, "guzhenren.item.gu.vital_lost").size(), 1,
                "one loss message");
        float healthAfterFirst = owner.getHealth();

        serverTicks(owner, Ticks.SECOND);
        helper.assertValueEqual(ledger.count(owner.getUUID()), 0,
                "second loss settles on the next heartbeat");
        helper.assertValueEqual(SoulService.get(owner).currentSoul(), 250L,
                "second settle halves again");
        helper.assertTrue(owner.getHealth() < healthAfterFirst, "second hurt cleared the hurt cooldown");
        helper.assertValueEqual(messages(inbox, "guzhenren.item.gu.vital_lost").size(), 2,
                "two loss messages");
        helper.succeed();
    }

    private static ServerPlayer twoPathPlayer(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        ApertureService.openSecond(player, Rank.THREE);
        ApertureService.setPrimaryPath(player, 0, GuPath.WOOD);
        ApertureService.setPrimaryPath(player, 1, GuPath.STRENGTH);
        return player;
    }

    private static ItemStack boundVital(ServerPlayer owner) {
        ItemStack vital = new ItemStack(ModItems.WHITE_BOAR_GU_1.get());
        VitalGuState.bind(vital, owner, ApertureData.PRIMARY);
        ApertureStorageService.adoptVital(owner, ApertureData.PRIMARY, vital);
        return vital;
    }

    private static ItemStack vitalOwnedBy(UUID owner, int aperture) {
        ItemStack vital = new ItemStack(ModItems.WHITE_BOAR_GU_1.get());
        vital.set(ModDataComponents.VITAL_OWNER, owner);
        vital.set(ModDataComponents.VITAL_APERTURE, aperture);
        return vital;
    }
}
