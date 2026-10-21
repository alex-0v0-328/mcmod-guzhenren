package net.alex.guzhenren.core;

import io.netty.handler.codec.DecoderException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The buffer reads and writes this mod adds: an enum as its ordinal, and enum-keyed maps and sets.
 *
 * <p>Provides {@code writeEnum}/{@code readEnum} (ordinal round-trip), {@code writeNullableEnum}/{@code
 * readNullableEnum} (ordinal + 1, zero = unset), {@code writeEnumMap}/{@code readEnumMap}, and {@code
 * writeEnumSet}/{@code readEnumSet}. Used by the player-data sync packets and the client-intent packets.
 *
 * <p>⚠ A nullable enum travels as its ordinal plus one, with zero meaning unset. No enum here has a
 * NONE constant to lean on, so this is where "has not chosen" becomes representable on the wire -- it
 * is what lets the two nullable {@code GuPath} fields on {@code Aperture} travel.
 *
 * <p>⚠ Decoding rejects an out-of-range ordinal with a {@code DecoderException} ({@link #byOrdinal}):
 * the client-intent packets read enums through these helpers and a forged packet can carry any VarInt,
 * so it fails as a malformed packet that disconnects the sender with a readable reason instead of a bare
 * {@code ArrayIndexOutOfBoundsException}. Vanilla {@link FriendlyByteBuf#readEnum} does not check.
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

    public static <K extends Enum<K>, V> void writeEnumMap(
            @NotNull FriendlyByteBuf buf, Map<K, V> map, FriendlyByteBuf.Writer<V> value) {
        buf.writeMap(map, ModByteBufs::writeEnum, value);
    }

    public static <K extends Enum<K>, V> Map<K, V> readEnumMap(
            @NotNull FriendlyByteBuf buf, Class<K> key, FriendlyByteBuf.Reader<V> value) {
        return buf.readMap(HashMap::new, b -> readEnum(b, key), value);
    }

    public static <E extends Enum<E>> void writeEnumSet(@NotNull FriendlyByteBuf buf, Set<E> set) {
        buf.writeCollection(set, ModByteBufs::writeEnum);
    }

    public static <E extends Enum<E>> Set<E> readEnumSet(@NotNull FriendlyByteBuf buf, Class<E> type) {
        return buf.readCollection(HashSet::new, b -> readEnum(b, type));
    }

    private static <E extends Enum<E>> E byOrdinal(Class<E> type, E[] values, int ordinal) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new DecoderException(type.getSimpleName() + " ordinal " + ordinal + " is outside 0 to "
                    + (values.length - 1));
        }
        return values[ordinal];
    }
}
