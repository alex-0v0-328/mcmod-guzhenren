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
 * 知心草作物：一格高、8 个阶段的耕地作物，种子为知心草种子。
 *
 * <ul>
 *   <li>底速是小麦的 1/{@value #GROWTH_SLOWDOWN}：每次随机刻先过 1/{@value #GROWTH_SLOWDOWN} 判定，过了才走原版
 *       {@code CropBlock} 的生长判定（光照、耕地湿润、邻格同种减速照旧），期望耗时恰为小麦的
 *       {@value #GROWTH_SLOWDOWN} 倍。</li>
 *   <li>⚠ 判定必须留在 {@link #randomTick} 入口：黑土耕地靠给上方作物补随机刻来加速，补的每一刻都要过这道判定，
 *       两者相乘才回到小麦速度；改成缩放原版概率会因取整偏离 4 倍。</li>
 *   <li>骨粉、踩踏、形状、存活条件全同小麦；数值与理由见 wiki《植物与采集》。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
