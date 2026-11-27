package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.immortalaperture.herd.ProduceWork;
import net.alex.guzhenren.gameplay.immortalaperture.herd.PastureWork;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLand;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.storage.BlissfulStorage;
import net.alex.guzhenren.gameplay.immortalaperture.territory.LandTeleport;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 福地地图传送的落脚处（地面上、低垂树冠下站不下就站树冠顶、水面就近挪到干地）与牧场产物入库（剪羊毛、捡栏内掉落物）。
 * 都在测试区里，不碰福地维度。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlissfulTravelGameTests {

    private BlissfulTravelGameTests() {}

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void teleportLandsOnTheGroundUnderALowCanopyOnTopAndBesideWater(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true));
        for (int x = 4; x <= 8; x++) {
            for (int z = 4; z <= 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.WATER);
        }

        BlockPos plain = helper.absolutePos(new BlockPos(1, 1, 6));
        helper.assertValueEqual(LandTeleport.landing(level, plain.getX(), plain.getZ()), Vec3.atBottomCenterOf(plain),
                "open ground is stood on");
        BlockPos canopy = helper.absolutePos(new BlockPos(2, 3, 2));
        helper.assertValueEqual(LandTeleport.landing(level, canopy.getX(), canopy.getZ()),
                Vec3.atBottomCenterOf(canopy), "a canopy one block up leaves no headroom, so its top is stood on");
        BlockPos pond = helper.absolutePos(new BlockPos(5, 1, 5));
        Vec3 dry = LandTeleport.landing(level, pond.getX(), pond.getZ());
        helper.assertTrue(dry != null, "a dry spot is found next to the pond");
        BlockPos at = BlockPos.containing(dry);
        helper.assertTrue(level.getBlockState(at.below()).is(Blocks.STONE), "the spot stands on the stone floor");
        helper.assertTrue(Math.max(Math.abs(at.getX() - pond.getX()), Math.abs(at.getZ() - pond.getZ())) == 2,
                "the nearest dry ring is two blocks off the pond centre");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void pastureProduceGoesIntoStorage(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        BlockPos low = helper.absolutePos(new BlockPos(1, 0, 1));
        BlockPos high = helper.absolutePos(new BlockPos(7, 0, 7));
        PastureWork.Pasture pasture = new PastureWork.Pasture(Math.min(low.getX(), high.getX()),
                Math.min(low.getZ(), high.getZ()), Math.max(low.getX(), high.getX()),
                Math.max(low.getZ(), high.getZ()));
        Sheep sheep = helper.spawn(EntityType.SHEEP, new BlockPos(3, 1, 3));
        Sheep lamb = helper.spawn(EntityType.SHEEP, new BlockPos(5, 1, 3));
        lamb.setBaby(true);
        BlockPos eggAt = helper.absolutePos(new BlockPos(4, 1, 5));
        ItemEntity egg = new ItemEntity(level, eggAt.getX() + 0.5, eggAt.getY(), eggAt.getZ() + 0.5,
                new ItemStack(Items.EGG, 2));
        level.addFreshEntity(egg);

        ProduceWork.Produce done = ProduceWork.collect(level, data, pasture);
        helper.assertValueEqual(done, new ProduceWork.Produce(1, 1), "one grown sheep is sheared, one drop picked");
        helper.assertTrue(sheep.isSheared(), "the grown sheep is sheared");
        helper.assertFalse(lamb.isSheared(), "the lamb keeps its wool");
        helper.assertFalse(egg.isAlive(), "the eggs are picked off the ground");
        BlissfulStorage storage = BlissfulStorage.of(data);
        helper.assertValueEqual(storage.count(stack -> stack.is(Items.EGG)), 2L, "both eggs are stored");
        helper.assertTrue(storage.count(stack -> stack.is(Items.WHITE_WOOL)) >= 1L, "the wool is stored");
        helper.assertValueEqual(ProduceWork.collect(level, data, pasture), new ProduceWork.Produce(0, 0),
                "a shorn sheep and a clean pen give nothing more");
        helper.assertTrue(List.of(sheep, lamb).stream().allMatch(Sheep::isAlive), "nobody is hurt");
        helper.succeed();
    }
}
