package net.alex.guzhenren.block;

import java.util.List;
import net.alex.guzhenren.gameplay.immortalaperture.spring.SpringRules;
import net.alex.guzhenren.gameplay.immortalaperture.spring.SpringWork;
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
 * 元泉液体方块：在原版水液体方块的基础上驱动定时产出元石[Primeval Stone]的计划刻循环。
 * <ul>
 *   <li>元石产出通过方块计划刻（{@link #tick}，间隔 {@link #PRODUCTION_INTERVAL_TICKS}）而非流体流动刻调度，避免与
 *       {@link LiquidBlock} 每 5 tick 的流体扩散重排互相干扰。</li>
 *   <li>产出链在方块放置（{@link #onPlace}）或邻格更新（{@link #neighborChanged}）时挂起；区块重加载或结构生成后的静态
 *       源头若丢失方块计划刻，则由流体源头的随机刻（{@link SpiritSpringFluid.Source}）兜底重新唤醒。</li>
 *   <li>每次触发时要求 {@link #PRODUCTION_PLAYER_RANGE}（128 格，对齐原版自然刷怪视距）内存在存活玩家，且周围
 *       {@link #NEARBY_STONES_CAP_RADIUS} 半径内的元石掉落物总数未达到上限 {@link #NEARBY_STONES_CAP}，防止在出生点或强
 *       制加载区块中无人时无限堆积掉落物。</li>
 *   <li>福地有主时福地里的元泉不弹掉落物（{@link SpringWork#collects}）：产出由地灵按登记的泉位结算进库藏，运行时放下的
 *       源方块在 {@link #onPlace} 登记；节拍与每次颗数取 {@link SpringRules}，与地灵的结算同源。</li>
 * </ul>
 */

public class SpiritSpringBlock extends LiquidBlock {

    public static final int PRODUCTION_INTERVAL_TICKS = SpringRules.PRODUCTION_INTERVAL_TICKS;
    public static final int STONES_PER_PRODUCTION = SpringRules.STONES_PER_PRODUCTION;
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
        if (level instanceof ServerLevel server) SpringWork.onPlaced(server, pos);
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
        boolean collected = SpringWork.collects(level);
        boolean playerNear = !collected && level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5,
                pos.getZ() + 0.5, PRODUCTION_PLAYER_RANGE);
        if (shouldProduce(collected, playerNear, playerNear ? countNearbyStones(level, pos) : 0)) {
            produceStones(level, pos, random);
        }
        scheduleProduction(level, pos);
    }

    static boolean shouldProduce(boolean playerNear, int nearbyStones) {
        return playerNear && nearbyStones < NEARBY_STONES_CAP;
    }

    static boolean shouldProduce(boolean collected, boolean playerNear, int nearbyStones) {
        return !collected && shouldProduce(playerNear, nearbyStones);
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
