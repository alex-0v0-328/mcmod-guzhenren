package net.alex.guzhenren.gameplay.immortalaperture.herd;

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
import static org.junit.jupiter.api.Assertions.assertNull;

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
    @DisplayName("an untouched pasture has no animal and no floor, and a floor needs an animal")
    void defaults() {
        assertNull(settings.animal(HOME));
        settings.setFloor(HOME, 3);
        assertEquals(0, settings.floor(HOME));
        assertEquals(0, dirty.get());
    }

    @Test
    @DisplayName("choosing the same animal twice marks dirty once, and a new animal clears the floor")
    void animal() {
        settings.setAnimal(HOME, COW);
        settings.setAnimal(HOME, COW);
        settings.setFloor(HOME, 4);
        settings.setFloor(HOME, 4);
        assertEquals(COW, settings.animal(HOME));
        assertEquals(4, settings.floor(HOME));
        assertEquals(2, dirty.get());
        settings.setAnimal(HOME, PIG);
        assertEquals(0, settings.floor(HOME), "a new animal starts from no floor");
        assertNull(settings.animal(AWAY), "other pastures are untouched");
    }

    @Test
    @DisplayName("moving carries the settings to the new corner and forgetting drops them")
    void moveAndForget() {
        settings.setAnimal(HOME, COW);
        settings.setFloor(HOME, 5);
        settings.move(HOME, AWAY);
        assertEquals(COW, settings.animal(AWAY));
        assertEquals(5, settings.floor(AWAY));
        assertNull(settings.animal(HOME), "the old corner is clear");
        settings.forget(AWAY);
        assertNull(settings.animal(AWAY));
        assertEquals(0, settings.floor(AWAY));
    }

    @Test
    @DisplayName("a saved pasture loads back with its animal and floor")
    void roundTrip() {
        settings.setAnimal(HOME, COW);
        settings.setFloor(HOME, 6);
        settings.setAnimal(AWAY, PIG);
        CompoundTag parent = new CompoundTag();
        settings.save(parent, "pasture_settings", NO_STACKS);
        PastureSettings loaded = new PastureSettings(() -> {});
        loaded.load(parent, "pasture_settings", NO_STACKS);
        assertEquals(COW, loaded.animal(HOME));
        assertEquals(6, loaded.floor(HOME));
        assertEquals(PIG, loaded.animal(AWAY));
        assertEquals(0, loaded.floor(AWAY));
    }
}
