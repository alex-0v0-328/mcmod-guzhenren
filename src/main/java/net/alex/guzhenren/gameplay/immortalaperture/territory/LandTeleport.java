package net.alex.guzhenren.gameplay.immortalaperture.territory;

import net.alex.guzhenren.gameplay.immortalaperture.state.LandRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 地图传送[Map Teleport]：主人在地图上右键选的点换算成落脚处并传过去；面板不关（调用方给面板置漫游）。
 * <ul>
 *   <li>{@link #landing}：先同步加载目标区块；该列真实地面（跳过 {@link LandRules#isPassable}）上方两格都没有碰撞箱
 *       就站在地面上；地面上方被低垂的树冠挡住就站到这一列最高方块上面；该列是流体或两处都站不下，就按切比雪夫距离由近到
 *       远找 {@value #SEARCH} 格内已加载区块里能站的列，都不行返回空。</li>
 *   <li>迷雾、疆域外不在这里查：调用方先看地图上是否已探明（{@code LandMaps#isKnown}），本类只管落点。</li>
 *   <li>{@link #go}：同维度传送，下坐骑、清跌落距离，朝向不变。</li>
 * </ul>
 */

public final class LandTeleport {

    public static final int SEARCH = 16;

    private LandTeleport() {}

    public static @Nullable Vec3 landing(@NotNull ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        for (int ring = 0; ring <= SEARCH; ring++) {
            for (int dz = -ring; dz <= ring; dz++) {
                int step = Math.abs(dz) == ring ? 1 : 2 * ring;
                for (int dx = -ring; dx <= ring; dx += Math.max(1, step)) {
                    int columnX = x + dx;
                    int columnZ = z + dz;
                    if (level.getChunkSource().getChunkNow(columnX >> 4, columnZ >> 4) == null) continue;
                    BlockPos stand = stand(level, columnX, columnZ);
                    if (stand != null) return Vec3.atBottomCenterOf(stand);
                }
            }
        }
        return null;
    }

    private static @Nullable BlockPos stand(ServerLevel level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
        for (; pos.getY() >= level.getMinBuildHeight(); pos.move(0, -1, 0)) {
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) return null;
            if (!LandRules.isPassable(state)) break;
        }
        if (pos.getY() < level.getMinBuildHeight()) return null;
        BlockPos ground = pos.above();
        if (free(level, ground) && free(level, ground.above())) return ground.immutable();
        BlockPos roof = new BlockPos(x, top + 1, z);
        return free(level, roof) && free(level, roof.above()) ? roof : null;
    }

    private static boolean free(ServerLevel level, BlockPos pos) {
        if (pos.getY() >= level.getMaxBuildHeight()) return false;
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isEmpty() && state.getCollisionShape(level, pos).isEmpty();
    }

    public static void go(@NotNull ServerPlayer player, @NotNull ServerLevel level, @NotNull Vec3 to) {
        player.stopRiding();
        player.teleportTo(level, to.x, to.y, to.z, player.getYRot(), player.getXRot());
        player.resetFallDistance();
    }
}
