package net.alex.guzhenren.gameplay.aperture.pressure;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/**
 * 空窍[Aperture]压力爆炸坑的区块票：触及区块载到挖完，无人在场也挖完。
 * <ul>
 *   <li>重启后无人加载这些区块，远处重生者也不再占着；方形范围比最宽列多一格（{@code reach}）：
 *       边缘更新触及最外已清方块的邻居，未载区块会在服务端线程同步加载。</li>
 *   <li>每区块一张 33 级票，{@code FULL} 但不 tick：方块可读写，生物、方块实体、计划刻休眠；只耗加载与内存，不耗 tick；
 *       半径 32 至 112 成形的几十秒里占约 25 至 256 个区块。</li>
 *   <li>{@link #isLoaded} 首调加票，查 {@code getChunkNow} 不查 {@code hasChunk}：后者只看票等级，未载完即为真；
 *       到齐后锁存，持票区块不卸。</li>
 *   <li>⚠ 票键为爆炸种子：两坑共区块各持一票，先完者不卸另一坑的区块。</li>
 *   <li>⚠ 票不存档，停服即丢；恢复的爆炸首 tick 新建票重询。</li>
 * </ul>
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
