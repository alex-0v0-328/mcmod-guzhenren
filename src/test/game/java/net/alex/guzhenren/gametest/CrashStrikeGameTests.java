package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.strength.AreaDamage;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.ComboRound;
import net.alex.guzhenren.gameplay.path.strength.CrashSources;
import net.alex.guzhenren.gameplay.path.strength.CrashStrike;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestGu.inSlot;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 终点破坏打击[Crash Strike]：五转活玩家（最大生命 100，满血）在场景西缘朝东。
 * <ul>
 *   <li>两猪 48 点（承受力 56 内，无过载反噬）：空手拳击 7，打击倍率 0.75 打 5.25，开全力以赴也不变；收尾招中也不抽
 *       虚影，20 tick 内再打落空。</li>
 *   <li>溢出结算：两猪加虎 112 点，打击 11.25，最近的猪吃整击，下一只吃剩余 1.25；打中结算一次过载反噬（超出 56 点，
 *       扣最大生命的 50%），挥空与冷却内再打不扣。</li>
 *   <li>苦力：生命 40% 时伤害倍率 +30%，结算 62 点，打击 6.5625 并按 62 点结算过载反噬；冲撞距离、破坏硬度与打击半径仍读总
 *       力量点 48，不随苦力变化。</li>
 *   <li>手持钻石剑（仿装备刷新挂上其攻击修饰）时打击仍按空手。</li>
 *   <li>冲撞流程：无力量（半径 3 格、打 0.5）从西缘冲 4 格，移动那 tick 与其后第一个静止 tick 不打，连续第二个静止 tick
 *       才打到停点前方的猪；无力量路程 6.05 格，一 tick 跳出 12 格（传送）作废，此后静止也不打。</li>
 *   <li>两猪硬度 3、半径 2.9 格：终点整圆前后的石头都碎，黑曜石与地面留。</li>
 *   <li>起点坑：从 (2.5, 2.5) 起步，一 tick 落到 4.95 格外即挖（越过半径 + 1.2），起点四周地面空、坑外与终点脚下地面留；
 *       只冲 1 格就停时停下那 tick 补挖，脚下那列地面留。</li>
 *   <li>每个测试用独立档名：打击冷却按玩家 UUID 记，同 tick 并排的测试共用游戏时间。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrashStrikeGameTests {

    private CrashStrikeGameTests() {}

    private static final Vec3 WEST = new Vec3(1.5D, 1.0D, 4.5D);
    private static final Vec3 NEAR = new Vec3(3.5D, 1.0D, 4.5D);
    private static final Vec3 STOP = new Vec3(5.5D, 1.0D, 4.5D);
    private static final Vec3 EAST_EDGE = new Vec3(7.5D, 1.0D, 4.5D);
    private static final Vec3 FAR_WEST = new Vec3(-10.5D, 1.0D, 4.5D);
    private static final Vec3 CENTER = new Vec3(4.5D, 1.0D, 4.5D);
    private static final Vec3 PIT_START = new Vec3(2.5D, 1.0D, 2.5D);
    private static final Vec3 PIT_STOP = new Vec3(6.0D, 1.0D, 6.0D);
    private static final Vec3 ONE_EAST = new Vec3(5.5D, 1.0D, 4.5D);
    private static final Vec3 EAST = new Vec3(1.0D, 0.0D, 0.0D);
    private static final int BOAR_POINTS = 48;
    private static final int HEAVY_POINTS = 112;
    private static final int HARDSHIP_POINTS = 62;
    private static final int CAPACITY = 56;
    private static final float TWO_BOARS_RATIO = 0.75F;
    private static final float PIG_HEALTH = 10.0F;
    private static final int BAG_SLOT = 9;
    private static final int HUNGER = 98;
    private static final int ALL_OUT_TICKS = 200;
    private static final int RANK_FIVE_TIER = 4;
    private static final double DELTA = 1.0E-3D;

    @GameTest(template = "empty9x9x9")
    public static void allOutEffortLeavesTheStrikeAloneAndDrawsNoPhantom(GameTestHelper helper) {
        ServerPlayer player = boars(helper, "gzr-crash-strike-round");
        CrashSources plain = CrashSources.of(player);
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, ALL_OUT_TICKS, RANK_FIVE_TIER));
        assertEquals(plain, CrashSources.of(player), "All-Out Effort moved the crash sources");
        Pig pig = idlePig(helper, NEAR);
        float blow = blow(BOAR_POINTS, TWO_BOARS_RATIO);

        ComboRound.beginStrike(player, true);
        List<LivingEntity> landed = CrashStrike.strike(player, EAST, CrashSources.of(player));
        assertTrue(landed.contains(pig), "the strike missed the pig ahead");
        assertEquals(pig.getMaxHealth() - blow, pig.getHealth(), DELTA);
        assertTrue(pig.getDeltaMovement().x > 0.0D, () -> "the strike shoved " + pig.getDeltaMovement());
        assertTrue(ComboRound.getFamilies(player).isEmpty(), "the strike drew a phantom at All-Out Effort's 100%");

        assertTrue(CrashStrike.strike(player, EAST, CrashSources.of(player)).isEmpty(), "the cooldown let it strike");
        assertEquals(pig.getMaxHealth() - blow, pig.getHealth(), DELTA);
        assertEquals(player.getMaxHealth(), player.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theStrikeSpillsFromTheNearestTargetAndOverloadsTheBodyOnce(GameTestHelper helper) {
        ServerPlayer player = heavy(helper, "gzr-crash-strike-spill");
        Pig near = idlePig(helper, NEAR);
        Pig far = idlePig(helper, STOP);
        float blow = blow(HEAVY_POINTS, TWO_BOARS_RATIO);

        assertEquals(List.of(near, far), CrashStrike.strike(player, EAST, CrashSources.of(player)));
        assertEquals(0.0D, near.getHealth(), DELTA);
        assertEquals(PIG_HEALTH - (blow - PIG_HEALTH), far.getHealth(), DELTA);
        float left = player.getMaxHealth() - recoil(player, HEAVY_POINTS);
        assertEquals(left, player.getHealth(), DELTA);

        assertTrue(CrashStrike.strike(player, EAST, CrashSources.of(player)).isEmpty(), "the cooldown let it strike");
        assertEquals(left, player.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aStrikeThatLandsOnNothingCostsNoHealth(GameTestHelper helper) {
        ServerPlayer player = heavy(helper, "gzr-crash-strike-whiff");

        assertTrue(CrashStrike.strike(player, EAST, CrashSources.of(player)).isEmpty(), "the strike hit thin air");
        assertTrue(CrashStrike.isCooling(player), "the whiffed strike did not fire");
        assertEquals(player.getMaxHealth(), player.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void hardshipRaisesTheStrikeAndLeavesTheDashAlone(GameTestHelper helper) {
        ServerPlayer player = boars(helper, "gzr-crash-strike-hardship");
        CrashSources before = CrashSources.of(player);
        inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        player.setHealth(player.getMaxHealth() * 0.4F);
        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "Hardship Strength did not start");
        assertEquals(HARDSHIP_POINTS, StrengthService.getSettledPoints(player));

        CrashSources sources = CrashSources.of(player);
        assertEquals(BOAR_POINTS, sources.points());
        assertEquals(before, sources);
        assertEquals(before.getTravel(), sources.getTravel(), DELTA);
        assertEquals(before.getMaxHardness(), sources.getMaxHardness(), DELTA);
        assertEquals(AreaDamage.getRange(BOAR_POINTS), sources.getStrikeRange(), DELTA);

        Pig pig = idlePig(helper, NEAR);
        float health = player.getHealth();
        assertEquals(List.of(pig), CrashStrike.strike(player, EAST, before));
        assertEquals(PIG_HEALTH - blow(HARDSHIP_POINTS, TWO_BOARS_RATIO), pig.getHealth(), DELTA);
        assertEquals(health - recoil(player, HARDSHIP_POINTS), player.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theStrikeIgnoresTheHeldWeapon(GameTestHelper helper) {
        ServerPlayer player = boars(helper, "gzr-crash-strike-weapon");
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        sword.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null && !instance.hasModifier(modifier.id())) instance.addTransientModifier(modifier);
        });
        assertEquals(13.0D, AttackDamageService.getFistDamage(player, 48), DELTA);
        assertEquals(7.0D, AttackDamageService.getBareFistDamage(player, 48), DELTA);
        Pig pig = idlePig(helper, NEAR);

        CrashStrike.strike(player, EAST, CrashSources.of(player));
        assertEquals(pig.getMaxHealth() - 5.25D, pig.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theDashStrikesOnlyWhereItStops(GameTestHelper helper) {
        ServerPlayer player = player(helper, "gzr-crash-strike-stop");
        Pig pig = idlePig(helper, EAST_EDGE);

        CrashStrike.start(player, EAST, CrashSources.of(player));
        GameTestPlayers.place(player, helper, STOP);
        GameTestPlayers.serverTicks(player, 2);
        assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);

        GameTestPlayers.serverTicks(player, 1);
        assertEquals(pig.getMaxHealth() - 0.5D, pig.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aTeleportMidDashDropsTheStrike(GameTestHelper helper) {
        ServerPlayer player = player(helper, "gzr-crash-strike-teleport");

        CrashStrike.start(player, EAST, CrashSources.of(player));
        GameTestPlayers.place(player, helper, FAR_WEST);
        try {
            GameTestPlayers.serverTicks(player, 4);
            assertFalse(CrashStrike.isCooling(player), "the strike fired where the teleport landed");
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theStrikeShattersADiscAroundTheStopAndSparesTheFloor(GameTestHelper helper) {
        ServerPlayer player = boars(helper, "gzr-crash-strike-shatter");
        GameTestPlayers.place(player, helper, CENTER);
        BlockPos ahead = new BlockPos(6, 1, 4);
        BlockPos behindHead = new BlockPos(2, 2, 4);
        BlockPos obsidian = new BlockPos(4, 1, 6);
        helper.setBlock(ahead, Blocks.STONE);
        helper.setBlock(behindHead, Blocks.STONE);
        helper.setBlock(obsidian, Blocks.OBSIDIAN);

        CrashStrike.strike(player, EAST, CrashSources.of(player));
        assertBlock(helper, ahead, Blocks.AIR);
        assertBlock(helper, behindHead, Blocks.AIR);
        assertBlock(helper, obsidian, Blocks.OBSIDIAN);
        assertBlock(helper, new BlockPos(4, 0, 4), Blocks.STONE);
        assertBlock(helper, new BlockPos(6, 0, 4), Blocks.STONE);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theDashDigsThePitOnceItClearsTheEdge(GameTestHelper helper) {
        ServerPlayer player = boars(helper, "gzr-crash-strike-pit");
        GameTestPlayers.place(player, helper, PIT_START);
        BlockPos besideStop = new BlockPos(7, 1, 6);
        helper.setBlock(besideStop, Blocks.STONE);

        CrashStrike.start(player, EAST, CrashSources.of(player));
        assertBlock(helper, new BlockPos(2, 0, 2), Blocks.STONE);
        GameTestPlayers.place(player, helper, PIT_STOP);
        GameTestPlayers.serverTicks(player, 1);
        assertBlock(helper, new BlockPos(2, 0, 2), Blocks.AIR);
        assertBlock(helper, new BlockPos(2, 0, 4), Blocks.AIR);
        assertBlock(helper, new BlockPos(5, 0, 5), Blocks.STONE);
        assertBlock(helper, besideStop, Blocks.STONE);

        GameTestPlayers.serverTicks(player, 2);
        assertBlock(helper, besideStop, Blocks.AIR);
        assertBlock(helper, new BlockPos(6, 0, 6), Blocks.STONE);
        assertBlock(helper, new BlockPos(7, 0, 6), Blocks.STONE);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aDashStoppedInsideThePitKeepsTheFloorUnderfoot(GameTestHelper helper) {
        ServerPlayer player = boars(helper, "gzr-crash-strike-short");
        GameTestPlayers.place(player, helper, CENTER);

        CrashStrike.start(player, EAST, CrashSources.of(player));
        GameTestPlayers.place(player, helper, ONE_EAST);
        GameTestPlayers.serverTicks(player, 2);
        assertBlock(helper, new BlockPos(4, 0, 4), Blocks.STONE);

        GameTestPlayers.serverTicks(player, 1);
        assertBlock(helper, new BlockPos(4, 0, 4), Blocks.AIR);
        assertBlock(helper, new BlockPos(3, 0, 4), Blocks.AIR);
        assertBlock(helper, new BlockPos(5, 0, 4), Blocks.STONE);
        helper.succeed();
    }

    private static float blow(int settledPoints, float ratio) {
        return (1.0F + settledPoints * 0.125F) * ratio;
    }

    private static float recoil(ServerPlayer player, int settledPoints) {
        return (float) Math.max(0, settledPoints - CAPACITY) / CAPACITY * player.getMaxHealth() * 0.5F;
    }

    private static void assertBlock(GameTestHelper helper, BlockPos pos, Block block) {
        assertTrue(helper.getBlockState(pos).is(block),
                pos + " is " + helper.getBlockState(pos) + ", not " + block);
    }

    private static Pig idlePig(GameTestHelper helper, Vec3 at) {
        Pig pig = helper.spawn(EntityType.PIG, at);
        pig.setNoAi(true);
        pig.setNoGravity(true);
        pig.setDeltaMovement(Vec3.ZERO);
        return pig;
    }

    private static ServerPlayer boars(GameTestHelper helper, String name) {
        ServerPlayer player = player(helper, name);
        assertTrue(PathStrengthService.grant(player, BeastStrength.WHITE_BOAR), "the white boar was refused");
        assertTrue(PathStrengthService.grant(player, BeastStrength.BLACK_BOAR), "the black boar was refused");
        assertEquals(BOAR_POINTS, CrashSources.of(player).points());
        return player;
    }

    private static ServerPlayer heavy(GameTestHelper helper, String name) {
        ServerPlayer player = boars(helper, name);
        assertTrue(PathStrengthService.grant(player, BeastStrength.TIGER), "the tiger was refused");
        assertEquals(HEAVY_POINTS, CrashSources.of(player).points());
        return player;
    }

    private static ServerPlayer player(GameTestHelper helper, String name) {
        GameTestPlayers.ground(helper);
        ServerPlayer player = GameTestGu.awakened(GameTestPlayers.survival(helper, name).pastSpawnInvulnerability()
                .at(WEST), Rank.FIVE);
        assertEquals(CAPACITY, StrengthService.getCapacity(player));
        player.setHealth(player.getMaxHealth());
        return player;
    }
}
