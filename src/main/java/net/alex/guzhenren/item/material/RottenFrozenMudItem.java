package net.alex.guzhenren.item.material;

import java.util.Collection;
import net.alex.guzhenren.block.BlackSoilFarmlandBlock;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 腐泥冻土[Rotten Frozen Mud]物品：右键原版耕地或其上方作物，可将对应耕地掺转化为黑土耕地[Black Soil Farmland]。
 * <ul>
 *   <li>因需处理对方块的右键交互才单开叶子类：仅识别 {@code minecraft:farmland}，其余方块直接放行，已转化为黑土耕地者
 *       不可重复掺杂。</li>
 *   <li>点击作物时自动定位其正下方那格耕地，作物的判定范围与 {@link BlackSoilFarmlandBlock#isCrop} 保持一致。</li>
 *   <li>方块替换仅在服务端执行，并完整继承原耕地的湿润度属性。</li>
 *   <li>⚠ 替换方块时携带 {@code UPDATE_KNOWN_SHAPE} 标志以跳过邻格形状更新：否则上方作物会重新校验存活条件，导致亮
 *       度不足 8 的作物直接掉落；由于新旧耕地外形完全相同，邻格本身也无形状变化，唯一代价是旁边的侦测器不会触发。</li>
 *   <li>仅在转换成功时消耗 1 个物品，创造模式不消耗；放置音效沿用黑土耕地的方块放置声，与 {@code BlockItem} 放置体
 *       验一致。</li>
 *   <li>{@link #convertEach} 供连锁采集类兼容模组（{@code compat.FtbUltimineIntegration}）逐格走同一条 {@code useOn}
 *       路径：手中不再持有腐泥冻土即返回 0，耗尽即停，并返回实际转换格数。</li>
 * </ul>
 */

public class RottenFrozenMudItem extends GuMaterialItem {

    public RottenFrozenMudItem(Properties properties) { super(properties, Rank.ONE, GuPath.EARTH); }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = findFarmland(level, context.getClickedPos());
        if (pos == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockState farmland = level.getBlockState(pos);
        Player player = context.getPlayer();
        BlockState blackSoil = ModBlocks.BLACK_SOIL_FARMLAND.get().defaultBlockState()
                .setValue(FarmBlock.MOISTURE, farmland.getValue(FarmBlock.MOISTURE));
        level.setBlock(pos, blackSoil, Block.UPDATE_ALL_IMMEDIATE | Block.UPDATE_KNOWN_SHAPE);
        SoundType sound = blackSoil.getSoundType(level, pos, player);
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, blackSoil));
        context.getItemInHand().consume(1, player);
        return InteractionResult.CONSUME;
    }

    public static int convertEach(ServerPlayer player, InteractionHand hand, Collection<BlockPos> positions) {
        ItemStack mud = player.getItemInHand(hand);
        if (!(mud.getItem() instanceof RottenFrozenMudItem)) return 0;

        int converted = 0;
        for (BlockPos pos : positions) {
            if (mud.isEmpty()) break;

            BlockHitResult hit = new BlockHitResult(Vec3.atBottomCenterOf(pos.above()), Direction.UP, pos, false);
            if (mud.useOn(new UseOnContext(player, hand, hit)).consumesAction()) converted++;
        }
        return converted;
    }

    private static @Nullable BlockPos findFarmland(Level level, BlockPos clicked) {
        if (level.getBlockState(clicked).is(Blocks.FARMLAND)) return clicked;

        BlockPos below = clicked.below();
        boolean cropOnFarmland = BlackSoilFarmlandBlock.isCrop(level.getBlockState(clicked))
                && level.getBlockState(below).is(Blocks.FARMLAND);
        return cropOnFarmland ? below : null;
    }
}
