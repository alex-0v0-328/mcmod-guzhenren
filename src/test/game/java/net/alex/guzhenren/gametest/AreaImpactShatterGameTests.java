package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.strength.AreaImpact;
import net.alex.guzhenren.gameplay.path.strength.AreaImpactShatter;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestGu.inSlot;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 范围冲击[Area Impact]拳击的崩裂（场景西缘朝东，眼高 2.62）。
 * <ul>
 *   <li>五转、人力 168 斤全额发挥 168 点：破硬度 ≤ 0.6，破坏半径约 4.1 格；人力 99 斤不足 100 点，不破。</li>
 *   <li>正前方一排：2 格泥土（破）、3 格石头（太硬）、4 格泥土（被石头挡住）、5 格泥土（在半径外）。</li>
 *   <li>门先破上半：生存由下半连带掉一扇门，创造不掉（原版先静默抹掉下半）；石头按空手挖碎而不掉。</li>
 *   <li>苦力运行中的人力钧力流锥形放大（半径 ×1.5、半角 135°），崩裂仍按未放大的半径与 45° 半角：结算 184 点，破坏半
 *       径约 4.2 格，5 格外与正侧方的泥土都留。</li>
 *   <li>蓄力一拳的扇形崩裂与硬度上档在 {@code StoreStrengthGameTests}。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AreaImpactShatterGameTests {

    private AreaImpactShatterGameTests() {}

    private static final Vec3 WEST = new Vec3(1.5D, 1.0D, 4.5D);
    private static final Vec3 EAST = new Vec3(1.0D, 0.0D, 0.0D);
    private static final BlockPos NEAR = new BlockPos(3, 2, 4);
    private static final BlockPos HARD = new BlockPos(4, 2, 4);
    private static final BlockPos SHIELDED = new BlockPos(5, 2, 4);
    private static final BlockPos BEYOND = new BlockPos(6, 2, 4);
    private static final BlockPos SIDE = new BlockPos(1, 2, 6);
    private static final int STRONG_POINTS = 168;
    private static final int WEAK_POINTS = 99;
    private static final int HARDSHIP_POINTS = 184;
    private static final int BAG_SLOT = 9;
    private static final int HUNGER = 98;

    @GameTest(template = "empty9x9x9")
    public static void aStrongFistShattersTheSoftBlocksItSees(GameTestHelper helper) {
        ServerPlayer player = strong(helper, GameTestPlayers.survival(helper, "gzr-shatter-cone")
                .pastSpawnInvulnerability().at(WEST));
        row(helper);

        AreaImpact.release(player, EAST, List.of());

        assertBlock(helper, NEAR, Blocks.AIR);
        assertBlock(helper, HARD, Blocks.STONE);
        assertBlock(helper, SHIELDED, Blocks.DIRT);
        assertBlock(helper, BEYOND, Blocks.DIRT);
        assertBlock(helper, new BlockPos(3, 0, 4), Blocks.STONE);
        assertTrue(!drops(helper).isEmpty(), "the broken dirt dropped nothing");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void underAHundredPointsNothingBreaks(GameTestHelper helper) {
        ServerPlayer player = human(helper, GameTestPlayers.survival(helper, "gzr-shatter-weak")
                .pastSpawnInvulnerability().at(WEST), 3, 0, 9);
        assertEquals(WEAK_POINTS, AreaImpact.getPoints(player));
        assertTrue(WEAK_POINTS < AreaImpactShatter.SOFT_POINTS, "the weak fist is not under the soft tier");
        row(helper);

        AreaImpact.release(player, EAST, List.of());

        assertBlock(helper, NEAR, Blocks.DIRT);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void adventureModeBreaksNothingAndCreativeDropsNothing(GameTestHelper helper) {
        ServerPlayer adventurer = strong(helper, GameTestPlayers.survival(helper, "gzr-shatter-adventure")
                .pastSpawnInvulnerability().at(WEST));
        row(helper);
        adventurer.setGameMode(GameType.ADVENTURE);
        AreaImpact.release(adventurer, EAST, List.of());
        assertBlock(helper, NEAR, Blocks.DIRT);
        adventurer.discard();

        ServerPlayer builder = strong(helper, GameTestPlayers.survival(helper, "gzr-shatter-creative")
                .creative(() -> true).pastSpawnInvulnerability().at(WEST));
        AreaImpact.release(builder, EAST, List.of());
        assertBlock(helper, NEAR, Blocks.AIR);
        assertTrue(drops(helper).isEmpty(), "creative dropped the broken dirt");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theShatterStopsAtItsCapInCellOrder(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-shatter-cap").pastSpawnInvulnerability().at(WEST);
        GameTestPlayers.ground(helper);
        List<BlockPos> cells = List.of(new BlockPos(3, 2, 4), new BlockPos(3, 2, 2), new BlockPos(3, 2, 6));
        for (BlockPos pos : cells) helper.setBlock(pos, Blocks.DIRT);

        int broken = AreaImpactShatter.shatter(player, player.getEyePosition(),
                cells.stream().map(helper::absolutePos).toList(), AreaImpactShatter.SOFT_POINTS, 2);

        assertEquals(2, broken);
        assertBlock(helper, cells.get(0), Blocks.AIR);
        assertBlock(helper, cells.get(1), Blocks.AIR);
        assertBlock(helper, cells.get(2), Blocks.DIRT);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void stoneBreaksBareHandedWithoutADrop(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-shatter-bare").pastSpawnInvulnerability().at(WEST);
        GameTestPlayers.ground(helper);
        helper.setBlock(NEAR, Blocks.STONE);
        helper.setBlock(BEYOND, Blocks.DIRT);

        assertEquals(2, AreaImpactShatter.shatter(player, player.getEyePosition(),
                List.of(helper.absolutePos(NEAR), helper.absolutePos(BEYOND)), AreaImpactShatter.STONE_POINTS,
                AreaImpactShatter.CONE_MAX_BLOCKS));
        assertBlock(helper, NEAR, Blocks.AIR);
        assertEquals(0, count(helper, Items.COBBLESTONE) + count(helper, Items.STONE));
        assertEquals(1, count(helper, Items.DIRT));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void mobGriefingOffBreaksNothing(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-shatter-griefing").pastSpawnInvulnerability()
                .at(WEST);
        GameTestPlayers.ground(helper);
        helper.setBlock(NEAR, Blocks.DIRT);
        GameRules.BooleanValue griefing = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean was = griefing.get();
        try {
            griefing.set(false, helper.getLevel().getServer());
            assertEquals(0, AreaImpactShatter.shatter(player, player.getEyePosition(),
                    List.of(helper.absolutePos(NEAR)), AreaImpactShatter.HARD_POINTS,
                    AreaImpactShatter.CONE_MAX_BLOCKS));
        } finally {
            griefing.set(was, helper.getLevel().getServer());
        }
        assertBlock(helper, NEAR, Blocks.DIRT);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aDoorHitAtItsTopDropsOnceInSurvival(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-shatter-door").pastSpawnInvulnerability().at(WEST);

        assertEquals(1, breakDoorTopFirst(helper, player));
        assertEquals(1, count(helper, Items.OAK_DOOR));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aDoorHitAtItsTopDropsNothingInCreative(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-shatter-door-creative").creative(() -> true)
                .pastSpawnInvulnerability().at(WEST);

        assertEquals(1, breakDoorTopFirst(helper, player));
        assertEquals(0, count(helper, Items.OAK_DOOR));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHeldItemKeepsTheFistFromShattering(GameTestHelper helper) {
        ServerPlayer player = strong(helper, GameTestPlayers.survival(helper, "gzr-shatter-held")
                .pastSpawnInvulnerability().at(WEST));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        assertEquals(0, AreaImpact.getPoints(player));
        row(helper);

        AreaImpact.release(player, EAST, List.of());

        assertBlock(helper, NEAR, Blocks.DIRT);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theWidenedConeShattersNoWiderThanTheNarrowOne(GameTestHelper helper) {
        ServerPlayer player = strong(helper, GameTestPlayers.survival(helper, "gzr-shatter-widened")
                .pastSpawnInvulnerability().at(WEST));
        inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        player.setHealth(player.getMaxHealth() * 0.7F);
        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "Hardship Strength did not start");
        assertTrue(AreaImpact.isWidened(player), "Hardship did not widen the human branch");
        assertEquals(HARDSHIP_POINTS, AreaImpact.getPoints(player));
        double reach = AreaImpact.getRange(HARDSHIP_POINTS, true) / 2.0D;
        Vec3 eyes = player.getEyePosition();
        assertTrue(helper.absolutePos(BEYOND).getCenter().distanceTo(eyes) < reach,
                "the far dirt is outside even the widened half range");
        assertTrue(helper.absolutePos(SIDE).getCenter().distanceTo(eyes) < AreaImpact.getRange(HARDSHIP_POINTS) / 2.0D,
                "the side dirt is outside the narrow half range");
        helper.setBlock(NEAR, Blocks.DIRT);
        helper.setBlock(BEYOND, Blocks.DIRT);
        helper.setBlock(SIDE, Blocks.DIRT);

        AreaImpact.release(player, EAST, List.of());

        assertBlock(helper, NEAR, Blocks.AIR);
        assertBlock(helper, BEYOND, Blocks.DIRT);
        assertBlock(helper, SIDE, Blocks.DIRT);
        helper.succeed();
    }

    private static int breakDoorTopFirst(GameTestHelper helper, ServerPlayer player) {
        GameTestPlayers.ground(helper);
        BlockPos lower = new BlockPos(3, 1, 4);
        BlockPos upper = lower.above();
        BlockState door = Blocks.OAK_DOOR.defaultBlockState();
        helper.setBlock(lower, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(upper, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        int broken = AreaImpactShatter.shatter(player, player.getEyePosition(),
                List.of(helper.absolutePos(upper), helper.absolutePos(lower)), AreaImpactShatter.HARD_POINTS,
                AreaImpactShatter.CONE_MAX_BLOCKS);
        assertBlock(helper, lower, Blocks.AIR);
        assertBlock(helper, upper, Blocks.AIR);
        return broken;
    }

    private static int count(GameTestHelper helper, Item item) {
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(9.0D);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, drop -> drop.getItem().is(item)).stream()
                .mapToInt(drop -> drop.getItem().getCount()).sum();
    }

    private static ServerPlayer human(GameTestHelper helper, ServerPlayer player, int jun, int tenJin, int jin) {
        GameTestPlayers.ground(helper);
        GameTestGu.awakened(player, Rank.FIVE);
        player.setYRot(-90.0F);
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JUN, jun), "the jun was refused");
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.TEN_JIN, tenJin),
                "the ten jin was refused");
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JIN, jin), "the jin was refused");
        return player;
    }

    private static ServerPlayer strong(GameTestHelper helper, ServerPlayer player) {
        human(helper, player, 5, 1, 8);
        assertEquals(STRONG_POINTS, AreaImpact.getPoints(player));
        return player;
    }

    private static void row(GameTestHelper helper) {
        helper.setBlock(NEAR, Blocks.DIRT);
        helper.setBlock(HARD, Blocks.STONE);
        helper.setBlock(SHIELDED, Blocks.DIRT);
        helper.setBlock(BEYOND, Blocks.DIRT);
    }

    private static List<ItemEntity> drops(GameTestHelper helper) {
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(9.0D);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, item -> item.getItem().is(Items.DIRT));
    }

    private static void assertBlock(GameTestHelper helper, BlockPos pos, Block block) {
        assertTrue(helper.getBlockState(pos).is(block),
                pos + " is " + helper.getBlockState(pos) + ", not " + block);
    }
}
