package net.alex.guzhenren.block;

import com.mojang.serialization.MapCodec;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 知心草耕地作物方块：单格高、共 8 个生长阶段的农作物，对应种子为知心草种子。
 * <ul>
 *   <li>基础生长速度为原版小麦的 1/{@value #GROWTH_SLOWDOWN}：在 {@link #randomTick} 入口处先做一次
 *       1/{@value #GROWTH_SLOWDOWN} 的概率过滤，通过后才调用父类 {@link CropBlock} 的生长判定（保留原版光照、耕地湿润度
 *       与同种邻格减速规则），其余骨粉催熟、踩踏、碰撞箱与存活条件均与小麦一致。</li>
 *   <li>⚠ 减速概率过滤必须保留在 {@link #randomTick} 入口而非缩放原版生长速度公式：这样当种在
 *       {@link BlackSoilFarmlandBlock} 上接收 4 倍随机刻时，每次补发的随机刻都会经过同一过滤，两者相乘恰好恢复至普通小
 *       麦的基准生长速度，避免因原版公式整数取整造成倍率偏差。</li>
 * </ul>
 */

public final class IntimateGrassCropBlock extends CropBlock {

    public static final MapCodec<IntimateGrassCropBlock> CODEC = simpleCodec(IntimateGrassCropBlock::new);
    public static final int GROWTH_SLOWDOWN = 4;

    public IntimateGrassCropBlock(Properties properties) { super(properties); }

    @Override
    public @NotNull MapCodec<IntimateGrassCropBlock> codec() { return CODEC; }

    @Override
    protected void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos,
                              @NotNull RandomSource random) {
        if (random.nextInt(GROWTH_SLOWDOWN) == 0) super.randomTick(state, level, pos, random);
    }

    @Override
    protected @NotNull ItemLike getBaseSeedId() { return ModItems.INTIMATE_GRASS_SEEDS.get(); }
}
