package net.alex.guzhenren.gameplay.aperture.pressure;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/**
 * 空窍压力爆炸坑的区块加载票管理：确保触及区块在挖掘期间常驻内存，即使无玩家在场也能挖完。
 * <ul>
 *   <li>票覆盖范围比最大水平影响半径多扩 1 格（{@code reach}），确保最后阶段对最外圈已清方块做邻居更新时，相邻区块也已
 *       加载，不会在服务端主线程触发同步阻塞加载。</li>
 *   <li>每个区块挂一张 33 级区域票（对应 {@code FULL} 状态但不执行实体与随机刻 tick），仅占内存而不增加每 tick 实体负
 *       担。</li>
 *   <li>⚠ {@link #isLoaded} 首次调用时挂票，并通过 {@code getChunkNow != null}（而非仅看票等级的 {@code hasChunk}）确
 *       认区块真正加载完毕，全部就绪后锁存结果。</li>
 *   <li>⚠ 票键取爆炸随机种子（{@code key}），使相邻两场爆炸重叠的区块各自持票，先挖完的一方释放票时不会卸载另一方仍在挖
 *       的区块；区块票不落盘，重启恢复后在首个 tick 重新挂票查询。</li>
 * </ul>
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
