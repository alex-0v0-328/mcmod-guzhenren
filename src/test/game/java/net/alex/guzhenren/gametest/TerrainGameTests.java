package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.aperture.pressure.AperturePressureExplosionTask;
import net.alex.guzhenren.gameplay.aperture.pressure.AperturePressureExplosions;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;

/**
 * 地形改动测试：场景冒烟、空窍[Aperture]压力爆坑及其 mobGriefing 门槛。{@link #pressureExplosionResumesAfterReload}：
 * 零预算使每次爆炸固定步进 255 个位置，半径 5 的坑（约 400～500 个位置加坑沿）至少中途停一次；每次停下把该维度爆炸经存
 * 档格式往返一次，副本换进数据存储，模拟重启；先加载相邻区块，因爆炸要等所触及的每个区块，而整个测试在一个 tick 内。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TerrainGameTests {

    private TerrainGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void sceneSupportsBlockPlacement(GameTestHelper helper) {
        helper.setBlock(CENTER_POS, Blocks.DIRT);
        helper.succeedWhen(() -> helper.assertBlockState(CENTER_POS, state -> state.is(Blocks.DIRT),
                () -> "dirt missing"));
    }

    @GameTest(template = "empty9x9x9")
    public static void terrainChangesFollowMobGriefing(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-grief-gametest").unconnected().build();
        GameRules.BooleanValue griefing = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean was = griefing.get();
        try {
            griefing.set(true, helper.getLevel().getServer());
            helper.assertTrue(ModEvents.canGrief(helper.getLevel(), player), "mobGriefing on refuses the crater");
            griefing.set(false, helper.getLevel().getServer());
            helper.assertTrue(!ModEvents.canGrief(helper.getLevel(), player), "mobGriefing off allows the crater");
        } finally {
            griefing.set(was, helper.getLevel().getServer());
        }
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 200)
    public static void pressureExplosionSparesUnbreakableBlocks(GameTestHelper helper) {
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.DIRT);
        }
        helper.setBlock(new BlockPos(4, 0, 4), Blocks.BEDROCK);
        Vec3 center = helper.absoluteVec(new Vec3(4.5D, 1.5D, 4.5D));
        AperturePressureExplosionTask.start(helper.getLevel(), center.x, center.y, center.z, 2, ExtremePhysique.NONE);

        helper.succeedWhen(() -> {
            helper.assertBlockState(new BlockPos(3, 0, 3), BlockState::isAir, () -> "crater block survived");
            helper.assertBlockState(new BlockPos(4, 0, 4), state -> state.is(Blocks.BEDROCK),
                    () -> "the crater removed bedrock");
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 200)
    public static void pressureExplosionSpreadsOverTicks(GameTestHelper helper) {
        for (int x = 2; x <= 6; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 2; z <= 6; z++) helper.setBlock(new BlockPos(x, y, z), Blocks.DIRT);
            }
        }
        Vec3 center = helper.absoluteVec(new Vec3(4.5D, 1.5D, 4.5D));
        GameTestWorld.Drops drops = GameTestWorld.recordDrops(helper, AABB.ofSize(center, 6.0D, 6.0D, 6.0D));
        AperturePressureExplosionTask.start(helper.getLevel(), center.x, center.y, center.z, 2, ExtremePhysique.NONE);

        helper.succeedWhen(() -> {
            for (int x = 2; x <= 6; x++) {
                for (int y = 0; y <= 2; y++) {
                    for (int z = 2; z <= 6; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        boolean inner = x >= 3 && x <= 5 && z >= 3 && z <= 5;
                        helper.assertBlockState(pos, state -> inner == state.isAir(),
                                () -> inner ? "crater block survived" : "rim block was cleared");
                    }
                }
            }
            helper.assertTrue(drops.joined().stream().noneMatch(ItemEntity.class::isInstance),
                    "crater dropped item entities");
            drops.stop().run();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void pressureExplosionResumesAfterReload(GameTestHelper helper) {
        for (int x = 0; x <= 8; x++) {
            for (int y = 0; y <= 8; y++) {
                for (int z = 0; z <= 8; z++) helper.setBlock(new BlockPos(x, y, z), Blocks.DIRT);
            }
        }
        ServerLevel level = helper.getLevel();
        Vec3 center = helper.absoluteVec(new Vec3(4.5D, 4.5D, 4.5D));
        ChunkPos.rangeClosed(new ChunkPos(BlockPos.containing(center)), 1).forEach(pos -> level.getChunk(pos.x, pos.z));
        AperturePressureExplosionTask.start(level, center.x, center.y, center.z, 5, ExtremePhysique.NONE);

        int reloads = 0;
        while (!AperturePressureExplosions.get(level).isEmpty() && reloads < 64) {
            AperturePressureExplosions.tickAll(level.getServer(), 0L);
            CompoundTag saved = GameTestWorld.save(level, AperturePressureExplosions.get(level));
            AperturePressureExplosions reloaded = AperturePressureExplosions.load(level, saved);
            helper.assertValueEqual(GameTestWorld.save(level, reloaded), saved,
                    "blast state after a reload");
            level.getDataStorage().set(AperturePressureExplosions.NAME, reloaded);
            reloads++;
        }
        helper.assertTrue(AperturePressureExplosions.get(level).isEmpty(), "the reloaded blast never finished");
        helper.assertTrue(reloads > 1, "the blast finished before any mid-carve reload");
        for (int x = 0; x <= 8; x++) {
            for (int y = 0; y <= 8; y++) {
                for (int z = 0; z <= 8; z++) {
                    int distanceSquared = (x - 4) * (x - 4) + (y - 4) * (y - 4) + (z - 4) * (z - 4);
                    BlockPos pos = new BlockPos(x, y, z);
                    if (distanceSquared <= 20) {
                        helper.assertBlockState(pos, BlockState::isAir, () -> "crater block survived a reload");
                    } else if (distanceSquared >= 25) {
                        helper.assertBlockState(pos, state -> state.is(Blocks.DIRT),
                                () -> "a reload cleared a block outside the crater");
                    }
                }
            }
        }
        helper.succeed();
    }
}
