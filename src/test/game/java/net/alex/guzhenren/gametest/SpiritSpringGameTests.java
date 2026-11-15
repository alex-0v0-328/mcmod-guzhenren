package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.block.SpiritSpringBlock;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestApertures.mortal;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;

/**
 * 元泉[Spirit Spring]流体与玩家在场时元石[Primeval Stone]产出测试。
 * {@link #spiritSpringFlowsBetweenSourcesWithoutMintingNewOnes} 搭封闭水槽：元泉高度除三格槽位外全是石头，
 * 缺口是流体唯一去处；附近有落差时水跳过平地格。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpiritSpringGameTests {

    private SpiritSpringGameTests() {}

    @GameTest(template = "empty9x9x9", timeoutTicks = 200)
    public static void spiritSpringFlowsBetweenSourcesWithoutMintingNewOnes(GameTestHelper helper) {
        BlockPos middle = CENTER_POS;
        for (int x = -3; x <= 3; x++) {
            for (int z = -2; z <= 2; z++) {
                helper.setBlock(middle.offset(x, -1, z), Blocks.STONE);
                boolean trough = z == 0 && x >= -1 && x <= 1;
                if (!trough) helper.setBlock(middle.offset(x, 0, z), Blocks.STONE);
            }
        }
        helper.setBlock(middle.offset(-1, 0, 0), ModBlocks.SPIRIT_SPRING.get());
        helper.setBlock(middle.offset(1, 0, 0), ModBlocks.SPIRIT_SPRING.get());
        helper.succeedWhen(() -> helper.assertBlockState(middle,
                state -> state.getFluidState().getType() == ModFluids.FLOWING_SPIRIT_SPRING.get()
                        && !state.getFluidState().isSource(),
                () -> "the gap must fill with the flowing arm, never a new source, but is "
                        + helper.getLevel().getBlockState(helper.absolutePos(middle))));
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 600)
    public static void spiritSpringProducesPrimevalStonesUpToTheCap(GameTestHelper helper) {
        BlockPos spring = CENTER_POS;
        helper.setBlock(spring.below(), Blocks.STONE);
        helper.setBlock(spring, ModBlocks.SPIRIT_SPRING.get());
        ServerPlayer player = mortal(helper);
        BlockPos absoluteSpring = helper.absolutePos(spring);
        player.setPos(absoluteSpring.getX() + 0.5, absoluteSpring.getY() + 1.0, absoluteSpring.getZ() + 0.5);
        helper.succeedWhen(() -> {
            int stones = SpiritSpringBlock.countNearbyStones(helper.getLevel(), helper.absolutePos(spring));
            helper.assertTrue(stones >= SpiritSpringBlock.STONES_PER_PRODUCTION, "first batch of stones spawned");
            helper.assertTrue(stones <= SpiritSpringBlock.NEARBY_STONES_CAP, "the cap pauses further batches");
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 600)
    public static void spiritSpringProducesWhenOneOfSeveralPlayersIsNear(GameTestHelper helper) {
        BlockPos spring = CENTER_POS;
        helper.setBlock(spring.below(), Blocks.STONE);
        helper.setBlock(spring, ModBlocks.SPIRIT_SPRING.get());
        BlockPos absoluteSpring = helper.absolutePos(spring);
        ServerPlayer far = mortal(helper);
        far.setPos(absoluteSpring.getX() + 0.5 + 200.0, absoluteSpring.getY() + 1.0,
                absoluteSpring.getZ() + 0.5);
        ServerPlayer near = mortal(helper);
        near.setPos(absoluteSpring.getX() + 0.5, absoluteSpring.getY() + 1.0, absoluteSpring.getZ() + 0.5);
        helper.succeedWhen(() -> {
            int stones = SpiritSpringBlock.countNearbyStones(helper.getLevel(), absoluteSpring);
            helper.assertTrue(stones >= SpiritSpringBlock.STONES_PER_PRODUCTION,
                    "one near player keeps production alive even with another beyond the range");
        });
    }
}
