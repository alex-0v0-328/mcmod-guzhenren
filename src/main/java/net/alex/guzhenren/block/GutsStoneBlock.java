package net.alex.guzhenren.block;

import com.mojang.serialization.MapCodec;
import net.alex.guzhenren.gameplay.secretrealm.GutsStones;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * 胆石[Guts Stone]：荡魂山[Soulquake Mountain]上生灵死后长出的非整格小石块，一格叠 1–3 块（{@link GutsStones#STONES}），
 * 摆成三角形。
 * <ul>
 *   <li>{@link #SHAPES} 对齐模型 {@code guzhenren:block/guts_stone}、{@code _2}、{@code _3} 的方块盒，碰撞箱与选择框随块
 *       数变；模型改尺寸时同步改它。</li>
 *   <li>玩家每次敲只碎 1 块（{@link #onDestroyedByPlayer}）：多于 1 块时块数减 1，并按 1 块的状态掷一次战利品表（50%
 *       一只一转胆识蛊[Guts Gu]）；爆炸等整格移除按块数掷，每块各自 50%。</li>
 *   <li>⚠ {@link #onRemove} 在块数变化时把新块数写回本维度 {@link GutsStones}，换成别的方块时删去该格，128 块上限才会腾
 *       出名额；只更新已登记的格子，不跟踪 {@code /setblock} 放下的胆石。</li>
 * </ul>
 */

public final class GutsStoneBlock extends Block {

    public static final MapCodec<GutsStoneBlock> CODEC = simpleCodec(GutsStoneBlock::new);
    private static final VoxelShape FIRST = Block.box(3.0D, 0.0D, 3.0D, 8.0D, 2.0D, 7.0D);
    private static final VoxelShape SECOND = Block.box(9.0D, 0.0D, 5.0D, 14.0D, 2.0D, 9.0D);
    private static final VoxelShape THIRD = Block.box(5.0D, 0.0D, 9.0D, 10.0D, 2.0D, 13.0D);
    private static final VoxelShape[] SHAPES = {
            Block.box(6.0D, 0.0D, 6.0D, 11.0D, 2.0D, 10.0D),
            Shapes.or(FIRST, SECOND),
            Shapes.or(FIRST, SECOND, THIRD)
    };

    public GutsStoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GutsStones.STONES, 1));
    }

    @Override
    protected @NotNull MapCodec<GutsStoneBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(GutsStones.STONES);
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                           @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPES[state.getValue(GutsStones.STONES) - 1];
    }

    @Override
    public boolean onDestroyedByPlayer(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                       @NotNull Player player, boolean willHarvest, @NotNull FluidState fluid) {
        int stones = state.getValue(GutsStones.STONES);
        if (stones <= 1) return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);

        if (willHarvest && level instanceof ServerLevel) {
            dropResources(state.setValue(GutsStones.STONES, 1), level, pos, null, player, player.getMainHandItem());
        }
        level.setBlock(pos, state.setValue(GutsStones.STONES, stones - 1), Block.UPDATE_ALL);
        return false;
    }

    @Override
    protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                            @NotNull BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel server) {
            GutsStones stones = GutsStones.get(server);
            if (!newState.is(this)) stones.remove(pos);
            else if (stones.contains(pos)) stones.set(pos, newState.getValue(GutsStones.STONES));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
