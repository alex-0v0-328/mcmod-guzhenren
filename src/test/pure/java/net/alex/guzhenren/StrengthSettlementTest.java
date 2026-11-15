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
    @DisplayName("at rest a body uses min(points, capacity) and keeps the rest -- no overflow ramp")
    void usableIsTheMinimum() {
        assertEquals(0, StrengthService.getUsablePoints(24, 0));
        assertEquals(24, StrengthService.getUsablePoints(24, 24));
        assertEquals(24, StrengthService.getUsablePoints(24, 25));
        assertEquals(24, StrengthService.getUsablePoints(24, 9999));
        assertEquals(16, StrengthService.getUsablePoints(16, BeastStrength.WHITE_BOAR.getPoints()));
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
    @DisplayName("a full release counts the family's permanent points three times, still cut by the capacity")
    void fullReleaseIsCutByTheCapacity() {
        assertEquals(3, StrengthService.FULL_RELEASE_FACTOR);
        assertEquals(16, StrengthService.getReleasedExtra(56, 40, 40), "a rank five bear stops at its 56");
        assertEquals(80, StrengthService.getReleasedExtra(168, 40, 40), "rank five All-Out did not carry 120");
        assertEquals(44, StrengthService.getReleasedExtra(84, 40, 40), "rank three All-Out did not stop at 84");
        assertEquals(0, StrengthService.getReleasedExtra(16, 64, 64), "an already cut tiger added past the capacity");
        assertEquals(80, StrengthService.getReleasedExtra(168, 64, 40), "a timed boar joined the 3x");
        assertEquals(0, StrengthService.getReleasedExtra(56, 40, 0), "a plain draw added points");
        assertEquals(0, StrengthService.getReleasedExtra(16, 0, 0));
        assertEquals(0, StrengthService.getReleasedExtra(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE),
                "overflow");
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
    @DisplayName("a timed beast brings no capacity of its own")
    void timedBeastBringsNoCapacity() {
        assertEquals(0, BOAR.capacityBonus());
        assertEquals(0, OX.capacityBonus());
        assertEquals(64, StrengthService.getTimedBeastPoints(
                List.of(BOAR, OX, new StrengthAmplifier(SOURCE, null, 0, 28))), "Hardship counted as a timed beast");
    }

    @Test
    @DisplayName("the overload is the timed beasts' points past the capacity, beside the permanent beasts")
    void overloadIsTheTimedBeastsPastTheCapacity() {
        assertEquals(8, StrengthService.getOverloadPoints(56, 40, 24), "rank five bear and boar");
        assertEquals(24, StrengthService.getOverloadPoints(56, 40, 40), "rank five bear and ox");
        assertEquals(0, StrengthService.getOverloadPoints(56, 0, 24), "a boar alone under rank five's 56 overloaded");
        assertEquals(24, StrengthService.getOverloadPoints(16, 0, 40), "a mortal's ox past 16");
        assertEquals(24, StrengthService.getOverloadPoints(16, 64, 24), "the tiger past 16 overloaded");
        assertEquals(0, StrengthService.getOverloadPoints(56, 0, 0));
        assertEquals(0, StrengthService.getOverloadPoints(56, Integer.MAX_VALUE, -5), "a negative timed beast");
        assertEquals(40, StrengthService.getOverloadPoints(0, Integer.MAX_VALUE, 40), "overflow");
    }

    @Test
    @DisplayName("the snapshot carries the overload, and All-Out Effort or Hardship lifts it away")
    void snapshotCarriesTheOverload() {
        int none = StrengthService.NO_ALL_OUT;
        StrengthSnapshot boar = StrengthSnapshot.of(56, 40, 40, List.of(BOAR), none);
        assertEquals(56, boar.capacity(), "the boar moved the capacity");
        assertEquals(64, boar.totalPoints());
        assertEquals(8, boar.overloadPoints());
        assertEquals(64, StrengthService.getUsablePoints(boar.capacity(), boar.totalPoints()) + boar.overloadPoints(),
                "the overload was not brought to bear in full");

        StrengthSnapshot ox = StrengthSnapshot.of(56, 40, 40, List.of(OX), none);
        assertEquals(24, ox.overloadPoints());
        assertEquals(0, StrengthSnapshot.of(56, 40, 40, List.of(OX), 2).overloadPoints(),
                "rank three All-Out Effort's 84 did not carry the ox");
        assertEquals(0, StrengthSnapshot.of(56, 40, 40, List.of(BOAR), 2).overloadPoints(),
                "rank three All-Out Effort's 84 did not carry the boar");
        assertEquals(0, StrengthSnapshot.of(56, 40, 40, List.of(BOAR, new StrengthAmplifier(SOURCE, null, 0, 28)),
                none).overloadPoints(), "Hardship's 28 did not carry the boar");
        assertEquals(0, StrengthSnapshot.of(56, 200, 0, List.of(), none).overloadPoints(), "human jin overloaded");
        assertEquals(0, StrengthSnapshot.of(16, 64, 64, List.of(), none).overloadPoints(),
                "a command's tiger overloaded");
    }

    @Test
    @DisplayName("each fist hit costs overload / capacity * max health / 2, stopping the health at 2")
    void recoilIsHalfTheOverloadShareOfHealth() {
        assertEquals(1.4286F, BodyOverload.getRecoil(8, 56, 20.0F), DELTA);
        assertEquals(4.2857F, BodyOverload.getRecoil(24, 56, 20.0F), DELTA);
        assertEquals(0.0F, BodyOverload.getRecoil(0, 84, 20.0F), DELTA);
        assertEquals(0.0F, BodyOverload.getRecoil(8, 0, 20.0F), DELTA);
        assertEquals(1.4286F, BodyOverload.getDamage(BodyOverload.getRecoil(8, 56, 20.0F), 20.0F), DELTA);
        assertEquals(1.0F, BodyOverload.getDamage(4.2857F, 3.0F), DELTA);
        assertEquals(0.0F, BodyOverload.getDamage(4.2857F, 2.0F), DELTA);
        assertEquals(0.0F, BodyOverload.getDamage(4.2857F, 1.5F), DELTA);
    }
}
