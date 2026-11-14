package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.core.ModEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import org.jetbrains.annotations.NotNull;

/**
 * 横冲系冲撞的沿路破方块。
 * <ul>
 *   <li>冲撞（横冲蛊[Horizontal Crash Gu] / 直撞蛊[Vertical Crash Gu] / 横冲直撞蛊[Charging Crash Gu]）
 *       由 Epic Fight 在本机推动，前 2 tick 推完大半，服务端跟不上；故 {@link #clear} 在开始那 tick、发动画前破完全路，
 *       并经同一有序连接立即把破块发给冲撞者，客户端不会撞上已破方块。</li>
 *   <li>硬度上限 {@link #getMaxHardness}：发挥点数按崩裂三档（{@link AreaImpactShatter#getMaxHardness}），全力以赴升
 *       一档，两猪之力至少到顶档；无档（{@link AreaImpactShatter#NONE}）不破，冲撞照旧被墙挡住。</li>
 *   <li>路径长恰为 {@code CrashSources#getTravel}，不加余量：多清的一段会在停点前方留成通道（Alex 定：沿路只破挡路
 *       的）。只算碰撞形状与扫箱相交的方块，脚下地面、所踩下半砖、无碰撞方块（草、花、作物、火把、铁轨、红石、液体）
 *       不动；起点坑与终点圆由 {@link CrashStrike} 另破。</li>
 *   <li>着地时箱底抬一台阶（≤ 半身高），脚前一步高的边沿（土径、耕地、灵魂沙旁整块，前方台阶）可迈上，不挡路。</li>
 *   <li>{@link #mayBreak} 先问地形门槛 {@link ModEvents#canGrief}（{@code mobGriefing}），再按玩家破坏走，冒险模式、
 *       游戏管理方块、领地模组、出生点保护、世界边界都可拒；按空手掉落（战利品表不看工具，矿石照掉），创造不掉；遇首个
 *       不可破或被拒的方块即停，冲撞本身也停在那里。</li>
 *   <li>⚠ 不走 {@code Level#destroyBlock}：它每块向旁人发破坏粒子与音效，32 格冲撞最多约 136 块时太吵；改为每
 *       {@link AreaImpactShatter#PARTICLE_EVERY} 块一组碎屑，整次在首块处播一次破坏音，其余步骤（掉落、置换流体、
 *       游戏事件）照它的顺序补齐。</li>
 *   <li>⚠ 扫箱宽 {@link #DASH_WIDTH}，同 Epic Fight 冲撞期间的碰撞箱（窄于它会留下擦边的一列把冲撞挡停），高仍为站立身高；
 *       居中时占 1 列，偏离格心超过 0.1 占 2 列；路程截到 32 格，故无另设块数上限。未加载区块当挡路，不强行加载。</li>
 *   <li>创造模式破双格方块上半时同崩裂先抹掉下半（{@link AreaImpactShatter#clearLowerHalf}），否则下半连带掉落。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see CrashSources
 * @since 1.0.0
 */

public final class CrashBreach {

    private CrashBreach() {}

    public static final double DASH_WIDTH = 0.8D;
    public static final double STEP = 0.25D;
    private static final double INSET = 1.0E-3D;
    private static final double MIN_LENGTH_SQR = 1.0E-6D;

    //region Hardness [硬度]
    public static float getMaxHardness(int points, boolean twoBoars, boolean allOut) {
        float hardness = AreaImpactShatter.getMaxHardness(points);
        if (allOut) hardness = raise(hardness);
        return twoBoars ? Math.max(hardness, AreaImpactShatter.HARD_HARDNESS) : hardness;
    }

    private static float raise(float hardness) {
        if (hardness < AreaImpactShatter.SOFT_HARDNESS) return AreaImpactShatter.SOFT_HARDNESS;
        if (hardness < AreaImpactShatter.STONE_HARDNESS) return AreaImpactShatter.STONE_HARDNESS;
        return AreaImpactShatter.HARD_HARDNESS;
    }
    //endregion

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
    //endregion

    //region Break [破坏]
    public static int clear(@NotNull ServerPlayer player, @NotNull Vec3 direction, double length, float maxHardness) {
        if (maxHardness < 0.0F) return 0;

        ServerLevel level = player.serverLevel();
        List<BlockPos> broken = new ArrayList<>();
        SoundType sound = null;
        sweep:
        for (AABB box : getBoxes(getBody(player), direction, length)) {
            for (BlockPos pos : getCells(box)) {
                if (!level.hasChunkAt(pos)) break sweep;
                BlockState state = level.getBlockState(pos);
                if (!meets(level, pos, state, box)) continue;
                if (!AreaImpactShatter.canBreak(state.getDestroySpeed(level, pos), state.hasBlockEntity(), maxHardness)
                        || !mayBreak(player, level, pos, state)) break sweep;

                if (broken.isEmpty()) sound = state.getSoundType(level, pos, player);
                if (!breakQuietly(player, level, pos, state, broken.size())) break sweep;
                broken.add(pos);
            }
        }
        if (!broken.isEmpty()) AreaImpactShatter.playBreak(level, broken.getFirst(), sound);
        for (BlockPos pos : broken) player.connection.send(new ClientboundBlockUpdatePacket(level, pos));
        return broken.size();
    }

    private static AABB getBody(ServerPlayer player) {
        AABB stand = player.getBoundingBox();
        double grow = Math.max(0.0D, (DASH_WIDTH - stand.getXsize()) / 2.0D);
        AABB body = stand.inflate(grow, 0.0D, grow);
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

    private static boolean breakQuietly(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state,
                                        int index) {
        if (index % AreaImpactShatter.PARTICLE_EVERY == 0) AreaImpactShatter.showDebris(level, pos, state);
        if (player.isCreative()) AreaImpactShatter.clearLowerHalf(level, pos, state);
        else Block.dropResources(state, level, pos, null, player, ItemStack.EMPTY);
        if (!level.setBlock(pos, level.getFluidState(pos).createLegacyBlock(), Block.UPDATE_ALL)) return false;

        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        return true;
    }

    public static boolean mayBreak(@NotNull ServerPlayer player, @NotNull ServerLevel level, @NotNull BlockPos pos,
                                   @NotNull BlockState state) {
        return ModEvents.canGrief(level, player) && level.mayInteract(player, pos)
                && !CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, state)
                .isCanceled();
    }
    //endregion
}
