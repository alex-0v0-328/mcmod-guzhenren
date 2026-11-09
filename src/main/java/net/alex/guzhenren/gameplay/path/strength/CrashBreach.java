package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.List;
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
 * The two boars' strength [两猪之力] breaking its way along a Crash Gu dash [横冲 / 直撞 / 横冲直撞]: a body holding
 * the white and the black boar ({@link PathStrengthData#hasTwoBoars}) clears the path its box sweeps before the dash
 * moves it. Epic Fight drives the dash on the owning client and carries most of it in the first two ticks, so the
 * server cannot follow the body and break as it goes: {@link #clear} breaks the whole path on the tick the dash starts,
 * before the animation is sent, and sends the dasher each broken block at once, on the same ordered connection, so
 * the client never meets a block the server already broke.
 *
 * <p>{@link #getBoxes} sweeps the body's box along the level direction, {@link #STEP} blocks at a time, up to the
 * length the caller gives (the dash's {@code CrashDash#getTravel} plus {@link #MARGIN}). A block counts only where its
 * collision shape meets a swept box, so the floor underfoot, a bottom slab stood on and anything without collision
 * (grass, flowers, crops, torches, rails, redstone, fluids) stay. On the ground the box starts one step up
 * ({@code maxUpStep}, at most half the body's height): the floor's edge a step below the feet (a full block beside the
 * dirt path, farmland or soul sand stood on, a slab ahead) is one the dash steps onto, never a block in its way. Each
 * one met breaks when it is
 * {@link #canBreak}: hardness from zero to {@link #MAX_HARDNESS}, no block entity. It breaks as a player's break would
 * reach the world: {@code CommonHooks.fireBlockBreak} (adventure mode, game master blocks and every claim mod's
 * {@code BlockEvent.BreakEvent} can refuse it), spawn protection and the world border through
 * {@code mayInteract}, and its loot dropped as with a bare hand, none in creative. The first block met that cannot
 * break, or whose break is refused, stops the path there: the dash will meet it anyway.
 *
 * <p>⚠ {@link #MAX_HARDNESS} breaks stone, deepslate, planks, logs, glass and the hardness-3 ores (their loot drops)
 * and stops at cobbled deepslate, the deepslate ores, iron blocks and obsidian. The box is 0.6 wide, one column when it
 * runs along one and two when it straddles a block line, so a 24-block dash through soft ground breaks at most about
 * 100 blocks at once.
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
        if (!level.mayInteract(player, pos)) return false;
        if (CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, state)
                .isCanceled()) {
            return false;
        }
        return level.destroyBlock(pos, !player.isCreative(), player);
    }
    //endregion
}
