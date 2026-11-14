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
 * 模组新增的缓冲区读写：枚举按序号，及枚举为键的映射、集合。
 * <ul>
 *   <li>集合先写 VarInt 长度再逐个写元素；{@link #enumCodec}、{@link #nullableEnumCodec} 是枚举、可空枚举的 stream
 *       codec 形式，供 payload 的 {@code StreamCodec.composite}。</li>
 *   <li>作为附件或数据组件同步的 record 在自己的 {@code encode}/{@code decode} 里调用这些方法。</li>
 *   <li>⚠ 可空枚举写序号加一，0 表示未设：枚举都没有 NONE 常量，“尚未选择”只能在此表示；{@code Aperture} 的两个可空
 *       {@code GuPath} 字段靠它传输。</li>
 *   <li>⚠ 越界序号抛 {@code DecoderException}（{@link #byOrdinal}）：客户端意图包经此读枚举，伪造包可带任意 VarInt；这
 *       样按格式错误包断开发送方并给出可读原因，而非裸 {@code ArrayIndexOutOfBoundsException}。</li>
 * </ul>
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
        //noinspection DataFlowIssue
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
