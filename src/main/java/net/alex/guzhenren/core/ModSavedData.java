package net.alex.guzhenren.core;

import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

/**
 * The base of every saved-data file this mod keeps on a level: a subclass writes itself into a tag
 * ({@link #write}) and is read back by the function it hands {@link #get}.
 *
 * <p>Item stacks go through the {@link StackIo} both sides receive, because writing a stack needs registry
 * context on one game version and not on the other.
 *
 * <p>⚠ A version seam: subclasses override {@link #write} only and fetch through {@link #get}, so they stay
 * identical between the NeoForge 1.21.1 mainline and the Forge 1.20.1 port.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public abstract class ModSavedData extends SavedData {

    /** Writes and reads one non-empty item stack inside a saved-data tag. */
    public interface StackIo {

        @NotNull Tag save(@NotNull ItemStack stack);

        @NotNull ItemStack load(@NotNull CompoundTag tag);
    }

    private static final StackIo STACK_IO = new StackIo() {
        @Override
        public @NotNull Tag save(@NotNull ItemStack stack) { return stack.save(new CompoundTag()); }

        @Override
        public @NotNull ItemStack load(@NotNull CompoundTag tag) { return ItemStack.of(tag); }
    };

    protected abstract void write(@NotNull CompoundTag tag, @NotNull StackIo stacks);

    @Override
    public final @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        write(tag, STACK_IO);
        return tag;
    }

    public static <T extends ModSavedData> @NotNull T get(@NotNull ServerLevel level, @NotNull String name,
            @NotNull Supplier<T> create, @NotNull BiFunction<CompoundTag, StackIo, T> read) {
        return level.getDataStorage().computeIfAbsent(tag -> read.apply(tag, STACK_IO), create, name);
    }
}
