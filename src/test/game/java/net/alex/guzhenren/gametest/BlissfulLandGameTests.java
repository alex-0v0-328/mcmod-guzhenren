package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.blissful.BlissfulLand;
import net.alex.guzhenren.gameplay.blissful.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.BlissfulLandService;
import net.alex.guzhenren.gameplay.blissful.BlissfulLands;
import net.alex.guzhenren.gameplay.blissful.BlissfulStorage;
import net.alex.guzhenren.gameplay.blissful.EarthSpiritEntity;
import net.alex.guzhenren.gameplay.blissful.EarthSpiritMenu;
import net.alex.guzhenren.gameplay.blissful.EarthSpiritPanel;
import net.alex.guzhenren.gameplay.blissful.FarmWork;
import net.alex.guzhenren.gameplay.blissful.HerdWork;
import net.alex.guzhenren.gameplay.blissful.MountainWork;
import net.alex.guzhenren.gameplay.blissful.PanelAction;
import net.alex.guzhenren.gameplay.blissful.Territory;
import net.alex.guzhenren.gameplay.blissful.TerritoryWork;
import net.alex.guzhenren.gameplay.secretrealm.GutsStones;
import net.alex.guzhenren.gameplay.secretrealm.RealmSite;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderMountain;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderShakeEvent;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.lifecycle.PlayerDataService;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;

/**
 * 福地[Blissful Land]：登顶认主、死亡（看「死亡不掉落」）与重置失主、免震荡白名单、驱逐外人，以及面板各页的服务端操作——
 * 挪生物上山、胆识蛊催动、喂养繁殖、宰杀收取、养殖托管、开垦标准田、收割补种、割弃疆域、库藏存取。
 * 操作都喂测试区里的实体、小范围山心或自建的福地存档，不碰真福地维度；改全局存档与游戏规则的失主用例单独成批。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlissfulLandGameTests {

    private static final String GLOBAL_BATCH = "blissful_land_global";
    private static final int TEST_RADIUS = 2;

    private BlissfulLandGameTests() {}

    //region Ownership [认主与失主]
    @GameTest(template = "empty9x9x9")
    public static void theFirstPlayerOnTheSummitClaimsTheLand(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        helper.setBlock(CENTER_POS, ModBlocks.SOULSUNDER_STONE.get());
        int summitY = helper.absolutePos(CENTER_POS).getY();
        RealmSite site = new RealmSite(helper.absolutePos(CENTER_POS), TEST_RADIUS);
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        List<Component> inbox = new ArrayList<>();
        ServerPlayer below = GameTestPlayers.survival(helper, "gzr-land-below-mock").at(new Vec3(1.5D, 1.0D, 1.5D));
        ServerPlayer first = GameTestPlayers.survival(helper, "gzr-land-first-mock").inbox(inbox)
                .at(new Vec3(4.5D, 2.0D, 4.5D));
        ServerPlayer second = GameTestPlayers.survival(helper, "gzr-land-second-mock").at(new Vec3(4.5D, 2.0D, 4.5D));

        helper.assertFalse(BlissfulLandService.tryClaim(helper.getLevel(), data, site, summitY, below),
                "standing on the ground below the summit claims nothing");
        helper.assertTrue(BlissfulLandService.tryClaim(helper.getLevel(), data, site, summitY, first),
                "the first player on the top soulsunder stone claims the land");
        helper.assertTrue(data.isOwner(first.getUUID()), "the first player owns it");
        helper.assertTrue(data.isWhitelisted(first.getUUID()), "the owner starts on the whitelist");
        helper.assertFalse(GameTestPlayers.messages(inbox, BlissfulLandService.MSG_CLAIMED).isEmpty(),
                "the owner is told");
        helper.assertFalse(BlissfulLandService.tryClaim(helper.getLevel(), data, site, summitY, second),
                "a claimed land cannot be claimed again");
        helper.assertTrue(data.isOwner(first.getUUID()), "the first owner stays");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", batch = GLOBAL_BATCH)
    public static void dyingLosesTheLandUnlessKeepInventoryIsOn(GameTestHelper helper) {
        GameRules.BooleanValue keep = helper.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
        boolean before = keep.get();
        BlissfulLandData data = BlissfulLandData.get(helper.getLevel().getServer(), BlissfulLand.FOX_IMMORTAL);
        ServerPlayer owner = GameTestPlayers.survival(helper, "gzr-land-death-mock").onGround();
        try {
            BlissfulLandService.claim(null, data, owner);
            data.storage().insert(new ItemStack(Items.WHEAT, 5));
            data.setHosted(HerdWork.typeId(owner), true);

            keep.set(true, helper.getLevel().getServer());
            BlissfulLands.onDeath(owner);
            helper.assertTrue(data.isOwner(owner.getUUID()), "with keepInventory on, dying keeps the land");

            keep.set(false, helper.getLevel().getServer());
            BlissfulLands.onDeath(owner);
            helper.assertFalse(data.hasOwner(), "with keepInventory off, dying loses the land");
            helper.assertTrue(data.whitelist().isEmpty(), "losing clears the whitelist");
            helper.assertFalse(data.herdHosting(), "losing stops hosting");
            helper.assertValueEqual(data.storage().count(stack -> stack.is(Items.WHEAT)), 5L,
                    "the storage stays for the next owner");
            helper.succeed();
        } finally {
            keep.set(before, helper.getLevel().getServer());
            BlissfulLandService.lose(helper.getLevel().getServer(), null, data);
            data.storage().consume(stack -> true, Integer.MAX_VALUE);
        }
    }

    @GameTest(template = "empty9x9x9", batch = GLOBAL_BATCH)
    public static void resettingTheCharacterLosesTheLand(GameTestHelper helper) {
        BlissfulLandData data = BlissfulLandData.get(helper.getLevel().getServer(), BlissfulLand.FOX_IMMORTAL);
        ServerPlayer owner = GameTestPlayers.survival(helper, "gzr-land-reset-mock").onGround();
        try {
            BlissfulLandService.claim(null, data, owner);
            PlayerDataService.resetAll(owner);
            helper.assertFalse(data.hasOwner(), "a character reset loses the land");
            helper.succeed();
        } finally {
            BlissfulLandService.lose(helper.getLevel().getServer(), null, data);
        }
    }

    @GameTest(template = "empty9x9x9")
    public static void theWhitelistSparesTheShakeAndTheExpulsion(GameTestHelper helper) {
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        ServerPlayer owner = GameTestPlayers.survival(helper, "gzr-land-owner-mock").onGround();
        ServerPlayer guest = GameTestPlayers.survival(helper, "gzr-land-guest-mock").at(new Vec3(2.5D, 1.0D, 2.5D));
        ServerPlayer stranger = GameTestPlayers.survival(helper, "gzr-land-stranger-mock")
                .at(new Vec3(6.5D, 1.0D, 6.5D));
        BlissfulLandService.claim(null, data, owner);
        helper.assertTrue(BlissfulLandService.isExempt(data, owner), "the owner is exempt by default");
        helper.assertTrue(BlissfulLandService.setWhitelisted(helper.getLevel().getServer(), data, guest.getUUID(),
                true), "an online guest can be listed");
        helper.assertTrue(BlissfulLandService.isExempt(data, guest), "a listed guest is exempt");
        helper.assertFalse(BlissfulLandService.isExempt(data, stranger), "a stranger is not");
        data.removeWhitelist(owner.getUUID());
        helper.assertFalse(BlissfulLandService.isExempt(data, owner), "the owner can take themself off the list");

        Vec3 guestAt = guest.position();
        Vec3 strangerAt = stranger.position();
        int expelled = BlissfulLandService.expelOutsiders(List.of(owner, guest, stranger), data);
        helper.assertValueEqual(expelled, 1, "only the stranger is expelled");
        helper.assertTrue(guest.position().equals(guestAt), "a listed guest stays");
        helper.assertFalse(stranger.position().equals(strangerAt), "the stranger is sent back");
        helper.assertFalse(BlissfulLandService.expel(helper.getLevel(), data, owner.getUUID()),
                "the owner cannot be expelled");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aCancelledShakeDrainsNoSoul(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-land-shake-mock").onGround();
        Consumer<SoulsunderShakeEvent> listener = event -> {
            if (event.getPlayer() == player) event.setCanceled(true);
        };
        long current = SoulService.get(player).currentSoul();
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, SoulsunderShakeEvent.class, listener);
        try {
            SoulsunderMountain.drainSoul(player);
            helper.assertValueEqual(SoulService.get(player).currentSoul(), current, "a cancelled shake drains nothing");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        SoulsunderMountain.drainSoul(player);
        helper.assertValueEqual(SoulService.get(player).currentSoul(), current - SoulsunderMountain.SOUL_DRAIN,
                "an uncancelled shake drains as before");
        helper.succeed();
    }
    //endregion

    //region Mountain [荡魂山]
    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void movingMobsUpPutsThemOnTheMountainAndStartsTheCooldown(GameTestHelper helper) {
        layStonePad(helper);
        RealmSite site = new RealmSite(helper.absolutePos(CENTER_POS), TEST_RADIUS);
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(8, 1, 8));
        Pig named = helper.spawn(EntityType.PIG, new BlockPos(0, 1, 8));
        named.setCustomName(Component.literal("Named"));

        int moved = MountainWork.moveMobsUp(helper.getLevel(), site, data, helper.getLevel().getRandom(),
                List.of(pig, named));
        helper.assertValueEqual(moved, 1, "only the unnamed pig is moved");
        helper.assertTrue(site.contains(pig), "the pig now stands inside the mountain");
        helper.assertTrue(helper.getLevel().getBlockState(pig.blockPosition().below())
                .is(ModBlocks.SOULSUNDER_STONE.get()), "the pig stands on soulsunder stone");
        helper.assertFalse(site.contains(named), "the named pig stays put");
        helper.assertValueEqual(MountainWork.moveCooldown(helper.getLevel(), data),
                (long) MountainWork.MOVE_COOLDOWN_TICKS, "moving starts the sixty-second cooldown");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void drivingGutsGuBreaksEveryStoneAndRaisesTheOwnersCap(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        RealmSite site = new RealmSite(helper.absolutePos(CENTER_POS), TEST_RADIUS);
        GutsStones stones = GutsStones.get(level);
        BlockState full = ModBlocks.GUTS_STONE.get().defaultBlockState().setValue(GutsStones.STONES, 3);
        List<BlockPos> cells = List.of(CENTER_POS.offset(1, 0, 0), CENTER_POS.offset(-1, 0, 0));
        for (BlockPos cell : cells) {
            helper.setBlock(cell, full);
            stones.set(helper.absolutePos(cell), 3);
        }
        ServerPlayer outside = GameTestGu.awakened(
                GameTestPlayers.survival(helper, "gzr-land-drive-out-mock").at(new Vec3(8.5D, 1.0D, 8.5D)), Rank.ONE);
        helper.assertTrue(MountainWork.driveGutsGu(level, site, outside) == null,
                "the owner must stand inside the mountain");

        ServerPlayer owner = GameTestGu.awakened(GameTestPlayers.survival(helper, "gzr-land-drive-mock").at(CENTER),
                Rank.ONE);
        long max = SoulService.get(owner).maxSoul();
        MountainWork.Drive drive = MountainWork.driveGutsGu(level, site, owner);
        helper.assertTrue(drive != null, "inside the mountain the drive runs");
        helper.assertValueEqual(drive.stones(), 6, "every stone in both cells breaks");
        helper.assertTrue(drive.gu() <= drive.stones(), "at most one Gu per stone");
        for (BlockPos cell : cells) helper.assertBlockPresent(Blocks.AIR, cell);
        helper.assertValueEqual(SoulService.get(owner).maxSoul(),
                max + (long) drive.gu() * SoulsunderMountain.GUTS_GU_SOUL_BONUS, "each Gu raises the cap by ten");
        helper.assertFalse(owner.getInventory().hasAnyMatching(stack -> stack.is(ModItems.GUTS_GU_1.get())),
                "no Guts Gu item is handed out");
        helper.succeed();
    }
    //endregion

    //region Herd [养殖]
    @GameTest(template = "empty9x9x9")
    public static void feedingBreedsPairsOutOfStorage(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        List<Entity> cows = new ArrayList<>();
        for (int i = 0; i < 5; i++) cows.add(helper.spawn(EntityType.COW, new BlockPos(1 + i, 1, 4)));
        BlissfulStorage storage = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL).storage();
        storage.insert(new ItemStack(Items.WHEAT, 3));

        HerdWork.Breed bred = HerdWork.breed(HerdWork.census(cows), storage, HerdWork.LIMIT, null);
        helper.assertValueEqual(bred.animals(), 2, "three wheat feeds only one pair");
        helper.assertValueEqual(storage.count(stack -> stack.is(Items.WHEAT)), 1L, "one wheat is left");
        long inLove = cows.stream().filter(cow -> ((Animal) cow).isInLove()).count();
        helper.assertValueEqual(inLove, 2L, "two cows are in love");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void slaughterKeepsTheChosenNumberAndPaysTheOwner(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        List<Entity> herd = new ArrayList<>();
        for (int i = 0; i < 4; i++) herd.add(helper.spawn(EntityType.COW, new BlockPos(1 + i, 1, 2)));
        Cow calf = helper.spawn(EntityType.COW, new BlockPos(6, 1, 2));
        calf.setBaby(true);
        herd.add(calf);
        for (int i = 0; i < 3; i++) herd.add(helper.spawn(EntityType.PIG, new BlockPos(1 + i, 1, 6)));
        ServerPlayer owner = GameTestPlayers.survival(helper, "gzr-land-slaughter-mock").onGround();
        BlissfulStorage storage = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL).storage();
        ResourceLocation cow = HerdWork.typeId(calf);

        Map<ResourceLocation, List<Animal>> herds = HerdWork.census(herd);
        int killed = HerdWork.slaughter(helper.getLevel(), herds, Map.of(cow, 2), storage, owner, HerdWork.LIMIT);
        helper.assertValueEqual(killed, 3, "five cows down to two: three adults go, the calf stays");
        helper.assertFalse(calf.isRemoved(), "a calf is never slaughtered");
        helper.assertTrue(herd.stream().filter(entity -> entity instanceof Pig).noneMatch(Entity::isRemoved),
                "a kind kept at zero is left alone");
        helper.assertTrue(storage.count(stack -> stack.is(Items.BEEF)) >= 3L, "each cow leaves beef in storage");
        helper.assertTrue(owner.totalExperience > 0, "the owner gets the experience");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void herdHostingBreedsUpToTheKeptNumber(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        List<Entity> herd = new ArrayList<>();
        for (int i = 0; i < 2; i++) herd.add(helper.spawn(EntityType.COW, new BlockPos(1 + i, 1, 4)));
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        data.setKeep(HerdWork.typeId(herd.getFirst()), 3);
        data.setHosted(HerdWork.typeId(herd.getFirst()), true);
        data.storage().insert(new ItemStack(Items.WHEAT, 10));

        int handled = HerdWork.host(helper.getLevel(), HerdWork.census(herd), data, null, BlissfulLands.HOSTING_LIMIT);
        helper.assertValueEqual(handled, 2, "one cow short: one pair is fed");
        helper.assertValueEqual(data.storage().count(stack -> stack.is(Items.WHEAT)), 8L, "two wheat are eaten");
        helper.succeed();
    }
    //endregion

    //region Farm [种植]
    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void reclaimingLaysAWateredFieldAndHarvestReplants(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        data.storage().insert(new ItemStack(Items.WHEAT_SEEDS, 100));
        ServerPlayer owner = GameTestPlayers.survival(helper, "gzr-land-farm-mock").at(new Vec3(4.5D, 3.0D, 4.5D));
        try {
            BlockPos slot = helper.absolutePos(CENTER_POS);
            helper.setBlock(new BlockPos(0, 0, 0), Blocks.WATER);
            helper.assertFalse(FarmWork.isArable(level, slot), "a pool inside the nine by nine makes it wet land");
            helper.assertTrue(FarmWork.reclaim(level, data, slot, owner) == null, "wet land is not reclaimed");
            helper.setBlock(new BlockPos(0, 0, 0), Blocks.STONE);
            helper.assertFalse(FarmWork.isSeed(new ItemStack(Items.TORCHFLOWER_SEEDS)), "other crop seeds are refused");
            helper.assertTrue(FarmWork.isSeed(new ItemStack(Items.WHEAT_SEEDS)), "wheat seeds are taken");
            BlockPos center = FarmWork.reclaim(level, data, slot, owner);
            helper.assertTrue(center != null, "the field is laid");
            helper.assertTrue(level.getBlockState(center).is(Blocks.WATER), "the middle is a water hole");
            helper.assertTrue(level.getBlockState(center.offset(4, 0, -4)).is(Blocks.FARMLAND), "a corner is farmland");
            helper.assertTrue(level.getBlockState(center.offset(-4, 1, 4)).is(Blocks.WHEAT), "a corner is sown");
            helper.assertValueEqual(data.storage().count(stack -> stack.is(Items.WHEAT_SEEDS)), 20L,
                    "eighty seeds are planted");
            helper.assertValueEqual(data.fields().size(), 1, "the field is recorded");
            helper.assertTrue(FarmWork.reclaimCooldown(level, data) > 0, "reclaiming starts the cooldown");

            BlockState ripe = ((CropBlock) Blocks.WHEAT).getStateForAge(((CropBlock) Blocks.WHEAT).getMaxAge());
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (dx != 0 || dz != 0) level.setBlock(center.offset(dx, 1, dz), ripe, 2);
                }
            }
            helper.assertValueEqual(FarmWork.census(level, data).get(Blocks.WHEAT), new FarmWork.Count(80, 0),
                    "eighty ripe wheat are counted");
            helper.assertValueEqual(FarmWork.harvest(level, data, Integer.MAX_VALUE), 80, "all eighty are cut");
            helper.assertValueEqual(FarmWork.census(level, data).get(Blocks.WHEAT), new FarmWork.Count(0, 80),
                    "every cell is replanted");
            helper.assertTrue(data.storage().count(stack -> stack.is(Items.WHEAT)) >= 80L,
                    "the wheat goes to storage");
            helper.succeed();
        } finally {
            FarmWork.release(level, data);
        }
    }

    @GameTest(template = "empty9x9x9")
    public static void theFarmZoneHoldsThirtySixFieldsInRows(GameTestHelper helper) {
        BlissfulLand land = BlissfulLand.FOX_IMMORTAL;
        BlissfulLandData data = BlissfulLandData.create(land);
        helper.assertValueEqual(FarmWork.capacity(land), 36, "a four-chunk zone holds six by six fields");
        BlockPos first = FarmWork.freeSlots(land, data).getFirst();
        helper.assertValueEqual(first, new BlockPos(-32 + 2 + 4, 0, 176 + 2 + 4), "the first slot is the north-west");
        data.addField(first.atY(64));
        helper.assertValueEqual(FarmWork.freeSlots(land, data).getFirst(), new BlockPos(-32 + 12 + 4, 0, 176 + 2 + 4),
                "the next slot is one field and an aisle east");
        helper.succeed();
    }
    //endregion

    //region Territory [疆域]
    @GameTest(template = "empty9x9x9")
    public static void cedingRemovesMobsAndSendsPlayersHome(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        Pig pig = helper.spawn(EntityType.PIG, CENTER_POS);
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-land-cede-mock").onGround();
        ChunkPos at = pig.chunkPosition();
        int half = Math.max(Math.abs(at.x), Math.abs(at.z)) + 4;
        BlissfulLand land = new BlissfulLand("gametest_land", level.dimension(), SoulsunderMountain.INSTANCE, 0,
                BlockPos.ZERO, half, 0, new ChunkPos(0, 0), 1);
        BlissfulLandData data = BlissfulLandData.create(land);
        Territory.Side side = at.x >= 0 ? Territory.Side.EAST : Territory.Side.WEST;
        int width = at.x >= 0 ? half - at.x : at.x + half + 1;
        Vec3 playerAt = player.position();
        try {
            helper.assertFalse(TerritoryWork.cede(level, data, side, half + 1, List.of()), "the core cannot go");
            helper.assertTrue(TerritoryWork.cede(level, data, side, width, List.of(pig, player)),
                    "a strip reaching the test area can go");
            helper.assertTrue(data.territory().isCeded(at.x, at.z), "the strip holding the test area is ceded");
            helper.assertTrue(pig.isRemoved(), "a mob in the strip is removed");
            helper.assertFalse(player.position().equals(playerAt), "a player in the strip is sent home");
            TerritoryWork.onChunkLoad(level, data.territory(), level.getChunk(at.x, at.z));
            helper.assertTrue(TerritoryWork.isQueued(level, at.x, at.z), "a loaded ceded chunk is queued to clear");
            helper.succeed();
        } finally {
            TerritoryWork.clear();
        }
    }
    //endregion

    //region Panel [面板]
    @GameTest(template = "empty9x9x9", batch = GLOBAL_BATCH)
    public static void onlyTheOwnerWithALivePanelCanAct(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlissfulLandData data = BlissfulLandData.get(level.getServer(), BlissfulLand.FOX_IMMORTAL);
        ServerPlayer owner = GameTestPlayers.survival(helper, "gzr-land-panel-owner-mock").onGround();
        ServerPlayer stranger = GameTestPlayers.survival(helper, "gzr-land-panel-stranger-mock")
                .at(new Vec3(3.5D, 1.0D, 3.5D));
        EarthSpiritEntity spirit = spawnSpirit(helper);
        ResourceLocation cowId = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW);
        String cow = cowId.toString();
        try {
            BlissfulLandService.claim(null, data, owner);
            stranger.containerMenu = openPanel(level, data, stranger, spirit);
            EarthSpiritPanel.handle(stranger, PanelAction.HERD_KEEP, 5, cow);
            helper.assertValueEqual(data.keep(cowId), 0, "a stranger's forged panel is refused");
            EarthSpiritPanel.handle(owner, PanelAction.HERD_KEEP, 5, cow);
            helper.assertValueEqual(data.keep(cowId), 0, "without the panel open nothing happens");
            owner.containerMenu = openPanel(level, data, owner, spirit);
            EarthSpiritPanel.handle(owner, PanelAction.HERD_KEEP, 5, cow);
            helper.assertValueEqual(data.keep(cowId), 5, "the owner with the panel open is obeyed");
            spirit.discard();
            EarthSpiritPanel.handle(owner, PanelAction.HERD_KEEP, 7, cow);
            helper.assertValueEqual(data.keep(cowId), 5, "a panel whose spirit is gone is stale");
            helper.succeed();
        } finally {
            owner.containerMenu = owner.inventoryMenu;
            stranger.containerMenu = stranger.inventoryMenu;
            data.setKeep(cowId, 0);
            BlissfulLandService.lose(level.getServer(), null, data);
        }
    }

    @GameTest(template = "empty9x9x9", batch = GLOBAL_BATCH)
    public static void takingAndDepositingKeepsEveryItem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlissfulLandData data = BlissfulLandData.get(level.getServer(), BlissfulLand.FOX_IMMORTAL);
        ServerPlayer owner = GameTestPlayers.survival(helper, "gzr-land-storage-mock").onGround();
        EarthSpiritEntity spirit = spawnSpirit(helper);
        BlissfulStorage storage = data.storage();
        try {
            BlissfulLandService.claim(null, data, owner);
            EarthSpiritMenu menu = openPanel(level, data, owner, spirit);
            owner.containerMenu = menu;
            storage.insert(new ItemStack(Items.WHEAT, 100));
            storage.insert(new ItemStack(Items.CARROT, 5));

            EarthSpiritPanel.handle(owner, PanelAction.TAKE_STACK, 0, "minecraft:wheat");
            helper.assertValueEqual(menu.getCarried().getCount(), 64, "one take puts a stack on the cursor");
            helper.assertValueEqual(wheat(storage) + menu.getCarried().getCount(), 100L, "taking keeps the total");
            EarthSpiritPanel.handle(owner, PanelAction.DEPOSIT_CARRIED, 0, "");
            helper.assertTrue(menu.getCarried().isEmpty(), "depositing empties the cursor");
            helper.assertValueEqual(wheat(storage), 100L, "depositing keeps the total");

            owner.getInventory().setItem(0, new ItemStack(Items.WHEAT, 10));
            menu.quickMoveStack(owner, 27);
            helper.assertTrue(owner.getInventory().getItem(0).isEmpty(), "shift-clicking a hotbar slot stores it");
            helper.assertValueEqual(wheat(storage), 110L, "storing keeps the total");

            EarthSpiritPanel.handle(owner, PanelAction.TAKE_STACK, 0, "minecraft:carrot");
            helper.assertTrue(menu.getCarried().is(Items.CARROT), "a stale index still takes the named item");
            menu.setCarried(ItemStack.EMPTY);

            EarthSpiritPanel.handle(owner, PanelAction.TAKE_ALL, 0, "minecraft:wheat");
            helper.assertValueEqual(wheat(storage), 0L, "taking all empties the entry");
            helper.assertValueEqual(owner.getInventory().countItem(Items.WHEAT), 110, "all of it is in the bag");

            owner.getInventory().clearContent();
            for (int slot = 0; slot < owner.getInventory().items.size(); slot++) {
                owner.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
            }
            storage.insert(new ItemStack(Items.WHEAT, 30));
            owner.getAbilities().instabuild = true;
            EarthSpiritPanel.handle(owner, PanelAction.TAKE_ALL, 0, "minecraft:wheat");
            helper.assertValueEqual(wheat(storage), 30L, "a full creative bag takes nothing and loses nothing");
            helper.succeed();
        } finally {
            owner.getAbilities().instabuild = false;
            owner.containerMenu = owner.inventoryMenu;
            storage.consume(stack -> true, Integer.MAX_VALUE);
            BlissfulLandService.lose(level.getServer(), null, data);
        }
    }

    private static long wheat(BlissfulStorage storage) { return storage.count(stack -> stack.is(Items.WHEAT)); }

    private static EarthSpiritEntity spawnSpirit(GameTestHelper helper) {
        return helper.spawn(ModEntityTypes.EARTH_SPIRIT_FOX_IMMORTAL_BLISSFUL_LAND.get(), CENTER_POS.offset(1, 0, 1));
    }

    private static EarthSpiritMenu openPanel(ServerLevel level, BlissfulLandData data, ServerPlayer player,
                                             EarthSpiritEntity spirit) {
        return new EarthSpiritMenu(1, player.getInventory(), spirit,
                EarthSpiritPanel.snapshot(level, data, Component.empty()));
    }
    //endregion

    //region Storage [库藏]
    @GameTest(template = "empty9x9x9")
    public static void storageMergesWithoutALimitAndRefusesGutsGu(GameTestHelper helper) {
        BlissfulStorage storage = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL).storage();
        for (int i = 0; i < 100; i++) storage.insert(new ItemStack(Items.WHEAT, 64));
        helper.assertValueEqual(storage.entries().size(), 1, "one kind is one entry");
        helper.assertValueEqual(storage.count(stack -> stack.is(Items.WHEAT)), 6400L, "no limit on the count");
        helper.assertFalse(storage.insert(new ItemStack(ModItems.GUTS_GU_1.get())), "Guts Gu never enters");
        ItemStack taken = storage.extract(0, 1000);
        helper.assertValueEqual(taken.getCount(), 64, "one take is at most a stack");
        helper.assertValueEqual(storage.count(stack -> stack.is(Items.WHEAT)), 6336L, "the take is subtracted");
        helper.succeed();
    }
    //endregion

    private static void layStonePad(GameTestHelper helper) {
        for (int x = -TEST_RADIUS; x <= TEST_RADIUS; x++) {
            for (int z = -TEST_RADIUS; z <= TEST_RADIUS; z++) {
                helper.setBlock(CENTER_POS.offset(x, 0, z), ModBlocks.SOULSUNDER_STONE.get());
            }
        }
    }
}
