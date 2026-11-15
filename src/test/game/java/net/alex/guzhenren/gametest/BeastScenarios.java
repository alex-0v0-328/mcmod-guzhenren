package net.alex.guzhenren.gametest;

import java.util.function.BiFunction;
import net.alex.guzhenren.entity.beast.BeastEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;
import static net.alex.guzhenren.gametest.GameTestPlayers.PLAYER_MAX_HEALTH;
import static net.alex.guzhenren.gametest.GameTestPlayers.ground;
import static net.alex.guzhenren.gametest.GameTestPlayers.place;

/**
 * 所有 {@link BeastEntity} 物种共用的场景。
 * <ul>
 *   <li>狩猎、首次锁定咆哮、方向受击、攻击冷却、死亡、掉落、重载、和平难度、追击范围、坐下；{@link BearGameTests}、
 *       {@link TigerGameTests} 逐个跑，失败能指出物种；仅各物种不同的时间作参数。</li>
 *   <li>狩猎先清掉场景内早先批次逗留的模拟玩家（结构清场不删玩家，逗留在场景中心的比测试玩家更近，批次排布一变
 *       就抢走目标）；牲畜狩猎不用玩家，猪是附近唯一活目标。</li>
 *   <li>方向受击先正面挑衅（咆哮），咆哮完再侧击；冷却场景目标在追击范围内、横扫与重击触及外，不触发自动攻击。</li>
 * </ul>
 */

final class BeastScenarios {

    private BeastScenarios() {}

    /** 受测物种：失败信息里的名称和生成方式。 */
    record Species(String name, BiFunction<GameTestHelper, BlockPos, ? extends BeastEntity> spawn) {

        BeastEntity at(GameTestHelper helper, BlockPos pos) { return spawn.apply(helper, pos); }
    }

    private static void clearLeftoverPlayers(GameTestHelper helper) {
        helper.getLevel().getEntitiesOfClass(ServerPlayer.class, helper.getBounds()).forEach(ServerPlayer::discard);
    }

    /** 站在场景相对坐标 {@code x, z} 地面上的生存模式玩家。 */
    static ServerPlayer player(GameTestHelper helper, double x, double z) {
        return GameTestPlayers.survival(helper, "gzr-beast-gametest").pastSpawnInvulnerability()
                .at(new Vec3(x, 1.0D, z));
    }

    static void huntsPlayer(GameTestHelper helper, Species species) {
        clearLeftoverPlayers(helper);
        ground(helper);
        ServerPlayer player = player(helper, 4.5D, 2.8D);
        BeastEntity beast = species.at(helper, CENTER_POS);
        helper.onEachTick(() -> {
            beast.getNavigation().stop();
            player.setDeltaMovement(Vec3.ZERO);
        });

        helper.runAtTickTime(30L, () -> {
            helper.assertTrue(beast.getTarget() == player,
                    species.name() + " did not hunt the nearest survival player");
            helper.assertValueEqual(beast.getAction(), BeastEntity.Action.ROAR,
                    species.name() + " first lock did not roar");
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH,
                    species.name() + " damaged player before any attack");
            helper.succeed();
        });
    }

    static void huntsLivestock(GameTestHelper helper, Species species) {
        ground(helper);
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(4, 1, 2));
        BeastEntity beast = species.at(helper, CENTER_POS);
        helper.onEachTick(() -> {
            beast.getNavigation().stop();
            pig.getNavigation().stop();
            pig.setDeltaMovement(Vec3.ZERO);
        });

        helper.runAtTickTime(30L, () -> {
            helper.assertTrue(beast.getTarget() == pig, species.name() + " did not hunt the nearest pig");
            helper.assertValueEqual(beast.getAction(), BeastEntity.Action.ROAR,
                    species.name() + " livestock first lock did not roar");
            helper.assertValueEqual(pig.getHealth(), pig.getMaxHealth(),
                    species.name() + " damaged the pig before any attack");
            helper.succeed();
        });
    }

    static void firstLockRoarsAndIgnoresDamageDuringRoar(GameTestHelper helper, Species species) {
        ground(helper);
        ServerPlayer player = player(helper, 4.5D, 2.5D);
        BeastEntity beast = species.at(helper, CENTER_POS);
        beast.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);

        helper.assertTrue(beast.getTarget() == player, species.name() + " did not target its attacker");
        helper.assertValueEqual(beast.getAction(), BeastEntity.Action.ROAR,
                species.name() + " first lock did not play roar");
        helper.runAtTickTime(10L, () -> {
            beast.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.assertValueEqual(beast.getAction(), BeastEntity.Action.ROAR,
                    "incoming damage replaced roar before it finished");
            helper.succeed();
        });
    }

    /** 正面攻击者站在 {@code frontZ} 及再远 2 格处；侧面击打在 {@code sideHitTick} 落下。 */
    static void directionalHurtAfterRoar(GameTestHelper helper, Species species, double frontZ, long sideHitTick) {
        ground(helper);
        ServerPlayer leftFront = player(helper, 4.5D, frontZ);
        ServerPlayer leftSide = player(helper, 7.5D, 4.5D);
        BeastEntity left = species.at(helper, new BlockPos(4, 1, 4));

        ServerPlayer rightFront = player(helper, 4.5D, frontZ + 2.0D);
        ServerPlayer rightSide = player(helper, 1.5D, 6.5D);
        BeastEntity right = species.at(helper, new BlockPos(4, 1, 6));

        left.hurt(helper.getLevel().damageSources().playerAttack(leftFront), 1.0F);
        right.hurt(helper.getLevel().damageSources().playerAttack(rightFront), 1.0F);

        helper.onEachTick(() -> {
            left.getNavigation().stop();
            right.getNavigation().stop();
            leftFront.setDeltaMovement(Vec3.ZERO);
            leftSide.setDeltaMovement(Vec3.ZERO);
            rightFront.setDeltaMovement(Vec3.ZERO);
            rightSide.setDeltaMovement(Vec3.ZERO);
        });

        helper.runAtTickTime(sideHitTick, () -> {
            left.hurt(helper.getLevel().damageSources().playerAttack(leftSide), 1.0F);
            right.hurt(helper.getLevel().damageSources().playerAttack(rightSide), 1.0F);
            helper.assertValueEqual(left.getAction(), BeastEntity.Action.HURT_RIGHT,
                    "left-side attacker did not play hurt_right");
            helper.assertValueEqual(right.getAction(), BeastEntity.Action.HURT_LEFT,
                    "right-side attacker did not play hurt_left");
            helper.succeed();
        });
    }

    /** 横扫在 {@code swipeEndTick} 前结束；恢复期再阻止重击 10 tick。 */
    static void swipeAndHeavyCooldownsBlockOverlappingAttacks(GameTestHelper helper, Species species,
                                                              long swipeEndTick) {
        ground(helper);
        ServerPlayer player = player(helper, 4.5D, 13.5D);
        BeastEntity beast = species.at(helper, CENTER_POS);
        beast.setTarget(player);
        helper.onEachTick(() -> {
            beast.getNavigation().stop();
            player.setDeltaMovement(Vec3.ZERO);
        });

        helper.assertTrue(beast.tryStartAction(BeastEntity.Action.ATTACK_SWIPE),
                species.name() + " swipe did not start");
        helper.assertValueEqual(beast.getSwipeCooldown(), 20,
                species.name() + " swipe cooldown did not begin immediately");

        helper.runAtTickTime(5L, () -> {
            helper.assertValueEqual(beast.getAction(), BeastEntity.Action.ATTACK_SWIPE,
                    species.name() + " swipe left its action during wind-up");
            helper.assertTrue(!beast.tryStartAction(BeastEntity.Action.ATTACK_SWIPE), "overlapping swipe accepted");
            helper.assertTrue(!beast.tryStartAction(BeastEntity.Action.ATTACK_HEAVY), "overlapping heavy accepted");
        });
        helper.runAtTickTime(swipeEndTick, () -> {
            helper.assertTrue(beast.getAction() != BeastEntity.Action.ATTACK_SWIPE,
                    species.name() + " swipe action did not finish");
            helper.assertTrue(!beast.tryStartAction(BeastEntity.Action.ATTACK_HEAVY),
                    species.name() + " heavy started during post-attack recovery");
        });
        helper.runAtTickTime(swipeEndTick + 10L, () -> {
            helper.assertTrue(beast.tryStartAction(BeastEntity.Action.ATTACK_HEAVY),
                    species.name() + " heavy did not start after swipe cooldown and recovery elapsed");
            helper.assertValueEqual(beast.getHeavyCooldown(), 80,
                    species.name() + " heavy cooldown did not begin immediately");
            helper.succeed();
        });
    }

    static void deathRemovesAt(GameTestHelper helper, Species species, int deathTicks) {
        ground(helper);
        BeastEntity beast = species.at(helper, CENTER_POS);
        beast.tryStartAction(BeastEntity.Action.ATTACK_SWIPE);
        helper.assertTrue(beast.hurt(helper.getLevel().damageSources().generic(), Float.MAX_VALUE),
                species.name() + " did not accept lethal damage");
        helper.assertValueEqual(beast.getAction(), BeastEntity.Action.DEATH,
                "lethal damage did not enter death action");
        helper.runAtTickTime(deathTicks - 4L, () -> {
            helper.assertTrue(!beast.isRemoved(), species.name() + " was removed before death tick " + deathTicks);
            helper.assertTrue(beast.deathTime < deathTicks,
                    species.name() + " death timer passed " + deathTicks + " too early");
            helper.assertValueEqual(beast.getAction(), BeastEntity.Action.DEATH, "death action was interrupted");
        });
        helper.runAtTickTime(deathTicks, () -> {
            helper.assertTrue(beast.isRemoved(), species.name() + " remained after death tick " + deathTicks);
            helper.succeed();
        });
    }

    static void dropsNoItemsAndExperienceOnce(GameTestHelper helper, Species species) {
        ground(helper);
        ServerPlayer player = player(helper, 4.5D, 2.5D);
        BeastEntity beast = species.at(helper, CENTER_POS);
        GameTestWorld.Drops drops = GameTestWorld.recordDrops(helper, beast.getBoundingBox().inflate(0.5D));
        beast.hurt(helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE);

        int[] xp = { -1 };
        helper.runAtTickTime(40L, () -> {
            helper.assertTrue(drops.joined().stream().noneMatch(ItemEntity.class::isInstance),
                    species.name() + " dropped items");
            xp[0] = GameTestWorld.experience(drops.joined());
            helper.assertTrue(xp[0] >= 1 && xp[0] <= 3,
                    species.name() + " experience reward outside one-to-three: " + xp[0]);
        });
        helper.runAtTickTime(60L, () -> {
            drops.stop().run();
            helper.assertTrue(drops.joined().stream().noneMatch(ItemEntity.class::isInstance),
                    species.name() + " duplicated items after death");
            helper.assertValueEqual(GameTestWorld.experience(drops.joined()), xp[0],
                    species.name() + " experience was duplicated after death");
            helper.succeed();
        });
    }

    static void reloadClearsActionKeepsCooldowns(GameTestHelper helper, Species species) {
        ground(helper);
        ServerPlayer player = player(helper, 4.5D, 2.5D);
        BeastEntity original = species.at(helper, CENTER_POS);
        original.setTarget(player);
        original.tryStartAction(BeastEntity.Action.ATTACK_SWIPE);
        int savedCooldown = original.getSwipeCooldown();
        CompoundTag saved = new CompoundTag();
        original.saveWithoutId(saved);
        original.discard();

        BeastEntity restored = species.at(helper, new BlockPos(6, 1, 6));
        restored.load(saved);
        helper.assertValueEqual(restored.getAction(), BeastEntity.Action.IDLE,
                "reloaded " + species.name() + " kept a transient action");
        helper.assertValueEqual(restored.getActionTicks(), 0L, "reloaded " + species.name() + " action ticks");
        helper.assertTrue(restored.getTarget() == null, "reloaded " + species.name() + " kept its target");
        helper.assertTrue(restored.getDeltaMovement().lengthSqr() < 1.0E-6D,
                "reloaded " + species.name() + " kept attack movement");
        helper.assertValueEqual(restored.getSwipeCooldown(), savedCooldown,
                "reloaded " + species.name() + " lost swipe cooldown");
        helper.succeed();
    }

    static void doesNotAcquireTargetOnPeaceful(GameTestHelper helper, Species species) {
        ground(helper);
        ServerPlayer player = player(helper, 4.5D, 2.5D);
        BeastEntity beast = species.at(helper, CENTER_POS);
        Difficulty previous = helper.getLevel().getDifficulty();
        helper.getLevel().getServer().setDifficulty(Difficulty.PEACEFUL, true);
        try {
            beast.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.assertTrue(beast.getTarget() == null, species.name() + " acquired a target on peaceful");
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH,
                    "peaceful " + species.name() + " damaged player");
        } finally {
            helper.getLevel().getServer().setDifficulty(previous, true);
        }
        helper.succeed();
    }

    /** 攻击者退到场景相对坐标 {@code farZ}，超出该物种的追击范围。 */
    static void clearsTargetOutsideFollowRange(GameTestHelper helper, Species species, double farZ) {
        ground(helper);
        ServerPlayer player = player(helper, 4.5D, 2.5D);
        BeastEntity beast = species.at(helper, CENTER_POS);
        beast.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.runAtTickTime(1L, () -> place(player, helper, new Vec3(4.5D, 1.0D, farZ)));
        helper.runAtTickTime(10L, () -> {
            helper.assertTrue(beast.getTarget() == null, species.name() + " kept a target outside its follow range");
            helper.succeed();
        });
    }

    static void daytimeSitReturnsToIdleAfterOneHundredSixtyTicks(GameTestHelper helper, Species species) {
        ground(helper);
        BeastEntity beast = species.at(helper, CENTER_POS);
        beast.tryStartAction(BeastEntity.Action.SIT);
        helper.onEachTick(() -> beast.getNavigation().stop());

        helper.runAtTickTime(80L, () -> helper.assertValueEqual(beast.getAction(), BeastEntity.Action.SIT,
                species.name() + " sit ended early"));
        helper.runAtTickTime(160L, () -> {
            helper.assertValueEqual(beast.getAction(), BeastEntity.Action.IDLE,
                    species.name() + " sit did not return to idle after 160 ticks");
            helper.succeed();
        });
    }

    static void hurtDuringSitSnapsToCombat(GameTestHelper helper, Species species) {
        ground(helper);
        ServerPlayer player = player(helper, 4.5D, 2.5D);
        BeastEntity beast = species.at(helper, CENTER_POS);
        beast.tryStartAction(BeastEntity.Action.SIT);
        helper.onEachTick(() -> beast.getNavigation().stop());

        helper.runAtTickTime(20L, () -> {
            beast.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.assertTrue(beast.getTarget() == player, "hurt during sit did not assign target");
            helper.assertValueEqual(beast.getAction(), BeastEntity.Action.ROAR,
                    "hurt during sit did not snap to first-lock roar");
            helper.succeed();
        });
    }
}
