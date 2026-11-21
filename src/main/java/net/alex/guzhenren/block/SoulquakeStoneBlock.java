package net.alex.guzhenren.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 荡魂山石[Soulquake Stone]：荡魂山[Soulquake Mountain]整座山体唯一的半透明方块，由 Gu World 结构片逐列写入。
 * <ul>
 *   <li>继承 {@link HalfTransparentBlock}，与同种方块相邻的面不画：约 29 万格山体只渲染外表面；属性在
 *       {@link net.alex.guzhenren.registry.block.ModBlocks#SOULQUAKE_STONE} 注册处设定（不可破坏、爆炸抗性同基岩、禁止
 *       刷怪、活塞推不动、无战利品表、无物品形态）。</li>
 *   <li>⚠ 性能：{@link #getLightBlock} 恒为 15 且不发光、不随机刻、无方块实体，山体内部不做光照传播；
 *       {@code noOcclusion} 只为让山后方块透出来。半透明靠模型的 {@code render_type: translucent}，不在客户端代码注册。</li>
 *   <li>⚠ 凋灵、末影龙、地物替换的豁免靠方块标签（{@code ModBlockTagsProvider}），本类不覆写破坏判定。</li>
 * </ul>
 */

public final class SoulquakeStoneBlock extends HalfTransparentBlock {

    public static final MapCodec<SoulquakeStoneBlock> CODEC = simpleCodec(SoulquakeStoneBlock::new);

    public SoulquakeStoneBlock(Properties properties) { super(properties); }

    @Override
    protected @NotNull MapCodec<SoulquakeStoneBlock> codec() { return CODEC; }

    @Override
    protected int getLightBlock(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return level.getMaxLightLevel();
    }
}
