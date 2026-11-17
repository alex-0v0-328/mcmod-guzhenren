package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.compat.EpicFightIntegration;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.effect.timed.StrengthQiGuEffect;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.ShieldLayer;
import net.alex.guzhenren.gameplay.body.ShieldService;
import net.alex.guzhenren.gameplay.body.ShieldType;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.CrashBreach;
import net.alex.guzhenren.gameplay.path.strength.CrashSources;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestGu.click;
import static net.alex.guzhenren.gametest.GameTestGu.hold;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 五转活玩家的兽力[Beast Strength]特性。
 * <ul>
 *   <li>冲撞来源相加：五转无力量倍率 4.5；驰马 48 点为 4.5 ×（2 + √48 / 10），再开全力以赴加 1（不翻倍）；驰马播放速度
 *       1.5，加全力以赴 2.0。</li>
 *   <li>青牛劳力：横冲系增益各加 10 秒（30 → 40；五转横冲直撞 60 → 70），力气蛊[Strength Qi Gu]不加；石龟负力：护盾 180
 *       → 270，时长不变。</li>
 *   <li>两猪之力清冲撞路径：挡身的石、土碎落，地面与草留；黑曜石截停，其后石头留；箱子亦截停；冒险模式不破坏；站土径时
 *       前方高一级的地面留。熊 40 点开全力以赴只升到软档：土碎，石头截停。</li>
 *   <li>玩家在场景北缘石地朝南（yaw 0）；冲撞本身在所属客户端经 Epic Fight 运行，此处不测：
 *       {@link EpicFightIntegration#getDashScale} 为倍率、{@link CrashBreach#clear} 为路径（硬度取
 *       {@link CrashSources#getMaxHardness}），按冲撞方式调用。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BeastTraitGameTests {

    private static final Vec3 STAND = new Vec3(4.5, 1.0, 0.5);
    private static final Vec3 PATH_STAND = new Vec3(4.5, 15.0 / 16.0, 0.5);
    private static final Vec3 SOUTH = new Vec3(0.0, 0.0, 1.0);
    private static final double PATH = 8.0D;
    private static final BlockPos FIRST = new BlockPos(4, 1, 2);
    private static final BlockPos FIRST_HEAD = new BlockPos(4, 2, 2);
    private static final BlockPos GRASS = new BlockPos(4, 1, 4);
    private static final BlockPos WALL = new BlockPos(4, 1, 5);
    private static final BlockPos PAST_WALL = new BlockPos(4, 1, 7);
    private static final BlockPos FLOOR = new BlockPos(4, 0, 2);
    private static final BlockPos UNDERFOOT = new BlockPos(4, 0, 0);
    private static final BlockPos FLOOR_AHEAD = new BlockPos(4, 0, 1);
    private static final BlockPos OBSTACLE = new BlockPos(4, 1, 3);
    private static final int GU_HUNGER = 50;
    private static final int RANK_FIVE_TIER = 4;
    private static final int ALL_OUT_TICKS = 200;
    private static final int SHIELD_TICKS = 30 * Ticks.SECOND;
    private static final int BULL_TICKS = 10 * Ticks.SECOND;
    private static final double DELTA = 1.0E-4D;

    private BeastTraitGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void theHorsePointsAndAllOutAddToTheDash(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        assertEquals(4.5D, EpicFightIntegration.getDashScale(player), DELTA);
        assertEquals(1.0D, CrashSources.of(player).getPlaySpeed(), DELTA);

        grant(player, BeastStrength.GALLOPING_HORSE);
        double horse = 4.5D * (2.0D + Math.sqrt(48.0D) / 10.0D);
        assertEquals(horse, EpicFightIntegration.getDashScale(player), DELTA);
        assertEquals(1.5D, CrashSources.of(player).getPlaySpeed(), DELTA);

        allOut(player);
        assertEquals(horse + 4.5D, EpicFightIntegration.getDashScale(player), DELTA);
        assertEquals(2.0D, CrashSources.of(player).getPlaySpeed(), DELTA);
        player.removeEffect(ModEffects.ALL_OUT_EFFORT_GU);
        assertEquals(horse, EpicFightIntegration.getDashScale(player), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theBullLengthensEveryCrashGuButNotTheStrengthQiGu(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        grant(player, BeastStrength.GREEN_BULL);

        assertBuff(helper, player, ModItems.HORIZONTAL_CRASH_GU_3.get(), ModEffects.HORIZONTAL_CRASH_GU,
                30 * Ticks.SECOND + BULL_TICKS);
        assertBuff(helper, player, ModItems.VERTICAL_CRASH_GU_3.get(), ModEffects.VERTICAL_CRASH_GU,
                30 * Ticks.SECOND + BULL_TICKS);
        assertBuff(helper, player, ModItems.CHARGING_CRASH_GU_5.get(), ModEffects.CHARGING_CRASH_GU,
                60 * Ticks.SECOND + BULL_TICKS);
        assertBuff(helper, player, ModItems.STRENGTH_QI_GU_3.get(), ModEffects.STRENGTH_QI_GU,
                StrengthQiGuEffect.DURATION_TICKS);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theTurtleRaisesAShieldWithHalfItsPointsAgain(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ShieldService.raise(player, ShieldType.HEAVENLY_MUGWORT, 180.0F, SHIELD_TICKS);
        assertEquals(180.0D, shield(player).points(), DELTA);

        grant(player, BeastStrength.STONE_TURTLE);
        ShieldService.raise(player, ShieldType.HEAVENLY_MUGWORT, 180.0F, SHIELD_TICKS);
        ShieldLayer layer = shield(player);
        assertEquals(270.0D, layer.points(), DELTA);
        assertEquals(270.0D, layer.max(), DELTA);
        assertEquals(SHIELD_TICKS, ShieldService.getTicksLeft(player, ShieldType.HEAVENLY_MUGWORT));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theTwoBoarsBreakThePathUpToTheFirstUnbreakableBlock(GameTestHelper helper) {
        ServerPlayer player = boars(helper);
        helper.setBlock(FIRST, Blocks.STONE);
        helper.setBlock(FIRST_HEAD, Blocks.DIRT);
        helper.setBlock(GRASS.below(), Blocks.DIRT);
        helper.setBlock(GRASS, Blocks.SHORT_GRASS);
        helper.setBlock(WALL, Blocks.OBSIDIAN);
        helper.setBlock(PAST_WALL, Blocks.STONE);

        assertEquals(2, clear(player), "the path broke other than the stone and the dirt");
        assertBlock(helper, FIRST, Blocks.AIR);
        assertBlock(helper, FIRST_HEAD, Blocks.AIR);
        assertBlock(helper, GRASS, Blocks.SHORT_GRASS);
        assertBlock(helper, WALL, Blocks.OBSIDIAN);
        assertBlock(helper, PAST_WALL, Blocks.STONE);
        assertBlock(helper, FLOOR, Blocks.STONE);
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(helper.absolutePos(FIRST)).inflate(2.0D));
        assertTrue(drops.size() >= 2, "the broken blocks dropped " + drops.size() + " items");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theBoarsLeaveTheFloorEdgeAStepBelowTheFeet(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        helper.setBlock(UNDERFOOT, Blocks.DIRT_PATH);
        ServerPlayer player = boars(helper, PATH_STAND);
        helper.setBlock(OBSTACLE, Blocks.STONE);

        assertEquals(1, clear(player), "the path broke other than the stone ahead");
        assertBlock(helper, FLOOR_AHEAD, Blocks.STONE);
        assertBlock(helper, FLOOR, Blocks.STONE);
        assertBlock(helper, OBSTACLE, Blocks.AIR);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aChestStopsThePathAndAdventureModeBreaksNothing(GameTestHelper helper) {
        ServerPlayer player = boars(helper);
        helper.setBlock(FIRST, Blocks.CHEST);
        helper.setBlock(WALL, Blocks.STONE);

        assertEquals(0, clear(player), "the path broke past a chest");
        assertBlock(helper, FIRST, Blocks.CHEST);
        assertBlock(helper, WALL, Blocks.STONE);

        helper.setBlock(FIRST, Blocks.STONE);
        player.setGameMode(GameType.ADVENTURE);
        assertEquals(0, clear(player), "adventure mode broke a block");
        assertBlock(helper, FIRST, Blocks.STONE);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void mobGriefingOffKeepsTheBoarsFromBreaking(GameTestHelper helper) {
        ServerPlayer player = boars(helper);
        helper.setBlock(FIRST, Blocks.STONE);
        GameRules.BooleanValue griefing = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean was = griefing.get();
        try {
            griefing.set(false, helper.getLevel().getServer());
            assertEquals(0, clear(player), "mobGriefing off broke a block");
        } finally {
            griefing.set(was, helper.getLevel().getServer());
        }
        assertBlock(helper, FIRST, Blocks.STONE);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void allOutWithoutTheBoarsBreaksOnlySoftBlocks(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.setOnGround(true);
        grant(player, BeastStrength.BEAR);
        helper.setBlock(FIRST, Blocks.DIRT);
        helper.setBlock(FIRST_HEAD, Blocks.DIRT);
        helper.setBlock(OBSTACLE, Blocks.STONE);
        helper.setBlock(GRASS, Blocks.DIRT);
        assertEquals(0, clear(player), "40 points without All-Out Effort broke the path");

        allOut(player);
        assertEquals(2, clear(player), "All-Out Effort on 40 points broke other than the two dirt blocks");
        assertBlock(helper, FIRST, Blocks.AIR);
        assertBlock(helper, FIRST_HEAD, Blocks.AIR);
        assertBlock(helper, OBSTACLE, Blocks.STONE);
        assertBlock(helper, GRASS, Blocks.DIRT);
        helper.succeed();
    }

    private static int clear(ServerPlayer player) {
        return CrashBreach.clear(player, SOUTH, PATH, CrashSources.of(player).getMaxHardness());
    }

    private static void assertBuff(GameTestHelper helper, ServerPlayer player, Item item,
                                   Holder<MobEffect> effect, int ticks) {
        ItemStack gu = hold(player, item, GU_HUNGER);
        click(helper, player, gu);
        assertEquals(ticks, TimedEffects.getBuffTicksLeft(player, effect));
    }

    private static void assertBlock(GameTestHelper helper, BlockPos pos, Block block) {
        assertTrue(helper.getBlockState(pos).is(block),
                pos + " is " + helper.getBlockState(pos) + ", not " + block);
    }

    private static ShieldLayer shield(ServerPlayer player) {
        return assertNotNull(ShieldService.find(player, ShieldType.HEAVENLY_MUGWORT), () -> "no shield raised");
    }

    private static void grant(ServerPlayer player, BeastStrength beast) {
        assertTrue(PathStrengthService.grant(player, beast), "the " + beast + " was refused");
    }

    private static void allOut(ServerPlayer player) {
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, ALL_OUT_TICKS, RANK_FIVE_TIER));
    }

    private static ServerPlayer boars(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        return boars(helper, STAND);
    }

    private static ServerPlayer boars(GameTestHelper helper, Vec3 stand) {
        ServerPlayer player = player(helper, stand);
        player.setOnGround(true);
        grant(player, BeastStrength.WHITE_BOAR);
        grant(player, BeastStrength.BLACK_BOAR);
        assertTrue(PathStrengthService.get(player).hasTwoBoars(), "the two boars are not held together");
        return player;
    }

    private static ServerPlayer player(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        return player(helper, STAND);
    }

    private static ServerPlayer player(GameTestHelper helper, Vec3 stand) {
        return GameTestGu.awakened(GameTestPlayers.survival(helper, "gzr-beast-trait-gametest").at(stand),
                Rank.FIVE);
    }
}
