package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.ChargerEntity;
import net.alex.guzhenren.entity.beast.ElephantEntity;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;
import static net.alex.guzhenren.gametest.GameTestPlayers.PLAYER_MAX_HEALTH;
import static net.alex.guzhenren.gametest.GameTestPlayers.ground;
import static net.alex.guzhenren.gametest.GameTestPlayers.place;

/**
 * 象：与其他冲撞型野兽共用的反击性情；踩踏命中附近除象外一切；象鼻横扫；三种近身攻击的顺序；匹配步幅的行走与追击速度；
 * 皮革掉落；冷却存档。冲撞型野兽机制仍归 {@code WildBoarGameTests}。
 * <ul>
 *   <li>⚠ 测速只许测试驾驭导航：先强令游荡触发，再移除全部 {@code MOVE} 目标，仍有目标接管即红；漏移除时游荡抢走一 tick
 *       导航，追击峰值偶发跌出容差。</li>
 *   <li>行走到站后从静止起追，追击段约 13 tick 到终速的 99%；两段容差均约为步幅的 ±5%。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ElephantGameTests {

    private static final double BLOCKS_PER_SECOND_PER_SPEED_SQUARED = 43.17D;

    private ElephantGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void elephantsDoNotAttackFromProximity(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 2.0D);
        ElephantEntity elephant = spawn(helper, CENTER_POS);
        hold(helper, elephant);
        helper.runAtTickTime(50L, () -> {
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH, "a neutral elephant damaged the player");
            helper.assertTrue(elephant.getTarget() == null, "an elephant targeted a bystander");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void elephantAlertsThenTargetsOnlyItsAttacker(GameTestHelper helper) {
        ground(helper);
        ServerPlayer attacker = survivalMock(helper, 4.5D, 1.5D);
        ServerPlayer bystander = survivalMock(helper, 1.5D, 4.5D);
        ElephantEntity elephant = spawn(helper, CENTER_POS);
        helper.assertTrue(elephant.hurt(helper.getLevel().damageSources().playerAttack(attacker), 1.0F),
                "the elephant did not accept damage");
        helper.assertTrue(elephant.getTarget() == attacker, "the elephant did not target its attacker");
        helper.assertValueEqual(elephant.getAction(), ChargerEntity.Action.ALERT, "a first hit did not play the alert");
        helper.runAtTickTime(ChargerEntity.ALERT_ACTION_TICKS + 2L, () -> {
            helper.assertTrue(elephant.getTarget() == attacker, "the elephant lost its attacker after the alert");
            helper.assertTrue(elephant.getTarget() != bystander, "the elephant turned on a bystander");
            helper.assertTrue(elephant.getAction() != ChargerEntity.Action.ALERT, "the alert did not end");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void stompHitsEveryoneNearbyAtFourteenTicksForEighteenButSparesAnElephant(GameTestHelper helper) {
        ground(helper);
        ServerPlayer target = survivalMock(helper, 2.0D, 3.0D);
        ServerPlayer bystander = survivalMock(helper, 7.0D, 3.0D);
        ElephantEntity elephant = spawn(helper, new BlockPos(4, 1, 3));
        ElephantEntity herdMate = helper.spawn(ModEntityTypes.WHITE_ELEPHANT.get(), new BlockPos(4, 1, 6));
        hold(helper, elephant);
        hold(helper, herdMate);
        elephant.setTarget(target);
        helper.assertTrue(elephant.tryStartAction(ChargerEntity.Action.ATTACK_STOMP), "the stomp did not start");
        int[] hitTick = { -1 };
        helper.onEachTick(() -> {
            if (hitTick[0] < 0) {
                if (target.getHealth() < PLAYER_MAX_HEALTH) {
                    hitTick[0] = (int) elephant.getActionTicks();
                } else {
                    place(target, helper, new Vec3(2.0D, 1.0D, 3.5D));
                    place(bystander, helper, new Vec3(7.0D, 1.0D, 3.5D));
                }
            }
        });
        helper.runAtTickTime(30L, () -> {
            helper.assertValueEqual(hitTick[0], ElephantEntity.STOMP_HIT_TICK, "stomp hit tick");
            helper.assertValueEqual(target.getHealth(), PLAYER_MAX_HEALTH - ElephantEntity.STOMP_DAMAGE,
                    "stomp damage on the target");
            helper.assertValueEqual(bystander.getHealth(), PLAYER_MAX_HEALTH - ElephantEntity.STOMP_DAMAGE,
                    "stomp damage on a bystander in range");
            helper.assertValueEqual(herdMate.getHealth(), herdMate.getMaxHealth(), "the stomp hurt another elephant");
            helper.assertTrue(herdMate.getTarget() == null, "the stomp provoked another elephant");
            helper.assertValueEqual(elephant.getStompCooldown() > 0, true, "the stomp set no cooldown");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void trunkSweepHitsAtNineTicksForEightAndKnocksFlat(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 2.0D);
        ElephantEntity elephant = spawn(helper, CENTER_POS);
        hold(helper, elephant);
        elephant.setTarget(player);
        helper.assertTrue(elephant.tryStartAction(ChargerEntity.Action.ATTACK_TRUNK), "the trunk sweep did not start");
        int[] hitTick = { -1 };
        Vec3[] knock = { Vec3.ZERO };
        helper.onEachTick(() -> {
            if (hitTick[0] < 0) {
                if (player.getHealth() < PLAYER_MAX_HEALTH) {
                    hitTick[0] = (int) elephant.getActionTicks();
                    knock[0] = player.getDeltaMovement();
                } else {
                    place(player, helper, new Vec3(4.5D, 1.0D, 2.0D));
                    player.setDeltaMovement(Vec3.ZERO);
                }
            }
        });
        helper.runAtTickTime(25L, () -> {
            helper.assertValueEqual(hitTick[0], ElephantEntity.TRUNK_HIT_TICK, "trunk hit tick");
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH - ElephantEntity.TRUNK_DAMAGE, "trunk damage");
            helper.assertTrue(knock[0].horizontalDistance() > 1.0D, "the trunk sweep did not knock back: " + knock[0]);
            helper.assertTrue(knock[0].y() <= 0.0D, "the trunk sweep lifted the player: " + knock[0]);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 140)
    public static void closeAttacksRunStompThenTossThenTrunk(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 2.0D);
        ElephantEntity elephant = spawn(helper, CENTER_POS);
        hold(helper, elephant);
        elephant.setTarget(player);
        List<ChargerEntity.Action> attacks = new ArrayList<>();
        helper.onEachTick(() -> {
            place(player, helper, new Vec3(4.5D, 1.0D, 2.0D));
            player.setDeltaMovement(Vec3.ZERO);
            player.setHealth(PLAYER_MAX_HEALTH);
            ChargerEntity.Action action = elephant.getAction();
            if (action.isAttack() && (attacks.isEmpty() || attacks.getLast() != action)) attacks.add(action);
        });
        helper.runAtTickTime(100L, () -> {
            helper.assertTrue(attacks.size() >= 3, "fewer than three close attacks in 100 ticks: " + attacks);
            helper.assertValueEqual(attacks.subList(0, 3), List.of(ChargerEntity.Action.ATTACK_STOMP,
                    ChargerEntity.Action.ATTACK_TOSS, ChargerEntity.Action.ATTACK_TRUNK), "close attack order");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 200)
    public static void walkAndPursuitSpeedsMatchTheClipStrides(GameTestHelper helper) {
        ground(helper);
        ElephantEntity elephant = spawn(helper, new BlockPos(1, 1, 4));
        GoalSelector goals = GameTestWorld.goals(elephant);
        goals.getAvailableGoals().stream().map(WrappedGoal::getGoal).filter(RandomStrollGoal.class::isInstance)
                .map(RandomStrollGoal.class::cast).forEach(RandomStrollGoal::trigger);
        goals.removeAllGoals(goal -> goal.getFlags().contains(Goal.Flag.MOVE));
        double[] fastest = { 0.0D, 0.0D };
        Vec3[] last = { elephant.position() };
        int[] leg = { 0 };
        boolean[] underway = { false };
        Set<String> steering = new LinkedHashSet<>();
        Vec3 far = helper.absoluteVec(new Vec3(7.5D, 1.0D, 4.5D));
        Vec3 near = helper.absoluteVec(new Vec3(1.5D, 1.0D, 4.5D));
        helper.onEachTick(() -> {
            if (leg[0] > 1) return;
            Vec3 now = elephant.position();
            fastest[leg[0]] = Math.max(fastest[leg[0]], now.subtract(last[0]).horizontalDistance());
            last[0] = now;
            goals.getAvailableGoals().stream()
                    .filter(goal -> goal.isRunning() && goal.getFlags().contains(Goal.Flag.MOVE))
                    .forEach(goal -> steering.add(goal.getGoal().getClass().getSimpleName()));
            boolean done = elephant.getNavigation().isDone();
            if (underway[0] && done) leg[0]++;
            underway[0] = !done;
            if (leg[0] == 0) {
                elephant.getNavigation().moveTo(far.x, far.y, far.z, 1.0D);
            } else if (leg[0] == 1) {
                elephant.getNavigation().moveTo(near.x, near.y, near.z, ElephantEntity.PURSUIT_SPEED);
            } else {
                double walk = fastest[0] * 20.0D;
                double run = fastest[1] * 20.0D;
                double expectedWalk = BLOCKS_PER_SECOND_PER_SPEED_SQUARED
                        * Math.pow(ElephantEntity.MOVEMENT_SPEED, 2.0D);
                double expectedRun = expectedWalk * Math.pow(ElephantEntity.PURSUIT_SPEED, 2.0D);
                helper.assertTrue(steering.isEmpty(), "elephant goals steered the speed run: " + steering);
                helper.assertTrue(Math.abs(walk - 1.27D) < 0.07D, "walk blocks a second (stride 1.27, formula "
                        + expectedWalk + "): " + walk);
                helper.assertTrue(Math.abs(run - 3.58D) < 0.18D, "pursuit blocks a second (stride 3.58, formula "
                        + expectedRun + "): " + run);
                helper.succeed();
            }
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void elephantReloadClearsActionKeepsStompCooldown(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 2.0D);
        ElephantEntity original = spawn(helper, CENTER_POS);
        original.setTarget(player);
        original.tryStartAction(ChargerEntity.Action.ATTACK_STOMP);
        int stompCooldown = original.getStompCooldown();
        CompoundTag saved = new CompoundTag();
        original.saveWithoutId(saved);
        original.discard();

        ElephantEntity restored = spawn(helper, CENTER_POS);
        restored.load(saved);
        helper.assertValueEqual(restored.getAction(), ChargerEntity.Action.IDLE, "a reloaded elephant kept its action");
        helper.assertTrue(restored.getTarget() == null, "a reloaded elephant kept its target");
        helper.assertValueEqual(restored.getStompCooldown(), stompCooldown, "a reloaded elephant lost its cooldown");
        helper.assertTrue(stompCooldown > 0, "the stomp set no cooldown");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void elephantDeathHoldsThirtyTwoTicksAndDropsLeatherAndExperienceOnce(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 8.0D);
        ElephantEntity elephant = spawn(helper, CENTER_POS);
        List<Entity> drops = GameTestWorld.entitiesAddedBy(helper,
                () -> elephant.hurt(helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE));
        helper.assertValueEqual(elephant.getAction(), ChargerEntity.Action.DEATH, "lethal damage did not enter death");
        helper.assertTrue(drops.stream().allMatch(entity -> entity instanceof ExperienceOrb
                        || entity instanceof ItemEntity item && item.getItem().is(Items.LEATHER)),
                "an elephant dropped an unexpected item: " + drops);
        int leather = drops.stream().mapToInt(entity -> entity instanceof ItemEntity item ? item.getItem().getCount()
                : 0).sum();
        helper.assertTrue(leather >= 2 && leather <= 4, "leather count outside two-to-four: " + leather);
        helper.assertTrue(drops.stream().anyMatch(entity -> entity instanceof ExperienceOrb), "no experience dropped");
        GameTestWorld.Drops later = GameTestWorld.recordDrops(helper, elephant.getBoundingBox().inflate(0.5D));
        helper.runAtTickTime(ChargerEntity.DEATH_REMOVE_TICK - 4L, () -> {
            helper.assertTrue(!elephant.isRemoved(), "the elephant was removed before death tick 32");
            helper.assertValueEqual(elephant.getAction(), ChargerEntity.Action.DEATH, "death was interrupted");
        });
        helper.runAtTickTime(ChargerEntity.DEATH_REMOVE_TICK + 4L, () -> {
            helper.assertTrue(elephant.isRemoved(), "the elephant remained after death tick 32");
            later.stop().run();
            helper.assertTrue(later.joined().stream().noneMatch(entity -> entity instanceof ExperienceOrb
                    || entity instanceof ItemEntity), "elephant loot was duplicated after the death animation");
            helper.succeed();
        });
    }

    private static ElephantEntity spawn(GameTestHelper helper, BlockPos pos) {
        return helper.spawn(ModEntityTypes.ELEPHANT.get(), pos);
    }

    private static void hold(GameTestHelper helper, ElephantEntity elephant) {
        helper.onEachTick(() -> {
            elephant.getNavigation().stop();
            elephant.setDeltaMovement(Vec3.ZERO);
        });
    }

    private static ServerPlayer survivalMock(GameTestHelper helper, double x, double z) {
        return GameTestPlayers.survival(helper, "gzr-elephant-gametest").pastSpawnInvulnerability()
                .at(new Vec3(x, 1.0D, z));
    }
}
