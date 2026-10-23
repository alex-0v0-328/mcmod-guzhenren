package net.alex.guzhenren.registry.item;

import com.mojang.serialization.Codec;
import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.NbtCodecs;
import net.alex.guzhenren.item.gu.RefinedGuState;
import net.alex.guzhenren.item.gu.mortal.wood.LeafGrowth;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The item components this mod adds, for state belonging to one particular stack.
 *
 * <p>Keys: {@link RefinedGuState}, {@code VITAL_OWNER}, {@code VITAL_APERTURE}, {@code VITAL_TOKEN},
 * {@code STORED_STONES}, {@code REFINED_AT}, {@code AWAKEN_BASE}, {@code USED_AT}, {@code HEAL_BANK} and {@link
 * LeafGrowth}. A component here is for state that not every tended Gu shares; anything all of them carry belongs on the
 * shared state record instead.
 *
 * <p>⚠ Game-clock timestamps default to {@code 0}, which is a real game time -- judge {@code null},
 * never {@code 0}, or a fresh world reads "already used".
 *
 * <p>⚠ A version seam: callers read and write through {@link #get}, {@link #getOrDefault}, {@link #set}, {@link #has}
 * and {@link #remove} only, never the stack's own data API, so they stay identical between the NeoForge 1.21.1
 * mainline (data components) and this Forge 1.20.1 port. Here each key is an entry {@code guzhenren:<id>} in the
 * stack's tag, written through the key's {@link Codec}; removing the last one drops the whole tag, so a stack that
 * lost its data stacks with a fresh one again.
 *
 * @author Alex
 * @version 1.0.0
 * @see RefinedGuState
 * @since 1.0.0
 */

public final class ModDataComponents {

    private ModDataComponents() {}

    /** One kind of per-stack data: its tag entry name and how its value is written. */
    public record Key<T>(String name, Codec<T> codec) {}

    public static final Key<RefinedGuState> REFINED_GU_STATE = key("refined_gu_state", RefinedGuState.CODEC);
    public static final Key<UUID> VITAL_OWNER = key("vital_owner", UUIDUtil.CODEC);
    public static final Key<Integer> VITAL_APERTURE = key("vital_aperture", Codec.INT);
    public static final Key<Long> VITAL_TOKEN = key("vital_token", Codec.LONG);
    public static final Key<Long> STORED_STONES = key("stored_stones", Codec.LONG);
    public static final Key<Long> REFINED_AT = key("refined_at", Codec.LONG);
    public static final Key<Integer> AWAKEN_BASE = key("awaken_base", Codec.INT);
    public static final Key<Long> USED_AT = key("used_at", Codec.LONG);
    public static final Key<Integer> HEAL_BANK = key("heal_bank", Codec.INT);
    public static final Key<LeafGrowth> LEAF_GROWTH = key("leaf_growth", LeafGrowth.CODEC);

    public static <T> @Nullable T get(@NotNull ItemStack stack, @NotNull Key<T> key) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return null;
        Tag encoded = tag.get(key.name());
        if (encoded == null) return null;
        return NbtCodecs.decode(key.codec(), encoded, "read item data", key.name()).orElse(null);
    }

    public static <T> T getOrDefault(@NotNull ItemStack stack, @NotNull Key<T> key, T fallback) {
        T value = get(stack, key);
        return value == null ? fallback : value;
    }

    public static boolean has(@NotNull ItemStack stack, @NotNull Key<?> key) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(key.name());
    }

    /** Stores {@code value}; {@code null} removes the entry. */
    public static <T> void set(@NotNull ItemStack stack, @NotNull Key<T> key, @Nullable T value) {
        if (value == null) {
            remove(stack, key);
            return;
        }
        NbtCodecs.encode(key.codec(), value, "write item data", key.name())
                .ifPresent(encoded -> stack.getOrCreateTag().put(key.name(), encoded));
    }

    public static void remove(@NotNull ItemStack stack, @NotNull Key<?> key) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;
        tag.remove(key.name());
        if (tag.isEmpty()) stack.setTag(null);
    }

    private static <T> Key<T> key(String id, Codec<T> codec) { return new Key<>(Guzhenren.id(id).toString(), codec); }
}
