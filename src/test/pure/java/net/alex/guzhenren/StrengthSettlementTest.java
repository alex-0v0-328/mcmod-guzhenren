package net.alex.guzhenren;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.BodyOverload;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.SprintSpeedService;
import net.alex.guzhenren.gameplay.path.strength.StrengthAmplifier;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthSnapshot;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrengthSettlementTest {

    private static final ResourceLocation SOURCE = ResourceLocation.fromNamespaceAndPath("guzhenren", "test");
    private static final StrengthAmplifier BOAR = StrengthAmplifier.beast(SOURCE, BeastStrengthFamily.BOAR, 24);
    private static final StrengthAmplifier OX = StrengthAmplifier.beast(SOURCE, BeastStrengthFamily.OX, 40);
    private static final float DELTA = 0.001F;

    @Test
    @DisplayName("the base capacity is 16 for a mortal and 24 to 56 for ranks one to five")
    void baseCapacityByRank() {
        assertEquals(16, StrengthService.getBaseCapacity(Rank.NONE, ExtremePhysique.NONE));
        assertEquals(24, StrengthService.getBaseCapacity(Rank.ONE, ExtremePhysique.NONE));
        assertEquals(32, StrengthService.getBaseCapacity(Rank.TWO, ExtremePhysique.NONE));
        assertEquals(40, StrengthService.getBaseCapacity(Rank.THREE, ExtremePhysique.NONE));
        assertEquals(48, StrengthService.getBaseCapacity(Rank.FOUR, ExtremePhysique.NONE));
        assertEquals(56, StrengthService.getBaseCapacity(Rank.FIVE, ExtremePhysique.NONE));
    }

    @Test
    @DisplayName("ranks past five are cut to the fifth rank's 56 -- the mod stops at rank five")
    void ranksPastFiveAreCutToFifty() {
        for (Rank rank : new Rank[] { Rank.SIX, Rank.SEVEN, Rank.EIGHT, Rank.NINE }) {
            assertEquals(56, StrengthService.getBaseCapacity(rank, ExtremePhysique.NONE), rank.name());
            assertEquals(224, StrengthService.getBaseCapacity(rank, ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL),
                    rank.name());
        }
    }

    @Test
    @DisplayName("a ten-extreme body doubles the base, the Great Strength True Martial body quadruples it")
    void physiqueMultipliesTheBase() {
        assertEquals(32, StrengthService.getBaseCapacity(Rank.NONE, ExtremePhysique.VERDANT_GREAT_SUN));
        assertEquals(80, StrengthService.getBaseCapacity(Rank.THREE, ExtremePhysique.CAREFREE_WISDOM_HEART));
        assertEquals(64, StrengthService.getBaseCapacity(Rank.NONE, ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL));
        assertEquals(224, StrengthService.getBaseCapacity(Rank.FIVE, ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL));
    }

    @Test
    @DisplayName("tempering adds its own points, outside the physique multiplier")
    void temperingAddsFlatPoints() {
        assertEquals(4, Skin.BRONZE_SKIN_1.getCapacityBonus());
        assertEquals(32, Skin.BRONZE_SKIN_4.getCapacityBonus());
        assertEquals(4, Skin.IRON_SKIN_1.getCapacityBonus());
        assertEquals(108, Skin.IRON_SKIN_4.getCapacityBonus());
        assertEquals(0, Skin.JADE_SKIN.getCapacityBonus(), "a timed skin adds no capacity");
        assertEquals(0, Skin.WHITE_JADE_SKIN.getCapacityBonus(), "a timed skin adds no capacity");
        assertEquals(128, Tendon.STEEL_TENDON_3.getCapacityBonus());
        assertEquals(256, Tendon.STEEL_TENDON_4.getCapacityBonus());
        assertEquals(128, Bone.IRON_BONE_3.getCapacityBonus());
        assertEquals(256, Bone.IRON_BONE_4.getCapacityBonus());
        assertEquals(0, Skin.NATIVE.getCapacityBonus() + Tendon.NATIVE.getCapacityBonus() + Bone.NATIVE.getCapacityBonus());
    }

    @Test
    @DisplayName("every point is brought to bear: the capacity never cuts the settled points")
    void pointsAreNeverCutByTheCapacity() {
        int none = StrengthService.NO_ALL_OUT;
        assertEquals(0, StrengthService.getSettledPoints(0, 0, 1.0D));
        assertEquals(24, StrengthService.getSettledPoints(24, 0, 1.0D));
        assertEquals(9999, StrengthService.getSettledPoints(9999, 0, 1.0D));

        StrengthSnapshot mortal = StrengthSnapshot.of(16, BeastStrength.BEAR.getPoints(), List.of(), none);
        assertEquals(16, mortal.capacity());
        assertEquals(40, mortal.totalPoints());
        assertEquals(40, mortal.settledPoints(), "a mortal's bear was cut to the capacity");
        assertEquals(24, mortal.overloadPoints(mortal.settledPoints()), "the points past the capacity went missing");
    }

    @Test
    @DisplayName("settled = floor(sum(points x source factor) x product(punch factors)): 152 with the tiger out is 842")
    void settlementMultipliesSourcesThenThePunchAndRoundsDown() {
        int total = BeastStrength.BEAR.getPoints() + BeastStrength.TIGER.getPoints()
                + BeastStrength.WHITE_BOAR.getPoints() + BeastStrength.BLACK_BOAR.getPoints();
        assertEquals(152, total);
        assertEquals(842, StrengthService.getSettledPoints(total, BeastStrength.TIGER.getPoints(), 1.3D * 3.0D),
                "(40 + 64 x 2 + 48) x 1.3 x 3 = 842.4");

        StrengthSnapshot hardship = StrengthSnapshot.of(56, total, List.of(StrengthAmplifier.punch(SOURCE, 30)),
                StrengthService.NO_ALL_OUT);
        assertEquals(1.3D, hardship.punchFactor(), DELTA);
        assertEquals(842, hardship.settle(BeastStrength.TIGER.getPoints(), 3.0D));
        assertEquals(197, hardship.settledPoints(), "152 x 1.3 = 197.6 at rest");
        assertEquals(280, hardship.settle(BeastStrength.TIGER.getPoints(), 1.0D), "216 x 1.3 = 280.8");
        assertEquals(296, hardship.settle(0, 1.5D), "152 x 1.3 x 1.5 = 296.4");
    }

    @Test
    @DisplayName("a fully released family counts twice, every other source once")
    void onlyTheReleasedFamilyIsDoubled() {
        assertEquals(2, StrengthService.FULL_RELEASE_FACTOR);
        assertEquals(80, StrengthService.getSettledPoints(40, 40, 1.0D), "a released bear is not 80");
        assertEquals(104, StrengthService.getSettledPoints(64, 40, 1.0D), "the timed boar beside the bear was doubled");
        assertEquals(168, StrengthService.getSettledPoints(104, 64, 1.0D), "bear 40 + tiger 64 x 2");
        assertEquals(208, StrengthService.getSettledPoints(104, 104, 1.0D), "both families out");
        assertEquals(200, StrengthService.getSettledPoints(200, 0, 1.0D), "human jin has no release");
        assertEquals(Integer.MAX_VALUE,
                StrengthService.getSettledPoints(Integer.MAX_VALUE, Integer.MAX_VALUE, 3.0D), "overflow");
    }

    @Test
    @DisplayName("whole-punch factors multiply each other and exact products are not rounded under")
    void punchFactorsMultiplyAndRoundDown() {
        assertEquals(44, StrengthService.getSettledPoints(40, 0, 1.1D));
        assertEquals(45, StrengthService.getSettledPoints(41, 0, 1.1D), "45.1 rounds down");
        assertEquals(60, StrengthService.getSettledPoints(40, 0, 1.5D));
        assertEquals(120, StrengthService.getSettledPoints(40, 0, 3.0D));
        assertEquals(90, StrengthService.getSettledPoints(50, 0, 1.2D * 1.5D));
        assertEquals(180, StrengthService.getSettledPoints(40, 0, 1.5D * 3.0D), "Hardship five on a full charge");
        assertEquals(360, StrengthService.getSettledPoints(40, 40, 1.5D * 3.0D), "the release joined the factors");

        assertEquals(1.0D, StrengthService.getPunchFactor(List.of()), DELTA);
        assertEquals(1.0D, StrengthService.getPunchFactor(List.of(BOAR, OX)), DELTA, "a source moved the factor");
        assertEquals(1.3D, StrengthService.getPunchFactor(List.of(BOAR, StrengthAmplifier.punch(SOURCE, 30))), DELTA);
        assertEquals(1.5D * 1.2D, StrengthService.getPunchFactor(
                List.of(StrengthAmplifier.punch(SOURCE, 50), StrengthAmplifier.punch(SOURCE, 20))), DELTA);
    }

    @Test
    @DisplayName("the current capacity adds the amplifier bonus and the All-Out lift on the normal one, never multiplied")
    void capacityAddsEveryBonusOnTheNormalOne() {
        int none = StrengthService.NO_ALL_OUT;
        assertEquals(16, StrengthService.getCapacity(16, 0, none));
        assertEquals(40, StrengthService.getCapacity(16, 24, none), "the amplifier bonus was not added");
        assertEquals(72, StrengthService.getCapacity(16, 24, 4), "the lift multiplied the amplifier bonus");
    }

    @Test
    @DisplayName("All-Out Effort adds 50/100/200% of the normal capacity for the rank three/four/five Gu")
    void allOutLiftsByTheGuTier() {
        assertEquals(2, StrengthService.ALL_OUT_LOWEST_TIER, "the rank three Gu's tier");
        assertEquals(84, StrengthService.getCapacity(56, 0, 2));
        assertEquals(112, StrengthService.getCapacity(56, 0, 3));
        assertEquals(168, StrengthService.getCapacity(56, 0, 4));
        assertEquals(112, StrengthService.getAllOutCapacityBonus(56, 9), "a tier past five is not five's 200%");
        assertEquals(0, StrengthService.getAllOutCapacityBonus(56, 1), "a tier under three lifted the capacity");
        assertEquals(0, StrengthService.getAllOutCapacityBonus(56, StrengthService.NO_ALL_OUT));
        assertEquals(7, StrengthService.getAllOutCapacityBonus(15, 2), "7.5 rounds down");
        assertEquals(0, StrengthService.getAllOutCapacityBonus(-8, 4), "a negative capacity lifted nothing");
    }

    @Test
    @DisplayName("a new permanent beast must fit beside those held under the normal capacity")
    void admissionReadsTheNormalCapacity() {
        assertFalse(StrengthService.canAdmit(16, 0, 24), "a mortal admitted a boar");
        assertTrue(StrengthService.canAdmit(24, 0, 24), "rank one refused its boar");
        assertFalse(StrengthService.canAdmit(24, 24, 24), "rank one admitted a second boar");
        assertTrue(StrengthService.canAdmit(48, 24, 24), "rank four refused the second boar");
        assertFalse(StrengthService.canAdmit(56, 0, 64), "rank five admitted a tiger bare");
        assertTrue(StrengthService.canAdmit(32 + 36, 0, 64), "rank two with iron skin three refused the tiger");
    }

    @Test
    @DisplayName("a human layer passes the same admission, and a hundred jun fits no mortal body")
    void humanLayersShareTheAdmission() {
        assertTrue(StrengthService.canAdmit(24, 0, HumanStrength.JIN.getJin()), "rank one refused its first jin");
        assertTrue(StrengthService.canAdmit(24, 23, HumanStrength.JIN.getJin()), "the last point was refused");
        assertFalse(StrengthService.canAdmit(24, 24, HumanStrength.JIN.getJin()), "a full rank one admitted a jin");
        assertFalse(StrengthService.canAdmit(24, 20, HumanStrength.TEN_JIN.getJin()), "30 passed rank one's 24");
        assertFalse(StrengthService.canAdmit(56, 0, HumanStrength.TEN_JUN.getJin()), "rank five bare took ten jun");

        int largest = StrengthService.getBaseCapacity(Rank.FIVE, ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL)
                + Skin.IRON_SKIN_4.getCapacityBonus() + Tendon.STEEL_TENDON_4.getCapacityBonus()
                + Bone.IRON_BONE_4.getCapacityBonus();
        assertEquals(844, largest);
        assertEquals(3000, HumanStrength.HUNDRED_JUN.getJin());
        assertTrue(StrengthService.canAdmit(largest, 0, HumanStrength.TEN_JUN.getJin()));
        assertFalse(StrengthService.canAdmit(largest, 0, HumanStrength.HUNDRED_JUN.getJin()),
                "a mortal body admitted a hundred jun");
    }

    @Test
    @DisplayName("one jin is one point and a boar 24, a bear 40, at 0.125 attack each")
    void pointsShareOneUnit() {
        PathStrengthData data = new PathStrengthData(Set.of(BeastStrength.WHITE_BOAR, BeastStrength.BEAR),
                Map.of(HumanStrength.JIN, 5, HumanStrength.TEN_JIN, 2));
        assertEquals(25, data.getTotalJin());
        assertEquals(64, data.getBeastPoints());
        assertEquals(89, data.getStrengthPoints());
        assertEquals(3.0D, BeastStrength.WHITE_BOAR.getPoints() * PathStrengthData.ATTACK_PER_POINT);
        assertEquals(5.0D, BeastStrength.BEAR.getPoints() * PathStrengthData.ATTACK_PER_POINT);
    }

    @Test
    @DisplayName("a family's points add every species of it: two boars are 48, a bear 40")
    void familyPointsAddEverySpecies() {
        PathStrengthData data = new PathStrengthData(
                Set.of(BeastStrength.WHITE_BOAR, BeastStrength.BLACK_BOAR, BeastStrength.BEAR), Map.of());
        assertEquals(48, data.getBeastPoints(BeastStrengthFamily.BOAR));
        assertEquals(40, data.getBeastPoints(BeastStrengthFamily.BEAR));
        assertEquals(0, data.getBeastPoints(BeastStrengthFamily.TIGER));
    }

    @Test
    @DisplayName("the stone turtle and the galloping horse are 48 points each, the turtle carrying 128 capacity")
    void turtleAndHorseCarryTheirTraits() {
        PathStrengthData data = new PathStrengthData(
                Set.of(BeastStrength.STONE_TURTLE, BeastStrength.GALLOPING_HORSE, BeastStrength.BEAR), Map.of());
        assertEquals(48, BeastStrength.STONE_TURTLE.getPoints());
        assertEquals(48, BeastStrength.GALLOPING_HORSE.getPoints());
        assertEquals(BeastStrengthFamily.TURTLE, BeastStrength.STONE_TURTLE.getFamily());
        assertEquals(BeastStrengthFamily.HORSE, BeastStrength.GALLOPING_HORSE.getFamily());
        assertEquals(136, data.getBeastPoints());
        assertEquals(128, data.getBeastCapacityBonus(), "only the turtle carries capacity");
        assertEquals(0.5D, data.getBeastSprintBonus(), DELTA, "only the horse adds sprint");
        assertEquals(0, PathStrengthData.DEFAULT.getBeastCapacityBonus());
        assertEquals(0.0D, PathStrengthData.DEFAULT.getBeastSprintBonus(), DELTA);
        assertEquals(6.0D, BeastStrength.GALLOPING_HORSE.getPoints() * PathStrengthData.ATTACK_PER_POINT);
    }

    @Test
    @DisplayName("the horse adds a dash doubling, the turtle half again on shields, only both boars break the way")
    void horseTurtleAndBoarsCarryTheirCombatTraits() {
        PathStrengthData data = new PathStrengthData(
                Set.of(BeastStrength.STONE_TURTLE, BeastStrength.GALLOPING_HORSE, BeastStrength.WHITE_BOAR), Map.of());
        assertEquals(1.0D, data.getBeastDashBonus(), DELTA, "only the horse adds to the dash");
        assertEquals(1.5F, data.getShieldMultiplier(), DELTA, "only the turtle adds to shields");
        assertFalse(data.hasTwoBoars(), "one boar broke the way");
        assertTrue(data.with(BeastStrength.BLACK_BOAR).hasTwoBoars(), "the two boars together did not");
        assertEquals(0.0D, PathStrengthData.DEFAULT.getBeastDashBonus(), DELTA);
        assertEquals(1.0F, PathStrengthData.DEFAULT.getShieldMultiplier(), DELTA);
        assertEquals(0.0D, BeastStrength.GREEN_BULL.getDashBonus(), DELTA);
        assertEquals(0.0F, BeastStrength.BROWN_BEAR.getShieldBonus(), DELTA);
    }

    @Test
    @DisplayName("the green bull is 48 points of ox carrying 64 capacity, which stacks with the turtle's")
    void greenBullCarriesSixtyFourCapacity() {
        assertEquals(48, BeastStrength.GREEN_BULL.getPoints());
        assertEquals(BeastStrengthFamily.OX, BeastStrength.GREEN_BULL.getFamily());
        assertEquals(64, BeastStrength.GREEN_BULL.getCapacityBonus());
        assertEquals(0.0D, BeastStrength.GREEN_BULL.getSprintBonus(), DELTA);
        assertEquals(200, BeastStrength.GREEN_BULL.getDurationBonusTicks(), "ten seconds of duration");
        assertEquals(0, BeastStrength.STONE_TURTLE.getDurationBonusTicks());
        assertEquals(0, PathStrengthData.DEFAULT.getBeastDurationBonus());
        PathStrengthData data = PathStrengthData.DEFAULT.with(BeastStrength.GREEN_BULL)
                .with(BeastStrength.STONE_TURTLE);
        assertEquals(96, data.getBeastPoints());
        assertEquals(192, data.getBeastCapacityBonus());
        assertEquals(200, data.getBeastDurationBonus(), "only the bull lengthens the strength path's windows");
        assertEquals(48, data.getBeastPoints(BeastStrengthFamily.OX));
    }

    @Test
    @DisplayName("the brown bear's innate strength is 96 points of bear, the same whether it replaces the bear or not")
    void brownBearReplacesTheBearToTheSameNinetySix() {
        assertEquals(96, BeastStrength.BROWN_BEAR.getPoints());
        assertEquals(BeastStrengthFamily.BEAR, BeastStrength.BROWN_BEAR.getFamily());
        assertEquals(BeastStrength.Form.INNATE, BeastStrength.BROWN_BEAR.getForm());
        assertEquals(0.05F, BeastStrength.BROWN_BEAR.getChanceBonus(), DELTA);
        assertEquals(0, BeastStrength.BROWN_BEAR.getCapacityBonus());
        assertEquals(0, BeastStrength.BROWN_BEAR.getDurationBonusTicks());
        assertEquals(0.0D, BeastStrength.BROWN_BEAR.getSprintBonus(), DELTA);

        PathStrengthData bear = PathStrengthData.DEFAULT.with(BeastStrength.WHITE_BOAR).with(BeastStrength.BEAR);
        PathStrengthData upgraded = bear.with(BeastStrength.BROWN_BEAR);
        PathStrengthData fresh = PathStrengthData.DEFAULT.with(BeastStrength.WHITE_BOAR)
                .with(BeastStrength.BROWN_BEAR);
        assertEquals(fresh, upgraded, "the upgrade and the fresh grant left different bodies");
        assertFalse(upgraded.has(BeastStrength.BEAR), "the bear stayed beside its innate strength");
        assertEquals(96, upgraded.getBeastPoints(BeastStrengthFamily.BEAR));
        assertEquals(120, upgraded.getBeastPoints());
        assertEquals(1, upgraded.getBeastReadings().get(BeastStrengthFamily.BEAR));
        assertEquals(BeastStrength.BROWN_BEAR, upgraded.getInnate(BeastStrengthFamily.BEAR));
        assertNull(bear.getInnate(BeastStrengthFamily.BEAR));
        assertEquals(0.05F, upgraded.getBeastChanceBonus(), DELTA);
        assertEquals(0.0F, bear.getBeastChanceBonus(), DELTA);
        assertEquals(PathStrengthData.DEFAULT.with(BeastStrength.WHITE_BOAR),
                bear.withoutReplacedBy(BeastStrength.BROWN_BEAR), "the boar did not stay beside the upgrade");
    }

    @Test
    @DisplayName("only the white and black boars stack; any other family holds one strength and keeps out a weaker one")
    void onlyTheTwoBoarsStack() {
        PathStrengthData white = PathStrengthData.DEFAULT.with(BeastStrength.WHITE_BOAR);
        PathStrengthData boars = white.with(BeastStrength.BLACK_BOAR);
        assertEquals(48, boars.getBeastPoints(BeastStrengthFamily.BOAR));
        assertEquals(2, boars.getBeastReadings().get(BeastStrengthFamily.BOAR));
        assertNull(white.getCovering(BeastStrength.BLACK_BOAR), "the white boar kept out the black");
        assertEquals(BeastStrength.WHITE_BOAR, white.getCovering(BeastStrength.WHITE_BOAR));

        PathStrengthData bear = PathStrengthData.DEFAULT.with(BeastStrength.BEAR);
        PathStrengthData brownBear = PathStrengthData.DEFAULT.with(BeastStrength.BROWN_BEAR);
        assertNull(bear.getCovering(BeastStrength.BROWN_BEAR), "the bear kept out its innate strength");
        assertEquals(BeastStrength.BEAR, bear.getCovering(BeastStrength.BEAR));
        assertEquals(BeastStrength.BROWN_BEAR, brownBear.getCovering(BeastStrength.BEAR),
                "the bear could step the innate strength down");
        assertEquals(40, brownBear.with(BeastStrength.BEAR).getBeastPoints(),
                "a write did not swap the family's one strength");
        assertTrue(BeastStrength.BROWN_BEAR.replaces(BeastStrength.BEAR));
        assertFalse(BeastStrength.BLACK_BOAR.replaces(BeastStrength.WHITE_BOAR));
        assertFalse(BeastStrength.BROWN_BEAR.replaces(BeastStrength.TIGER));
    }

    @Test
    @DisplayName("the horse takes vanilla's 1.3 sprint to 1.8 times the walk; released, 1.5 walking and 2.3 sprinting")
    void sprintBonusComposesWithVanillaSprint() {
        double horse = BeastStrength.GALLOPING_HORSE.getSprintBonus();
        double sprint = 1.0D + SprintSpeedService.getModifier(true, horse, 0.0D);
        assertEquals(1.8D, sprint * SprintSpeedService.VANILLA_SPRINT_FACTOR, DELTA);
        assertEquals(0.0D, SprintSpeedService.getModifier(false, horse, 0.0D), DELTA, "the walk was sped");
        assertEquals(1.5D, 1.0D + SprintSpeedService.getModifier(false, horse, horse), DELTA);
        assertEquals(2.3D, (1.0D + SprintSpeedService.getModifier(true, horse, horse))
                * SprintSpeedService.VANILLA_SPRINT_FACTOR, DELTA);
        assertEquals(0.0D, SprintSpeedService.getModifier(true, 0.0D, 0.0D), DELTA);
    }

    @Test
    @DisplayName("only the released horse lends its speed, and only to the families named")
    void releasedSpeedSumsTheNamedFamilies() {
        PathStrengthData data = PathStrengthData.DEFAULT.with(BeastStrength.GALLOPING_HORSE)
                .with(BeastStrength.STONE_TURTLE);
        assertEquals(0.5D, data.getBeastSprintBonus(List.of(BeastStrengthFamily.HORSE)), DELTA);
        assertEquals(0.0D, data.getBeastSprintBonus(List.of(BeastStrengthFamily.TURTLE)), DELTA);
        assertEquals(0.0D, data.getBeastSprintBonus(List.of()), DELTA);
    }

    @Test
    @DisplayName("every family takes shape, the ox since it got the buffalo's model, in declaration order")
    void everyFamilyTakesShape() {
        assertEquals(List.of(BeastStrengthFamily.values()),
                BeastStrengthFamily.getManifesting(List.of(BeastStrengthFamily.values())));
        assertEquals(List.of(BeastStrengthFamily.OX),
                BeastStrengthFamily.getManifesting(List.of(BeastStrengthFamily.OX)));
    }

    @Test
    @DisplayName("an amplifier fills one layer: a beast or plain source brings points, Hardship only a punch factor")
    void amplifiersBringNoCapacity() {
        StrengthAmplifier big = StrengthAmplifier.points(SOURCE, 32);
        StrengthAmplifier hardship = StrengthAmplifier.punch(SOURCE, 30);
        assertEquals(new StrengthAmplifier(SOURCE, BeastStrengthFamily.BOAR, 24, 0, 0), BOAR);
        assertEquals(new StrengthAmplifier(SOURCE, null, 32, 0, 0), big);
        assertEquals(new StrengthAmplifier(SOURCE, null, 0, 0, 30), hardship);
        assertTrue(BOAR.isBeast(), "the timed boar has no family");
        assertFalse(big.isBeast(), "the big strength counts as a beast");
        assertFalse(hardship.isBeast(), "Hardship counts as a beast");

        List<StrengthAmplifier> all = List.of(BOAR, OX, big, hardship);
        assertEquals(96, StrengthService.getAmplifierPoints(all), "Hardship lent points");
        assertEquals(0, StrengthService.getCapacityBonus(all), "an amplifier lifted the capacity");
        assertEquals(1.3D, StrengthService.getPunchFactor(all), DELTA);
    }

    @Test
    @DisplayName("the overload is the settled points past the current capacity, whatever their source")
    void overloadIsTheSettledPointsPastTheCapacity() {
        assertEquals(8, StrengthService.getOverloadPoints(64, 56), "rank five bear and boar");
        assertEquals(0, StrengthService.getOverloadPoints(56, 56), "a body at its capacity overloaded");
        assertEquals(0, StrengthService.getOverloadPoints(40, 56));
        assertEquals(48, StrengthService.getOverloadPoints(64, 16), "a command's tiger past a mortal's 16");
        assertEquals(144, StrengthService.getOverloadPoints(200, 56), "human jin past the capacity");
        assertEquals(674, StrengthService.getOverloadPoints(842, 168));
        assertEquals(0, StrengthService.getOverloadPoints(0, 0));
        assertEquals(40, StrengthService.getOverloadPoints(40, -5), "a negative capacity");
    }

    @Test
    @DisplayName("the snapshot settles in full: only All-Out Effort lifts the capacity, no amplifier does")
    void snapshotSettlesPastTheCapacity() {
        int none = StrengthService.NO_ALL_OUT;
        StrengthSnapshot boar = StrengthSnapshot.of(56, 40, List.of(BOAR), none);
        assertEquals(56, boar.capacity(), "the boar moved the capacity");
        assertEquals(40, boar.permanentPoints());
        assertEquals(64, boar.totalPoints());
        assertEquals(64, boar.settledPoints(), "the overload was not brought to bear in full");
        assertEquals(8, boar.overloadPoints(boar.settledPoints()));
        assertFalse(boar.allOut());

        StrengthSnapshot lifted = StrengthSnapshot.of(56, 40, List.of(OX), 2);
        assertTrue(lifted.allOut());
        assertEquals(84, lifted.capacity());
        assertEquals(0, lifted.overloadPoints(lifted.settledPoints()), "rank three All-Out's 84 did not carry the ox");
        assertEquals(24, StrengthSnapshot.of(56, 40, List.of(OX), none).overloadPoints(80));

        StrengthSnapshot hardship = StrengthSnapshot.of(56, 40, List.of(BOAR, StrengthAmplifier.punch(SOURCE, 50)),
                none);
        assertEquals(56, hardship.capacity(), "Hardship lifted the capacity");
        assertEquals(64, hardship.totalPoints(), "Hardship moved the total");
        assertEquals(96, hardship.settledPoints());
        assertEquals(40, hardship.overloadPoints(hardship.settledPoints()));

        StrengthSnapshot big = StrengthSnapshot.of(56, 40, List.of(StrengthAmplifier.points(SOURCE, 32)), none);
        assertEquals(56, big.capacity(), "the big strength lifted the capacity");
        assertEquals(72, big.settledPoints());
        assertEquals(16, big.overloadPoints(big.settledPoints()));

        StrengthSnapshot human = StrengthSnapshot.of(56, 200, List.of(), none);
        assertEquals(144, human.overloadPoints(human.settledPoints()), "human jin past the capacity paid nothing");
        StrengthSnapshot command = StrengthSnapshot.of(16, 64, List.of(), none);
        assertEquals(48, command.overloadPoints(command.settledPoints()), "a command's tiger paid nothing");
        assertEquals(112, command.overloadPoints(command.settle(64, 1.0D)), "the release was left out of the overload");
    }

    @Test
    @DisplayName("the recoil is overload / capacity * max health / 2, stopping the health at 2")
    void recoilIsHalfTheOverloadShareOfHealth() {
        assertEquals(0.5F, BodyOverload.RECOIL_RATIO, DELTA);
        assertEquals(2.0F, BodyOverload.HEALTH_FLOOR, DELTA);
        assertEquals(1.4286F, BodyOverload.getRecoil(8, 56, 20.0F), DELTA);
        assertEquals(4.2857F, BodyOverload.getRecoil(24, 56, 20.0F), DELTA);
        assertEquals(15.0F, BodyOverload.getRecoil(24, 16, 20.0F), DELTA);
        assertEquals(40.119F, BodyOverload.getRecoil(674, 168, 20.0F), DELTA);
        assertEquals(0.0F, BodyOverload.getRecoil(0, 84, 20.0F), DELTA);
        assertEquals(0.0F, BodyOverload.getRecoil(8, 0, 20.0F), DELTA);
        assertEquals(1.4286F, BodyOverload.getDamage(BodyOverload.getRecoil(8, 56, 20.0F), 20.0F), DELTA);
        assertEquals(18.0F, BodyOverload.getDamage(BodyOverload.getRecoil(674, 168, 20.0F), 20.0F), DELTA,
                "a recoil past the health did not stop at 2");
        assertEquals(1.0F, BodyOverload.getDamage(4.2857F, 3.0F), DELTA);
        assertEquals(0.0F, BodyOverload.getDamage(4.2857F, 2.0F), DELTA);
        assertEquals(0.0F, BodyOverload.getDamage(4.2857F, 1.5F), DELTA);
    }
}
