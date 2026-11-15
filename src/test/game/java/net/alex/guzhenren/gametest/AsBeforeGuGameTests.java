package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.rewind.ChunkRewindService;
import net.alex.guzhenren.gameplay.rewind.PlayerRewindData;
import net.alex.guzhenren.gameplay.rewind.PlayerSnapshot;
import net.alex.guzhenren.gameplay.rewind.RewindClock;
import net.alex.guzhenren.item.gu.mortal.path.time.AsBeforeGuItem;
import net.alex.guzhenren.item.gu.stack.RefinedGuState;
import net.alex.guzhenren.item.gu.tending.GuHunger;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredItem;
import static net.alex.guzhenren.gametest.GameTestGu.charge;
import static net.alex.guzhenren.gametest.GameTestPlayers.STAND;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 三种如故蛊[As Before Gu]。
 * <ul>
 *   <li>人如故[Man As Before Gu]无记忆或记忆不足一天则拒绝、零消耗，满一天则复原身体、扣 2 点；点数每 1/3 天回 1 点至
 *       满。</li>
 *   <li>江如故[River As Before Gu]陆上拒绝；山如故[Mountain As Before Gu]在 {@code mobGriefing} 关闭时拒绝，否则把远处
 *       区块复原为生成原貌。</li>
 *   <li>点击为真实 {@code use} + {@code finishUsingItem}；测试体在一 tick 内跑完，故记忆用过去时间戳预置；GameTest 群系
 *       非海非河，区块算陆地。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AsBeforeGuGameTests {
    private static final long MAN_COST = 163_840L;
    private static final int PACE = Ticks.DAY / 3;

    private AsBeforeGuGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void manGuRefusesWithoutAMemoryAndSpendsNothing(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack gu = refined(player, ModItems.MAN_AS_BEFORE_GU_5);
        long essence = ApertureEssenceService.getSpendable(player);

        gu.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        assertFalse(player.isUsingItem(), "no memory refuses before the charge starts");
        assertEquals(essence, ApertureEssenceService.getSpendable(player), "a refusal spends no essence");
        assertEquals(bar(gu).max(), bar(gu).getPoints(gu), "a refusal spends no point");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void manGuRefusesAMemoryYoungerThanADay(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack gu = refined(player, ModItems.MAN_AS_BEFORE_GU_5);
        long now = helper.getLevel().getGameTime();
        PlayerData.get(player).setRewind(PlayerRewindData.DEFAULT.with(PlayerSnapshot.capture(player, now - 1_000L)));

        gu.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        assertFalse(player.isUsingItem(), "a memory younger than a day refuses");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void manGuRestoresTheBodyAndSpendsAPoint(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack gu = refined(player, ModItems.MAN_AS_BEFORE_GU_5);
        long now = helper.getLevel().getGameTime();
        player.setHealth(player.getMaxHealth());
        PlayerData.get(player).setRewind(PlayerRewindData.DEFAULT
                .with(PlayerSnapshot.capture(player, now - RewindClock.DAY_TICKS)));
        player.setHealth(4.0F);
        long essence = ApertureEssenceService.getSpendable(player);

        charge(helper, player, gu);

        assertEquals(player.getMaxHealth(), player.getHealth(), 0.001);
        assertEquals(bar(gu).max() - 2, bar(gu).getPoints(gu), "a use spends two points");
        assertEquals(essence - MAN_COST, ApertureEssenceService.getSpendable(player), "a use pays its essence");
        assertEquals(PACE, bar(gu).getTicksToNextPoint(gu, now), "a spend from a full bar restarts the wait");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void barRefillsOnePointPerThirdOfADay(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack gu = refined(player, ModItems.MAN_AS_BEFORE_GU_5);
        long now = helper.getLevel().getGameTime();
        GuHunger.RecoverBar bar = bar(gu);
        gu.set(ModDataComponents.REFINED_GU_STATE,
                TendedGuItem.getState(gu).withHunger(0));
        gu.set(ModDataComponents.RECOVERED_AT, now - PACE - 5L);

        assertEquals(1, bar.getPoints(gu, now), "a third of a day refills one point");
        assertEquals(PACE - 5, bar.getTicksToNextPoint(gu, now), "the second point keeps its progress");
        bar.settle(gu, now);
        assertEquals(1, bar.getPoints(gu), "settling stores the point");
        assertEquals(2, bar.getPoints(gu, now + PACE), "another third of a day gives the second point");
        assertEquals(PACE - 5, bar.getTicksToNextPoint(gu, now + PACE),
                "the third point keeps its progress");
        assertEquals(bar.max(), bar.getPoints(gu, now + 9L * PACE), "three days fill the bar");
        assertEquals(0, bar.getTicksToNextPoint(gu, now + 9L * PACE), "a full bar waits for nothing");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void healthMendsOnePointPerThirdOfADay(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack gu = refined(player, ModItems.MOUNTAIN_AS_BEFORE_GU_5);
        long now = helper.getLevel().getGameTime();
        GuHunger.RecoverBar bar = bar(gu);
        TendedGuItem item = (TendedGuItem) gu.getItem();
        assertEquals(24, item.getMaxHealth(), "as before health");
        gu.set(ModDataComponents.REFINED_GU_STATE,
                TendedGuItem.getState(gu).withDamageTaken(item.getMaxHealth()));

        bar.settleHealth(gu, now);
        assertEquals(0, item.getHealth(gu), "the first hurt settle only starts the clock");
        bar.settleHealth(gu, now + PACE - 1L);
        assertEquals(0, item.getHealth(gu), "no point before a third of a day");
        bar.settleHealth(gu, now + PACE + 5L);
        assertEquals(1, item.getHealth(gu), "a third of a day mends one point");
        bar.settleHealth(gu, now + 72L * PACE);
        assertEquals(24, item.getHealth(gu), "eight days mend it whole");
        assertEquals(null, gu.get(ModDataComponents.HEALED_AT), "a whole Gu keeps no stamp");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void riverGuRefusesOnLand(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack gu = refined(player, ModItems.RIVER_AS_BEFORE_GU_5);

        gu.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        assertFalse(player.isUsingItem(), "a land chunk is no river");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void mountainGuRefusesWithMobGriefingOff(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack gu = refined(player, ModItems.MOUNTAIN_AS_BEFORE_GU_5);
        GameRules.BooleanValue griefing = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean was = griefing.get();
        try {
            griefing.set(false, helper.getLevel().getServer());
            gu.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

            assertFalse(player.isUsingItem(), "mobGriefing off refuses");
            assertEquals(bar(gu).max(), bar(gu).getPoints(gu), "a refusal spends nothing");
        } finally {
            griefing.set(was, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 400)
    public static void mountainGuPutsTheLandBackAsGenerated(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack gu = refined(player, ModItems.MOUNTAIN_AS_BEFORE_GU_5);
        LevelChunk chunk = ChunkRewindGameTests.farChunk(helper, 3);
        BlockPos stand = ChunkRewindGameTests.surface(chunk, 8, 8);
        BlockPos placed = ChunkRewindGameTests.surface(chunk, 10, 8);
        player.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
        helper.getLevel().setBlock(placed, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);

        charge(helper, player, gu);

        assertEquals(bar(gu).max() - 2, bar(gu).getPoints(gu), "a use spends two points");
        helper.succeedWhen(() -> {
            helper.assertTrue(ChunkRewindService.getGenerating() == 0 && ChunkRewindService.getQueued() == 0,
                    "the rewind is still running");
            helper.assertTrue(helper.getLevel().getBlockState(placed).isAir(), "the placed stone is gone");
            ChunkRewindGameTests.release(helper, chunk.getPos());
        });
    }

    private static ServerPlayer holder(GameTestHelper helper) {
        return GameTestGu.awakened(GameTestPlayers.survival(helper, "gzr-as-before-mock").at(STAND), Rank.FIVE);
    }

    private static ItemStack refined(ServerPlayer player, DeferredItem<Item> item) {
        ItemStack stack = new ItemStack(item.get());
        AsBeforeGuItem gu = (AsBeforeGuItem) stack.getItem();
        stack.set(ModDataComponents.REFINED_GU_STATE,
                new RefinedGuState(true, gu.getRefineCost(), 0, bar(stack).max(), 0));
        stack.set(ModDataComponents.RECOVERED_AT, player.level().getGameTime());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack;
    }

    private static GuHunger.RecoverBar bar(ItemStack stack) {
        return ((AsBeforeGuItem) stack.getItem()).getBar();
    }
}
