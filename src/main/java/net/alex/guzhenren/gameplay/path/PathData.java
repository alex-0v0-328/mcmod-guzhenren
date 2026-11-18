package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.alex.guzhenren.core.ModByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家流派进度附件（键 {@code path_data}）：按流派稀疏存储造诣与裸道痕[Path Marks]（{@link PathEntry}）。
 * <ul>
 *   <li>运行时仅由 {@link PathService} 写入。</li>
 *   <li>⚠ 紧凑构造器会自动剔除处于默认状态（{@link PathEntry#isDefault}，即无造诣且无裸道痕）的流派条目，因此映射表中不
 *       存在某条流派仅代表当前值为空，不代表玩家从未接触过该流派。</li>
 * </ul>
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
