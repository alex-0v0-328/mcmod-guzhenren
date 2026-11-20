package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BigStrength;
import net.alex.guzhenren.gameplay.path.strength.BodyOverload;
import net.alex.guzhenren.gameplay.path.strength.ComboRound;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthSnapshot;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.mortal.path.strength.BigStrengthBehavior;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestGu.click;
import static net.alex.guzhenren.gametest.GameTestGu.hold;
import static net.alex.guzhenren.gametest.GameTestGu.hunger;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 大力蛊[Big Strength Gu]测试。
 * <ul>
 *   <li>五转裸身（承受力 56）：无力量者也可催动，总点数 +32，承受力不变；增幅不过准入，人力 200 斤与熊 40 之上照
 *       样催动，点数全额发挥。</li>
 *   <li>熊 40 + 大力 32 超出承受力 16 点，空手命中按过载反噬扣血。</li>
 *   <li>状态在身再催动被拒不扣费；状态结束点数同撤；持青牛时时长 30 → 40 秒。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BigStrengthGameTests {

    private static final Vec3 STAND = new Vec3(4.5, 1.0, 4.5);
    private static final int GU_HUNGER = 50;
    private static final int CAPACITY = 56;
    private static final int JIN = 200;
    private static final int BEAR_POINTS = 40;
    private static final double DELTA = 0.001D;

    private BigStrengthGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void aBodyWithNoStrengthGainsThePointsAndNoCapacity(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ItemStack gu = hold(player, ModItems.BIG_STRENGTH_GU_2.get(), GU_HUNGER);
        TendedGuItem item = (TendedGuItem) gu.getItem();
        assertEquals(CAPACITY, StrengthService.snapshot(player).capacity(), "the bare capacity is not 56");

        click(helper, player, gu);
        assertTrue(BigStrength.isActive(player), "the Gu laid no big strength");
        assertTrue(player.hasEffect(ModEffects.BIG_STRENGTH_GU), "the big strength shows no icon");
        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        assertEquals(BigStrength.POINTS, snapshot.totalPoints(), "the big strength is not 32 points");
        assertEquals(0, snapshot.permanentPoints(), "the big strength counted as permanent");
        assertEquals(CAPACITY, snapshot.capacity(), "the big strength lifted the capacity");
        assertEquals(BigStrength.POINTS, StrengthService.getSettledPoints(player), "the points are not all settled");
        assertEquals(0, snapshot.overloadPoints(snapshot.settledPoints()), "32 points under 56 overloaded");

        long essence = ApertureEssenceService.getSpendable(player);
        int hunger = hunger(gu);
        GuItem.Refusal active = assertNotNull(item.getBehavior(BigStrengthBehavior.class).checkEffect(item, player, gu),
                () -> "no refusal while the big strength runs");
        assertEquals(GuItem.Refusal.EFFECT_ACTIVE, active.key());
        click(helper, player, gu);
        assertEquals(essence, ApertureEssenceService.getSpendable(player), "the refused use paid essence");
        assertEquals(hunger, hunger(gu), "the refused use paid hunger");

        PathEffectService.removeType(player, ModPathEffectTypes.BIG_STRENGTH);
        assertFalse(BigStrength.isActive(player), "the big strength was not ended");
        StrengthSnapshot after = StrengthService.snapshot(player);
        assertEquals(0, after.totalPoints(), "the points outlived the big strength");
        assertEquals(CAPACITY, after.capacity(), "the capacity moved with the big strength");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aBodyPastItsCapacityStillTakesThePointsInFull(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JUN, 6), "the jun was refused");
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.TEN_JIN, 2), "the ten jin was refused");
        assertEquals(JIN, StrengthService.getSettledPoints(player), "200 jin were cut to the capacity");

        click(helper, player, hold(player, ModItems.BIG_STRENGTH_GU_2.get(), GU_HUNGER));
        assertTrue(BigStrength.isActive(player), "the amplifier was held to the admission");
        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        assertEquals(JIN + BigStrength.POINTS, snapshot.totalPoints(), "the points were not added to the jin");
        assertEquals(JIN, snapshot.permanentPoints(), "the big strength counted as permanent");
        assertEquals(CAPACITY, snapshot.capacity(), "the big strength lifted the capacity");
        assertEquals(JIN + BigStrength.POINTS, StrengthService.getSettledPoints(player), "the points were cut");
        assertEquals(JIN + BigStrength.POINTS - CAPACITY, snapshot.overloadPoints(snapshot.settledPoints()),
                "the points past the capacity do not all overload");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void onABeastBodyThePointsPastTheCapacityRecoil(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        assertTrue(PathStrengthService.grant(player, BeastStrength.BEAR), "the bear was refused");
        float max = player.getMaxHealth();
        player.setHealth(max);
        strike(helper, player, new BlockPos(2, 1, 2));
        assertEquals(max, player.getHealth(), DELTA);

        click(helper, player, hold(player, ModItems.BIG_STRENGTH_GU_2.get(), GU_HUNGER));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        assertEquals(BEAR_POINTS + BigStrength.POINTS, snapshot.totalPoints(), "the points were not added to the bear");
        assertEquals(CAPACITY, snapshot.capacity(), "the big strength lifted the capacity");
        assertEquals(BEAR_POINTS + BigStrength.POINTS, StrengthService.getSettledPoints(player),
                "72 points were cut to the 56 capacity");
        assertEquals(16, snapshot.overloadPoints(snapshot.settledPoints()), "72 over 56 is not 16 over");

        strike(helper, player, new BlockPos(6, 1, 2));
        assertEquals(max - 16.0F / CAPACITY * max * BodyOverload.RECOIL_RATIO, player.getHealth(), DELTA);

        PathEffectService.removeType(player, ModPathEffectTypes.BIG_STRENGTH);
        player.setHealth(max);
        strike(helper, player, new BlockPos(2, 1, 6));
        assertEquals(max, player.getHealth(), DELTA);
        ComboRound.clear(player);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theGreenBullAddsTenSecondsToTheBigStrength(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        assertTrue(PathStrengthService.grant(player, BeastStrength.GREEN_BULL), "the bull was refused");

        click(helper, player, hold(player, ModItems.BIG_STRENGTH_GU_2.get(), GU_HUNGER));
        PathEffect<BigStrength.State> effect = assertNotNull(
                PathEffectService.find(player, ModPathEffectTypes.BIG_STRENGTH), () -> "the Gu laid no big strength");
        assertEquals((long) (30 + 10) * Ticks.SECOND, effect.getTicksLeft(player.level().getGameTime()),
                "the green bull did not add its 10 seconds");
        helper.succeed();
    }

    private static void strike(GameTestHelper helper, ServerPlayer player, BlockPos at) {
        ComboRound.beginStrike(player, false);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, at);
        assertTrue(Punches.land(player, pig, 1.0F), "the punch did no damage");
    }

    private static ServerPlayer player(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        return GameTestGu.awakened(GameTestPlayers.survival(helper, "gzr-big-strength-gametest")
                .pastSpawnInvulnerability().at(STAND), Rank.FIVE);
    }
}
