package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.secretrealm.GutsStones;
import net.alex.guzhenren.gameplay.secretrealm.RealmSite;
import net.alex.guzhenren.gameplay.secretrealm.SecretRealms;
import net.alex.guzhenren.gameplay.secretrealm.SoulquakeMountain;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.lifecycle.Heartbeat;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.item.ModItems;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestApertures.mortal;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;

/**
 * 天地秘境[Secret Realm]·荡魂山[Soulquake Mountain]：山石不可破坏，胆石叠放、逐块敲碎、掉落、上限与流体，死亡长石，
 * 扣魂扣血与 128 格玩家门槛，胆识蛊[Guts Gu]催动门槛、出山消散与不进收纳容器。
 * 效果方法接受显式小半径范围，不影响并排测试；需钉住全局山心的门槛与心跳用例单独成批，结束即清除。长胆石用例开
 * {@code skyAccess}，否则测试区的屏障天花板会成为高度图顶层。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SecretRealmGameTests {

    private static final String PINNED_BATCH = "secret_realm_pinned";
    private static final int TEST_RADIUS = 2;
    private static final int LOOT_ROLLS = 400;
    private static final long LOOT_SEED = 20261120L;
    private static final int CRACKED_CELLS = 12;
    private static final int DEATHS = 16;
    private static final int FAR_SITE = 100_000;

    private SecretRealmGameTests() {}

    //region Soulquake stone [荡魂山石]
    @GameTest(template = "empty9x9x9")
    public static void soulquakeStoneIsUnbreakableAndHasNoItem(GameTestHelper helper) {
        helper.setBlock(CENTER_POS, ModBlocks.SOULQUAKE_STONE.get());
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(CENTER_POS);
        BlockState state = level.getBlockState(pos);
        ServerPlayer player = mortal(helper);
        helper.assertValueEqual(state.getDestroySpeed(level, pos), -1.0F, "destroy speed");
        helper.assertValueEqual(state.getDestroyProgress(player, level, pos), 0.0F, "a hand makes no progress");
        helper.assertTrue(state.getBlock().getExplosionResistance() >= Blocks.BEDROCK.getExplosionResistance(),
                "explosion resistance matches bedrock");
        helper.assertTrue(state.is(BlockTags.WITHER_IMMUNE) && state.is(BlockTags.DRAGON_IMMUNE),
                "withers and dragons cannot eat it");
        helper.assertValueEqual(state.getPistonPushReaction(), PushReaction.BLOCK, "pistons cannot move it");
        helper.assertTrue(state.getBlock().asItem() == Items.AIR, "it has no item form");
        helper.assertFalse(state.isValidSpawn(level, pos, EntityType.ZOMBIE), "mobs cannot spawn on it");
        helper.assertValueEqual(state.getLightBlock(level, pos), 15, "it blocks all light");
        helper.assertValueEqual(state.getLightEmission(level, pos), 0, "it gives no light");
        helper.assertFalse(state.isRandomlyTicking(), "it never random-ticks");
        helper.assertTrue(state.skipRendering(state, Direction.UP), "faces between two stones are culled");

        level.explode(null, pos.getX() + 1.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 2.0F,
                Level.ExplosionInteraction.TNT);
        helper.assertBlockPresent(ModBlocks.SOULQUAKE_STONE.get(), CENTER_POS);
        helper.succeed();
    }
    //endregion

    //region Guts stone [胆石]
    @GameTest(template = "empty9x9x9")
    public static void gutsStoneDropsAGutsGuHalfTheTime(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockState state = ModBlocks.GUTS_STONE.get().defaultBlockState();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(state.getBlock().getLootTable());
        RandomSource random = RandomSource.create(LOOT_SEED);
        int guts = 0;
        for (int roll = 0; roll < LOOT_ROLLS; roll++) {
            for (ItemStack drop : table.getRandomItems(blockLoot(helper, state), random)) {
                helper.assertTrue(drop.is(ModItems.GUTS_GU_1.get()) && drop.getCount() == 1,
                        "a guts stone drops only single rank-one Guts Gu, got " + drop);
                guts++;
            }
        }
        helper.assertTrue(guts > LOOT_ROLLS * 0.4 && guts < LOOT_ROLLS * 0.6,
                "about half of " + LOOT_ROLLS + " breaks drop a Guts Gu, got " + guts);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aThreeStoneCellRollsOncePerStone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockState state = ModBlocks.GUTS_STONE.get().defaultBlockState().setValue(GutsStones.STONES, 3);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(state.getBlock().getLootTable());
        RandomSource random = RandomSource.create(LOOT_SEED);
        int guts = 0;
        int most = 0;
        for (int roll = 0; roll < LOOT_ROLLS; roll++) {
            int drops = 0;
            for (ItemStack drop : table.getRandomItems(blockLoot(helper, state), random)) drops += drop.getCount();
            guts += drops;
            most = Math.max(most, drops);
        }
        helper.assertValueEqual(most, 3, "a whole three-stone cell drops at most three");
        helper.assertTrue(guts > LOOT_ROLLS * 3 * 0.4 && guts < LOOT_ROLLS * 3 * 0.6,
                "each of the three stones drops half the time, got " + guts + " from " + LOOT_ROLLS + " cells");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void gutsStoneGrowsOnTopOfTheMountainStone(GameTestHelper helper) {
        layStonePad(helper);
        GutsStones stones = new GutsStones();
        BlockPos grown = SoulquakeMountain.growGutsStone(helper.getLevel(), stones, helper.absolutePos(CENTER_POS),
                TEST_RADIUS, helper.getLevel().getRandom());
        helper.assertTrue(grown != null, "a guts stone grows on a full pad");
        helper.assertTrue(helper.getLevel().getBlockState(grown).is(ModBlocks.GUTS_STONE.get()), "it is a guts stone");
        helper.assertTrue(helper.getLevel().getBlockState(grown.below()).is(ModBlocks.SOULQUAKE_STONE.get()),
                "it sits on the mountain stone");
        helper.assertTrue(stones.contains(grown) && stones.size() == 1 && stones.total() == 1,
                "its position is recorded as one stone");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void gutsStonesStopAtTheCap(GameTestHelper helper) {
        layStonePad(helper);
        GutsStones stones = new GutsStones();
        for (int i = 0; i < SoulquakeMountain.GUTS_STONE_CAP / 2; i++) {
            stones.set(new BlockPos(100_000 + i, 0, 100_000), 2);
        }
        helper.assertValueEqual(stones.total(), SoulquakeMountain.GUTS_STONE_CAP, "half as many cells hold the cap");
        BlockPos grown = SoulquakeMountain.growGutsStone(helper.getLevel(), stones, helper.absolutePos(CENTER_POS),
                TEST_RADIUS, helper.getLevel().getRandom());
        helper.assertTrue(grown == null, "nothing grows at " + SoulquakeMountain.GUTS_STONE_CAP + " stones");
        helper.assertTrue(SoulquakeMountain.stackGutsStone(helper.getLevel(), stones, helper.getLevel().getRandom())
                == null, "nothing stacks at the cap either");
        for (int x = -TEST_RADIUS; x <= TEST_RADIUS; x++) {
            for (int z = -TEST_RADIUS; z <= TEST_RADIUS; z++) {
                helper.assertBlockNotPresent(ModBlocks.GUTS_STONE.get(), CENTER_POS.offset(x, 1, z));
            }
        }
        stones.remove(new BlockPos(100_000, 0, 100_000));
        helper.assertTrue(SoulquakeMountain.growGutsStone(helper.getLevel(), stones, helper.absolutePos(CENTER_POS),
                TEST_RADIUS, helper.getLevel().getRandom()) != null, "one free slot lets one grow again");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void gutsStonesStackToThreeAndGrowTheirShape(GameTestHelper helper) {
        layStonePad(helper);
        ServerLevel level = helper.getLevel();
        GutsStones stones = new GutsStones();
        BlockPos pos = SoulquakeMountain.growGutsStone(level, stones, helper.absolutePos(CENTER_POS), TEST_RADIUS,
                level.getRandom());
        helper.assertTrue(pos != null, "a first stone grows");
        double single = shapeArea(level, pos);

        helper.assertValueEqual(SoulquakeMountain.stackGutsStone(level, stones, level.getRandom()), pos,
                "the only open cell takes the second stone");
        helper.assertValueEqual(level.getBlockState(pos).getValue(GutsStones.STONES), 2, "two stones");
        double pair = shapeArea(level, pos);
        helper.assertValueEqual(SoulquakeMountain.stackGutsStone(level, stones, level.getRandom()), pos, "and a third");
        helper.assertValueEqual(level.getBlockState(pos).getValue(GutsStones.STONES), 3, "three stones");
        helper.assertTrue(stones.count(pos) == 3 && stones.total() == 3 && stones.size() == 1,
                "the record keeps one cell of three stones");
        helper.assertTrue(single < pair && pair < shapeArea(level, pos), "the outline grows with each stone");
        helper.assertTrue(SoulquakeMountain.stackGutsStone(level, stones, level.getRandom()) == null,
                "a full cell takes no fourth stone");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void eachBreakCracksOneStoneAndDropsAtMostOneGu(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(CENTER_POS);
        GutsStones stones = GutsStones.get(level);
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-guts-crack-mock").build();
        int dropped = 0;
        for (int cell = 0; cell < CRACKED_CELLS; cell++) {
            helper.setBlock(CENTER_POS, ModBlocks.GUTS_STONE.get().defaultBlockState().setValue(GutsStones.STONES, 3));
            stones.set(pos, 3);
            for (int left = 2; left >= 0; left--) {
                helper.assertTrue(player.gameMode.destroyBlock(pos), "the survival break lands");
                int drops = collectDrops(level, pos);
                helper.assertTrue(drops <= 1, "one break drops at most one Guts Gu, got " + drops);
                dropped += drops;
                if (left == 0) break;
                helper.assertValueEqual(level.getBlockState(pos).getValue(GutsStones.STONES), left, "one stone cracks");
                helper.assertValueEqual(stones.count(pos), left, "the record follows the stone count");
            }
            helper.assertBlockNotPresent(ModBlocks.GUTS_STONE.get(), CENTER_POS);
            helper.assertFalse(stones.contains(pos), "the last stone frees the cell");
        }
        int breaks = CRACKED_CELLS * 3;
        helper.assertTrue(dropped > breaks * 0.25 && dropped < breaks * 0.75,
                "about half of " + breaks + " breaks drop a Guts Gu, got " + dropped);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void halfOfTheDeathsStackOntoAnOpenCell(GameTestHelper helper) {
        layStonePad(helper);
        ServerLevel level = helper.getLevel();
        GutsStones stones = new GutsStones();
        RandomSource random = RandomSource.create(LOOT_SEED);
        int stacked = 0;
        int grown = 0;
        for (int death = 0; death < DEATHS; death++) {
            int cells = stones.size();
            int total = stones.total();
            BlockPos left = SoulquakeMountain.leaveGutsStone(level, stones, helper.absolutePos(CENTER_POS),
                    TEST_RADIUS, random);
            if (left == null) continue;
            helper.assertValueEqual(stones.total(), total + 1, "each death adds one stone");
            if (stones.size() > cells) grown++;
            else stacked++;
        }
        helper.assertTrue(stacked >= 3 && grown >= 3,
                "deaths both stack and grow, got " + stacked + " stacked and " + grown + " grown");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void waterCannotWashAGutsStoneAway(GameTestHelper helper) {
        BlockState state = ModBlocks.GUTS_STONE.get().defaultBlockState();
        helper.assertFalse(state.canBeReplaced(Fluids.WATER), "water cannot replace it");
        helper.assertFalse(state.canBeReplaced(Fluids.LAVA), "lava cannot replace it");
        helper.assertTrue(state.blocksMotion(), "flowing fluids treat it as a wall");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void breakingAGutsStoneFreesItsSlot(GameTestHelper helper) {
        layStonePad(helper);
        ServerLevel level = helper.getLevel();
        GutsStones stones = GutsStones.get(level);
        BlockPos grown = SoulquakeMountain.growGutsStone(level, stones, helper.absolutePos(CENTER_POS), TEST_RADIUS,
                level.getRandom());
        helper.assertTrue(grown != null && stones.contains(grown), "the grown stone is recorded in the level data");
        level.destroyBlock(grown, true);
        helper.assertFalse(stones.contains(grown), "breaking it removes the record");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void onlyMobsWithTwentyMaxHealthLeaveAGutsStone(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, CENTER_POS);
        Chicken chicken = helper.spawn(EntityType.CHICKEN, CENTER_POS);
        helper.assertTrue(SoulquakeMountain.leavesGutsStone(zombie), "a zombie (20 health) leaves one");
        helper.assertTrue(SoulquakeMountain.leavesGutsStone(mortal(helper)), "a player (20 health) leaves one");
        helper.assertFalse(SoulquakeMountain.leavesGutsStone(chicken), "a chicken (4 health) leaves none");
        helper.succeed();
    }
    //endregion

    //region Shaking [震荡]
    @GameTest(template = "empty9x9x9")
    public static void mobsInsideTakeTwoArmourPiercingDamageAndImmuneOnesNone(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, CENTER_POS);
        zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, CENTER_POS);
        Pig outside = helper.spawn(EntityType.PIG, CENTER_POS.offset(TEST_RADIUS, 0, TEST_RADIUS));
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-soulquake-mock").at(CENTER);
        float zombieHealth = zombie.getHealth();
        float standHealth = stand.getHealth();
        float pigHealth = outside.getHealth();
        float playerHealth = player.getHealth();

        SoulquakeMountain.damageMobs(helper.getLevel(), new RealmSite(helper.absolutePos(CENTER_POS), TEST_RADIUS));

        helper.assertValueEqual(zombie.getHealth(), zombieHealth - SoulquakeMountain.MOB_DAMAGE,
                "an armoured zombie loses the full damage");
        helper.assertValueEqual(stand.getHealth(), standHealth, "armour stands are immune");
        helper.assertValueEqual(outside.getHealth(), pigHealth, "a pig past the circle but inside the box is spared");
        helper.assertValueEqual(player.getHealth(), playerHealth, "players lose soul, not health");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void playersLoseFiveSoulASecondUntilCollapseAndCreativeNone(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        SoulService.setCurrent(player, 7L);
        SoulquakeMountain.drainSoul(player);
        helper.assertValueEqual(SoulService.get(player).currentSoul(), 7L - SoulquakeMountain.SOUL_DRAIN,
                "one second drains five");
        SoulquakeMountain.drainSoul(player);
        helper.assertValueEqual(SoulService.get(player).currentSoul(), 0L, "the drain stops at zero");
        helper.assertTrue(SoulService.get(player).isCollapsed(), "a drained soul collapses");

        boolean[] creative = {true};
        ServerPlayer builder = GameTestPlayers.survival(helper, "gzr-soulquake-creative").creative(() -> creative[0])
                .build();
        long full = SoulService.get(builder).currentSoul();
        SoulquakeMountain.drainSoul(builder);
        helper.assertValueEqual(SoulService.get(builder).currentSoul(), full, "creative players are immune");
        helper.succeed();
    }
    //endregion

    //region Guts Gu away [胆识蛊出山]
    @GameTest(template = "empty9x9x9")
    public static void dissolvingClearsTheInventoryAndTheApertureStore(GameTestHelper helper) {
        ServerPlayer player = GameTestGu.awakened(GameTestPlayers.survival(helper, "gzr-guts-dissolve-mock").at(CENTER),
                Rank.ONE);
        player.getInventory().setItem(3, new ItemStack(ModItems.GUTS_GU_1.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.GUTS_GU_1.get()));
        player.getInventory().setItem(5, new ItemStack(Items.STONE));
        player.inventoryMenu.getCraftSlots().setItem(0, new ItemStack(ModItems.GUTS_GU_1.get()));
        ItemStack boar = new ItemStack(ModItems.WHITE_BOAR_GU_1.get());
        ApertureStorageService.set(player, ApertureData.PRIMARY,
                List.of(new ItemStack(ModItems.GUTS_GU_1.get()), boar));
        ApertureStorageMenu menu = new ApertureStorageMenu(1, player.getInventory(), ApertureData.PRIMARY, 0);
        player.containerMenu = menu;
        menu.setCarried(new ItemStack(ModItems.GUTS_GU_1.get()));

        helper.assertValueEqual(SoulquakeMountain.dissolveGutsGu(player), 5, "all five Guts Gu dissolve");
        helper.assertTrue(menu.getCarried().isEmpty(), "the cursor is cleared");
        helper.assertTrue(player.inventoryMenu.getCraftSlots().getItem(0).isEmpty(), "the crafting grid is cleared");
        player.containerMenu = player.inventoryMenu;
        helper.assertFalse(player.getInventory().hasAnyMatching(stack -> stack.is(ModItems.GUTS_GU_1.get())),
                "none left on the player");
        helper.assertTrue(player.getInventory().getItem(5).is(Items.STONE), "other items stay");
        List<ItemStack> stored = ApertureStorageService.getItems(player, ApertureData.PRIMARY);
        helper.assertTrue(stored.stream().noneMatch(stack -> stack.is(ModItems.GUTS_GU_1.get())),
                "none left in the aperture store");
        helper.assertTrue(stored.stream().anyMatch(stack -> stack.is(ModItems.WHITE_BOAR_GU_1.get())),
                "other stored Gu stay");
        helper.assertValueEqual(SoulquakeMountain.dissolveGutsGu(player), 0, "a second pass finds nothing");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void creativePlayersKeepTheirGutsGuAndNoGutsGuFitsInABag(GameTestHelper helper) {
        boolean[] creative = {true};
        ServerPlayer builder = GameTestPlayers.survival(helper, "gzr-guts-creative-mock").creative(() -> creative[0])
                .build();
        builder.getInventory().setItem(0, new ItemStack(ModItems.GUTS_GU_1.get()));
        helper.assertValueEqual(SoulquakeMountain.dissolveGutsGu(builder), 0, "creative players keep it");
        creative[0] = false;
        helper.assertValueEqual(SoulquakeMountain.dissolveGutsGu(builder), 1, "back in survival it dissolves");

        helper.assertFalse(ModItems.GUTS_GU_1.get().canFitInsideContainerItems(),
                "a Guts Gu cannot go into a bundle or shulker box");
        helper.assertTrue(ModItems.WHITE_BOAR_GU_1.get().canFitInsideContainerItems(), "other Gu still can");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void areaTicksWaitForAPlayerWithinOneHundredTwentyEightBlocks(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-soulquake-gate-mock").at(CENTER);
        Vec3 home = player.position();
        RealmSite site = new RealmSite(BlockPos.containing(home).offset(FAR_SITE, 0, 0), SoulquakeMountain.RADIUS);
        int gate = SoulquakeMountain.RADIUS + SecretRealms.PLAYER_MARGIN;
        try {
            player.moveTo(site.center().getX() + 0.5D + gate, home.y, site.center().getZ() + 0.5D);
            helper.assertTrue(SecretRealms.hasPlayerNear(helper.getLevel(), site), "a player at 128 wakes the area");
            player.moveTo(site.center().getX() + 0.5D + gate + 1.0D, home.y, site.center().getZ() + 0.5D);
            helper.assertFalse(SecretRealms.hasPlayerNear(helper.getLevel(), site), "a player at 129 does not");
        } finally {
            player.moveTo(home.x, home.y, home.z);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void soulquakeMountainIsANineRankSoulPathRealm(GameTestHelper helper) {
        helper.assertValueEqual(SoulquakeMountain.INSTANCE.rank(), Rank.NINE, "rank");
        helper.assertValueEqual(SoulquakeMountain.INSTANCE.path(), GuPath.SOUL, "path");
        helper.succeed();
    }
    //endregion

    //region Pinned site [钉住山心]
    @GameTest(template = "empty9x9x9", batch = PINNED_BATCH)
    public static void gutsGuOnlyWorksInsideAndTheHeartbeatDrainsSoul(GameTestHelper helper) {
        SecretRealms.pin(SoulquakeMountain.INSTANCE, new RealmSite(helper.absolutePos(CENTER_POS), TEST_RADIUS));
        try {
            GameTestPlayers.ground(helper);
            ServerPlayer player = GameTestGu.awakened(
                    GameTestPlayers.survival(helper, "gzr-guts-gate-mock").at(new Vec3(8.5D, 1.0D, 8.5D)), Rank.ONE);
            long max = SoulService.get(player).maxSoul();
            ItemStack guts = new ItemStack(ModItems.GUTS_GU_1.get(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, guts);

            GameTestGu.click(helper, player, guts);
            helper.assertValueEqual(guts.getCount(), 2, "outside the mountain the use is refused and the Gu kept");
            helper.assertValueEqual(SoulService.get(player).maxSoul(), max, "outside the cap is unchanged");

            GameTestPlayers.place(player, helper, CENTER);
            GameTestGu.click(helper, player, guts);
            helper.assertValueEqual(guts.getCount(), 1, "inside the mountain the Gu is used");
            helper.assertValueEqual(SoulService.get(player).maxSoul(), max + 10L, "inside the cap rises by ten");

            long current = SoulService.get(player).currentSoul();
            player.tickCount = Heartbeat.HEARTBEAT_TICKS * 10;
            Heartbeat.beat(player);
            helper.assertValueEqual(SoulService.get(player).currentSoul(), current - SoulquakeMountain.SOUL_DRAIN,
                    "the heartbeat drains five soul inside the mountain");
            helper.assertValueEqual(guts.getCount(), 1, "inside the heartbeat keeps the Gu");

            GameTestPlayers.place(player, helper, new Vec3(8.5D, 1.0D, 8.5D));
            player.tickCount += Heartbeat.HEARTBEAT_TICKS;
            Heartbeat.beat(player);
            helper.assertFalse(player.getInventory().hasAnyMatching(stack -> stack.is(ModItems.GUTS_GU_1.get())),
                    "outside the heartbeat dissolves the Gu");
            helper.succeed();
        } finally {
            SecretRealms.pin(SoulquakeMountain.INSTANCE, null);
        }
    }

    @GameTest(template = "empty9x9x9", batch = PINNED_BATCH, skyAccess = true)
    public static void deathsInsideLeaveAGutsStoneAndOutsideNone(GameTestHelper helper) {
        layStonePad(helper);
        SecretRealms.pin(SoulquakeMountain.INSTANCE, new RealmSite(helper.absolutePos(CENTER_POS), TEST_RADIUS));
        try {
            GutsStones stones = GutsStones.get(helper.getLevel());
            int before = stones.total();
            helper.spawn(EntityType.ZOMBIE, CENTER_POS.offset(4, 1, 4)).kill();
            helper.assertValueEqual(stones.total(), before, "a zombie dying outside leaves nothing");
            helper.spawn(EntityType.CHICKEN, CENTER_POS.above()).kill();
            helper.assertValueEqual(stones.total(), before, "a chicken dying inside leaves nothing");
            helper.spawn(EntityType.ZOMBIE, CENTER_POS.above()).kill();
            helper.assertValueEqual(stones.total(), before + 1, "a zombie dying inside leaves one stone");
            helper.succeed();
        } finally {
            SecretRealms.pin(SoulquakeMountain.INSTANCE, null);
        }
    }

    @GameTest(template = "empty9x9x9", batch = PINNED_BATCH)
    public static void theHeartbeatRefreshesAnOpenStoreAfterDissolving(GameTestHelper helper) {
        SecretRealms.pin(SoulquakeMountain.INSTANCE, new RealmSite(helper.absolutePos(CENTER_POS), TEST_RADIUS));
        try {
            ServerPlayer player = GameTestGu.awakened(
                    GameTestPlayers.survival(helper, "gzr-guts-menu-mock").at(new Vec3(8.5D, 1.0D, 8.5D)), Rank.ONE);
            ApertureStorageService.set(player, ApertureData.PRIMARY, List.of(new ItemStack(ModItems.GUTS_GU_1.get())));
            ApertureStorageMenu menu = new ApertureStorageMenu(1, player.getInventory(), ApertureData.PRIMARY, 0);
            player.containerMenu = menu;
            helper.assertTrue(menu.getSlot(0).getItem().is(ModItems.GUTS_GU_1.get()), "the open store shows the Gu");

            player.tickCount = Heartbeat.HEARTBEAT_TICKS * 10;
            Heartbeat.beat(player);
            helper.assertTrue(ApertureStorageService.getItems(player, ApertureData.PRIMARY).stream()
                    .noneMatch(stack -> stack.is(ModItems.GUTS_GU_1.get())), "the store no longer holds it");
            helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "the open view no longer shows it");
            player.containerMenu = player.inventoryMenu;
            helper.succeed();
        } finally {
            SecretRealms.pin(SoulquakeMountain.INSTANCE, null);
        }
    }

    @GameTest(template = "empty9x9x9", batch = PINNED_BATCH)
    public static void droppedGutsGuVanishOutsideTheMountain(GameTestHelper helper) {
        SecretRealms.pin(SoulquakeMountain.INSTANCE, new RealmSite(helper.absolutePos(CENTER_POS), TEST_RADIUS));
        try {
            ItemEntity inside = dropGuts(helper, CENTER);
            ItemEntity outside = dropGuts(helper, new Vec3(8.5D, 1.0D, 8.5D));
            helper.assertFalse(inside.isRemoved(), "a Guts Gu dropped inside stays");
            helper.assertTrue(outside.isRemoved(), "a Guts Gu dropped outside vanishes on its next check");
            helper.succeed();
        } finally {
            SecretRealms.pin(SoulquakeMountain.INSTANCE, null);
        }
    }

    @GameTest(template = "empty9x9x9", batch = PINNED_BATCH)
    public static void withoutAMountainGutsGuIsRefusedButNeverDissolves(GameTestHelper helper) {
        helper.assertTrue(SecretRealms.siteOf(helper.getLevel().getServer(), SoulquakeMountain.INSTANCE) == null,
                "the flat test world has no soulquake mountain ring");
        ServerPlayer player = GameTestGu.awakened(GameTestPlayers.survival(helper, "gzr-guts-free-mock").at(CENTER),
                Rank.ONE);
        long max = SoulService.get(player).maxSoul();
        ItemStack guts = new ItemStack(ModItems.GUTS_GU_1.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, guts);
        GameTestGu.click(helper, player, guts);
        helper.assertValueEqual(guts.getCount(), 1, "without the mountain the Gu is kept");
        helper.assertValueEqual(SoulService.get(player).maxSoul(), max, "without the mountain the use is refused");

        player.tickCount = Heartbeat.HEARTBEAT_TICKS * 10;
        Heartbeat.beat(player);
        helper.assertTrue(player.getMainHandItem().is(ModItems.GUTS_GU_1.get()), "the heartbeat keeps it");
        helper.assertFalse(dropGuts(helper, CENTER).isRemoved(), "a dropped one stays too");
        helper.succeed();
    }
    //endregion

    private static int collectDrops(ServerLevel level, BlockPos pos) {
        int drops = 0;
        for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.0D))) {
            if (drop.getItem().is(ModItems.GUTS_GU_1.get())) drops += drop.getItem().getCount();
            drop.discard();
        }
        return drops;
    }

    private static LootParams blockLoot(GameTestHelper helper, BlockState state) {
        return new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(CENTER_POS)))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .create(LootContextParamSets.BLOCK);
    }

    private static double shapeArea(ServerLevel level, BlockPos pos) {
        double[] area = {0.0D};
        level.getBlockState(pos).getShape(level, pos)
                .forAllBoxes((x0, y0, z0, x1, y1, z1) -> area[0] += (x1 - x0) * (z1 - z0));
        return area[0];
    }

    private static ItemEntity dropGuts(GameTestHelper helper, Vec3 at) {
        Vec3 absolute = helper.absoluteVec(at);
        ItemEntity entity = new ItemEntity(helper.getLevel(), absolute.x, absolute.y, absolute.z,
                new ItemStack(ModItems.GUTS_GU_1.get()));
        entity.tickCount = Heartbeat.HEARTBEAT_TICKS;
        entity.getItem().onEntityItemUpdate(entity);
        return entity;
    }

    private static void layStonePad(GameTestHelper helper) {
        for (int x = -TEST_RADIUS; x <= TEST_RADIUS; x++) {
            for (int z = -TEST_RADIUS; z <= TEST_RADIUS; z++) {
                helper.setBlock(CENTER_POS.offset(x, 0, z), ModBlocks.SOULQUAKE_STONE.get());
            }
        }
    }
}
