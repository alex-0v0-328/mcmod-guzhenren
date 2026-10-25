package net.alex.guzhenren.gameplay.aperture;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/**
 * Keeps every chunk a pressure-explosion crater [空窍压力爆炸] touches loaded until the crater is done, so the crater
 * finishes with no player near -- after a server restart nobody has loaded those chunks yet, and a player who
 * respawned far away no longer holds them. The square reaches one block past the crater's widest column ({@code
 * reach}): the rim pass updates the neighbors of the outermost cleared blocks, and a neighbor in an unloaded chunk
 * would load it synchronously on the server thread.
 *
 * <p>One ticket per chunk at level 33, {@code FULL} but not ticking: the blocks are readable and writable while the
 * chunk's mobs, block entities and scheduled ticks stay asleep, so a forced crater costs the load and the memory, not
 * tick time. Radius 32 to 112 holds ≈25 to 256 chunks for the tens of seconds the crater takes. {@link #isLoaded}
 * adds the tickets on its first call and asks {@code getChunkNow}, never {@code hasChunk}: the latter only reads the
 * ticket level and turns true before the chunk has finished loading. It latches once every chunk is in, because held
 * chunks never unload.
 *
 * <p>⚠ The ticket key is the explosion's seed: two craters over one chunk hold two tickets, so the first to finish
 * never unloads the other's chunks. ⚠ Tickets are never saved -- the server drops them on stop -- so a restored
 * explosion builds a fresh ticket and asks again on its first tick.
 *
 * @author Alex
 * @version 1.0.0
 * @see AperturePressureExplosionTask
 * @since 1.0.0
 */

final class CraterChunkTicket {

    CraterChunkTicket(ServerLevel level, int blockX, int blockZ, int reach, long key) {
        this.level = level;
        this.minX = SectionPos.blockToSectionCoord(blockX - reach);
        this.minZ = SectionPos.blockToSectionCoord(blockZ - reach);
        this.maxX = SectionPos.blockToSectionCoord(blockX + reach);
        this.maxZ = SectionPos.blockToSectionCoord(blockZ + reach);
        this.key = key;
    }

    private static final TicketType<Long> TYPE =
            TicketType.create(Guzhenren.id("pressure_explosion").toString(), Long::compareTo);
    private final ServerLevel level;
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final long key;
    private boolean held;
    private boolean loaded;

    boolean isLoaded() {
        if (loaded) return true;

        boolean all = true;
        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                if (!held) level.getChunkSource().addRegionTicket(TYPE, new ChunkPos(chunkX, chunkZ), 0, key);
                all &= level.getChunkSource().getChunkNow(chunkX, chunkZ) != null;
            }
        }
        held = true;
        loaded = all;
        return all;
    }

    void release() {
        if (!held) return;

        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                level.getChunkSource().removeRegionTicket(TYPE, new ChunkPos(chunkX, chunkZ), 0, key);
            }
        }
        held = false;
    }
}
