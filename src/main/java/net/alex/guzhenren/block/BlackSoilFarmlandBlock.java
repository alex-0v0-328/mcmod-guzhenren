package net.alex.guzhenren.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 黑土耕地方块：掺入腐泥冻土改良的耕地，使其上方作物的期望生长速度提升至普通耕地的 {@value #GROWTH_MULTIPLIER} 倍。
 * <ul>
 *   <li>继承自 {@link FarmBlock}，直接复用原版与 NeoForge 基于 {@code instanceof FarmBlock} 判定的可种植性、水源湿润、
 *       水分加成、踩踏退化等全部耕地逻辑。</li>
 *   <li>在自身每次 {@link #randomTick} 中先执行父类湿润与退化逻辑，若自身未退化为泥土，则向正上方满足 {@link #isCrop}
 *       的作物补发 {@value #EXTRA_GROWTH_TICKS} 次随机刻（由于耕地与作物被抽中随机刻的基准概率相同，叠加后作物期望获得
 *       {@value #GROWTH_MULTIPLIER} 倍随机刻，且每次生长仍遵循作物自身的光照与邻格判定）。</li>
 *   <li>{@link #isCrop} 将标签匹配（{@code #minecraft:crops}）与子类判定（{@code instanceof CropBlock}）拆入重载方法，
 *       以便单元测试独立覆盖两条分支，排除树苗或花朵等非作物植物。</li>
 *   <li>⚠ 每次补发随机刻前必须重新读取耕地自身与上方方块状态：父类随机刻可能已将无作物干涸耕地退化为泥土，而上方作物也
 *       可能在前一次随机刻中成熟并停止接收随机刻。</li>
 *   <li>⚠ 农民村民的次要工作点在原版 {@code VillagerProfession} 中硬编码为 {@code Blocks.FARMLAND}，因此在纯黑土耕地上
 *       不会触发村民自动收割与补种。</li>
 *   <li>⚠ {@link #CODEC} 的泛型类型参数必须保持为 {@link FarmBlock}，以匹配父类 {@link FarmBlock#codec()} 不带通配符的
 *       返回签名。</li>
 * </ul>
 */

public final class BlackSoilFarmlandBlock extends FarmBlock {

    public static final MapCodec<FarmBlock> CODEC = simpleCodec(BlackSoilFarmlandBlock::new);
    public static final int EXTRA_GROWTH_TICKS = 3;
    public static final int GROWTH_MULTIPLIER = EXTRA_GROWTH_TICKS + 1;

    public BlackSoilFarmlandBlock(Properties properties) { super(properties); }

    public static boolean isCrop(BlockState state) { return isCrop(state.is(BlockTags.CROPS), state.getBlock()); }

    public static boolean isCrop(boolean tagged, Block block) { return tagged || block instanceof CropBlock; }

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
