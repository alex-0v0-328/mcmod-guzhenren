package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.immortalaperture.herd.PastureWork;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLand;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.registry.entity.ModEntityTypeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Wolf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 福地牧场的自然刷新与捕食者：栏内只拦自然刷（刷怪蛋、刷怪笼、繁殖、区块生成等不拦）；狼、狐、猫、豹猫、北极熊归
 * 捕食者标签；收拢与迁移把捕食者留在原地；圈地后把栏内未命名未驯服的捕食者传到栏外离栅栏至少 2 格、不在任何牧场里的地面。
 * 牧场都取测试区里的小方格，不碰别的测试。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlissfulPastureSpawnGameTests {

    private BlissfulPastureSpawnGameTests() {}

    private static PastureWork.Pasture pasture(GameTestHelper helper, int min, int max) {
        BlockPos low = helper.absolutePos(new BlockPos(min, 0, min));
        BlockPos high = helper.absolutePos(new BlockPos(max, 0, max));
        return new PastureWork.Pasture(Math.min(low.getX(), high.getX()), Math.min(low.getZ(), high.getZ()),
                Math.max(low.getX(), high.getX()), Math.max(low.getZ(), high.getZ()));
    }

    @GameTest(template = "empty9x9x9")
    public static void predatorTagHoldsTheWildHuntersOnly(GameTestHelper helper) {
        for (EntityType<?> hunter : List.of(EntityType.WOLF, EntityType.FOX, EntityType.CAT, EntityType.OCELOT,
                EntityType.POLAR_BEAR)) {
            helper.assertTrue(hunter.is(ModEntityTypeTags.PASTURE_PREDATORS), hunter + " is a pasture predator");
        }
        for (EntityType<?> stock : List.of(EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN,
                EntityType.RABBIT, EntityType.HORSE)) {
            helper.assertFalse(stock.is(ModEntityTypeTags.PASTURE_PREDATORS), stock + " is livestock");
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void onlyNaturalSpawnsInsideThePastureAreBlocked(GameTestHelper helper) {
        PastureWork.Pasture pasture = pasture(helper, 2, 5);
        List<PastureWork.Pasture> pastures = List.of(pasture);
        double midZ = pasture.minZ() + 1.5D;

        helper.assertTrue(PastureWork.blocksSpawn(pastures, MobSpawnType.NATURAL, pasture.minX() + 1.5D, midZ),
                "a natural spawn inside the fence is blocked");
        helper.assertTrue(PastureWork.blocksSpawn(pastures, MobSpawnType.NATURAL, pasture.minX() - 1.0D, midZ),
                "the account is the whole chunk box: the strip outside the fence counts too");
        helper.assertTrue(PastureWork.blocksSpawn(pastures, MobSpawnType.NATURAL, pasture.maxX() + 1.9D, midZ),
                "the far strip counts as well");
        helper.assertFalse(PastureWork.blocksSpawn(pastures, MobSpawnType.NATURAL, pasture.maxX() + 2.0D, midZ),
                "one block past the chunk box is free");
        helper.assertFalse(PastureWork.blocksSpawn(pastures, MobSpawnType.NATURAL, pasture.minX() - 1.1D, midZ),
                "one block before the chunk box is free");
        helper.assertFalse(PastureWork.blocksSpawn(List.of(), MobSpawnType.NATURAL, pasture.minX() + 1.5D, midZ),
                "no pasture, nothing blocked");
        for (MobSpawnType type : List.of(MobSpawnType.SPAWN_EGG, MobSpawnType.SPAWNER, MobSpawnType.BREEDING,
                MobSpawnType.COMMAND, MobSpawnType.CHUNK_GENERATION, MobSpawnType.REINFORCEMENT,
                MobSpawnType.MOB_SUMMONED, MobSpawnType.BUCKET)) {
            helper.assertFalse(PastureWork.blocksSpawn(pastures, type, pasture.minX() + 1.5D, midZ),
                    type + " inside the pasture is not blocked");
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void gatheringAndRelocatingLeavePredatorsBehind(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        PastureWork.Pasture pen = pasture(helper, 0, 5);
        Cow loose = helper.spawn(EntityType.COW, new BlockPos(8, 1, 8));
        Wolf looseWolf = helper.spawn(EntityType.WOLF, new BlockPos(7, 1, 8));
        Fox looseFox = helper.spawn(EntityType.FOX, new BlockPos(8, 1, 7));

        int gathered = PastureWork.gather(level, data, pen, level.getRandom(), List.of(looseWolf, looseFox, loose));
        helper.assertValueEqual(gathered, 1, "only the cow is gathered");
        helper.assertTrue(pen.contains(loose), "the cow stands in the pasture");
        helper.assertFalse(pen.contains(looseWolf) || pen.contains(looseFox), "the hunters stay where they were");

        PastureWork.Pasture from = pasture(helper, 0, 1);
        PastureWork.Pasture to = pasture(helper, 3, 8);
        Cow penned = helper.spawn(EntityType.COW, new BlockPos(0, 1, 0));
        PolarBear bear = helper.spawn(EntityType.POLAR_BEAR, new BlockPos(1, 1, 0));
        int moved = PastureWork.relocate(level, from, to, level.getRandom(), List.of(bear, penned));
        helper.assertValueEqual(moved, 1, "only the cow moves with the pasture");
        helper.assertTrue(from.contains(bear) && !to.contains(bear), "the bear stays in the old spot");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void expellingSendsPredatorsOutsideEveryPasture(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerLevel level = helper.getLevel();
        PastureWork.Pasture pen = pasture(helper, 4, 4);
        PastureWork.Pasture neighbour = pasture(helper, 7, 7);
        List<PastureWork.Pasture> pastures = List.of(pen, neighbour);
        List<Wolf> wolves = new ArrayList<>();
        for (int i = 0; i < 8; i++) wolves.add(helper.spawn(EntityType.WOLF, new BlockPos(4, 1, 4)));
        Fox fox = helper.spawn(EntityType.FOX, new BlockPos(4, 1, 4));
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(4, 1, 5));
        Wolf named = helper.spawn(EntityType.WOLF, new BlockPos(5, 1, 4));
        named.setCustomName(Component.literal("Named"));
        Wolf tame = helper.spawn(EntityType.WOLF, new BlockPos(3, 1, 4));
        tame.setOwnerUUID(UUID.randomUUID());
        Wolf outsider = helper.spawn(EntityType.WOLF, new BlockPos(1, 1, 1));

        int moved = PastureWork.expel(level, pen, pastures, RandomSource.create(7L), 1);
        helper.assertValueEqual(moved, 9, "the eight wolves and the fox leave");
        List<Wolf> leavers = new ArrayList<>(wolves);
        for (Wolf wolf : leavers) {
            helper.assertFalse(pen.contains(wolf) || neighbour.contains(wolf),
                    "a wolf must land outside both pastures, not at " + wolf.blockPosition());
        }
        helper.assertFalse(pen.contains(fox) || neighbour.contains(fox), "the fox leaves too");
        helper.assertTrue(pen.contains(cow), "livestock stays");
        helper.assertTrue(pen.contains(named), "a named predator stays");
        helper.assertTrue(pen.contains(tame), "a tamed predator stays");
        helper.assertFalse(pen.contains(outsider), "a hunter that was never inside is not touched");
        helper.assertTrue(outsider.blockPosition().equals(helper.absolutePos(new BlockPos(1, 1, 1))),
                "the outsider keeps its place");
        helper.succeed();
    }
}
