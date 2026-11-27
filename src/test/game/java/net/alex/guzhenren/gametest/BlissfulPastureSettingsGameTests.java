package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.immortalaperture.herd.GatherWork;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdState;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdWork;
import net.alex.guzhenren.gameplay.immortalaperture.herd.PastureSettings;
import net.alex.guzhenren.gameplay.immortalaperture.herd.PastureWork;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLand;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 福地牧场设置：收拢只收这个牧场勾选的种类，自动收拢关着时托管轮不动。牧场取测试区里的小方格，设置按一个虚拟西北角记，
 * 不碰别的测试。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlissfulPastureSettingsGameTests {

    private BlissfulPastureSettingsGameTests() {}

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void gatheringSkipsTheKindsThePastureTurnedOff(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        BlockPos low = helper.absolutePos(BlockPos.ZERO);
        BlockPos high = helper.absolutePos(new BlockPos(5, 0, 5));
        PastureWork.Pasture pasture = new PastureWork.Pasture(Math.min(low.getX(), high.getX()),
                Math.min(low.getZ(), high.getZ()), Math.max(low.getX(), high.getX()),
                Math.max(low.getZ(), high.getZ()));
        ChunkPos corner = new ChunkPos(low);
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(8, 1, 8));
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(7, 1, 8));
        PastureSettings.of(data).setGathers(corner, HerdWork.typeId(pig), false);

        int moved = GatherWork.gather(level, data, corner, pasture, level.getRandom(), List.of(cow, pig));
        helper.assertValueEqual(moved, 1, "only the cow is gathered");
        helper.assertTrue(pasture.contains(cow), "the cow stands in the pasture");
        helper.assertFalse(pasture.contains(pig), "the pig the pasture turned off stays out");

        PastureSettings.of(data).setGathers(corner, HerdWork.typeId(pig), true);
        helper.assertValueEqual(GatherWork.gather(level, data, corner, pasture, level.getRandom(), List.of(cow, pig)),
                1, "turned back on, the pig follows at once");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void hostingLeavesPasturesWithoutAutoGatherAlone(GameTestHelper helper) {
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        HerdState.of(data).addPasture(new ChunkPos(helper.absolutePos(BlockPos.ZERO)));
        helper.assertValueEqual(GatherWork.host(helper.getLevel(), data, null), 0,
                "no pasture gathers by itself until asked");
        helper.succeed();
    }
}
