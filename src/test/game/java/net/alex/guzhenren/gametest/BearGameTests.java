package net.alex.guzhenren.gametest;

import java.util.function.Function;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.BearEntity;
import net.alex.guzhenren.entity.beast.BearSpecies;
import net.alex.guzhenren.entity.beast.BeastEntity;
import net.alex.guzhenren.entity.beast.TigerEntity;
import net.alex.guzhenren.entity.beast.TortoiseEntity;
import net.alex.guzhenren.entity.beast.WildBoarEntity;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static net.alex.guzhenren.gametest.BeastScenarios.player;
import static net.alex.guzhenren.gametest.GameTestPlayers.PLAYER_MAX_HEALTH;
import static net.alex.guzhenren.gametest.GameTestPlayers.ground;
import static net.alex.guzhenren.gametest.GameTestWorld.firstHit;

/**
 * 四种熊的属性、性情、攻击、死亡与持久化。共享世界难度全服共用，故不钉和平难度主动狩猎，只测同步的“和平难度阻止反击”接
 * 缝。与虎共用场景在 {@link BeastScenarios}；方向受击：野兽朝南（+Z）时 +X 侧攻击者在其左，头向右
 * （{@code HURT_RIGHT}）。{@code dayTime} 全局共用，故 {@link #beastNightSleepChainTransitions} 熊虎同跑。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BearGameTests {
    private static final BeastScenarios.Species BROWN =
            new BeastScenarios.Species("brown bear", (helper, pos) -> spawnBear(helper, pos, BearSpecies.BROWN));
    private static final BeastScenarios.Species ASIAN_BLACK = new BeastScenarios.Species("Asian black bear",
            (helper, pos) -> spawnBear(helper, pos, BearSpecies.ASIAN_BLACK));

    private BearGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void bearNeutralDoesNotAttackFromProximity(GameTestHelper helper) {
        ground(helper);
        int x = 1;
        for (BearSpecies species
                : new BearSpecies[] { BearSpecies.BROWN, BearSpecies.AMERICAN_BLACK, BearSpecies.ALBINO }) {
            ServerPlayer player = player(helper, x + 0.5D, 2.5D);
            BearEntity bear = spawnBear(helper, new BlockPos(x, 1, 4), species);
            helper.onEachTick(() -> {
                bear.getNavigation().stop();
                bear.setDeltaMovement(Vec3.ZERO);
            });
            helper.runAtTickTime(50L, () -> {
                helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH,
                        species + " proximity damaged player");
                helper.assertTrue(bear.getTarget() == null,
                        species + " proximity assigned a target");
                helper.assertTrue(bear.getAction() == BeastEntity.Action.IDLE || bear.getAction().isAmbient(),
                        species + " proximity started an attack: " + bear.getAction());
            });
            x += 2;
        }
        helper.runAtTickTime(55L, helper::succeed);
    }

    @GameTest(template = "empty9x9x9")
    public static void bearRevengeTargetsOnlyHurtIndividual(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = player(helper, 4.5D, 0.5D);
        BearEntity victim = spawnBear(helper, new BlockPos(4, 1, 4), BearSpecies.BROWN);
        BearEntity bystander = spawnBear(helper, new BlockPos(2, 1, 4), BearSpecies.AMERICAN_BLACK);
        victim.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.onEachTick(() -> {
            victim.getNavigation().stop();
            bystander.getNavigation().stop();
        });

        helper.runAtTickTime(10L, () -> {
            helper.assertTrue(victim.getTarget() == player,
                    "hurt bear did not target its attacker");
            helper.assertTrue(bystander.getTarget() == null,
                    "nearby bear joined another bear's revenge target");
            helper.assertValueEqual(victim.getAction(), BeastEntity.Action.ROAR,
                    "hurt bear did not enter its first-lock roar");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void bearAsianBlackBearHuntsPlayerWithoutDamage(GameTestHelper helper) {
        BeastScenarios.huntsPlayer(helper, ASIAN_BLACK);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void bearAsianBlackBearHuntsLivestock(GameTestHelper helper) {
        BeastScenarios.huntsLivestock(helper, ASIAN_BLACK);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void bearAsianBlackBearIgnoresModCreatureWildBoar(GameTestHelper helper) {
        ground(helper);
        WildBoarEntity boar = helper.spawn(ModEntityTypes.WILD_BOAR.get(), new BlockPos(5, 1, 4));
        BearEntity bear = spawnBear(helper, new BlockPos(3, 1, 4), BearSpecies.ASIAN_BLACK);
        helper.onEachTick(() -> {
            bear.getNavigation().stop();
            boar.getNavigation().stop();
            bear.setDeltaMovement(Vec3.ZERO);
            boar.setDeltaMovement(Vec3.ZERO);
        });

        helper.runAtTickTime(60L, () -> {
            helper.assertTrue(bear.getTarget() == null,
                    "Asian black bear targeted a mod creature");
            helper.assertValueEqual(boar.getHealth(), boar.getMaxHealth(),
                    "wild boar was hurt by a passive asian black bear");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void bearFirstLockRoarsAndIgnoresDamageDuringRoar(GameTestHelper helper) {
        BeastScenarios.firstLockRoarsAndIgnoresDamageDuringRoar(helper, BROWN);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 120)
    public static void bearDirectionalHurtAfterRoar(GameTestHelper helper) {
        BeastScenarios.directionalHurtAfterRoar(helper, BROWN, 8.5D, 55L);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void bearSwipeHitsAtNineTicksPerSpecies(GameTestHelper helper) {
        assertHitPerSpecies(helper, BeastEntity.Action.ATTACK_SWIPE, 0.5D, 2.5D, 9, BearSpecies::getSwipeDamage,
                "swipe");
        helper.runAtTickTime(15L, helper::succeed);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void bearRearSlamHitsAtSeventeenTicksPerSpecies(GameTestHelper helper) {
        assertHitPerSpecies(helper, BeastEntity.Action.ATTACK_HEAVY, 1.5D, 4.5D, 17, BearSpecies::getRearDamage,
                "rear slam");
        helper.runAtTickTime(25L, helper::succeed);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void bearSwipeAndHeavyCooldownsBlockOverlappingAttacks(GameTestHelper helper) {
        BeastScenarios.swipeAndHeavyCooldownsBlockOverlappingAttacks(helper, BROWN, 21L);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void bearDeathRemovesAtThirtySixTicks(GameTestHelper helper) {
        BeastScenarios.deathRemovesAt(helper, BROWN, 36);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void bearDropsNoItemsAndExperienceOnce(GameTestHelper helper) {
        BeastScenarios.dropsNoItemsAndExperienceOnce(helper, BROWN);
    }

    @GameTest(template = "empty9x9x9")
    public static void bearReloadClearsActionKeepsCooldowns(GameTestHelper helper) {
        BeastScenarios.reloadClearsActionKeepsCooldowns(helper, BROWN);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void bearDoesNotAcquireTargetOnPeaceful(GameTestHelper helper) {
        BeastScenarios.doesNotAcquireTargetOnPeaceful(helper, ASIAN_BLACK);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void bearClearsTargetOutsideFollowRange(GameTestHelper helper) {
        BeastScenarios.clearsTargetOutsideFollowRange(helper, BROWN, -12.0D);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 240)
    public static void beastNightSleepChainTransitions(GameTestHelper helper) {
        ground(helper);
        BearEntity bear = spawnBear(helper, new BlockPos(2, 1, 4), BearSpecies.BROWN);
        TigerEntity tiger = helper.spawn(ModEntityTypes.TIGER.get(), new BlockPos(6, 1, 4));
        TortoiseEntity tortoise = helper.spawn(ModEntityTypes.TORTOISE.get(), new BlockPos(4, 1, 7));
        helper.getLevel().setDayTime(13000L);
        bear.tryStartAction(BeastEntity.Action.LIE_DOWN);
        tiger.tryStartAction(BeastEntity.Action.LIE_DOWN);
        tortoise.tryStartAction(TortoiseEntity.Action.SLEEP);
        helper.onEachTick(() -> {
            bear.getNavigation().stop();
            tiger.getNavigation().stop();
            tortoise.getNavigation().stop();
        });

        helper.runAtTickTime(TigerEntity.LIE_DOWN_TICKS, () ->
                helper.assertValueEqual(tiger.getAction(), BeastEntity.Action.LIE,
                        "tiger did not transition to lie after lie-down"));
        helper.runAtTickTime(BearEntity.LIE_DOWN_TICKS, () ->
                helper.assertValueEqual(bear.getAction(), BeastEntity.Action.LIE,
                        "bear did not transition to lie after lie-down"));
        helper.runAtTickTime(TigerEntity.LIE_DOWN_TICKS + 80L, () ->
                helper.assertValueEqual(tiger.getAction(), BeastEntity.Action.SLEEP,
                        "tiger did not transition to sleep after lying"));
        helper.runAtTickTime(BearEntity.LIE_DOWN_TICKS + 80L, () ->
                helper.assertValueEqual(bear.getAction(), BeastEntity.Action.SLEEP,
                        "bear did not transition to sleep after lying"));

        helper.runAtTickTime(110L, () -> {
            helper.assertValueEqual(tortoise.getAction(), TortoiseEntity.Action.SLEEP,
                    "tortoise did not sleep through the night");
            helper.getLevel().setDayTime(1000L);
        });
        helper.runAtTickTime(111L, () -> {
            helper.assertValueEqual(bear.getAction(), BeastEntity.Action.GET_UP,
                    "bear did not get up at day");
            helper.assertValueEqual(tiger.getAction(), BeastEntity.Action.GET_UP,
                    "tiger did not get up at day");
            helper.assertValueEqual(tortoise.getAction(), TortoiseEntity.Action.IDLE,
                    "tortoise did not wake at day");
        });
        helper.runAtTickTime(111L + TigerEntity.GET_UP_TICKS + 1L, () ->
                helper.assertValueEqual(tiger.getAction(), BeastEntity.Action.IDLE,
                        "tiger did not return to idle after getting up"));
        helper.runAtTickTime(111L + BearEntity.GET_UP_TICKS + 1L, () -> {
            helper.assertValueEqual(bear.getAction(), BeastEntity.Action.IDLE,
                    "bear did not return to idle after getting up");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 180)
    public static void bearDaytimeSitReturnsToIdleAfterOneHundredSixtyTicks(GameTestHelper helper) {
        BeastScenarios.daytimeSitReturnsToIdleAfterOneHundredSixtyTicks(helper, BROWN);
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void bearHurtDuringSitSnapsToCombat(GameTestHelper helper) {
        BeastScenarios.hurtDuringSitSnapsToCombat(helper, BROWN);
    }

    /**
     * 每物种对其方块偏移 {@code playerDx, playerZ} 处玩家发起 {@code attack}：{@code hitTick} 前无伤害，该 tick 恰为
     * {@code damage}。
     */
    private static void assertHitPerSpecies(GameTestHelper helper, BeastEntity.Action attack, double playerDx,
                                            double playerZ, int hitTick, Function<BearSpecies, Float> damage,
                                            String label) {
        ground(helper);
        int x = 1;
        for (BearSpecies species : BearSpecies.values()) {
            ServerPlayer player = player(helper, x + playerDx, playerZ);
            BearEntity bear = spawnBear(helper, new BlockPos(x, 1, 4), species);
            bear.setTarget(player);
            bear.tryStartAction(attack);
            float initialHealth = player.getHealth();
            int[] hit = firstHit(helper, player, () -> (int) bear.getActionTicks());
            helper.runAtTickTime(hitTick - 1L, () -> helper.assertValueEqual(player.getHealth(), initialHealth,
                    species + " " + label + " damaged before tick " + hitTick));
            helper.runAtTickTime(hitTick + 3L, () -> {
                helper.assertValueEqual(hit[0], hitTick, species + " " + label + " hit tick");
                helper.assertValueEqual(player.getHealth(), 20.0F - damage.apply(species),
                        species + " " + label + " damage");
            });
            x += 2;
        }
    }

    private static BearEntity spawnBear(GameTestHelper helper, BlockPos pos, BearSpecies species) {
        return switch (species) {
            case BROWN -> helper.spawn(ModEntityTypes.BROWN_BEAR.get(), pos);
            case ASIAN_BLACK -> helper.spawn(ModEntityTypes.ASIAN_BLACK_BEAR.get(), pos);
            case AMERICAN_BLACK -> helper.spawn(ModEntityTypes.AMERICAN_BLACK_BEAR.get(), pos);
            case ALBINO -> helper.spawn(ModEntityTypes.ALBINO_BEAR.get(), pos);
        };
    }
}
