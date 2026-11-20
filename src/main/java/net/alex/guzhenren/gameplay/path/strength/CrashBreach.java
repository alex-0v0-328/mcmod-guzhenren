package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.core.ModEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
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
 * 横冲系冲撞（横冲蛊[Horizontal Crash Gu]、直撞蛊[Vertical Crash Gu]、横冲直撞蛊[Charging Crash Gu]）的沿途破块服务。
 * <ul>
 *   <li>冲撞位移由 Epic Fight 在所属客户端驱动，前 2 tick 即走完大半路程；因此服务端必须在起冲当 tick、下发动画包之前调
 *       用 {@link #clear} 一次性破除沿途可破方块，并通过同一连接立即补发 {@link ClientboundBlockUpdatePacket}，防止客户
 *       端撞上服务端已破的方块。</li>
 *   <li>可破坏硬度上限 {@link #getMaxHardness}：取力量点三档（{@link AreaDamageShatter#getMaxHardness}），两猪之力直接
 *       保底顶档（{@link AreaDamageShatter#HARD_HARDNESS}）；未入档（{@link AreaDamageShatter#NONE}）时不破块。</li>
 *   <li>扫描长度严格等于 {@link CrashSources#getTravel}（不加前伸余量，以免在停点前方多挖出空洞通道，Alex 定）：仅破坏
 *       碰撞箱与扫掠盒实际相交的方块，脚下地面、所踩下半砖以及无碰撞体积方块（草、花、火把、液体等）保持不动；地面起冲
 *       时扫掠盒底面抬高一个步高（{@code maxUpStep}，不超过半身高），允许直接迈上前方台阶或土径边沿。</li>
 *   <li>{@link #mayBreak} 依次校验 {@link ModEvents#canGrief}、{@code mayInteract} 与 NeoForge
 *       {@code BlockEvent.BreakEvent}；生存模式按空手战利品表掉落（不校验工具等级，矿石同样掉落），创造模式不掉落；遇到
 *       首个未加载区块、超硬度或无权限破坏的方块立即停止清路，玩家冲撞也随之停在该方块前。</li>
 *   <li>⚠ 为避免 32 格冲撞（最多约 136 块）逐块广播原版破坏音效与粒子造成噪音，不调用 {@code Level.destroyBlock}，改为
 *       每 {@link AreaDamageShatter#PARTICLE_EVERY} 块发一组碎屑、整次清路仅在首块位置播一次破坏音，并手动补齐掉落、流
 *       体回填与 {@code BLOCK_DESTROY} 游戏事件；创造模式破双格方块上半时同样调用
 *       {@link AreaDamageShatter#clearLowerHalf} 抑制下半掉落。</li>
 *   <li>⚠ 扫掠盒水平宽度必须取 {@link #DASH_WIDTH}（{@code 0.8}，与 Epic Fight 闪避动作期间放宽的碰撞箱一致），若按原版
 *       {@code 0.6} 宽度扫描会漏掉擦边方块并把客户端冲撞卡停。</li>
 * </ul>
 */

public final class CrashBreach {

    private CrashBreach() {}

    public static final double DASH_WIDTH = 0.8D;
    public static final double STEP = 0.25D;
    private static final double INSET = 1.0E-3D;
    private static final double MIN_LENGTH_SQR = 1.0E-6D;

    //region Hardness [硬度]
    public static float getMaxHardness(int points, boolean twoBoars) {
        float hardness = AreaDamageShatter.getMaxHardness(points);
        return twoBoars ? Math.max(hardness, AreaDamageShatter.HARD_HARDNESS) : hardness;
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
                if (!level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pos.getX()),
                        SectionPos.blockToSectionCoord(pos.getZ()))) break sweep;
                BlockState state = level.getBlockState(pos);
                if (!meets(level, pos, state, box)) continue;
                if (!AreaDamageShatter.canBreak(state.getDestroySpeed(level, pos), state.hasBlockEntity(), maxHardness)
                        || !mayBreak(player, level, pos, state)) break sweep;

                if (broken.isEmpty()) sound = state.getSoundType(level, pos, player);
                if (!breakQuietly(player, level, pos, state, broken.size())) break sweep;
                broken.add(pos);
            }
        }
        if (!broken.isEmpty()) AreaDamageShatter.playBreak(level, broken.getFirst(), sound);
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
        if (index % AreaDamageShatter.PARTICLE_EVERY == 0) AreaDamageShatter.showDebris(level, pos, state);
        if (player.isCreative()) AreaDamageShatter.clearLowerHalf(level, pos, state);
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
