package net.alex.guzhenren.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 黑土耕地：掺了腐泥冻土的耕地，其上作物期望快 {@value #GROWTH_MULTIPLIER} 倍。
 *
 * <ul>
 *   <li>继承 {@code FarmBlock} 即得原版耕地的种植、湿润加成、湿润、踩踏、退成泥土：原版与 NeoForge 认这些都用
 *       {@code instanceof FarmBlock}。</li>
 *   <li>⚠ 农民村民例外：其次要工作点写死为原版耕地（{@code VillagerProfession}），纯黑土田里既不收割也不补种。</li>
 *   <li>加速：自己每个随机刻，先走父类，再给正上方作物补 {@value #EXTRA_GROWTH_TICKS} 个随机刻。耕地与作物被抽中的频率
 *       相同，故作物期望得 {@value #GROWTH_MULTIPLIER} 倍随机刻；光照、邻格减速等判定仍由作物自己做。</li>
 *   <li>⚠ 每补一刻前重读上方方块：作物可能刚成熟（不再随机刻）或被自己的随机刻换掉；父类可能已把自己退成泥土，此时
 *       不补。</li>
 *   <li>作物指 {@code #minecraft:crops} 标签或 {@code CropBlock} 子类；能种在耕地上的树苗、花不算。</li>
 *   <li>⚠ {@link #CODEC} 的类型参数只能是 {@code FarmBlock}：父类 {@code codec()} 的返回类型不带通配符。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class BlackSoilFarmlandBlock extends FarmBlock {

    public static final MapCodec<FarmBlock> CODEC = simpleCodec(BlackSoilFarmlandBlock::new);
    public static final int EXTRA_GROWTH_TICKS = 3;
    public static final int GROWTH_MULTIPLIER = EXTRA_GROWTH_TICKS + 1;

    public BlackSoilFarmlandBlock(Properties properties) { super(properties); }

    public static boolean isCrop(BlockState state) {
        return state.is(BlockTags.CROPS) || state.getBlock() instanceof CropBlock;
    }

    @Override
    public @NotNull MapCodec<FarmBlock> codec() { return CODEC; }

    @Override
    protected void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos,
                              @NotNull RandomSource random) {
        super.randomTick(state, level, pos, random);
        if (!level.getBlockState(pos).is(this)) return;

        BlockPos above = pos.above();
        for (int tick = 0; tick < EXTRA_GROWTH_TICKS; tick++) {
            BlockState crop = level.getBlockState(above);
            if (!isCrop(crop) || !crop.isRandomlyTicking()) return;

            crop.randomTick(level, above, random);
        }
    }
}
