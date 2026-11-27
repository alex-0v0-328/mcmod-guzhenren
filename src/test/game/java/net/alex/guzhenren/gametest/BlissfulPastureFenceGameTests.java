package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.immortalaperture.herd.PastureWork;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 福地牧场圈地后关得住动物：水塘贴着周界、坡地脊线横穿周界、周界上有玩家的整格方块、北边正中（原门位）四种周界，各让动物
 * 沿精确路径走向栏外 8 秒，中心都不进栅栏线。牧场都取测试区里的小方格。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlissfulPastureFenceGameTests {

    private BlissfulPastureFenceGameTests() {}

    private static final long WALK_TICKS = 160L;

    private record Walker(String label, Mob mob, Vec3 target) {}

    private static PastureWork.Pasture enclose(GameTestHelper helper, String profile) {
        ServerPlayer owner = GameTestPlayers.survival(helper, profile).onGround();
        BlockPos low = helper.absolutePos(new BlockPos(1, 0, 1));
        BlockPos high = helper.absolutePos(new BlockPos(7, 0, 7));
        PastureWork.Pasture pasture = new PastureWork.Pasture(Math.min(low.getX(), high.getX()),
                Math.min(low.getZ(), high.getZ()), Math.max(low.getX(), high.getX()), Math.max(low.getZ(), high.getZ()));
        helper.assertTrue(PastureWork.enclose(helper.getLevel(), pasture, owner) != null, "the owner may enclose");
        return pasture;
    }

    private static Walker walker(GameTestHelper helper, String label, EntityType<? extends Mob> type, BlockPos from,
                                 Vec3 to) {
        Mob mob = helper.spawn(type, from);
        GameTestWorld.goals(mob).removeAllGoals(goal -> goal.getFlags().contains(Goal.Flag.MOVE));
        return new Walker(label, mob, helper.absoluteVec(to));
    }

    private static boolean reachedTheLine(PastureWork.Pasture pasture, Mob mob) {
        return mob.getX() < pasture.minX() + 1 || mob.getX() >= pasture.maxX() || mob.getZ() < pasture.minZ() + 1
                || mob.getZ() >= pasture.maxZ();
    }

    private static void walk(GameTestHelper helper, PastureWork.Pasture pasture, List<Walker> walkers) {
        helper.onEachTick(() -> {
            for (Walker walker : walkers) {
                PathNavigation navigation = walker.mob().getNavigation();
                if (!navigation.isInProgress()) {
                    navigation.moveTo(navigation.createPath(BlockPos.containing(walker.target()), 0), 1.0D);
                }
            }
        });
        helper.runAtTickTime(WALK_TICKS, () -> {
            List<String> out = new ArrayList<>();
            for (Walker walker : walkers) {
                if (reachedTheLine(pasture, walker.mob())) out.add(walker.label());
            }
            helper.assertTrue(out.isEmpty(), "got out: " + String.join(", ", out));
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", skyAccess = true, timeoutTicks = 200)
    public static void aPondAgainstTheLineHolds(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                boolean pond = x >= 5 && x <= 6 && z >= 3 && z <= 5;
                helper.setBlock(new BlockPos(x, 1, z), pond ? Blocks.WATER : Blocks.STONE);
            }
        }
        PastureWork.Pasture pasture = enclose(helper, "gzr-fence-pond-mock");
        walk(helper, pasture, List.of(walker(helper, "cow out of the pond", EntityType.COW, new BlockPos(5, 2, 4),
                new Vec3(8.5D, 2.0D, 4.5D))));
    }

    @GameTest(template = "empty9x9x9", skyAccess = true, timeoutTicks = 200)
    public static void aRidgeAcrossTheLineHolds(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        int[] ridge = {1, 2, 3, 2, 1};
        for (int x = 5; x <= 8; x++) {
            for (int i = 0; i < ridge.length; i++) {
                for (int y = 1; y <= ridge[i]; y++) helper.setBlock(new BlockPos(x, y, 2 + i), Blocks.STONE);
            }
        }
        PastureWork.Pasture pasture = enclose(helper, "gzr-fence-ridge-mock");
        walk(helper, pasture, List.of(
                walker(helper, "cow along the ridge", EntityType.COW, new BlockPos(5, 4, 4),
                        new Vec3(8.5D, 4.0D, 4.5D)),
                walker(helper, "pig down the slope", EntityType.PIG, new BlockPos(5, 3, 5),
                        new Vec3(8.5D, 3.0D, 5.5D))));
    }

    @GameTest(template = "empty9x9x9", skyAccess = true, timeoutTicks = 200)
    public static void aPlayersFullBlockOnTheLineHolds(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        helper.setBlock(new BlockPos(7, 1, 4), Blocks.STONE_BRICKS);
        PastureWork.Pasture pasture = enclose(helper, "gzr-fence-block-mock");
        walk(helper, pasture, List.of(walker(helper, "pig at the bricks", EntityType.PIG, new BlockPos(5, 1, 4),
                new Vec3(8.5D, 1.0D, 4.5D))));
    }

    @GameTest(template = "empty9x9x9", skyAccess = true, timeoutTicks = 200)
    public static void theNorthMiddleHasNoGate(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        PastureWork.Pasture pasture = enclose(helper, "gzr-fence-north-mock");
        walk(helper, pasture, List.of(walker(helper, "cow at the north middle", EntityType.COW, new BlockPos(4, 1, 3),
                new Vec3(4.5D, 1.0D, 0.5D))));
    }
}
