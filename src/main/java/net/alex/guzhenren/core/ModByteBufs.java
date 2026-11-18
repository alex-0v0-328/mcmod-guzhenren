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
 * 面向枚举及以枚举为元素的映射、集合的网络字节流编解码工具。
 * <ul>
 *   <li>枚举按 {@link Enum#ordinal()} 读写 VarInt，集合与映射先写 VarInt 元素数量再逐项序列化；{@link #enumCodec} 与
 *       {@link #nullableEnumCodec} 提供对应的 {@link StreamCodec} 包装，供网络 Payload 组合使用。</li>
 *   <li>⚠ 可空枚举（{@link #writeNullableEnum} / {@link #readNullableEnum}）以 {@code 0} 表示 {@code null}、非空值写入
 *       {@code ordinal + 1}：因为业务枚举（如 {@code GuPath}）不设占位的 {@code NONE} 常量，「未选择」状态完全靠该编码
 *       表达。</li>
 *   <li>⚠ {@link #byOrdinal} 遇到越界序号时统一抛出 {@link DecoderException} 而非数组越界异常，确保收到伪造的客户端意图
 *       包时能由网络层按协议格式错误安全断开连接并记录原因。</li>
 * </ul>
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
