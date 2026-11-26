package net.alex.guzhenren.gameplay.immortalaperture.mappipeline;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Collection;
import java.util.Locale;
import net.alex.guzhenren.gameplay.immortalaperture.state.Territory;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 福地标准田与牧场的选址规则[Siting]：服务端复判与客户端地图预判共用这一份，按 {@link Reason} 的顺序逐条查，第一条不过
 * 就返回它。
 * <ul>
 *   <li>地形只经 {@link Tiles} 读 {@link MapTile}：客户端传地图仓库，服务端同步加载目标区块后用 {@link MapScanner#tile}
 *       现扫，两端同一份数据同一份规则。所需区块任一取不到就是 {@link Reason#FOG}（服务端总取得到）。</li>
 *   <li>标准田 = 一个区块（{@link #field}）：256 列地面高差 ≤ {@value #FIELD_SLOPE}，任一列有流体或方块实体即否。</li>
 *   <li>牧场 = 西北角区块起 {@value #PEN_CHUNKS}×{@value #PEN_CHUNKS} 区块（{@link #pasture}）：栅栏圈在占地
 *       {@link #penMin} 到 {@link #penMax}（30×30）周界；占地地面高差 ≤ {@value #PASTURE_SLOPE}，周界列不许有流体。
 *       </li>
 *   <li>两者都要求所占区块全在疆域里、不与秘境圆（{@link Plot#realmRadius} 为 0 = 未解出，不查）相交、不占
 *       {@link Plot#taken} 里的区块；田连规划至多 {@value #FIELD_CAP} 块，牧场至多 {@value #PASTURE_CAP} 个。</li>
 *   <li>{@link #columns} 接受任意矩形，GameTest 用测试区里的小田复判同一套地形条件。</li>
 * </ul>
 */

public final class Siting {

    private Siting() {}

    public static final int FIELD_CAP = 64;
    public static final int PASTURE_CAP = 8;
    public static final int FIELD_SLOPE = 3;
    public static final int PASTURE_SLOPE = 8;
    public static final int PEN_CHUNKS = 2;
    public static final int PEN_SIDE = 30;

    public enum Reason {
        OK, CAP, OUT_OF_BOUNDS, REALM, OVERLAP, FOG, FLUID, BLOCK_ENTITY, SLOPE;

        public @NotNull String key() { return "guzhenren.blissful_land.siting." + name().toLowerCase(Locale.ROOT); }
    }

    @FunctionalInterface
    public interface Tiles {

        @Nullable MapTile at(int chunkX, int chunkZ);
    }

    public record Plot(@NotNull Territory territory, int mountainX, int mountainZ, int realmRadius,
                       @NotNull LongSet taken, int fields, int pastures) {

        public static @NotNull Plot of(@NotNull Territory territory, int mountainX, int mountainZ, int realmRadius,
                                       @NotNull Collection<Long> fieldChunks,
                                       @NotNull Collection<Long> pastureCorners) {
            LongSet taken = new LongOpenHashSet();
            for (long chunk : fieldChunks) taken.add(chunk);
            for (long corner : pastureCorners) addPen(taken, ChunkPos.getX(corner), ChunkPos.getZ(corner), true);
            return new Plot(territory, mountainX, mountainZ, realmRadius, taken, fieldChunks.size(),
                    pastureCorners.size());
        }

        public static @NotNull Plot of(@NotNull MapModule.Section section) {
            LongSet fields = new LongOpenHashSet();
            for (MapModule.Section.Field field : section.fields()) {
                fields.add(ChunkPos.asLong(field.chunkX(), field.chunkZ()));
            }
            for (ChunkPos planned : section.planned()) fields.add(planned.toLong());
            LongSet corners = new LongOpenHashSet();
            for (MapModule.Section.Pen pen : section.pastures()) {
                corners.add(ChunkPos.asLong(pen.chunkX(), pen.chunkZ()));
            }
            return of(section.territory(), section.mountain().getX(), section.mountain().getZ(), section.realmRadius(),
                    fields, corners);
        }

        public @NotNull Plot without(int chunkX, int chunkZ) {
            LongSet rest = new LongOpenHashSet(taken);
            addPen(rest, chunkX, chunkZ, false);
            return new Plot(territory, mountainX, mountainZ, realmRadius, rest, fields, Math.max(0, pastures - 1));
        }

        private static void addPen(LongSet set, int chunkX, int chunkZ, boolean add) {
            for (int dx = 0; dx < PEN_CHUNKS; dx++) {
                for (int dz = 0; dz < PEN_CHUNKS; dz++) {
                    long chunk = ChunkPos.asLong(chunkX + dx, chunkZ + dz);
                    if (add) set.add(chunk);
                    else set.remove(chunk);
                }
            }
        }
    }

    //region Rules [规则]
    public static @NotNull Reason field(@NotNull Plot plot, @NotNull Tiles tiles, int chunkX, int chunkZ) {
        if (plot.fields() >= FIELD_CAP) return Reason.CAP;
        Reason place = place(plot, chunkX, chunkZ, 1);
        if (place != Reason.OK) return place;
        return columns(tiles, chunkX << 4, chunkZ << 4, (chunkX << 4) + 15, (chunkZ << 4) + 15, FIELD_SLOPE, false,
                true);
    }

    public static @NotNull Reason pasture(@NotNull Plot plot, @NotNull Tiles tiles, int chunkX, int chunkZ) {
        if (plot.pastures() >= PASTURE_CAP) return Reason.CAP;
        Reason place = place(plot, chunkX, chunkZ, PEN_CHUNKS);
        if (place != Reason.OK) return place;
        return columns(tiles, penMin(chunkX), penMin(chunkZ), penMax(chunkX), penMax(chunkZ), PASTURE_SLOPE, true,
                false);
    }

    public static int penMin(int chunk) { return (chunk << 4) + 1; }

    public static int penMax(int chunk) { return (chunk << 4) + PEN_SIDE; }

    public static @NotNull Reason place(@NotNull Plot plot, int chunkX, int chunkZ, int chunks) {
        for (int dx = 0; dx < chunks; dx++) {
            for (int dz = 0; dz < chunks; dz++) {
                if (!plot.territory().contains(chunkX + dx, chunkZ + dz)) return Reason.OUT_OF_BOUNDS;
            }
        }
        if (touchesRealm(plot, chunkX << 4, chunkZ << 4, (chunkX + chunks) << 4, (chunkZ + chunks) << 4)) {
            return Reason.REALM;
        }
        for (int dx = 0; dx < chunks; dx++) {
            for (int dz = 0; dz < chunks; dz++) {
                if (plot.taken().contains(ChunkPos.asLong(chunkX + dx, chunkZ + dz))) return Reason.OVERLAP;
            }
        }
        return Reason.OK;
    }

    public static @NotNull Reason columns(@NotNull Tiles tiles, int minX, int minZ, int maxX, int maxZ, int slope,
                                          boolean rimFluidOnly, boolean blockEntities) {
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                if (tiles.at(chunkX, chunkZ) == null) return Reason.FOG;
            }
        }
        boolean entity = false;
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                MapTile tile = tiles.at(x >> 4, z >> 4);
                if (tile == null) return Reason.FOG;
                boolean rim = x == minX || x == maxX || z == minZ || z == maxZ;
                if (tile.hasFlag(x, z, MapTile.FLAG_FLUID) && (rim || !rimFluidOnly)) return Reason.FLUID;
                entity |= blockEntities && tile.hasFlag(x, z, MapTile.FLAG_BLOCK_ENTITY);
                low = Math.min(low, tile.groundAt(x, z));
                high = Math.max(high, tile.groundAt(x, z));
            }
        }
        if (entity) return Reason.BLOCK_ENTITY;
        return high - low > slope ? Reason.SLOPE : Reason.OK;
    }

    private static boolean touchesRealm(Plot plot, int minX, int minZ, int endX, int endZ) {
        if (plot.realmRadius() <= 0) return false;
        double centerX = plot.mountainX() + 0.5D;
        double centerZ = plot.mountainZ() + 0.5D;
        double dx = centerX - Math.max(minX, Math.min(centerX, endX));
        double dz = centerZ - Math.max(minZ, Math.min(centerZ, endZ));
        return dx * dx + dz * dz <= (double) plot.realmRadius() * plot.realmRadius();
    }
    //endregion
}
