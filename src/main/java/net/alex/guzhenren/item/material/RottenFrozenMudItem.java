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
 * 腐泥冻土：右键原版耕地或其上的作物，把耕地掺成黑土耕地。
 * <ul>
 *   <li>因有对方块的右键才开叶子类；只认 {@code minecraft:farmland}，别的方块放行，黑土耕地不能再掺。</li>
 *   <li>点到作物时取它正下方那格耕地；作物的范围同 {@code BlackSoilFarmlandBlock#isCrop}。</li>
 *   <li>换方块只在服务端，带上原耕地的湿润度。</li>
 *   <li>⚠ 换方块的标志带 {@code UPDATE_KNOWN_SHAPE}，不发邻格形状更新：否则上方作物复检存活，亮度不足 8 的会掉落；
 *       新旧耕地外形相同，邻格无形状可改。代价是旁边的侦测器不触发。</li>
 *   <li>成功才消耗 1 个，创造模式不消耗；音效用黑土耕地的放置声，同 {@code BlockItem} 放方块。</li>
 *   <li>{@link #convertEach} 供连锁类模组（{@code compat.FtbUltimineIntegration}）逐格走同一条 {@code useOn}：手里不是
 *       腐泥冻土即返回 0，用完即停，返回转换的格数。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see BlackSoilFarmlandBlock
 * @since 1.0.0
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
