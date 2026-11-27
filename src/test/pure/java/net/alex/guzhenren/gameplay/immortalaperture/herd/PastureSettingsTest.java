package net.alex.guzhenren.gameplay.immortalaperture.herd;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.alex.guzhenren.core.ModSavedData.StackIo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PastureSettingsTest {

    private static final StackIo NO_STACKS = new StackIo() {

        @Override
        public @NotNull Tag save(@NotNull ItemStack stack) { throw new AssertionError("settings hold no stacks"); }

        @Override
        public @NotNull ItemStack load(@NotNull CompoundTag tag) { throw new AssertionError("settings hold no stacks"); }
    };
    private static final ChunkPos HOME = new ChunkPos(3, -4);
    private static final ChunkPos AWAY = new ChunkPos(-6, 2);
    private static final ResourceLocation COW = ResourceLocation.withDefaultNamespace("cow");
    private static final ResourceLocation PIG = ResourceLocation.withDefaultNamespace("pig");
    private final AtomicInteger dirty = new AtomicInteger();
    private final PastureSettings settings = new PastureSettings(dirty::incrementAndGet);

    @Test
    @DisplayName("an untouched pasture gathers every kind and does not gather by itself")
    void defaults() {
        assertTrue(settings.gathers(HOME, COW));
        assertFalse(settings.autoGather(HOME));
        assertEquals(Set.of(), settings.excluded(HOME));
    }

    @Test
    @DisplayName("turning a kind off and on again marks dirty only on a change")
    void kinds() {
        settings.setGathers(HOME, PIG, false);
        settings.setGathers(HOME, PIG, false);
        assertFalse(settings.gathers(HOME, PIG));
        assertTrue(settings.gathers(HOME, COW), "other kinds still gather");
        assertTrue(settings.gathers(AWAY, PIG), "other pastures are untouched");
        settings.setGathers(HOME, PIG, true);
        assertTrue(settings.gathers(HOME, PIG));
        assertEquals(2, dirty.get());
    }

    @Test
    @DisplayName("moving carries the settings to the new corner and forgetting drops them")
    void moveAndForget() {
        settings.setAutoGather(HOME, true);
        settings.setGathers(HOME, COW, false);
        settings.move(HOME, AWAY);
        assertTrue(settings.autoGather(AWAY));
        assertFalse(settings.gathers(AWAY, COW));
        assertFalse(settings.autoGather(HOME), "the old corner is clear");
        assertTrue(settings.gathers(HOME, COW));
        settings.forget(AWAY);
        assertFalse(settings.autoGather(AWAY));
        assertTrue(settings.gathers(AWAY, COW));
    }

    @Test
    @DisplayName("losing the owner stops auto gathering but keeps the kinds")
    void clear() {
        settings.setAutoGather(HOME, true);
        settings.setGathers(HOME, PIG, false);
        settings.clearAutoGather();
        assertFalse(settings.autoGather(HOME));
        assertFalse(settings.gathers(HOME, PIG));
    }

    @Test
    @DisplayName("the settings survive a save and load")
    void roundTrip() {
        settings.setAutoGather(HOME, true);
        settings.setGathers(AWAY, PIG, false);
        CompoundTag tag = new CompoundTag();
        settings.save(tag, "pasture_settings", NO_STACKS);
        PastureSettings loaded = new PastureSettings(() -> {});
        loaded.load(tag, "pasture_settings", NO_STACKS);
        assertTrue(loaded.autoGather(HOME));
        assertFalse(loaded.autoGather(AWAY));
        assertEquals(Set.of(PIG), loaded.excluded(AWAY));
        assertEquals(Set.of(), loaded.excluded(HOME));
    }
}
