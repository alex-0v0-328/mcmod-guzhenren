package net.alex.guzhenren;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.EffectCarrier;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectCategory;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.PathMarks;
import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.tending.GuHunger;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuPropertiesTest {

    private static final PathEffectType<Unit> TEST_TYPE = new PathEffectType<>(
            ResourceLocation.fromNamespaceAndPath("guzhenren", "test_spec"), MapCodec.unit(Unit.INSTANCE),
            StreamCodec.unit(Unit.INSTANCE), PathEffectCategory.BENEFICIAL, false, null, effect -> Component.empty());

    private static GuProperties channelling() {
        return GuProperties.of(Rank.ONE, GuPath.STRENGTH)
                .refine(1_000)
                .channel(4_000)
                .hungerBar(12, 1)
                .hungerPerRound(10);
    }

    @Test
    @DisplayName("a real 灌注 registration passes its own check")
    void aRealChannelPasses() {
        assertDoesNotThrow(() -> channelling().validate("white_boar_gu_1"));
    }

    @Test
    @DisplayName("an 即时 Gu is not asked for hunger rates")
    void instantGuIsNotChecked() {
        assertDoesNotThrow(() -> GuProperties.of(Rank.TWO, GuPath.TRANSFORMATION)
                .refine(16_000).costPerUse(200)
                .hungerBar(4, 2).hungerPerUse(2)
                .validate("roaming_zombie_gu_2"));
    }

    @Test
    @DisplayName("a round that does not divide by its hunger points is refused")
    void roundMustDivideByHungerPoints() {
        GuProperties bad = channelling().hungerPerRound(7);
        assertThrows(IllegalStateException.class, () -> bad.validate("white_boar_gu_1"));
    }

    @Test
    @DisplayName("a channel that costs no hunger is refused")
    void aChannelMustPaySomething() {
        assertThrows(IllegalStateException.class, () -> GuProperties.of(Rank.ONE, GuPath.STRENGTH)
                .channel(3_600).validate("no_hunger"));
    }

    @Test
    @DisplayName("negative prices are refused on either shape")
    void negativePricesAreRefused() {
        assertThrows(IllegalStateException.class,
                () -> GuProperties.of(Rank.ONE, GuPath.STRENGTH).refine(-1).validate("negative_refine"));
        assertThrows(IllegalStateException.class,
                () -> GuProperties.of(Rank.ONE, GuPath.STRENGTH).costPerUse(-1).validate("negative_use"));
    }

    @Test
    @DisplayName("a recover bar builds the clock that refills with time, and refuses to channel or feed")
    void recoverBarBuildsItsOwnClock() {
        GuProperties guProperties = GuProperties.of(Rank.FIVE, GuPath.TIME).refine(1_000_000).costPerUse(320_000)
                .recoverBar(2, 12_000);
        assertDoesNotThrow(() -> guProperties.validate("man_as_before_gu_5"));
        assertTrue(guProperties.buildHunger() instanceof GuHunger.RecoverBar(int max, int ticksPerPoint, int perUse)
                && max == 2 && ticksPerPoint == 12_000 && perUse == 1);
        assertThrows(IllegalStateException.class, () -> GuProperties.of(Rank.FIVE, GuPath.TIME)
                .channel(4_000).hungerPerRound(10).recoverBar(2, 12_000).validate("channels_and_recovers"));
        assertThrows(IllegalStateException.class, () -> GuProperties.of(Rank.FIVE, GuPath.TIME)
                .recoverBar(0, 12_000).validate("recovers_without_a_bar"));
    }

    private static GuProperties allOutShape(int feedUnits) {
        return GuProperties.of(Rank.THREE, GuPath.STRENGTH).refine(80_000).costPerUse(1_600)
                .hungerBar(48, 1).hungerPerUse(16).hungerPerDay(4)
                .feed(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("guzhenren", "test_feed")),
                        feedUnits);
    }

    @Test
    @DisplayName("a food worth more than one point must divide the bar, a use and a day")
    void aBigFoodMustDivideTheBar() {
        assertThrows(IllegalStateException.class, () -> allOutShape(5).validate("all_out_effort_gu_3"));
        assertDoesNotThrow(() -> allOutShape(4).validate("all_out_effort_gu_3"));
        assertThrows(IllegalStateException.class,
                () -> allOutShape(4).hungerPerUse(18).validate("uneven_use"));
    }

    @Test
    @DisplayName("effect and item cooldowns can be configured independently")
    void cooldownsAreIndependent() {
        GuProperties guProperties = GuProperties.of(Rank.THREE, GuPath.STRENGTH).cooldown(600, 20);
        assertEquals(600, guProperties.getEffectCooldownTicks());
        assertEquals(20, guProperties.getItemCooldownTicks());
    }

    @Test
    @DisplayName("an effect takes its rank's default points on the Gu's own path unless the chain names them")
    void effectPointsDefaultByRank() {
        long[] defaults = new long[Rank.getSettable().length];
        for (int i = 0; i < defaults.length; i++) defaults[i] = GuProperties.getDefaultPoints(Rank.getSettable()[i]);
        assertArrayEquals(new long[]{12L, 48L, 192L, 768L, 3_072L}, defaults);

        GuProperties guProperties = GuProperties.of(Rank.THREE, GuPath.METAL).effect(EffectCarrier.BODY);
        assertEquals(EffectCarrier.BODY, guProperties.getEffectCarrier());
        assertEquals(PathMarks.ofPoints(192L), guProperties.getEffectMarks());
        assertEquals(Map.of(GuPath.METAL, PathMarks.ofPoints(192L)), guProperties.getEffectPaths());
        assertDoesNotThrow(() -> guProperties.validate("bronze_skin_gu_3"));

        GuProperties named = GuProperties.of(Rank.ONE, GuPath.WATER).effect(EffectCarrier.BODY, 20L);
        assertEquals(PathMarks.ofPoints(20L), named.getEffectMarks());
        PathEffect<Unit> effect = named.createEffect(TEST_TYPE,
                ResourceLocation.fromNamespaceAndPath("guzhenren", "heavenly_mugwort_gu_3"), 600L, Unit.INSTANCE);
        assertEquals(EffectCarrier.BODY, effect.carrier());
        assertEquals(Map.of(GuPath.WATER, PathMarks.ofPoints(20L)), effect.paths());
        assertEquals(600L, effect.endTick());
    }

    @Test
    @DisplayName("a Gu without an effect carries no marks and cannot build one")
    void noEffectNoMarks() {
        GuProperties guProperties = GuProperties.of(Rank.TWO, GuPath.STRENGTH);
        assertNull(guProperties.getEffectCarrier());
        assertTrue(guProperties.getEffectPaths().isEmpty());
        assertThrows(IllegalStateException.class, () -> guProperties.createEffect(TEST_TYPE,
                ResourceLocation.fromNamespaceAndPath("guzhenren", "instant_gu"), PathEffect.PERMANENT, Unit.INSTANCE));
    }

    @Test
    @DisplayName("a mortal Gu's effect carries points only, any number of them, and never whole marks")
    void mortalEffectCarriesPointsOnly() {
        assertDoesNotThrow(() -> GuProperties.of(Rank.FIVE, GuPath.TIME).effect(EffectCarrier.APERTURE, 1_000_000L)
                .validate("many_points"));
        assertThrows(IllegalStateException.class, () -> GuProperties.of(Rank.FIVE, GuPath.TIME)
                .effect(EffectCarrier.APERTURE, new PathMarks(1L, 5L)).validate("a_whole_mark"));
        assertThrows(IllegalStateException.class, () -> GuProperties.of(Rank.ONE, GuPath.TIME)
                .effect(EffectCarrier.MIND, 0L).validate("no_marks"));
    }

    @Test
    @DisplayName("a Gu with a bar builds the hunger clock from its chain, and one without builds no clock")
    void aBarBuildsTheHungerClock() {
        GuProperties bar = GuProperties.of(Rank.THREE, GuPath.STRENGTH).hungerBar(48, 2).channel(900)
                .hungerPerRound(3).hungerPerUse(5).hungerPerDay(7);
        assertTrue(bar.buildHunger() instanceof GuHunger.HungerBar(int max, int units, int essence, int round,
                int use, int day) && max == 48 && units == 2 && essence == 900 && round == 3 && use == 5 && day == 7);
        assertInstanceOf(GuHunger.NoClock.class, GuProperties.of(Rank.THREE, GuPath.STRENGTH).buildHunger());
    }

    @Test
    @DisplayName("a day's drain must be positive, and zero prices are fine")
    void aDayMustDrainSomething() {
        assertThrows(IllegalStateException.class,
                () -> GuProperties.of(Rank.ONE, GuPath.STRENGTH).hungerPerDay(0).validate("no_drain"));
        assertDoesNotThrow(() -> GuProperties.of(Rank.ONE, GuPath.STRENGTH).refine(0).costPerUse(0)
                .hungerPerDay(1).validate("free"));
    }

    @Test
    @DisplayName("a channel that pours nothing is refused even though zero divides by any hunger")
    void aChannelMustPourSomething() {
        assertThrows(IllegalStateException.class,
                () -> GuProperties.of(Rank.ONE, GuPath.STRENGTH).channel(0).hungerBar(12, 1).hungerPerRound(10)
                        .validate("pours_nothing"));
    }

    @Test
    @DisplayName("a food must be whole points of the bar, and its size in points is what must divide the sizes")
    void aFoodIsWholePointsOfTheBar() {
        TagKey<net.minecraft.world.item.Item> feed = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("guzhenren", "test_feed"));
        GuProperties halves = GuProperties.of(Rank.THREE, GuPath.STRENGTH).refine(80_000).costPerUse(1_600)
                .hungerBar(48, 2).hungerPerUse(16).hungerPerDay(4);
        assertThrows(IllegalStateException.class, () -> halves.feed(feed, 5).validate("half_a_point"));
        assertDoesNotThrow(() -> halves.feed(feed, 8).validate("four_points"));
    }
}
