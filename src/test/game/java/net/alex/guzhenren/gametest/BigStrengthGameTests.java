package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BigStrength;
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
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
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
 *   <li>五转裸身（常态承受 56）：无力量者也可催动，总点数 +32、承受 +16；人力 200 斤发挥 56 → 72；熊 40 发挥 40 → 72，
 *       不过载。</li>
 *   <li>状态在身再催动被拒不扣费；状态结束点数与承受同撤。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BigStrengthGameTests {

    private static final Vec3 STAND = new Vec3(4.5, 1.0, 4.5);
    private static final int GU_HUNGER = 50;
    private static final int NORMAL_CAPACITY = 56;
    private static final int JIN = 200;
    private static final int BEAR_POINTS = 40;

    private BigStrengthGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void aBodyWithNoStrengthGainsThePointsAndHalfAsCapacity(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ItemStack gu = hold(player, ModItems.BIG_STRENGTH_GU_2.get(), GU_HUNGER);
        TendedGuItem item = (TendedGuItem) gu.getItem();
        assertEquals(NORMAL_CAPACITY, StrengthService.snapshot(player).capacity(), "the bare capacity is not 56");

        click(helper, player, gu);
        assertTrue(BigStrength.isActive(player), "the Gu laid no big strength");
        assertTrue(player.hasEffect(ModEffects.BIG_STRENGTH_GU), "the big strength shows no icon");
        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        assertEquals(BigStrength.POINTS, snapshot.totalPoints(), "the big strength is not 32 points");
        assertEquals(NORMAL_CAPACITY + 16, snapshot.capacity(), "the capacity did not gain half the points");
        assertEquals(BigStrength.POINTS, StrengthService.getUsablePoints(player), "the points are not all usable");

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
        assertEquals(NORMAL_CAPACITY, after.capacity(), "the capacity outlived the big strength");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aBodyAtItsCapacityBringsHalfThePointsToBear(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JUN, 6), "the jun was refused");
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.TEN_JIN, 2), "the ten jin was refused");
        assertEquals(NORMAL_CAPACITY, StrengthService.getUsablePoints(player), "200 jin do not fill the capacity");

        click(helper, player, hold(player, ModItems.BIG_STRENGTH_GU_2.get(), GU_HUNGER));
        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        assertEquals(JIN + BigStrength.POINTS, snapshot.totalPoints(), "the points were not added to the jin");
        assertEquals(NORMAL_CAPACITY + 16, StrengthService.getUsablePoints(player), "half the points are not usable");
        assertEquals(0, snapshot.overloadPoints(), "the human path paid a body overload");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void onABeastBodyThePointsNeverOverload(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        assertTrue(PathStrengthService.grant(player, BeastStrength.BEAR), "the bear was refused");

        click(helper, player, hold(player, ModItems.BIG_STRENGTH_GU_2.get(), GU_HUNGER));
        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        assertEquals(BEAR_POINTS + BigStrength.POINTS, snapshot.totalPoints(), "the points were not added to the bear");
        assertEquals(NORMAL_CAPACITY + 16, snapshot.capacity(), "the capacity did not gain half the points");
        assertEquals(BEAR_POINTS + BigStrength.POINTS, StrengthService.getUsablePoints(player),
                "72 points do not fit a 72 capacity");
        assertEquals(0, snapshot.overloadPoints(), "the big strength overloaded the beast body");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        return GameTestGu.awakened(GameTestPlayers.survival(helper, "gzr-big-strength-gametest")
                .pastSpawnInvulnerability().at(STAND), Rank.FIVE);
    }
}
