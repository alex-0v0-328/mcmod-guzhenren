package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.BeastEntity;
import net.alex.guzhenren.entity.beast.TigerEntity;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static net.alex.guzhenren.gametest.BeastScenarios.player;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;
import static net.alex.guzhenren.gametest.GameTestPlayers.PLAYER_MAX_HEALTH;
import static net.alex.guzhenren.gametest.GameTestPlayers.ground;
import static net.alex.guzhenren.gametest.GameTestWorld.firstHit;

/**
 * 老虎主动狩猎、扑击、攻击、死亡、持久化测试；与熊共用的场景在 {@link BeastScenarios}。
 * <ul>
 *   <li>和平难度的主动狩猎不测，因共享 GameTest 世界难度是服务端全局；只测同步的「和平难度阻止反击」接缝。</li>
 *   <li>熊虎夜间睡眠链在 {@code BearGameTests.beastNightSleepChainTransitions} 测，因 dayTime 全局共享。</li>
 *   <li>{@link #tigerPounceStoppedByTwoHighWall}：第 12 tick 起跳，空中撞墙，约第 14 tick 中止。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TigerGameTests {
    private static final BeastScenarios.Species TIGER =
            new BeastScenarios.Species("tiger", (helper, pos) -> helper.spawn(ModEntityTypes.TIGER.get(), pos));

    private TigerGameTests() {}

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void tigerHuntsPlayerProactively(GameTestHelper helper) {
        BeastScenarios.huntsPlayer(helper, TIGER);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void tigerHuntsLivestock(GameTestHelper helper) {
        BeastScenarios.huntsLivestock(helper, TIGER);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void tigerFirstLockRoarsAndIgnoresDamageDuringRoar(GameTestHelper helper) {
        BeastScenarios.firstLockRoarsAndIgnoresDamageDuringRoar(helper, TIGER);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 120)
    public static void tigerDirectionalHurtAfterRoar(GameTestHelper helper) {
        BeastScenarios.directionalHurtAfterRoar(helper, TIGER, 13.5D, 45L);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void tigerSwipeHitsAtEightTicks(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = player(helper, 4.5D, 2.5D);
        TigerEntity tiger = helper.spawn(ModEntityTypes.TIGER.get(), CENTER_POS);
        tiger.setTarget(player);
        tiger.tryStartAction(BeastEntity.Action.ATTACK_SWIPE);
        float initialHealth = player.getHealth();
        int[] hitTick = firstHit(helper, player, () -> (int) tiger.getActionTicks());
        helper.runAtTickTime(7L, () -> helper.assertValueEqual(player.getHealth(), initialHealth,
                "tiger swipe damaged before tick 8"));
        helper.runAtTickTime(12L, () -> {
            helper.assertValueEqual(hitTick[0], 8, "tiger swipe hit tick");
            helper.assertValueEqual(player.getHealth(), 12.0F, "tiger swipe damage");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void tigerPounceHitsBetweenTwelveAndEighteenTicks(GameTestHelper helper) {
        ground(helper);
        TigerEntity tiger = helper.spawn(ModEntityTypes.TIGER.get(), new BlockPos(1, 1, 4));
        ServerPlayer player = player(helper, 6.0D, 4.5D);
        tiger.setTarget(player);
        tiger.tryStartAction(BeastEntity.Action.ATTACK_HEAVY);

        Vec3 expectedDirection = player.position().subtract(tiger.position()).normalize();
        float initialHealth = player.getHealth();
        int[] hitTick = { -1 };
        Vec3[] capturedKnockback = { Vec3.ZERO };
        helper.onEachTick(() -> {
            if (hitTick[0] < 0 && player.getHealth() < initialHealth) {
                hitTick[0] = (int) tiger.getActionTicks();
                capturedKnockback[0] = player.getDeltaMovement();
            }
            player.setDeltaMovement(Vec3.ZERO);
        });

        helper.runAtTickTime(11L, () -> helper.assertValueEqual(player.getHealth(), initialHealth,
                "tiger pounce damaged before its leap"));
        helper.runAtTickTime(18L, () -> {
            helper.assertTrue(hitTick[0] >= 12 && hitTick[0] <= 18,
                    "tiger pounce hit outside ticks 12-18: " + hitTick[0]);
            helper.assertValueEqual(player.getHealth(), 8.0F, "tiger pounce damage");
            Vec3 move = capturedKnockback[0];
            double dot = move.x * expectedDirection.x + move.z * expectedDirection.z;
            helper.assertTrue(dot > 0.0D, "tiger pounce knockback was not along leap direction");
            helper.assertTrue(move.horizontalDistanceSqr() > 1.0E-6D,
                    "tiger pounce did not apply horizontal knockback");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void tigerPounceStoppedByTwoHighWall(GameTestHelper helper) {
        ground(helper);
        for (int z = 3; z <= 5; z++) {
            helper.setBlock(new BlockPos(4, 1, z), Blocks.STONE);
            helper.setBlock(new BlockPos(4, 2, z), Blocks.STONE);
        }
        TigerEntity tiger = helper.spawn(ModEntityTypes.TIGER.get(), new BlockPos(1, 1, 4));
        ServerPlayer player = player(helper, 6.0D, 4.5D);
        tiger.setTarget(player);
        tiger.tryStartAction(BeastEntity.Action.ATTACK_HEAVY);

        helper.onEachTick(() -> player.setDeltaMovement(Vec3.ZERO));
        helper.runAtTickTime(16L, () -> helper.assertTrue(tiger.getAction() != BeastEntity.Action.ATTACK_HEAVY,
                "tiger pounce stayed active after hitting the wall"));
        helper.runAtTickTime(20L, () -> {
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH,
                    "tiger pounce hit through a two-high wall");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void tigerSwipeAndHeavyCooldownsBlockOverlappingAttacks(GameTestHelper helper) {
        BeastScenarios.swipeAndHeavyCooldownsBlockOverlappingAttacks(helper, TIGER, 17L);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void tigerDeathRemovesAtThirtyTwoTicks(GameTestHelper helper) {
        BeastScenarios.deathRemovesAt(helper, TIGER, 32);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void tigerDropsNoItemsAndExperienceOnce(GameTestHelper helper) {
        BeastScenarios.dropsNoItemsAndExperienceOnce(helper, TIGER);
    }

    @GameTest(template = "empty9x9x9")
    public static void tigerReloadClearsActionKeepsCooldowns(GameTestHelper helper) {
        BeastScenarios.reloadClearsActionKeepsCooldowns(helper, TIGER);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void tigerDoesNotAcquireTargetOnPeaceful(GameTestHelper helper) {
        BeastScenarios.doesNotAcquireTargetOnPeaceful(helper, TIGER);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void tigerClearsTargetOutsideFollowRange(GameTestHelper helper) {
        BeastScenarios.clearsTargetOutsideFollowRange(helper, TIGER, -20.0D);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 180)
    public static void tigerDaytimeSitReturnsToIdleAfterOneHundredSixtyTicks(GameTestHelper helper) {
        BeastScenarios.daytimeSitReturnsToIdleAfterOneHundredSixtyTicks(helper, TIGER);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void tigerHurtDuringSitSnapsToCombat(GameTestHelper helper) {
        BeastScenarios.hurtDuringSitSnapsToCombat(helper, TIGER);
    }
}
