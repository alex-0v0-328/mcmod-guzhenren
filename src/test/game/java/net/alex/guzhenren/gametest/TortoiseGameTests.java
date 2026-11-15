package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.TortoiseEntity;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;
import static net.alex.guzhenren.gametest.GameTestPlayers.PLAYER_MAX_HEALTH;
import static net.alex.guzhenren.gametest.GameTestPlayers.ground;

/**
 * 龟测试：温顺性情、进食与受伤姿态、石龟击退抗性、共用死亡周期、行走速度越过渲染器行走阈值（取水平速率的一半，即
 * GeckoLib 判定量沿轴行走时的最坏值）。夜间睡眠附在 {@code BearGameTests.beastNightSleepChainTransitions} 里测，因
 * GameTest 世界 {@code dayTime} 共享全局。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TortoiseGameTests {

    private TortoiseGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void tortoiseNeverTargetsANearbyPlayer(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 2.0D);
        TortoiseEntity tortoise = spawn(helper, CENTER_POS);
        helper.onEachTick(() -> tortoise.getNavigation().stop());

        helper.runAtTickTime(50L, () -> {
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH, "a tortoise damaged the player");
            helper.assertTrue(tortoise.getTarget() == null, "a tortoise took a target");
            helper.assertTrue(tortoise.getAction() == TortoiseEntity.Action.IDLE
                            || tortoise.getAction() == TortoiseEntity.Action.EAT,
                    "a tortoise left its ambient poses: " + tortoise.getAction());
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 40)
    public static void tortoiseHurtPlaysOnePoseAndKeepsNoTarget(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 2.0D);
        TortoiseEntity tortoise = spawn(helper, CENTER_POS);
        helper.assertTrue(tortoise.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F),
                "the tortoise did not accept damage");
        helper.assertValueEqual(tortoise.getAction(), TortoiseEntity.Action.HURT, "a hit did not play the hurt pose");
        int sequence = tortoise.getActionSequence();
        helper.runAtTickTime(5L, () -> {
            tortoise.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.assertValueEqual(tortoise.getActionSequence(), sequence, "a second hit restarted the hurt pose");
            helper.assertTrue(tortoise.getTarget() == null, "a hurt tortoise took a target");
        });
        helper.runAtTickTime(TortoiseEntity.HURT_ACTION_TICKS + 2L, () -> {
            helper.assertValueEqual(tortoise.getAction(), TortoiseEntity.Action.IDLE, "the hurt pose did not end");
            helper.assertTrue(tortoise.getTarget() == null, "a hurt tortoise took a target");
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH, "a tortoise retaliated");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 40)
    public static void tortoiseProjectileHurtKeepsNoTarget(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 0.5D);
        TortoiseEntity tortoise = spawn(helper, CENTER_POS);
        Arrow arrow = GameTestWorld.arrow(helper, player);
        arrow.setOwner(player);
        helper.assertTrue(tortoise.hurt(helper.getLevel().damageSources().arrow(arrow, player), 1.0F),
                "the arrow did not damage the tortoise");
        helper.runAtTickTime(8L, () -> {
            helper.assertTrue(tortoise.getTarget() == null, "an arrow made the tortoise target its shooter");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void tortoiseEatHoldsStillAndEndsAtFortyTicks(GameTestHelper helper) {
        ground(helper);
        TortoiseEntity tortoise = spawn(helper, CENTER_POS);
        helper.assertTrue(tortoise.tryStartAction(TortoiseEntity.Action.EAT), "eat did not start");
        Vec3 start = tortoise.position();
        helper.runAtTickTime(TortoiseEntity.EAT_ACTION_TICKS - 1L, () -> {
            helper.assertValueEqual(tortoise.getAction(), TortoiseEntity.Action.EAT, "eat ended early");
            helper.assertTrue(tortoise.position().distanceToSqr(start) < 1.0E-4D, "an eating tortoise walked");
        });
        helper.runAtTickTime(TortoiseEntity.EAT_ACTION_TICKS + 1L, () -> {
            helper.assertValueEqual(tortoise.getAction(), TortoiseEntity.Action.IDLE, "eat did not end");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 40)
    public static void tortoiseHurtInterruptsEat(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 2.0D);
        TortoiseEntity tortoise = spawn(helper, CENTER_POS);
        helper.assertTrue(tortoise.tryStartAction(TortoiseEntity.Action.EAT), "eat did not start");
        helper.runAtTickTime(5L, () -> {
            tortoise.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.assertValueEqual(tortoise.getAction(), TortoiseEntity.Action.HURT, "a hit did not interrupt eat");
        });
        helper.runAtTickTime(5L + TortoiseEntity.HURT_ACTION_TICKS + 2L, () -> {
            helper.assertValueEqual(tortoise.getAction(), TortoiseEntity.Action.IDLE,
                    "the tortoise did not return to idle after the hurt pose");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void stoneTortoiseResistsKnockbackWhereTheLandTortoiseDoesNot(GameTestHelper helper) {
        ground(helper);
        TortoiseEntity land = spawn(helper, new BlockPos(2, 1, 4));
        TortoiseEntity stone = helper.spawn(ModEntityTypes.STONE_TORTOISE.get(), new BlockPos(6, 1, 4));
        land.knockback(1.0D, 1.0D, 0.0D);
        stone.knockback(1.0D, 1.0D, 0.0D);
        helper.assertTrue(land.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D,
                "the land tortoise shrugged off knockback");
        helper.assertTrue(stone.getDeltaMovement().horizontalDistanceSqr() < 1.0E-8D,
                "the stone tortoise was knocked back");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void tortoiseReloadClearsTransientPose(GameTestHelper helper) {
        ground(helper);
        TortoiseEntity original = spawn(helper, CENTER_POS);
        original.tryStartAction(TortoiseEntity.Action.EAT);
        CompoundTag saved = new CompoundTag();
        original.saveWithoutId(saved);
        original.discard();

        TortoiseEntity restored = spawn(helper, new BlockPos(6, 1, 6));
        restored.load(saved);
        helper.assertValueEqual(restored.getAction(), TortoiseEntity.Action.IDLE, "a reloaded tortoise kept its pose");
        helper.assertValueEqual(restored.getActionTicks(), 0L, "reloaded tortoise action ticks");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void tortoiseDeathPersistsUntilThirtyTwoTicksAndDropsOnlyExperience(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 8.0D);
        TortoiseEntity tortoise = spawn(helper, CENTER_POS);
        tortoise.tryStartAction(TortoiseEntity.Action.EAT);
        List<Entity> drops = GameTestWorld.entitiesAddedBy(helper,
                () -> tortoise.hurt(helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE));
        helper.assertValueEqual(tortoise.getAction(), TortoiseEntity.Action.DEATH, "lethal damage did not enter death");
        helper.assertTrue(drops.stream().noneMatch(entity -> entity instanceof ItemEntity), "a tortoise dropped items");
        helper.assertTrue(drops.stream().anyMatch(entity -> entity instanceof ExperienceOrb),
                "a tortoise killed by a player dropped no experience");
        helper.runAtTickTime(TortoiseEntity.DEATH_REMOVE_TICK - 4L, () -> {
            helper.assertTrue(!tortoise.isRemoved(), "the tortoise was removed before death tick 32");
            helper.assertValueEqual(tortoise.getAction(), TortoiseEntity.Action.DEATH, "death was interrupted");
        });
        helper.runAtTickTime(TortoiseEntity.DEATH_REMOVE_TICK, () -> {
            helper.assertTrue(tortoise.isRemoved(), "the tortoise remained after death tick 32");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void walkingTortoisesExceedTheWalkAnimationThreshold(GameTestHelper helper) {
        ground(helper);
        TortoiseEntity land = spawn(helper, new BlockPos(1, 1, 2));
        TortoiseEntity stone = helper.spawn(ModEntityTypes.STONE_TORTOISE.get(), new BlockPos(1, 1, 6));
        double[] fastest = new double[2];
        helper.onEachTick(() -> {
            walkEast(helper, land, 2);
            walkEast(helper, stone, 6);
            fastest[0] = Math.max(fastest[0], land.getDeltaMovement().horizontalDistance());
            fastest[1] = Math.max(fastest[1], stone.getDeltaMovement().horizontalDistance());
        });
        helper.runAtTickTime(40L, () -> {
            helper.assertTrue(fastest[0] / 2.0D >= TortoiseEntity.WALK_ANIM_THRESHOLD,
                    "a walking land tortoise stays under the walk animation threshold: " + fastest[0]);
            helper.assertTrue(fastest[1] / 2.0D >= TortoiseEntity.WALK_ANIM_THRESHOLD,
                    "a walking stone tortoise stays under the walk animation threshold: " + fastest[1]);
            helper.succeed();
        });
    }

    private static void walkEast(GameTestHelper helper, TortoiseEntity tortoise, int z) {
        if (tortoise.getNavigation().isInProgress()) return;
        Vec3 target = helper.absoluteVec(new Vec3(7.5D, 1.0D, z + 0.5D));
        tortoise.getNavigation().moveTo(target.x, target.y, target.z, 1.0D);
    }

    private static TortoiseEntity spawn(GameTestHelper helper, BlockPos pos) {
        return helper.spawn(ModEntityTypes.TORTOISE.get(), pos);
    }

    private static ServerPlayer survivalMock(GameTestHelper helper, double x, double z) {
        return GameTestPlayers.survival(helper, "gzr-tortoise-gametest").pastSpawnInvulnerability()
                .at(new Vec3(x, 1.0D, z));
    }
}
