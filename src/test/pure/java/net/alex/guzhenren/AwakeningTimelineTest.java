package net.alex.guzhenren;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitual;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static net.alex.guzhenren.TestBuffers.withBuffer;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.AWAKEN_TICKS;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.BAR_FILL_TICKS;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.END_TICKS;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.FLASH_START_TICKS;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.FLASH_STEPS;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.FLY_TICKS;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.RELEASE_TICKS;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.SHATTER_BLOCK;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.SHATTER_TICKS;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.getBarBase;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.getBob;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.getBurnoutFrame;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.getFlashReach;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.getFlashStep;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.getFlight;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.getSheenOffset;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.getShatteredRows;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.getUnfoldedRows;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.holdsStill;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.isBarFull;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.isBurnoutStep;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.isOver;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.showsBar;
import static net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline.showsPlaque;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwakeningTimelineTest {

    private static final float EPSILON = 1.0E-6F;

    @Test
    @DisplayName("开窍仪式 marks are the documented ticks: flash 80, awaken 90, release 100, shatter 154, end 164")
    void marksAreTheDocumentedTicks() {
        assertEquals(12, FLY_TICKS);
        assertEquals(80, FLASH_START_TICKS);
        assertEquals(90, AWAKEN_TICKS);
        assertEquals(100, RELEASE_TICKS);
        assertEquals(154, SHATTER_TICKS);
        assertEquals(164, END_TICKS);
        assertEquals(60, BAR_FILL_TICKS);
    }

    @Test
    @DisplayName("开窍仪式 marks run in order: fly, flash, awaken, release, shatter, end")
    void marksRunInOrder() {
        assertTrue(FLY_TICKS < BAR_FILL_TICKS);
        assertTrue(BAR_FILL_TICKS < FLASH_START_TICKS);
        assertTrue(FLASH_START_TICKS < AWAKEN_TICKS);
        assertTrue(AWAKEN_TICKS < RELEASE_TICKS);
        assertTrue(RELEASE_TICKS < SHATTER_TICKS);
        assertTrue(SHATTER_TICKS + 64 / SHATTER_BLOCK <= END_TICKS, "the tallest plaque shatters before the end");
        assertTrue(isOver(END_TICKS));
        assertFalse(isOver(END_TICKS - 0.5F));
    }

    @Test
    @DisplayName("the aptitude bar climbs from 20 to the rolled base in 3 s, holds full, and is gone at the flash")
    void aptitudeBarClimbsThenHolds() {
        int rolled = 87;
        assertEquals(20, getBarBase(0.0F, rolled));
        for (float t = 0.0F; t < BAR_FILL_TICKS; t += 1.0F) {
            assertTrue(getBarBase(t + 1.0F, rolled) >= getBarBase(t, rolled), "the bar fell back at " + t);
            assertFalse(isBarFull(t), "the bar was full before it filled at " + t);
        }
        assertEquals(rolled, getBarBase(BAR_FILL_TICKS, rolled));
        assertEquals(rolled, getBarBase(FLASH_START_TICKS - 0.5F, rolled));
        assertTrue(isBarFull(BAR_FILL_TICKS));
        assertTrue(showsBar(0.0F));
        assertTrue(showsBar(FLASH_START_TICKS - 0.5F));
        assertFalse(showsBar(FLASH_START_TICKS));
        assertFalse(showsBar(-1.0F));
        assertEquals(100, getBarBase(BAR_FILL_TICKS, 100));
    }

    @Test
    @DisplayName("the stance holds through the flash and lifts when the plaque comes in")
    void stanceLiftsAtTheRelease() {
        assertTrue(holdsStill(0.0F));
        assertTrue(holdsStill(RELEASE_TICKS - 0.01F));
        assertFalse(holdsStill(RELEASE_TICKS));
    }

    @Test
    @DisplayName("the Hope Gu flies out without bobbing")
    void guFliesOutThenHovers() {
        assertEquals(0.0F, getFlight(0.0F), EPSILON);
        assertEquals(1.0F, getFlight(FLY_TICKS), EPSILON);
        assertEquals(0.0F, getBob(FLY_TICKS / 2.0F), EPSILON);
    }

    @Test
    @DisplayName("the Hope Gu burns out in eight frames, stepping at 24/36/48/60/66/72/76, and is gone at the flash")
    void guBurnsOutFrameByFrame() {
        int[] steps = {24, 36, 48, 60, 66, 72, 76};
        assertEquals(0, getBurnoutFrame(0));
        assertEquals(0, getBurnoutFrame(23));
        for (int i = 0; i < steps.length; i++) {
            assertTrue(isBurnoutStep(steps[i]));
            assertFalse(isBurnoutStep(steps[i] - 1));
            assertEquals(i + 1, getBurnoutFrame(steps[i]));
            assertEquals(i, getBurnoutFrame(steps[i] - 1));
        }
        assertEquals(7, getBurnoutFrame(FLASH_START_TICKS - 1));
        assertEquals(-1, getBurnoutFrame(FLASH_START_TICKS));
        assertFalse(isBurnoutStep(FLASH_START_TICKS));
    }

    @Test
    @DisplayName("the awakening ritual stream codec keeps every field")
    void ritualRoundTrip() {
        AwakeningRitual expected = new AwakeningRitual(123_456_789L, 100, ExtremePhysique.NORTHERN_DARK_ICE_SOUL,
                new Vec3(1.25, -60.5, 3_000_000.75), true);
        withBuffer(buffer -> {
            AwakeningRitual.encode(buffer, expected);
            assertEquals(expected, AwakeningRitual.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        });
    }

    @Test
    @DisplayName("the awakening ritual save codec keeps every field")
    void ritualSaveRoundTrip() {
        AwakeningRitual expected = new AwakeningRitual(123_456_789L, 100, ExtremePhysique.VERDANT_GREAT_SUN,
                new Vec3(-8.5, 64.0, 12.25), true);
        JsonElement saved = AwakeningRitual.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow();
        assertEquals(expected, AwakeningRitual.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow());
    }

    @Test
    @DisplayName("a ritual saved before the physique and the reveal joined loads with NONE and unrevealed")
    void olderSaveLoadsWithDefaults() {
        AwakeningRitual older = new AwakeningRitual(77L, 87, ExtremePhysique.NONE, new Vec3(1.0, 2.0, 3.0), false);
        JsonElement saved = AwakeningRitual.CODEC.encodeStart(JsonOps.INSTANCE, older).getOrThrow();
        saved.getAsJsonObject().remove("physique");
        saved.getAsJsonObject().remove("revealed");
        assertEquals(older, AwakeningRitual.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow());
    }

    @Test
    @DisplayName("the Hope Gu hovers once it arrives, bobbing at most 0.08 of a block")
    void theGuBobsAfterItArrives() {
        assertEquals((float) (Math.sin(1.5D) * 0.08D), getBob(FLY_TICKS + 10.0F), 1.0E-5F);
        float highest = 0.0F;
        for (float t = FLY_TICKS; t < AWAKEN_TICKS; t += 0.25F) highest = Math.max(highest, getBob(t));
        assertEquals(0.08F, highest, 1.0E-3F);
    }

    @Test
    @DisplayName("the flash diamond grows a step every 2 ticks to full at 90, then shrinks back and is off at 100")
    void flashStepsOutAndBack() {
        int[] expected = {1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 5, 5, 4, 4, 3, 3, 2, 2};
        assertEquals(0, getFlashStep(FLASH_START_TICKS - 1));
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], getFlashStep(FLASH_START_TICKS + i), "tick " + (FLASH_START_TICKS + i));
        }
        assertEquals(FLASH_STEPS, getFlashStep(AWAKEN_TICKS));
        assertEquals(0, getFlashStep(RELEASE_TICKS));
        assertEquals(37, getFlashReach(FLASH_STEPS, 37), "the full step reaches the farthest cell");
        assertEquals(6, getFlashReach(1, 37));
        assertEquals(30, getFlashReach(5, 37));
    }

    @Test
    @DisplayName("the plaque unfolds 16 rows a tick from its middle line, full by 103")
    void plaqueUnfoldsFromTheMiddle() {
        assertEquals(0, getUnfoldedRows(RELEASE_TICKS - 1, 52));
        assertEquals(16, getUnfoldedRows(RELEASE_TICKS, 52));
        assertEquals(32, getUnfoldedRows(RELEASE_TICKS + 1, 52));
        assertEquals(48, getUnfoldedRows(RELEASE_TICKS + 2, 52));
        assertEquals(52, getUnfoldedRows(RELEASE_TICKS + 3, 52));
        assertEquals(64, getUnfoldedRows(RELEASE_TICKS + 3, 64));
        assertEquals(64, getUnfoldedRows(SHATTER_TICKS, 64));
    }

    @Test
    @DisplayName("the sheen sweeps twice, at 110 and 130, eight pixels a tick, off once past the plaque")
    void sheenSweepsTwice() {
        int span = 16 + 52 - 1;
        assertEquals(-1, getSheenOffset(109, span));
        assertEquals(0, getSheenOffset(110, span));
        assertEquals(8, getSheenOffset(111, span));
        assertEquals(64, getSheenOffset(118, span));
        assertEquals(-1, getSheenOffset(119, span));
        assertEquals(0, getSheenOffset(130, span));
        assertEquals(-1, getSheenOffset(139, span));
    }

    @Test
    @DisplayName("the plaque shatters a block row a tick from the bottom: talent 154-160, ten extremes to 161")
    void plaqueShattersFromTheBottom() {
        assertEquals(0, getShatteredRows(SHATTER_TICKS - 1));
        assertEquals(1, getShatteredRows(SHATTER_TICKS));
        assertTrue(showsPlaque(RELEASE_TICKS, 52));
        assertFalse(showsPlaque(RELEASE_TICKS - 1, 52));
        assertTrue(showsPlaque(159, 52));
        assertFalse(showsPlaque(160, 52));
        assertTrue(showsPlaque(160, 64));
        assertFalse(showsPlaque(161, 64));
    }
}
