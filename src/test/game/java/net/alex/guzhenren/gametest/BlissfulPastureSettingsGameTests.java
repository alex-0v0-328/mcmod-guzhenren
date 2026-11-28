package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.immortalaperture.herd.GatherWork;
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
 * 福地牧场设置：一个牧场只养指定的一种（Alex 定），收拢只收这一种，没指定时什么都不收；换动物下限清零。牧场取测试区里的
 * 小方格，设置按一个虚拟西北角记，不碰别的测试。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlissfulPastureSettingsGameTests {

    private BlissfulPastureSettingsGameTests() {}

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void gatheringTakesOnlyThePasturesAnimal(GameTestHelper helper) {
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

        helper.assertValueEqual(GatherWork.gather(level, data, corner, pasture, level.getRandom(), List.of(cow, pig)),
                0, "a pasture without an animal gathers nothing");
        PastureSettings.of(data).setAnimal(corner, HerdWork.typeId(cow));
        int moved = GatherWork.gather(level, data, corner, pasture, level.getRandom(), List.of(cow, pig));
        helper.assertValueEqual(moved, 1, "only the cow is gathered");
        helper.assertTrue(pasture.contains(cow), "the cow stands in the pasture");
        helper.assertFalse(pasture.contains(pig), "the pig is another kind and stays out");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void changingTheAnimalClearsTheFloor(GameTestHelper helper) {
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        ChunkPos corner = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        PastureSettings settings = PastureSettings.of(data);
        settings.setFloor(corner, 4);
        helper.assertValueEqual(settings.floor(corner), 0, "no floor without an animal");
        settings.setAnimal(corner, HerdWork.typeId(helper.spawn(EntityType.COW, new BlockPos(4, 1, 4))));
        settings.setFloor(corner, 4);
        helper.assertValueEqual(settings.floor(corner), 4, "the floor sticks once the cow is chosen");
        settings.setAnimal(corner, HerdWork.typeId(helper.spawn(EntityType.PIG, new BlockPos(5, 1, 4))));
        helper.assertValueEqual(settings.floor(corner), 0, "a new animal starts again from no floor");
        helper.succeed();
    }
}
