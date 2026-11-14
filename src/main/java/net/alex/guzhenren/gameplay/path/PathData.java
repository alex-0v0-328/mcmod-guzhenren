package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.alex.guzhenren.core.ModByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 流派进度，稀疏存储：表里没有的道[Path]即没人走过。
 *
 * <ul>
 *   <li>附件 {@code path_data}，运行时只由 {@code PathService} 写入。</li>
 *   <li>⚠ 默认 {@link PathEntry}（无造诣、无纯道痕[Path Marks]）被剪除，撤尽的道也消失；不在表里不等于没碰过。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEntry
 * @see PathService
 * @since 1.0.0
 */

public record PathData(Map<GuPath, PathEntry> entries) {

    public static final PathData DEFAULT = new PathData(Map.of());
    public static final Codec<PathData> CODEC = Codec.unboundedMap(GuPath.CODEC, PathEntry.CODEC)
            .xmap(PathData::new, PathData::entries);

    public PathData {
        Map<GuPath, PathEntry> pruned = new EnumMap<>(GuPath.class);
        entries.forEach((path, entry) -> {
            if (!entry.isDefault()) pruned.put(path, entry);
        });
        entries = Collections.unmodifiableMap(pruned);
    }

    public static @NotNull PathData decode(@NotNull FriendlyByteBuf buf) {
        return new PathData(ModByteBufs.readEnumMap(buf, GuPath.class, PathEntry::decode));
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull PathData value) {
        ModByteBufs.writeEnumMap(buf, value.entries(), PathEntry::encode);
    }

    public PathEntry get(GuPath path) { return entries.getOrDefault(path, PathEntry.DEFAULT); }

    public PathData with(GuPath path, PathEntry entry) {
        Map<GuPath, PathEntry> next = new EnumMap<>(GuPath.class);
        next.putAll(entries);
        next.put(path, entry);
        return new PathData(next);
    }
}
