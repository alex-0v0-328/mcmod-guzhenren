package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.alex.guzhenren.core.ModByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * Path [流派] progress, sparse: a path missing from the map is simply one nobody has walked.
 *
 * <p>Immutable record attachment keyed {@code path_data}; {@code
 * PathService} is the only runtime writer. Each value is a
 * {@link PathEntry}. ⚠ A default {@link PathEntry} (no attainment, no marks) is pruned out of the map
 * entirely, so a path that has been fully revoked simply disappears -- do not read "absent" as
 * "never touched".
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
