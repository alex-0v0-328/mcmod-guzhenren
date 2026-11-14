package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.core.ModEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import org.jetbrains.annotations.NotNull;

/**
 * 两猪之力冲撞的沿路破方块。
 * <ul>
 *   <li>冲撞（横冲蛊[Horizontal Crash Gu] / 直撞蛊[Vertical Crash Gu] / 横冲直撞蛊[Charging Crash Gu]）
 *       由 Epic Fight 在本机推动，前 2 tick 推完大半，服务端跟不上；故 {@link #clear} 在开始那 tick、发动画前破完全路，
 *       并经同一有序连接立即把破块发给冲撞者，客户端不会撞上已破方块。</li>
 *   <li>路径长 = {@code CrashDash#getTravel} + {@link #MARGIN}；只算碰撞形状与扫箱相交的方块，脚下地面、所踩下半砖、
 *       无碰撞方块（草、花、作物、火把、铁轨、红石、液体）不动。</li>
 *   <li>着地时箱底抬一台阶（≤ 半身高），脚前一步高的边沿（土径、耕地、灵魂沙旁整块，前方台阶）可迈上，不挡路。</li>
 *   <li>{@link #mayBreak} 先问地形门槛 {@link ModEvents#canGrief}（{@code mobGriefing}），再按玩家破坏走，冒险模式、
 *       游戏管理方块、领地模组、出生点保护、世界边界都可拒；按空手掉落，创造不掉；遇首个不可破或被拒的方块即停，
 *       冲撞本身也停在那里。</li>
 *   <li>⚠ 硬度 ≤ 3 可破石头、深板岩、木板、原木、玻璃、硬度 3 矿石（照常掉落），挡于圆石深板岩、深层矿石、铁块、
 *       黑曜石；箱宽 0.6，沿列占 1 列，跨格线占 2 列；24 格冲撞穿软地一次最多约破 100 块。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PathStrengthData#hasTwoBoars
 * @since 1.0.0
 */

public final class CrashBreach {

    private CrashBreach() {}

    public static final float MAX_HARDNESS = 3.0F;
    public static final double MARGIN = 1.0D;
    public static final double STEP = 0.25D;
    private static final double INSET = 1.0E-3D;
    private static final double MIN_LENGTH_SQR = 1.0E-6D;

    //region Path [路径]
    public static @NotNull List<AABB> getBoxes(@NotNull AABB box, @NotNull Vec3 direction, double length) {
        Vec3 level = new Vec3(direction.x, 0.0D, direction.z);
        if (level.lengthSqr() < MIN_LENGTH_SQR || !(length > 0.0D)) return List.of();

        Vec3 unit = level.normalize();
        AABB inner = box.deflate(INSET);
        int steps = Mth.ceil(length / STEP);
        List<AABB> boxes = new ArrayList<>(steps + 1);
        for (int i = 0; i <= steps; i++) boxes.add(inner.move(unit.scale(Math.min(i * STEP, length))));
        return boxes;
    }

    public static @NotNull List<BlockPos> getCells(@NotNull AABB box) {
        List<BlockPos> cells = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
            cells.add(pos.immutable());
        }
        return cells;
    }

    public static boolean canBreak(float hardness, boolean hasBlockEntity) {
        return !hasBlockEntity && hardness >= 0.0F && hardness <= MAX_HARDNESS;
    }
    //endregion

    //region Break [破坏]
    public static int clear(@NotNull ServerPlayer player, @NotNull Vec3 direction, double length) {
        ServerLevel level = player.serverLevel();
        List<BlockPos> broken = new ArrayList<>();
        sweep:
        for (AABB box : getBoxes(getBody(player), direction, length)) {
            for (BlockPos pos : getCells(box)) {
                BlockState state = level.getBlockState(pos);
                if (!meets(level, pos, state, box)) continue;
                if (!breakBlock(player, level, pos, state)) break sweep;
                broken.add(pos);
            }
        }
        for (BlockPos pos : broken) player.connection.send(new ClientboundBlockUpdatePacket(level, pos));
        return broken.size();
    }

    private static AABB getBody(ServerPlayer player) {
        AABB body = player.getBoundingBox();
        if (!player.onGround()) return body;
        return body.setMinY(body.minY + Math.min(player.maxUpStep(), body.getYsize() / 2.0D));
    }

    private static boolean meets(ServerLevel level, BlockPos pos, BlockState state, AABB box) {
        if (state.isAir()) return false;

        VoxelShape shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty()) return false;
        for (AABB part : shape.toAabbs()) {
            if (part.move(pos).intersects(box)) return true;
        }
        return false;
    }

    private static boolean breakBlock(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (!canBreak(state.getDestroySpeed(level, pos), state.hasBlockEntity())) return false;
        if (!mayBreak(player, level, pos, state)) return false;
        return level.destroyBlock(pos, !player.isCreative(), player);
    }

    public static boolean mayBreak(@NotNull ServerPlayer player, @NotNull ServerLevel level, @NotNull BlockPos pos,
                                   @NotNull BlockState state) {
        return ModEvents.canGrief(level, player) && level.mayInteract(player, pos)
                && !CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, state)
                .isCanceled();
    }
    //endregion
}
