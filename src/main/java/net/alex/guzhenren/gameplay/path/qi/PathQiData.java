package net.alex.guzhenren.gameplay.path.qi;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.alex.guzhenren.core.ModByteBufs;
import net.alex.guzhenren.core.Ticks;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Qi [气] holdings, sparse: a kind missing from the map is simply one the player is not holding.
 * Immutable record attachment keyed {@code qi_data}; {@code
 * PathQiService} is the only runtime writer. Each {@link
 * PathQiEntry} is a time anchor, not a running total; {@code current(kind, now)} derives the live amount.
 *
 * <p>⚠ Every read takes the current tick: an entry anchors a moment, not a balance, and a reader
 * without it reports an amount that has already drained away. ⚠ {@code holding()} is false for untimed
 * kinds (they never expire), so "currently holding" is not "amount > 0" for a timed kind past its hold.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathQiEntry
 * @see PathQiService
 * @since 1.0.0
 */

public record PathQiData(Map<QiKind, PathQiEntry> entries) {

    public static final PathQiData DEFAULT = new PathQiData(Map.of());
    public static final Codec<PathQiData> CODEC = Codec.unboundedMap(QiKind.CODEC, PathQiEntry.CODEC)
            .xmap(PathQiData::new, PathQiData::entries);

    public PathQiData {
        Map<QiKind, PathQiEntry> copy = new EnumMap<>(QiKind.class);
        copy.putAll(entries);
        entries = Collections.unmodifiableMap(copy);
    }

    public static @NotNull PathQiData decode(@NotNull FriendlyByteBuf buf) {
        return new PathQiData(ModByteBufs.readEnumMap(buf, QiKind.class, PathQiEntry::decode));
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull PathQiData value) {
        ModByteBufs.writeEnumMap(buf, value.entries(), PathQiEntry::encode);
    }

    public @Nullable PathQiEntry get(QiKind kind) { return entries.get(kind); }

    public long current(QiKind kind, long now) {
        PathQiEntry entry = entries.get(kind);
        if (entry == null) return 0L;
        if (!kind.isTimed() || now < entry.holdEndTick()) return entry.amount();

        long elapsedSeconds = (now - entry.holdEndTick()) / Ticks.SECOND;
        int tier = Math.max(0, QiKind.tierOf(entry.amount()));
        long rate = QiKind.decayPerSecond(tier);
        long decaySeconds = -Math.floorDiv(-entry.amount(), rate);
        if (elapsedSeconds >= decaySeconds) return 0L;
        return entry.amount() - rate * elapsedSeconds;
    }

    public boolean holding(QiKind kind, long now) {
        PathQiEntry entry = entries.get(kind);
        return entry != null && kind.isTimed() && now < entry.holdEndTick();
    }

    public PathQiData with(QiKind kind, PathQiEntry entry) {
        Map<QiKind, PathQiEntry> next = new EnumMap<>(QiKind.class);
        next.putAll(entries);
        next.put(kind, entry);
        return new PathQiData(next);
    }

    public PathQiData without(QiKind kind) {
        if (!entries.containsKey(kind)) return this;
        Map<QiKind, PathQiEntry> next = new EnumMap<>(QiKind.class);
        next.putAll(entries);
        next.remove(kind);
        return new PathQiData(next);
    }
}
