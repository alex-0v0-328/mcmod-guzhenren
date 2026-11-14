package net.alex.guzhenren.block;

import java.util.List;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

/**
 * 元泉[Spirit Spring]液体方块：原版水行为，加产元石[Primeval Stone]的心跳。
 *
 * <ul>
 *   <li>产出走方块计划 tick，不走流体流动 tick，不与 {@code LiquidBlock} 每 5 tick 的流动重排相撞。</li>
 *   <li>产出链由 {@code onPlace}、{@code neighborChanged} 挂起；重载后由源头随机 tick
 *       （{@link SpiritSpringFluid.Source}）重挂，因静止源头收不到计划 tick。</li>
 *   <li>⚠ 下列五个常量是 Alex 定值，非随手可调参数：每源头每 100 tick 产一组元石，永不枯竭；上限半径内已满一组则暂停；
 *       仅 128 格内有存活非旁观玩家时产出。</li>
 *   <li>128 格即自然刷怪视距，用原版 {@code EntityGetter#hasNearbyAlivePlayer}（同刷怪笼）；计划 tick 在不 tick 的区块
 *       本就停转，显式门槛另管常加载区块（出生点、强制加载）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class SpiritSpringBlock extends LiquidBlock {

    public static final int PRODUCTION_INTERVAL_TICKS = 5 * Ticks.SECOND;
    public static final int STONES_PER_PRODUCTION = 64;
    public static final double NEARBY_STONES_CAP_RADIUS = 4.0;
    public static final int NEARBY_STONES_CAP = STONES_PER_PRODUCTION;
    public static final double PRODUCTION_PLAYER_RANGE = 128.0;

    public SpiritSpringBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level,
                        @NotNull BlockPos pos, @NotNull BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        scheduleProduction(level, pos);
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level level,
                                @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos,
                                boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        scheduleProduction(level, pos);
    }

    @Override
    public void tick(@NotNull BlockState state, ServerLevel level,
                     @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (!level.getFluidState(pos).isSource()) return;
        boolean playerNear = level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                PRODUCTION_PLAYER_RANGE);
        if (playerNear && shouldProduce(true, countNearbyStones(level, pos))) {
            produceStones(level, pos, random);
        }
        scheduleProduction(level, pos);
    }

    static boolean shouldProduce(boolean playerNear, int nearbyStones) {
        return playerNear && nearbyStones < NEARBY_STONES_CAP;
    }

    void scheduleProduction(Level level, BlockPos pos) {
        if (level.isClientSide() || !level.getFluidState(pos).isSource()) return;
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, PRODUCTION_INTERVAL_TICKS);
        }
    }

    private void produceStones(ServerLevel level, BlockPos pos, RandomSource random) {
        int maxStack = new ItemStack(ModItems.PRIMEVAL_STONE.get()).getMaxStackSize();
        int remaining = STONES_PER_PRODUCTION;
        while (remaining > 0) {
            int batch = Math.min(remaining, maxStack);
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                    new ItemStack(ModItems.PRIMEVAL_STONE.get(), batch));
            drop.setDefaultPickUpDelay();
            drop.setDeltaMovement((random.nextDouble() - 0.5) * 0.2, 0.2, (random.nextDouble() - 0.5) * 0.2);
            level.addFreshEntity(drop);
            remaining -= batch;
        }
    }

    public static int countNearbyStones(ServerLevel level, BlockPos pos) {
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class,
                new AABB(pos).inflate(NEARBY_STONES_CAP_RADIUS));
        int total = 0;
        for (ItemEntity drop : drops) {
            if (drop.getItem().is(ModItems.PRIMEVAL_STONE.get())) total += drop.getItem().getCount();
        }
        return total;
    }
}
