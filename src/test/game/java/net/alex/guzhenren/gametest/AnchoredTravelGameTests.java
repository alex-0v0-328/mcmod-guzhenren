package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.dimension.DimensionTravelService;
import net.alex.guzhenren.gameplay.flight.FlightAbility;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 锚定旅行 {@code SURFACE} 落点（南疆进入方式）。
 * <ul>
 *   <li>GameTestServer 不实例化 Gu World 维度（见 {@code TreasureYellowHeavenGameTests}），故以手建锚定维度指向已加载维
 *       度。</li>
 *   <li>{@link #surfaceArrivalStandsOnTopOfTheColumn} 在测试维度读落点，场景方块即列顶；须开天空，否则屏障顶成每列之
 *       顶。</li>
 *   <li>{@link #surfaceEnterLandsWithoutFlightAndExitReturns} 真进下界：当场生成该列区块，落其顶，撤带入的飞行授权，离
 *       开回记录起点。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.dimension.DimensionTravelService
 * @since 1.0.0
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AnchoredTravelGameTests {

    private static final BlockPos TOP = new BlockPos(4, 3, 4);

    private AnchoredTravelGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void surfaceArrivalStandsOnTopOfTheColumn(GameTestHelper helper) {
        GameTestWorld.openSky(helper);
        helper.setBlock(TOP, Blocks.STONE);
        Vec3 spawn = helper.absoluteVec(new Vec3(4.5D, 0.0D, 4.5D));
        var surface = new ModDimensions.AnchoredDimension(helper.getLevel().dimension(), spawn,
                ModDimensions.Arrival.SURFACE, 0, 1.0D);
        var flight = new ModDimensions.AnchoredDimension(helper.getLevel().dimension(), spawn,
                ModDimensions.Arrival.FLIGHT, 0, 1.0D);

        Vec3 arrival = DimensionTravelService.getArrival(helper.getLevel(), surface);

        helper.assertValueEqual(arrival.y, helper.absolutePos(TOP).getY() + 1.0D, "surface arrival y");
        helper.assertValueEqual(arrival.x, spawn.x, "surface arrival x");
        helper.assertValueEqual(arrival.z, spawn.z, "surface arrival z");
        helper.assertValueEqual(DimensionTravelService.getArrival(helper.getLevel(), flight), spawn,
                "a flight arrival is the spawn itself");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void surfaceEnterLandsWithoutFlightAndExitReturns(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-travel-gametest").build();
        Vec3 start = helper.absoluteVec(new Vec3(4.5D, 2.5D, 4.5D));
        player.moveTo(start.x, start.y, start.z, 0.0F, 0.0F);
        DimensionTravelService.ensureFlight(player);
        var destination = new ModDimensions.AnchoredDimension(Level.NETHER, new Vec3(0.5D, 0.0D, 0.5D),
                ModDimensions.Arrival.SURFACE, 0, 1.0D);

        boolean entered = DimensionTravelService.enter(player, destination);

        helper.assertTrue(entered, "enter must reach a loaded level");
        helper.assertTrue(player.level().dimension().equals(Level.NETHER), "the player is not in the destination");
        BlockPos feet = player.blockPosition();
        helper.assertTrue(!Heightmap.Types.MOTION_BLOCKING.isOpaque().test(player.level().getBlockState(feet)),
                "the player landed inside a block");
        helper.assertTrue(Heightmap.Types.MOTION_BLOCKING.isOpaque().test(player.level().getBlockState(feet.below())),
                "the player landed on nothing");
        helper.assertTrue(!FlightAbility.canFly(player), "a surface arrival kept the flight grant");
        helper.assertTrue(!player.getAbilities().flying, "a surface arrival started flying");
        helper.assertTrue(PlayerData.get(player).getDimensionReturn().isPresent(), "no return point recorded");

        boolean used = DimensionTravelService.exit(player);

        helper.assertTrue(used, "exit must use the recorded return point");
        helper.assertTrue(player.level().dimension().equals(helper.getLevel().dimension()),
                "exit did not return to the start level");
        helper.assertValueEqual(player.getX(), start.x, "return x");
        helper.assertValueEqual(player.getZ(), start.z, "return z");
        player.discard();
        helper.succeed();
    }
}
