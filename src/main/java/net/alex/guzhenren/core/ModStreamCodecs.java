package net.alex.guzhenren.core;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

/**
 * The stream codecs this mod adds: an enum as its ordinal, and enum-keyed maps and sets.
 *
 * <p>Provides {@code ofEnum} (ordinal round-trip), {@code ofNullableEnum} (ordinal + 1, zero = unset),
 * {@code enumMap}, and {@code enumSet}. Used by the attachment stream codecs and the client-intent payloads.
 *
 * <p>⚠ A nullable enum travels as its ordinal plus one, with zero meaning unset. No enum here has a
 * NONE constant to lean on, so this is where "has not chosen" becomes representable on the wire -- it
 * is what lets the two nullable {@code GuPath} fields on {@code Aperture} travel.
 *
 * <p>⚠ Decoding rejects an out-of-range ordinal with a {@code DecoderException} ({@link #byOrdinal}):
 * the client-intent payloads read enums through these codecs and a forged packet can carry any
 * {@code VAR_INT}, so it fails as a malformed packet that disconnects the sender with a readable reason
 * instead of a bare {@code ArrayIndexOutOfBoundsException}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModStreamCodecs {

    private ModStreamCodecs() {}

    public static <E extends Enum<E>> StreamCodec<ByteBuf, E> ofEnum(Class<E> type) {
        E[] values = type.getEnumConstants();
        return ByteBufCodecs.VAR_INT.map(ordinal -> byOrdinal(type, values, ordinal), Enum::ordinal);
    }

    public static <E extends Enum<E>> StreamCodec<ByteBuf, @Nullable E> ofNullableEnum(Class<E> type) {
        E[] values = type.getEnumConstants();
        return ByteBufCodecs.VAR_INT.map(i -> i == 0 ? null : byOrdinal(type, values, i - 1),
                value -> value == null ? 0 : value.ordinal() + 1);
    }

    public static <E extends Enum<E>> void writeNullableEnum(ByteBuf buf, @Nullable E value) {
        ByteBufCodecs.VAR_INT.encode(buf, value == null ? 0 : value.ordinal() + 1);
    }

    public static <E extends Enum<E>> @Nullable E readNullableEnum(ByteBuf buf, Class<E> type) {
        int i = ByteBufCodecs.VAR_INT.decode(buf);
        return i == 0 ? null : byOrdinal(type, type.getEnumConstants(), i - 1);
    }

    private static <E extends Enum<E>> E byOrdinal(Class<E> type, E[] values, int ordinal) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new DecoderException(type.getSimpleName() + " ordinal " + ordinal + " is outside 0.."
                    + (values.length - 1));
        }
        return values[ordinal];
    }

    public static <K extends Enum<K>, V> StreamCodec<ByteBuf, Map<K, V>> enumMap(
            Class<K> key, StreamCodec<ByteBuf, V> value) {
        return ByteBufCodecs.map(HashMap::new, ofEnum(key), value);
    }

    public static <E extends Enum<E>> StreamCodec<ByteBuf, Set<E>> enumSet(Class<E> type) {
        return ByteBufCodecs.collection(HashSet::new, ofEnum(type));
    }
}
