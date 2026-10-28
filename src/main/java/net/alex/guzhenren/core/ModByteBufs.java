package net.alex.guzhenren.core;

import io.netty.handler.codec.DecoderException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The buffer reads and writes this mod adds: an enum as its ordinal, and enum-keyed maps and sets.
 *
 * <p>Provides {@code writeEnum}/{@code readEnum} (ordinal round-trip), {@code writeNullableEnum}/{@code
 * readNullableEnum} (ordinal + 1, zero = unset), {@code writeEnumMap}/{@code readEnumMap}, and {@code
 * writeEnumSet}/{@code readEnumSet}; {@link #enumCodec} and {@link #nullableEnumCodec} are the first two as stream
 * codecs, for the payloads' {@code StreamCodec.composite}. A record that syncs as an attachment or a data component
 * writes itself through these from its own {@code encode}/{@code decode}. A collection is its VarInt size, then each
 * element.
 *
 * <p>⚠ A nullable enum travels as its ordinal plus one, with zero meaning unset. No enum here has a
 * NONE constant to lean on, so this is where "has not chosen" becomes representable on the wire -- it
 * is what lets the two nullable {@code GuPath} fields on {@code Aperture} travel.
 *
 * <p>⚠ Decoding rejects an out-of-range ordinal with a {@code DecoderException} ({@link #byOrdinal}):
 * the client-intent packets read enums through these helpers and a forged packet can carry any VarInt,
 * so it fails as a malformed packet that disconnects the sender with a readable reason instead of a bare
 * {@code ArrayIndexOutOfBoundsException}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModByteBufs {

    private ModByteBufs() {}

    public static void writeEnum(@NotNull FriendlyByteBuf buf, @NotNull Enum<?> value) {
        buf.writeVarInt(value.ordinal());
    }

    public static <E extends Enum<E>> @NotNull E readEnum(@NotNull FriendlyByteBuf buf, Class<E> type) {
        return byOrdinal(type, type.getEnumConstants(), buf.readVarInt());
    }

    public static void writeNullableEnum(@NotNull FriendlyByteBuf buf, @Nullable Enum<?> value) {
        buf.writeVarInt(value == null ? 0 : value.ordinal() + 1);
    }

    public static <E extends Enum<E>> @Nullable E readNullableEnum(@NotNull FriendlyByteBuf buf, Class<E> type) {
        int i = buf.readVarInt();
        return i == 0 ? null : byOrdinal(type, type.getEnumConstants(), i - 1);
    }

    public static <E extends Enum<E>> @NotNull StreamCodec<FriendlyByteBuf, E> enumCodec(Class<E> type) {
        return StreamCodec.of(ModByteBufs::writeEnum, buf -> readEnum(buf, type));
    }

    public static <E extends Enum<E>> @NotNull StreamCodec<FriendlyByteBuf, E> nullableEnumCodec(Class<E> type) {
        return StreamCodec.of(ModByteBufs::writeNullableEnum, buf -> readNullableEnum(buf, type));
    }

    public static <K extends Enum<K>, V> void writeEnumMap(
            @NotNull FriendlyByteBuf buf, Map<K, V> map, BiConsumer<FriendlyByteBuf, V> value) {
        buf.writeVarInt(map.size());
        for (Map.Entry<K, V> entry : map.entrySet()) {
            writeEnum(buf, entry.getKey());
            value.accept(buf, entry.getValue());
        }
    }

    public static <K extends Enum<K>, V> Map<K, V> readEnumMap(
            @NotNull FriendlyByteBuf buf, Class<K> key, Function<FriendlyByteBuf, V> value) {
        int size = buf.readVarInt();
        Map<K, V> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            K k = readEnum(buf, key);
            map.put(k, value.apply(buf));
        }
        return map;
    }

    public static <E extends Enum<E>> void writeEnumSet(@NotNull FriendlyByteBuf buf, Collection<E> set) {
        buf.writeVarInt(set.size());
        for (E e : set) writeEnum(buf, e);
    }

    public static <E extends Enum<E>> Set<E> readEnumSet(@NotNull FriendlyByteBuf buf, Class<E> type) {
        int size = buf.readVarInt();
        Set<E> set = new HashSet<>();
        for (int i = 0; i < size; i++) set.add(readEnum(buf, type));
        return set;
    }

    private static <E extends Enum<E>> E byOrdinal(Class<E> type, E[] values, int ordinal) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new DecoderException(type.getSimpleName() + " ordinal " + ordinal + " is outside 0 to "
                    + (values.length - 1));
        }
        return values[ordinal];
    }
}
