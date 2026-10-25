package net.alex.guzhenren.gameplay.rewind;

import com.mojang.serialization.Codec;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What one chunk remembers of its own past day: a ring of {@link ChunkSnapshot}s, oldest first, kept on the chunk as
 * a data attachment so it is saved and loaded with the chunk and freed when the chunk unloads.
 *
 * <p>Only a chunk near the carrier of a refined Mountain or River As Before Gu [山如故 / 江如故] records anything
 * ({@code ChunkRewindService.recordAround}); every other chunk stays {@link #EMPTY} and writes nothing to its file.
 * The ring follows {@link RewindClock}: one snapshot per interval, {@code RING_SIZE} kept, and {@link #pick} the one a
 * day back.
 *
 * @author Alex
 * @version 1.0.0
 * @see ChunkRewindService
 * @since 1.0.0
 */

public record ChunkMemory(List<ChunkSnapshot> snapshots) {

    public static final ChunkMemory EMPTY = new ChunkMemory(List.of());
    public static final Codec<ChunkMemory> CODEC = ChunkSnapshot.CODEC.listOf()
            .xmap(ChunkMemory::new, ChunkMemory::snapshots);

    public ChunkMemory {
        snapshots = List.copyOf(snapshots);
    }

    public boolean isEmpty() { return snapshots.isEmpty(); }

    public long getLastTakenAt() { return RewindClock.lastTakenAt(snapshots, ChunkSnapshot::takenAt); }

    public @Nullable ChunkSnapshot pick(long now) { return RewindClock.pick(snapshots, ChunkSnapshot::takenAt, now); }

    public @NotNull ChunkMemory with(@NotNull ChunkSnapshot next) {
        return new ChunkMemory(RewindClock.add(snapshots, next, ChunkSnapshot::takenAt));
    }
}
