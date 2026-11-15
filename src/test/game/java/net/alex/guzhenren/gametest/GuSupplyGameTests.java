package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorage;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.item.gu.PrimevalStoneSupply;
import net.alex.guzhenren.item.gu.mortal.path.space.PrimevalElderGuItem;
import net.alex.guzhenren.item.gu.tending.GuUpkeep;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.alex.guzhenren.registry.item.ModItems;
import net.alex.guzhenren.vault.ApertureStorageTick;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestGu.charge;
import static net.alex.guzhenren.gametest.GameTestGu.elder;
import static net.alex.guzhenren.gametest.GameTestGu.hold;
import static net.alex.guzhenren.gametest.GameTestGu.hunger;
import static net.alex.guzhenren.gametest.GameTestGu.refined;
import static net.alex.guzhenren.gametest.GameTestPlayers.STAND;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 活玩家持续照顾型蛊[Tended Gu]的两种自动补给。
 * <ul>
 *   <li>补食：饱食[Hunger]有空位时催动前从背包吃，每次扣量（催动、灌注[Channel]一步、日钟；随身或在空窍[Aperture]里）后
 *       也吃；总吃满、先回血，满条不为回血吃；日钟扣前也吃，故其后背包得食的饿蛊能活过日切。</li>
 *   <li>元石[Primeval Stone]：付费时真元[Essence]池低于一半补到 80%，余缺也烧元石；门槛计随身元石；散装先于元老蛊
 *       [Primeval Elder Gu]存量；元老蛊任意格可付，存储中的最后付；炼化逐步烧；无空窍时无用；创造模式不耗。</li>
 *   <li>花豕蛊[Flower Boar Gu]为普通催动（一转，40 真元，条 14，每次 3，猪排每块 1）；白豕蛊[White Boar Gu]为灌注（每轮
 *       1,800 真元、18 饱食，条 20），经 {@code onUseTick} 逐步驱动，按蛊节奏一轮 8 步、每步 225、隔 2 tick。</li>
 *   <li>一转基数 80 的空窍容 80 真元，低于 40 起补、补到 64；二转容 800（白豕蛊炼化费）；元石每块 100，溢出作废。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GuSupplyGameTests {
    private static final int BAG_SLOT = 9;

    private GuSupplyGameTests() {}

    //region feeding [喂食]
    @GameTest(template = "empty9x9x9")
    public static void aUseOnAnEmptyBarEatsFirstAndEatsBackToFullAfter(GameTestHelper helper) {
        ServerPlayer player = holder(helper, Rank.ONE);
        ItemStack boar = hold(player, ModItems.FLOWER_BOAR_GU_1.get(), 0);
        ItemStack pork = bag(player, BAG_SLOT, Items.PORKCHOP, 20);

        charge(helper, player, boar);

        assertTrue(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "the use did not land");
        assertTrue(player.getMainHandItem() == boar, "a Gu with food in the bag died of exhaustion");
        assertEquals(14, hunger(boar), "not fed back to full after the use");
        assertEquals(3, pork.getCount(), "14 before the use and 3 after");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aFullBarNeverEatsJustToMendButMendsFirstAfterADrop(GameTestHelper helper) {
        ServerPlayer player = holder(helper, Rank.ONE);
        ItemStack boar = hold(player, ModItems.FLOWER_BOAR_GU_1.get(), 14);
        hurt(boar, 2);
        ItemStack pork = bag(player, BAG_SLOT, Items.PORKCHOP, 10);

        GuUpkeep.tickCarried(player, 0L);

        assertEquals(2, TendedGuItem.getState(boar).damageTaken(), "a full bar ate to mend");
        assertEquals(10, pork.getCount());

        charge(helper, player, boar);

        assertEquals(0, TendedGuItem.getState(boar).damageTaken(), "the meal after the use did not mend");
        assertEquals(14, hunger(boar));
        assertEquals(5, pork.getCount(), "2 to mend, then 3 to fill");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theDayClockEatsACarriedGuBackToFull(GameTestHelper helper) {
        ServerPlayer player = holder(helper, Rank.ONE);
        ItemStack boar = hold(player, ModItems.FLOWER_BOAR_GU_1.get(), 14);
        ItemStack pork = bag(player, BAG_SLOT, Items.PORKCHOP, 5);

        GuUpkeep.tickCarried(player, 1L);

        assertEquals(14, hunger(boar), "a Gu above the hungry line was left a day's drain down");
        assertEquals(4, pork.getCount());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theDayClockEatsAGuInTheApertureFromTheHoldersBag(GameTestHelper helper) {
        ServerPlayer player = holder(helper, Rank.ONE);
        ItemStack stored = refined(ModItems.FLOWER_BOAR_GU_1.get(), 14);
        ItemStack pork = bag(player, BAG_SLOT, Items.PORKCHOP, 5);

        assertFalse(GuUpkeep.tickInContainer(player, stored, 1L), "a fed Gu starved");

        assertEquals(14, hunger(stored));
        assertEquals(4, pork.getCount());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHungryCarriedGuGivenFoodLaterEatsBeforeTheDayDrain(GameTestHelper helper) {
        ServerPlayer player = holder(helper, Rank.ONE);
        ItemStack boar = hold(player, ModItems.FLOWER_BOAR_GU_1.get(), 1);
        ItemStack pork = bag(player, BAG_SLOT, Items.PORKCHOP, 20);

        GuUpkeep.tickCarried(player, 0L);
        assertEquals(1, hunger(boar), "a heartbeat inside the day fed it");

        GuUpkeep.tickCarried(player, 1L);

        assertTrue(player.getMainHandItem() == boar, "a hungry Gu with food in the bag starved at the rollover");
        assertEquals(14, hunger(boar));
        assertEquals(6, pork.getCount(), "13 before the drain and 1 after");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHungryGuInTheApertureGivenFoodLaterEatsBeforeTheDayDrain(GameTestHelper helper) {
        ServerPlayer player = holder(helper, Rank.ONE);
        PlayerData.get(player).setApertureStorage(ApertureStorage.DEFAULT.with(ApertureData.PRIMARY,
                List.of(refined(ModItems.FLOWER_BOAR_GU_1.get(), 1))));
        ItemStack pork = bag(player, BAG_SLOT, Items.PORKCHOP, 20);

        ApertureStorageTick.tickStored(player, 1L);

        List<ItemStack> after = ApertureStorageService.getItems(player, ApertureData.PRIMARY);
        assertTrue(!after.isEmpty() && after.getFirst().is(ModItems.FLOWER_BOAR_GU_1.get()),
                "a hungry stored Gu with food in the bag starved at the rollover");
        assertEquals(14, hunger(after.getFirst()));
        assertEquals(6, pork.getCount(), "13 before the drain and 1 after");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aChannelStepEatsBackToFull(GameTestHelper helper) {
        ServerPlayer player = holder(helper, Rank.FIVE);
        ItemStack boar = hold(player, ModItems.WHITE_BOAR_GU_1.get(), 20);
        ItemStack pork = bag(player, BAG_SLOT, Items.PORKCHOP, 10);

        boar.getItem().onUseTick(helper.getLevel(), player, boar, boar.getUseDuration(player));

        assertEquals(225, TendedGuItem.getState(boar).investedEssence(), "a Gu-paced step pours an eighth of a round");
        assertEquals(20, hunger(boar), "not fed back to full after the step");
        assertEquals(8, pork.getCount(), "an eighth of a round bills the 2 points it has passed");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aGuPacedRoundTakesSixteenTicksAndEighteenPoints(GameTestHelper helper) {
        ServerPlayer player = holder(helper, Rank.FIVE);
        ItemStack boar = hold(player, ModItems.WHITE_BOAR_GU_1.get(), 20);
        ItemStack pork = bag(player, BAG_SLOT, Items.PORKCHOP, 20);
        int start = boar.getUseDuration(player);

        for (int elapsed = 0; elapsed < 14; elapsed++) {
            boar.getItem().onUseTick(helper.getLevel(), player, boar, start - elapsed);
        }
        assertFalse(PathStrengthService.has(player, BeastStrength.WHITE_BOAR), "the round landed before its last step");
        assertEquals(1_575, TendedGuItem.getState(boar).investedEssence(), "seven steps pour seven eighths");
        assertEquals(5, pork.getCount(), "seven eighths of a round bill 15 points");

        boar.getItem().onUseTick(helper.getLevel(), player, boar, start - 14);

        assertTrue(PathStrengthService.has(player, BeastStrength.WHITE_BOAR), "the eighth step did not land the round");
        assertEquals(0, TendedGuItem.getState(boar).investedEssence(), "the round did not reset");
        assertEquals(20, hunger(boar), "not fed back to full after the round");
        assertEquals(2, pork.getCount(), "a round billed other than 18 points");
        helper.succeed();
    }
    //endregion

    //region stones [元石]
    @GameTest(template = "empty9x9x9")
    public static void stonesCoverAShortPoolAndTopItUpToEightyPercent(GameTestHelper helper) {
        ServerPlayer player = holder(helper, Rank.ONE);
        ItemStack boar = hold(player, ModItems.FLOWER_BOAR_GU_1.get(), 12);
        ItemStack stones = bag(player, BAG_SLOT, ModItems.PRIMEVAL_STONE.get(), 10);
        drainTo(player, 10L);

        charge(helper, player, boar);

        assertTrue(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "a pool of 10 with stones on hand was refused");
        assertEquals(9, stones.getCount(), "the top-up took 1 stone: 10 up to 64 needs 54");
        assertEquals(80L - 40L, ApertureEssenceService.getSpendable(player), "the stone filled the pool of 80");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aCostPastTheTopUpBurnsStonesForTheRest(GameTestHelper helper) {
        ServerPlayer player = holder(helper, Rank.ONE);
        ItemStack stones = bag(player, BAG_SLOT, ModItems.PRIMEVAL_STONE.get(), 10);
        drainTo(player, 10L);

        assertTrue(PrimevalStoneSupply.pay(player, 100L), "a cost above the cap was refused");

        assertEquals(8, stones.getCount(), "1 to top up, 1 more for the 20 the pool could not cover");
        assertEquals(80L, ApertureEssenceService.getSpendable(player), "the change of the last stone");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void creativeDrawsNoStone(GameTestHelper helper) {
        ServerPlayer player = holder(helper, Rank.ONE);
        player.getAbilities().instabuild = true;
        ItemStack boar = hold(player, ModItems.FLOWER_BOAR_GU_1.get(), 12);
        ItemStack stones = bag(player, BAG_SLOT, ModItems.PRIMEVAL_STONE.get(), 10);
        drainTo(player, 10L);

        charge(helper, player, boar);

        assertTrue(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "creative with a short pool was refused");
        assertEquals(10, stones.getCount(), "creative drew a stone");
        assertEquals(0L, ApertureEssenceService.getSpendable(player), "the pool paid what it had; the rest went free");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void looseStonesGoBeforeAnElderGu(GameTestHelper helper) {
        ServerPlayer player = holder(helper, Rank.ONE);
        ItemStack boar = hold(player, ModItems.FLOWER_BOAR_GU_1.get(), 12);
        ItemStack elder = elder(10);
        player.setItemInHand(InteractionHand.OFF_HAND, elder);
        ItemStack stones = bag(player, BAG_SLOT, ModItems.PRIMEVAL_STONE.get(), 10);
        drainTo(player, 0L);

        charge(helper, player, boar);

        assertEquals(9, stones.getCount(), "the top-up of 64 did not start with the loose stones");
        assertEquals(10L, PrimevalElderGuItem.getStored(elder), "the Elder Gu paid before the loose stones ran out");
        assertEquals(40L, ApertureEssenceService.getSpendable(player));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void anElderGuPaysFromAnySlot(GameTestHelper helper) {
        ServerPlayer player = holder(helper, Rank.ONE);
        ItemStack boar = hold(player, ModItems.FLOWER_BOAR_GU_1.get(), 12);
        ItemStack elder = elder(10);
        player.getInventory().setItem(BAG_SLOT + 3, elder);
        drainTo(player, 0L);

        charge(helper, player, boar);

        assertTrue(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "an Elder Gu in the bag did not count");
        assertEquals(9L, PrimevalElderGuItem.getStored(elder));
        assertEquals(40L, ApertureEssenceService.getSpendable(player));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void anElderGuInTheApertureIsTheLastSource(GameTestHelper helper) {
        ServerPlayer player = holder(helper, Rank.TWO);
        ItemStack boar = hold(player, ModItems.FLOWER_BOAR_GU_1.get(), 12);
        ItemStack carried = elder(2);
        player.getInventory().setItem(BAG_SLOT + 3, carried);
        ApertureStorageService.set(player, ApertureData.PRIMARY, List.of(elder(10)));
        drainTo(player, 0L);

        charge(helper, player, boar);

        assertTrue(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "an Elder Gu in the aperture did not count");
        assertEquals(0L, PrimevalElderGuItem.getStored(carried), "the carried Elder Gu was not emptied first");
        ItemStack stored = ApertureStorageService.getView(player, ApertureData.PRIMARY).getFirst();
        assertEquals(5L, PrimevalElderGuItem.getStored(stored), "the stored Elder Gu did not pay the other 5");
        assertEquals(700L - 40L, ApertureEssenceService.getSpendable(player));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void refiningBurnsStonesFromTheBagStepByStep(GameTestHelper helper) {
        ServerPlayer player = holder(helper, Rank.TWO);
        ItemStack boar = new ItemStack(ModItems.WHITE_BOAR_GU_1.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, boar);
        ItemStack stones = bag(player, BAG_SLOT, ModItems.PRIMEVAL_STONE.get(), 64);
        drainTo(player, 0L);

        boar.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertTrue(player.isUsingItem(), "a wild Gu with stones on hand was refused");
        int start = player.getUseItemRemainingTicks();
        for (int remaining = start; remaining > start - 100 && player.isUsingItem(); remaining--) {
            boar.onUseTick(helper.getLevel(), player, remaining);
        }

        assertTrue(((TendedGuItem) boar.getItem()).isRefined(boar), "the held click did not refine it");
        assertFalse(player.isUsingItem(), "the refining charge ends once refined");
        assertEquals(50, stones.getCount(), "an empty rank II pool of 800 tops up to 640, 7 stones a step, twice");
        assertEquals(600L, ApertureEssenceService.getSpendable(player), "the second top-up paid the 100 left");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void stonesWithoutAnApertureBuyNothing(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-supply-mortal-mock").at(STAND);
        ItemStack boar = hold(player, ModItems.FLOWER_BOAR_GU_1.get(), 12);
        ItemStack stones = bag(player, BAG_SLOT, ModItems.PRIMEVAL_STONE.get(), 10);

        boar.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        assertFalse(player.isUsingItem(), "a mortal without an aperture paid with stones");
        assertFalse(player.hasEffect(ModEffects.FLOWER_BOAR_GU));
        assertEquals(10, stones.getCount());
        helper.succeed();
    }
    //endregion

    private static ServerPlayer holder(GameTestHelper helper, Rank rank) {
        return GameTestGu.awakened(GameTestPlayers.survival(helper, "gzr-supply-mock").at(STAND), rank);
    }

    private static void drainTo(ServerPlayer player, long left) {
        ApertureEssenceService.consume(player, ApertureEssenceService.getSpendable(player) - left);
    }

    private static ItemStack bag(ServerPlayer player, int slot, Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        player.getInventory().setItem(slot, stack);
        return stack;
    }

    private static void hurt(ItemStack stack, int damage) {
        stack.set(ModDataComponents.REFINED_GU_STATE, TendedGuItem.getState(stack).withDamageTaken(damage));
    }
}
