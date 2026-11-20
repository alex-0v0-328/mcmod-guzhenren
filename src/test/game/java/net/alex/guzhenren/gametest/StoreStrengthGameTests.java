package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.BeastPhantomEntity;
import net.alex.guzhenren.entity.projectile.BeastChargeEntity;
import net.alex.guzhenren.entity.projectile.QiWaveEntity;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.ComboRound;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StoreStrength;
import net.alex.guzhenren.gameplay.path.strength.StrengthCrit;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthSnapshot;
import net.alex.guzhenren.gameplay.path.timed.TimedBuff;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
import net.alex.guzhenren.item.gu.mortal.path.strength.ChargedPunch;
import net.alex.guzhenren.item.gu.mortal.path.strength.StoreStrengthBehavior;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.registry.entries.EpicFightItems;
import static net.alex.guzhenren.gametest.GameTestGu.click;
import static net.alex.guzhenren.gametest.GameTestGu.hold;
import static net.alex.guzhenren.gametest.GameTestGu.hunger;
import static net.alex.guzhenren.gametest.GameTestGu.inSlot;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 蓄力蛊[Store Strength Gu]、惯力蛊[Habitual Strength Gu]及其蓄力一拳测试，不含客户端锁定输入。
 * <ul>
 *   <li>未入力道分支则拒绝不扣费，有永久兽力、人力或生效中的限时兽力即可催动与起蓄；手持剑或 Epic Fight 拳套、增益消失
 *       则拒绝并丢弃蓄力。</li>
 *   <li>进度不足四分之一松手取消，不出拳不扣费；四分之一起出拳，倍率自 1.5 线性升到蓄满的 3；蓄满固定 80 tick，全力以赴
 *       蛊[All-Out Effort Gu]不缩短。</li>
 *   <li>人力钧力流蓄满（200 斤）结算 600 点，朝锁定目标打水平扇形并按溢出结算：锁定目标吃整拳，剩余减去其挨打前生命后
 *       由近到远传下去，扇形外无伤；打中结算一次过载反噬，挥空不扣。</li>
 *   <li>暴击：蓄满必定暴击，未蓄满按暴击率掷（全力以赴五转 100% 则必中）；兽力暴击抽 min(1 + 苦力档位, 兽种数) 种并显
 *       形，没暴击只吃蓄力与苦力倍率；限时兽力不暴击。</li>
 *   <li>苦力蛊[Hardship Strength Gu]下人力钧力流射程乘 1 + 0.4 × 档位（封顶 16 格）、半角 45° + 5.5° × 档位，崩裂范围
 *       不变；崩裂档位随结算力量点：100 点起破软块、200 点起破石头、400 点起破深板岩。</li>
 *   <li>力气蛊[Strength Qi Gu]增益中目标在触及距离外：伤害乘远程系数 0.5，人力流放一个追踪气波，兽力流按兽种份额各放一
 *       发兽力冲击（没暴击时一发带全部）；过载反噬等爆开首次造成伤害时才结算；追踪气波超时就地爆开。</li>
 *   <li>惯力蛊[Habitual Strength Gu]增益下起蓄带可移动标记，蓄力期间疾跑被清掉；取消、出拳、丢弃都撤；出拳结果与蓄力蛊相同。</li>
 *   <li>出拳者站石头地面朝南（yaw 0），已过出生无敌，反噬可生效；身后目标 id 不保留，挥击沿视线；目标为无意识村民，
 *       默认 {@link #TARGET_HEALTH} 生命；蓄力用 {@link GameTestPlayers#serverTicks} 推进。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StoreStrengthGameTests {

    private static final Vec3 NEAR_STAND = new Vec3(4.5, 1.0, 2.5);
    private static final Vec3 FAR_STAND = new Vec3(4.5, 1.0, 0.5);
    private static final BlockPos LOCKED = new BlockPos(4, 1, 6);
    private static final BlockPos IN_FAN = new BlockPos(6, 1, 5);
    private static final BlockPos FAN_FAR = new BlockPos(3, 1, 7);
    private static final BlockPos SIDE = new BlockPos(8, 1, 2);
    private static final BlockPos FLANK = new BlockPos(8, 1, 4);
    private static final BlockPos FLANK_BLOCK = new BlockPos(1, 1, 4);
    private static final BlockPos BEHIND = new BlockPos(4, 1, 0);
    private static final BlockPos PAST_REACH = new BlockPos(4, 1, 7);
    private static final BlockPos EDGE = new BlockPos(4, 1, 8);
    private static final BlockPos CLOSE = new BlockPos(4, 1, 3);
    private static final BlockPos BESIDE_THROW = new BlockPos(6, 1, 1);
    private static final BlockPos FAN_HEAD = new BlockPos(4, 2, 4);
    private static final BlockPos FAN_FEET = new BlockPos(4, 1, 4);
    private static final BlockPos FAN_LEFT = new BlockPos(3, 1, 4);
    private static final BlockPos FAN_RIGHT = new BlockPos(5, 1, 4);
    private static final BlockPos FAN_GROUND = new BlockPos(4, 0, 4);
    private static final int BAG_SLOT = 9;
    private static final int HUNGER = 98;
    private static final int GU_HUNGER = 50;
    private static final int JIN = 200;
    private static final int LEAST_POINTS = 300;
    private static final int FULL_POINTS = 600;
    private static final int FEW_JIN = 20;
    private static final int BEAR_POINTS = 40;
    private static final int TIGER_POINTS = 64;
    private static final int TURTLE_POINTS = 48;
    private static final int SOME_POINTS = 480;
    private static final int RANK_FIVE = 72;
    private static final int RANK_FIVE_TIER = 4;
    private static final int HALF_TICKS = 40;
    private static final int LEAST_TICKS = 20;
    private static final int EFFECT_TICKS = 600;
    private static final int NO_TARGET = -1;
    private static final int OUT_OF_TIME_TICKS = 1000;
    private static final double SHOVE_SQR = 1.0E-6D;
    private static final float TARGET_HEALTH = 1000.0F;
    private static final float LOW_HEALTH = 10.0F;
    private static final float TIER_ONE_HEALTH = 0.7F;
    private static final float TIER_TWO_HEALTH = 0.5F;
    private static final float TIER_FIVE_HEALTH = 0.1F;
    private static final double DELTA = 1.0E-3D;

    private StoreStrengthGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void theGuIsRefusedWithoutABranchAndLaysItsBuffWithOne(GameTestHelper helper) {
        ServerPlayer player = player(helper, NEAR_STAND);
        ItemStack gu = hold(player, ModItems.STORE_STRENGTH_GU_3.get(), GU_HUNGER);
        TendedGuItem item = (TendedGuItem) gu.getItem();
        long essence = ApertureEssenceService.getSpendable(player);

        click(helper, player, gu);
        assertFalse(TimedEffects.hasBuff(player, ModEffects.STORE_STRENGTH_GU), "a body with no branch got the buff");
        assertEquals(essence, ApertureEssenceService.getSpendable(player), "the refused use paid essence");
        assertEquals(GU_HUNGER, hunger(gu), "the refused use paid hunger");
        GuItem.Refusal none = assertNotNull(refusal(item, player, gu), () -> "no refusal without a branch");
        assertEquals("guzhenren.item.failed.all_out_needs_branch", none.key());

        PathStrengthService.setHumanStrength(player, HumanStrength.JIN, 1);
        assertTrue(refusal(item, player, gu) == null, "one jin is a branch enough, yet the Gu refused");
        click(helper, player, gu);
        assertTrue(TimedEffects.hasBuff(player, ModEffects.STORE_STRENGTH_GU), "a body with a branch got no buff");
        assertTrue(ApertureEssenceService.getSpendable(player) < essence, "the use paid no essence");
        assertTrue(hunger(gu) < GU_HUNGER, "the use paid no hunger");
        GuItem.Refusal active = assertNotNull(refusal(item, player, gu), () -> "no refusal while the buff runs");
        assertEquals(GuItem.Refusal.EFFECT_ACTIVE, active.key());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aTimedBeastIsBranchEnoughToChargeAndNeverCrits(GameTestHelper helper) {
        ServerPlayer player = player(helper, NEAR_STAND);
        ItemStack gu = hold(player, ModItems.STORE_STRENGTH_GU_3.get(), GU_HUNGER);
        TendedGuItem item = (TendedGuItem) gu.getItem();
        assertNotNull(refusal(item, player, gu), () -> "no refusal without a branch");

        TimedEffects.grantBuff(player, (TendedGuItem) ModItems.FLOWER_BOAR_GU_1.get(), ModEffects.FLOWER_BOAR_GU, 0,
                EFFECT_TICKS, Amplification.NONE);
        assertTrue(refusal(item, player, gu) == null, "a running timed beast is a branch, yet the Gu refused");
        useGu(helper, player);
        int total = StrengthService.getStrengthPoints(player);
        assertTrue(total > 0, "the timed boar added no points");
        Villager locked = villager(helper, LOCKED);

        fillCharge(player);
        float expected = fist(player, total * 3);
        assertTrue(ChargedPunch.release(player, locked.getId()), "the full charge did not strike");
        assertEquals(expected, TARGET_HEALTH - locked.getHealth(), DELTA);
        assertTrue(BeastPhantomEntity.findOwned(player).isEmpty(), "a timed beast showed a phantom");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void letGoBeforeAQuarterItStrikesNothingAndCostsNothing(GameTestHelper helper) {
        ServerPlayer player = human(helper, NEAR_STAND);
        Villager locked = villager(helper, LOCKED);

        assertTrue(StoreStrength.start(player), "the charge did not start");
        GameTestPlayers.serverTicks(player, LEAST_TICKS / 2);
        float health = player.getHealth();
        assertTrue(StoreStrength.isCharging(player), "the charge stopped on its own");
        float progress = StoreStrength.getProgress(player);
        assertTrue(progress > 0.0F && progress < StoreStrength.MIN_PROGRESS, "the charge is not short: " + progress);

        assertFalse(ChargedPunch.release(player, locked.getId()), "half a second of charge struck");
        assertFalse(StoreStrength.isCharging(player), "a release left the charge standing");
        assertEquals(0.0F, StoreStrength.getProgress(player), DELTA);
        assertEquals(TARGET_HEALTH, locked.getHealth(), DELTA);
        assertEquals(health, player.getHealth(), DELTA);
        assertTrue(player.getLastDamageSource() == null, "a short charge paid an overload");

        assertTrue(StoreStrength.start(player), "the second charge did not start");
        assertFalse(ChargedPunch.release(player, locked.getId()), "an empty charge struck");
        assertEquals(TARGET_HEALTH, locked.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theFactorGrowsFromAQuarterToFullAndAllOutEffortNeverShortensIt(GameTestHelper helper) {
        assertEquals(80, StoreStrength.CHARGE_TICKS);

        ServerPlayer player = human(helper, NEAR_STAND);
        assertFactor(player, LEAST_TICKS / 2, 0.0F);
        assertFactor(player, LEAST_TICKS, 1.5F);
        assertFactor(player, 2 * LEAST_TICKS, 2.0F);
        assertFactor(player, 3 * LEAST_TICKS, 2.5F);
        assertFactor(player, StoreStrength.CHARGE_TICKS, 3.0F);
        assertFactor(player, StoreStrength.CHARGE_TICKS + LEAST_TICKS, 3.0F);

        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        assertEquals(RANK_FIVE_TIER, StrengthService.getAllOutTier(player), "the rank five tier is not read");
        assertFactor(player, LEAST_TICKS / 2, 0.0F);
        assertFactor(player, LEAST_TICKS, 1.5F);
        assertFactor(player, HALF_TICKS, 2.0F);
        assertFactor(player, StoreStrength.CHARGE_TICKS, 3.0F);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theLeastAndTheFullHumanChargeMultiplyTheWholeFist(GameTestHelper helper) {
        ServerPlayer player = human(helper, NEAR_STAND);
        Villager locked = villager(helper, LOCKED);
        assertEquals((FULL_POINTS - LEAST_POINTS) * PathStrengthData.ATTACK_PER_POINT,
                fist(player, FULL_POINTS) - fist(player, LEAST_POINTS), DELTA);

        assertTrue(StoreStrength.start(player), "the charge did not start");
        GameTestPlayers.serverTicks(player, LEAST_TICKS);
        assertTrue(ChargedPunch.release(player, locked.getId()), "one second of charge did not strike");
        assertEquals(fist(player, LEAST_POINTS), TARGET_HEALTH - locked.getHealth(), DELTA);

        locked.setHealth(TARGET_HEALTH);
        player.setHealth(player.getMaxHealth());
        fillCharge(player);
        assertTrue(ChargedPunch.release(player, locked.getId()), "the full charge did not strike");
        assertEquals(fist(player, FULL_POINTS), TARGET_HEALTH - locked.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aFullHumanChargeSpillsThroughTheFanAndPaysTheOverload(GameTestHelper helper) {
        ServerPlayer player = human(helper, NEAR_STAND);
        Villager locked = villager(helper, LOCKED, LOW_HEALTH);
        Villager inFan = villager(helper, IN_FAN);
        Villager farther = villager(helper, FAN_FAR);
        Villager outside = villager(helper, SIDE);
        assertTrue(inFan.distanceToSqr(player) < locked.distanceToSqr(player), "the locked target stands nearest");

        fillCharge(player);
        float health = player.getHealth();
        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        assertEquals(JIN, snapshot.totalPoints(), "the body does not hold 200 points");
        assertEquals(RANK_FIVE, snapshot.capacity(), "the rank five capacity moved");
        float expected = fist(player, FULL_POINTS);

        assertTrue(ChargedPunch.release(player, locked.getId()), "the full charge did not strike");

        assertTrue(locked.isDeadOrDying(), "the locked target did not take the whole fist first");
        assertEquals(expected - LOW_HEALTH, TARGET_HEALTH - inFan.getHealth(), DELTA);
        assertEquals(TARGET_HEALTH, farther.getHealth(), DELTA);
        assertEquals(TARGET_HEALTH, outside.getHealth(), DELTA);
        DamageSource hit = assertNotNull(inFan.getLastDamageSource(), () -> "the fan target recorded no damage");
        assertTrue(hit.is(ModDamageTypes.CHARGED_PUNCH), "the hit is not a charged punch");
        assertTrue(hit.getEntity() == player, "the hit names another than the puncher");
        assertTrue(BeastPhantomEntity.findOwned(player).isEmpty(), "the human path showed a phantom");
        assertOverload(player, health, FULL_POINTS, RANK_FIVE);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theOverloadIsPaidOnceForTheWholeFanAndNotOnAMiss(GameTestHelper helper) {
        ServerPlayer player = fewJin(helper, NEAR_STAND);
        ApertureService.setRank(player, Rank.THREE);
        int points = FEW_JIN * 3;
        int capacity = 48;
        assertEquals(capacity, StrengthService.getCapacity(player), "the rank three capacity moved");
        assertEquals(0, StrengthService.getOverloadPoints(StrengthService.getSettledPoints(player), capacity),
                "20 jin overload a rank three body before the charge");

        fillCharge(player);
        float health = player.getHealth();
        assertTrue(ChargedPunch.release(player, NO_TARGET), "the full charge did not swing");
        assertEquals(health, player.getHealth(), DELTA);
        assertTrue(player.getLastDamageSource() == null, "a swing that hit nothing paid an overload");

        Villager locked = villager(helper, LOCKED, LOW_HEALTH / 2.0F);
        Villager inFan = villager(helper, IN_FAN);
        fillCharge(player);
        health = player.getHealth();
        float expected = fist(player, points);
        assertTrue(ChargedPunch.release(player, locked.getId()), "the full charge did not strike");
        assertTrue(locked.isDeadOrDying(), "the locked target outlived the fist");
        assertEquals(expected - LOW_HEALTH / 2.0F, TARGET_HEALTH - inFan.getHealth(), DELTA);
        float recoil = (points - capacity) / (float) capacity * player.getMaxHealth() * 0.5F;
        assertEquals(health - recoil, player.getHealth(), DELTA);
        assertOverload(player, health, points, capacity);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aCritDoublesTheBearAndAMissOnlyCharges(GameTestHelper helper) {
        ServerPlayer player = player(helper, NEAR_STAND);
        assertTrue(PathStrengthService.grant(player, BeastStrength.BEAR), "the bear was refused");
        useGu(helper, player);
        Villager locked = villager(helper, LOCKED);
        player.setHealth(player.getMaxHealth());
        float health = player.getHealth();
        float recoil = (BEAR_POINTS * 3 - RANK_FIVE) / (float) RANK_FIVE * health * 0.5F;
        assertTrue(recoil < health - 2.0F, "the plain charge already takes the body to two health");

        ChargedPunch.strike(player, locked, StoreStrength.MAX_FACTOR, false);
        assertEquals(fist(player, BEAR_POINTS * 3), TARGET_HEALTH - locked.getHealth(), DELTA);
        assertTrue(BeastPhantomEntity.findOwned(player).isEmpty(), "a missed roll showed a phantom");
        assertOverload(player, health, BEAR_POINTS * 3, RANK_FIVE);

        locked.setHealth(TARGET_HEALTH);
        player.setHealth(health);
        ChargedPunch.strike(player, locked, StoreStrength.MAX_FACTOR, true);
        assertEquals(fist(player, 2 * BEAR_POINTS * 3), TARGET_HEALTH - locked.getHealth(), DELTA);
        assertTrue(BeastPhantomEntity.findOwned(player, BeastStrengthFamily.BEAR) != null,
                "the bear phantom did not show");
        assertOverload(player, health, 2 * BEAR_POINTS * 3, RANK_FIVE);
        assertFalse(ComboRound.isFinishing(player), "the charged punch opened a finisher");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aCritDrawsOneMoreFamilyForEachHardshipTier(GameTestHelper helper) {
        ServerPlayer player = player(helper, NEAR_STAND);
        for (BeastStrength beast : List.of(BeastStrength.BEAR, BeastStrength.TIGER, BeastStrength.STONE_TURTLE)) {
            assertTrue(PathStrengthService.grant(player, beast), "the " + beast + " was refused");
        }
        useGu(helper, player);
        Villager locked = villager(helper, LOCKED);
        int held = BEAR_POINTS + TIGER_POINTS + TURTLE_POINTS;

        ChargedPunch.strike(player, locked, StoreStrength.MAX_FACTOR, true);
        int crit = phantomPoints(player);
        assertEquals(1, BeastPhantomEntity.findOwned(player).size(), "a crit off Hardship drew other than one family");
        assertEquals(fist(player, (held + crit) * 3), TARGET_HEALTH - locked.getHealth(), DELTA);

        inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        player.setHealth(player.getMaxHealth() * TIER_ONE_HEALTH);
        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "Hardship Strength did not start");
        assertEquals(1, HardshipStrength.getTier(player), "70% health is not tier one");

        locked.setHealth(TARGET_HEALTH);
        ChargedPunch.strike(player, locked, StoreStrength.MAX_FACTOR, true);
        crit = phantomPoints(player);
        assertEquals(2, BeastPhantomEntity.findOwned(player).size(), "a tier one crit drew other than two families");
        assertEquals(fist(player, (held + crit) * 110 * 3 / 100), TARGET_HEALTH - locked.getHealth(), DELTA);

        locked.setHealth(TARGET_HEALTH);
        player.setHealth(player.getMaxHealth() * TIER_TWO_HEALTH);
        ChargedPunch.strike(player, locked, StoreStrength.MAX_FACTOR, true);
        assertEquals(3, BeastPhantomEntity.findOwned(player).size(), "a tier two crit held a family back");
        assertEquals(fist(player, 2 * held * 120 * 3 / 100), TARGET_HEALTH - locked.getHealth(), DELTA);

        locked.setHealth(TARGET_HEALTH);
        player.setHealth(player.getMaxHealth() * TIER_TWO_HEALTH);
        ChargedPunch.strike(player, locked, StoreStrength.MAX_FACTOR, false);
        assertEquals(fist(player, held * 120 * 3 / 100), TARGET_HEALTH - locked.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aFullChargeAlwaysCritsAndAllOutEffortLiftsNoCapacity(GameTestHelper helper) {
        ServerPlayer player = player(helper, NEAR_STAND);
        assertTrue(PathStrengthService.grant(player, BeastStrength.BEAR), "the bear was refused");
        useGu(helper, player);
        assertEquals(0.05D, StrengthCrit.getChance(player), DELTA);
        Villager locked = villager(helper, LOCKED);

        fillCharge(player);
        player.setHealth(player.getMaxHealth());
        float health = player.getHealth();
        int points = 2 * BEAR_POINTS * 3;
        assertTrue(ChargedPunch.release(player, locked.getId()), "the full charge did not strike");
        assertEquals(fist(player, points), TARGET_HEALTH - locked.getHealth(), DELTA);
        assertTrue(BeastPhantomEntity.findOwned(player, BeastStrengthFamily.BEAR) != null,
                "a full charge at 5% did not crit");
        assertOverload(player, health, points, RANK_FIVE);

        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        locked.setHealth(TARGET_HEALTH);
        fillCharge(player);
        player.setHealth(player.getMaxHealth());
        health = player.getHealth();
        assertEquals(RANK_FIVE, StrengthService.getCapacity(player), "All-Out Effort lifted the capacity");
        assertTrue(ChargedPunch.release(player, locked.getId()), "the full charge did not strike");
        assertEquals(fist(player, points), TARGET_HEALTH - locked.getHealth(), DELTA);
        assertOverload(player, health, points, RANK_FIVE);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aShortChargeRollsTheCritChance(GameTestHelper helper) {
        ServerPlayer player = player(helper, NEAR_STAND);
        assertTrue(PathStrengthService.grant(player, BeastStrength.BEAR), "the bear was refused");
        useGu(helper, player);
        Villager locked = villager(helper, LOCKED);

        assertTrue(StoreStrength.start(player), "the charge did not start");
        GameTestPlayers.serverTicks(player, HALF_TICKS);
        AreaDamageGameTests.nextRoll(player.getRandom(), StrengthCrit.getChance(player), false);
        assertTrue(ChargedPunch.release(player, locked.getId()), "half a charge did not strike");
        assertEquals(fist(player, BEAR_POINTS * 2), TARGET_HEALTH - locked.getHealth(), DELTA);
        assertTrue(BeastPhantomEntity.findOwned(player).isEmpty(), "a missed roll on half a charge crit");

        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        assertEquals(1.0D, StrengthCrit.getChance(player), DELTA);
        locked.setHealth(TARGET_HEALTH);
        player.setHealth(player.getMaxHealth());
        assertTrue(StoreStrength.start(player), "the second charge did not start");
        GameTestPlayers.serverTicks(player, HALF_TICKS);
        assertTrue(ChargedPunch.release(player, locked.getId()), "half a charge did not strike");
        assertEquals(fist(player, 2 * BEAR_POINTS * 2), TARGET_HEALTH - locked.getHealth(), DELTA);
        assertTrue(BeastPhantomEntity.findOwned(player, BeastStrengthFamily.BEAR) != null,
                "half a charge at 100% did not crit");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aBearChargeShattersByItsSettledPoints(GameTestHelper helper) {
        ServerPlayer player = player(helper, NEAR_STAND);
        assertTrue(PathStrengthService.grant(player, BeastStrength.BEAR), "the bear was refused");
        useGu(helper, player);
        Villager locked = villager(helper, LOCKED);
        helper.setBlock(FAN_FEET, Blocks.DIRT);
        helper.setBlock(FAN_HEAD, Blocks.STONE);
        helper.setBlock(FAN_LEFT, Blocks.DEEPSLATE);

        ChargedPunch.strike(player, locked, StoreStrength.MAX_FACTOR, false);
        assertBlock(helper, FAN_FEET, Blocks.AIR);
        assertBlock(helper, FAN_HEAD, Blocks.STONE);

        ChargedPunch.strike(player, locked, StoreStrength.MAX_FACTOR, true);
        assertBlock(helper, FAN_HEAD, Blocks.AIR);
        assertBlock(helper, FAN_LEFT, Blocks.DEEPSLATE);
        assertBlock(helper, FAN_GROUND, Blocks.STONE);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aFullHumanChargeShattersTheFanUpToHardnessThree(GameTestHelper helper) {
        ServerPlayer player = human(helper, NEAR_STAND);
        Villager locked = villager(helper, LOCKED);
        helper.setBlock(FAN_HEAD, Blocks.DEEPSLATE);
        helper.setBlock(FAN_FEET, Blocks.STONE);
        helper.setBlock(FAN_LEFT, Blocks.COBBLED_DEEPSLATE);
        helper.setBlock(FAN_RIGHT, Blocks.OBSIDIAN);

        fillCharge(player);
        assertTrue(ChargedPunch.release(player, locked.getId()), "the full charge did not strike");

        assertBlock(helper, FAN_HEAD, Blocks.AIR);
        assertBlock(helper, FAN_FEET, Blocks.AIR);
        assertBlock(helper, FAN_LEFT, Blocks.COBBLED_DEEPSLATE);
        assertBlock(helper, FAN_RIGHT, Blocks.OBSIDIAN);
        assertBlock(helper, FAN_GROUND, Blocks.STONE);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theLeastHumanChargeShattersStoneButNotDeepslate(GameTestHelper helper) {
        ServerPlayer player = human(helper, NEAR_STAND);
        Villager locked = villager(helper, LOCKED);
        helper.setBlock(FAN_FEET, Blocks.STONE);
        helper.setBlock(FAN_RIGHT, Blocks.DEEPSLATE);

        assertTrue(StoreStrength.start(player), "the charge did not start");
        GameTestPlayers.serverTicks(player, LEAST_TICKS);
        assertTrue(ChargedPunch.release(player, locked.getId()), "one second of charge did not strike");

        assertBlock(helper, FAN_FEET, Blocks.AIR);
        assertBlock(helper, FAN_RIGHT, Blocks.DEEPSLATE);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void hardshipWidensTheHumanFanByItsTierButNotItsShatter(GameTestHelper helper) {
        ServerPlayer player = human(helper, NEAR_STAND);
        Villager locked = villager(helper, LOCKED, LOW_HEALTH);
        Villager inFan = villager(helper, IN_FAN, LOW_HEALTH);
        Villager flank = villager(helper, FLANK);
        Villager side = villager(helper, SIDE);
        Villager behind = villager(helper, BEHIND);
        helper.setBlock(FAN_RIGHT, Blocks.STONE);
        helper.setBlock(FLANK_BLOCK, Blocks.STONE);
        inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);

        fillCharge(player);
        player.setHealth(player.getMaxHealth() * TIER_ONE_HEALTH);
        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "Hardship Strength did not start");
        player.setHealth(player.getMaxHealth() * TIER_FIVE_HEALTH);
        assertEquals(5, HardshipStrength.getTier(player), "10% health is not tier five");
        float expected = fist(player, JIN * 150 * 3 / 100);

        assertTrue(ChargedPunch.release(player, locked.getId()), "the full charge did not strike");

        assertTrue(locked.isDeadOrDying(), "the locked target outlived the fist");
        assertTrue(inFan.isDeadOrDying(), "the fan target outlived the spill");
        assertEquals(expected - 2.0F * LOW_HEALTH, TARGET_HEALTH - flank.getHealth(), DELTA);
        assertEquals(TARGET_HEALTH, side.getHealth(), DELTA);
        assertEquals(TARGET_HEALTH, behind.getHealth(), DELTA);
        assertBlock(helper, FAN_RIGHT, Blocks.AIR);
        assertBlock(helper, FLANK_BLOCK, Blocks.STONE);
        assertEquals(2.0D, player.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void withoutHardshipTheHumanFanKeepsFortyFiveDegrees(GameTestHelper helper) {
        ServerPlayer player = human(helper, NEAR_STAND);
        Villager locked = villager(helper, LOCKED, LOW_HEALTH);
        Villager inFan = villager(helper, IN_FAN, LOW_HEALTH);
        Villager flank = villager(helper, FLANK);

        fillCharge(player);
        assertTrue(ChargedPunch.release(player, locked.getId()), "the full charge did not strike");

        assertTrue(inFan.isDeadOrDying(), "the fan target outlived the spill");
        assertEquals(TARGET_HEALTH, flank.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void hardshipLengthensTheHumanReachByItsTier(GameTestHelper helper) {
        ServerPlayer player = fewJin(helper, FAR_STAND);
        Villager far = villager(helper, EDGE);

        fillCharge(player);
        float health = player.getHealth();
        assertFalse(HardshipStrength.isActive(player), "Hardship Strength runs without its Gu");
        assertTrue(ChargedPunch.release(player, NO_TARGET), "the full charge did not swing");
        assertEquals(TARGET_HEALTH, far.getHealth(), DELTA);
        assertEquals(health, player.getHealth(), DELTA);

        inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        fillCharge(player);
        player.setHealth(player.getMaxHealth() * TIER_ONE_HEALTH);
        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "Hardship Strength did not start");
        assertEquals(1, HardshipStrength.getTier(player), "70% health is not tier one");
        float expected = fist(player, FEW_JIN * 110 * 3 / 100);
        assertTrue(ChargedPunch.release(player, NO_TARGET), "the widened charge did not swing");
        assertEquals(expected, TARGET_HEALTH - far.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theChargeNeedsABareHandAndTheBuff(GameTestHelper helper) {
        ServerPlayer player = human(helper, NEAR_STAND);
        Villager locked = villager(helper, LOCKED);

        assertTrue(StoreStrength.start(player), "the charge did not start");
        GameTestPlayers.serverTicks(player, 10);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        GameTestPlayers.serverTicks(player, 1);
        assertFalse(StoreStrength.isCharging(player), "the charge outlived the fist");
        assertFalse(StoreStrength.start(player), "a charge started with a sword in hand");
        assertFalse(ChargedPunch.release(player, locked.getId()), "a swordless-fist release struck");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        assertTrue(StoreStrength.start(player), "the charge did not start bare-handed");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EpicFightItems.GLOVE.get()));
        GameTestPlayers.serverTicks(player, 1);
        assertFalse(AttackDamageService.isFistHand(player), "a glove counted as a bare hand");
        assertFalse(StoreStrength.isCharging(player), "the charge outlived the bare hand under a glove");
        assertFalse(StoreStrength.start(player), "a charge started with a glove on");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        assertTrue(StoreStrength.start(player), "the charge did not start with the hand bare again");
        GameTestPlayers.serverTicks(player, 10);
        assertTrue(StoreStrength.isCharging(player), "the charge did not run");
        PathEffectService.remove(player, effect -> effect.type() == ModPathEffectTypes.BUFF);
        assertFalse(TimedEffects.hasBuff(player, ModEffects.STORE_STRENGTH_GU), "the buff was not ended");
        assertFalse(player.hasEffect(ModEffects.STORE_STRENGTH_GU), "the buff's icon outlived its path effect");
        GameTestPlayers.serverTicks(player, 1);
        assertFalse(StoreStrength.isCharging(player), "the charge outlived the buff");
        assertFalse(StoreStrength.start(player), "a charge started with no buff");
        assertEquals(TARGET_HEALTH, locked.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theHabitualGuChargesOnTheMoveAndStrikesTheSame(GameTestHelper helper) {
        ServerPlayer player = human(helper, NEAR_STAND);
        Villager locked = villager(helper, LOCKED);
        assertTrue(StoreStrength.start(player), "the Store Strength charge did not start");
        assertFalse(StoreStrength.isMobile(player), "the Store Strength Gu charges on the move");
        StoreStrength.cancel(player);

        ItemStack gu = hold(player, InteractionHand.MAIN_HAND, ModItems.HABITUAL_STRENGTH_GU_4.get(), GU_HUNGER);
        click(helper, player, gu);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        assertTrue(TimedEffects.hasBuff(player, ModEffects.HABITUAL_STRENGTH_GU), "the Habitual Gu laid no buff");
        PathEffectService.remove(player, effect -> effect.payload() instanceof TimedBuff buff
                && buff.is(ModEffects.STORE_STRENGTH_GU));
        GameTestPlayers.serverTicks(player, 1);
        assertFalse(player.hasEffect(ModEffects.STORE_STRENGTH_GU), "the Store Strength buff was not ended");

        assertTrue(StoreStrength.start(player), "the charge did not start under the Habitual Gu alone");
        assertTrue(StoreStrength.isMobile(player), "the Habitual Gu's charge is not mobile");
        player.setSprinting(true);
        GameTestPlayers.serverTicks(player, 1);
        assertFalse(player.isSprinting(), "the mobile charge let its holder sprint");
        StoreStrength.cancel(player);
        assertFalse(StoreStrength.isMobile(player), "the mobile mark outlived a cancelled charge");

        fillCharge(player);
        assertTrue(StoreStrength.isMobile(player), "the full charge lost its mobile mark");
        float expected = fist(player, FULL_POINTS);
        assertTrue(ChargedPunch.release(player, locked.getId()), "the full mobile charge did not strike");
        assertEquals(expected, TARGET_HEALTH - locked.getHealth(), DELTA);
        assertFalse(StoreStrength.isMobile(player), "the mobile mark outlived the strike");

        assertTrue(StoreStrength.start(player), "the charge did not restart");
        PathEffectService.remove(player, effect -> effect.type() == ModPathEffectTypes.BUFF);
        GameTestPlayers.serverTicks(player, 1);
        assertFalse(StoreStrength.isCharging(player), "the charge outlived the Habitual Gu's buff");
        assertFalse(StoreStrength.isMobile(player), "the mobile mark outlived a dropped charge");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void withTheQiBuffAFarTargetTakesOneTrackingQiWaveAtHalfTheFist(GameTestHelper helper) {
        ServerPlayer player = human(helper, FAR_STAND);
        Villager far = villager(helper, PAST_REACH);
        Villager close = villager(helper, CLOSE);

        fillCharge(player);
        assertTrue(ChargedPunch.release(player, far.getId()), "the full charge did not strike");
        assertTrue(waves(player).isEmpty(), "a Qi Wave flew without the Strength Qi Gu's buff");
        assertEquals(fist(player, FULL_POINTS), TARGET_HEALTH - far.getHealth(), DELTA);
        assertEquals(TARGET_HEALTH, close.getHealth(), DELTA);
        far.setHealth(TARGET_HEALTH);

        qiBuff(helper, player);
        player.setHealth(player.getMaxHealth());
        fillCharge(player);
        float health = player.getHealth();
        float expected = fist(player, FULL_POINTS) * 0.5F;
        assertTrue(ChargedPunch.release(player, far.getId()), "the second full charge did not strike");

        List<QiWaveEntity> waves = waves(player);
        assertEquals(1, waves.size(), "the ranged strike did not throw one Qi Wave");
        assertEquals(expected, waves.getFirst().getDamage(), DELTA);
        assertTrue(waves.getFirst().getOwner() == player, "the wave is not the puncher's");
        assertEquals(TARGET_HEALTH, far.getHealth(), DELTA);
        assertEquals(TARGET_HEALTH, close.getHealth(), DELTA);
        assertEquals(health, player.getHealth(), DELTA);
        assertTrue(charges(player).isEmpty(), "the human path threw a Beast Charge");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aTrackingQiWaveOutOfTimeBurstsWhereItIsAndOnlyThenPaysTheOverload(GameTestHelper helper) {
        ServerPlayer player = human(helper, FAR_STAND);
        qiBuff(helper, player);
        Villager far = villager(helper, PAST_REACH);
        Villager beside = villager(helper, BESIDE_THROW);

        fillCharge(player);
        float health = player.getHealth();
        float expected = fist(player, FULL_POINTS) * 0.5F;
        assertTrue(ChargedPunch.release(player, far.getId()), "the full charge did not strike");
        List<QiWaveEntity> waves = waves(player);
        assertEquals(1, waves.size(), "the ranged strike did not throw one Qi Wave");
        assertEquals(health, player.getHealth(), DELTA);
        QiWaveEntity wave = waves.getFirst();
        wave.tickCount = OUT_OF_TIME_TICKS;
        wave.tick();
        assertTrue(wave.isRemoved(), "the out-of-time wave lingered");
        assertEquals(expected, TARGET_HEALTH - beside.getHealth(), DELTA);
        assertEquals(TARGET_HEALTH, far.getHealth(), DELTA);
        assertOverload(player, health, FULL_POINTS, RANK_FIVE);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void withTheQiBuffABeastThrowsItsCritFamiliesByTheirShares(GameTestHelper helper) {
        ServerPlayer player = player(helper, FAR_STAND);
        assertTrue(PathStrengthService.grant(player, BeastStrength.TIGER), "the tiger was refused");
        assertTrue(PathStrengthService.grant(player, BeastStrength.BEAR), "the bear was refused");
        useGu(helper, player);
        qiBuff(helper, player);
        Villager far = villager(helper, PAST_REACH);
        Villager close = villager(helper, CLOSE);
        float health = player.getHealth();
        int held = TIGER_POINTS + BEAR_POINTS;

        ChargedPunch.strike(player, far, StoreStrength.MAX_FACTOR, false);
        List<BeastChargeEntity> one = charges(player);
        assertEquals(1, one.size(), "a missed roll did not throw one Beast Charge");
        assertEquals(fist(player, held * 3) * 0.5F, one.getFirst().getDamage(), DELTA);
        assertTrue(BeastPhantomEntity.findOwned(player).isEmpty(), "a missed roll showed a phantom");
        one.getFirst().discard();

        ChargedPunch.strike(player, far, StoreStrength.MAX_FACTOR, true);
        List<BeastChargeEntity> lone = charges(player);
        assertEquals(1, lone.size(), "a crit off Hardship did not throw its one family");
        int crit = phantomPoints(player);
        assertEquals(fist(player, (held + crit) * 3) * 0.5F, lone.getFirst().getDamage(), DELTA);
        assertEquals(BeastPhantomEntity.findOwned(player).getFirst().getFamily(), lone.getFirst().getFamily(),
                "the charge is not the crit family");
        lone.getFirst().discard();

        inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        player.setHealth(player.getMaxHealth() * TIER_ONE_HEALTH);
        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "Hardship Strength did not start");
        health = player.getHealth();
        ChargedPunch.strike(player, far, StoreStrength.MAX_FACTOR, true);
        List<BeastChargeEntity> charges = charges(player);
        assertEquals(2, charges.size(), "the tier one crit families did not each throw a Beast Charge");
        float total = fist(player, 2 * held * 110 * 3 / 100) * 0.5F;
        for (BeastChargeEntity charge : charges) {
            int share = charge.getFamily() == BeastStrengthFamily.TIGER ? TIGER_POINTS : BEAR_POINTS;
            assertEquals(total * share / held, charge.getDamage(), DELTA);
        }
        assertTrue(charges.getFirst().getFamily() != charges.getLast().getFamily(), "one family was thrown twice");
        assertTrue(waves(player).isEmpty(), "the beast path threw a Qi Wave");
        assertEquals(TARGET_HEALTH, far.getHealth(), DELTA);
        assertEquals(TARGET_HEALTH, close.getHealth(), DELTA);
        assertEquals(health, player.getHealth(), DELTA);
        assertEquals(2, BeastPhantomEntity.findOwned(player).size(), "a crit family showed no phantom");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aTargetBehindThePunchIsNotKept(GameTestHelper helper) {
        ServerPlayer player = human(helper, NEAR_STAND);
        Villager ahead = villager(helper, LOCKED);
        Villager behind = villager(helper, BEHIND);

        assertTrue(ChargedPunch.getTarget(player, ahead.getId()) == ahead, "the target ahead was not kept");
        assertTrue(ChargedPunch.getTarget(player, behind.getId()) == null, "a target behind the puncher was kept");
        assertTrue(ChargedPunch.getTarget(player, player.getId()) == null, "the puncher was kept as its own target");
        assertTrue(ChargedPunch.getTarget(player, NO_TARGET) == null, "no id kept a target");

        fillCharge(player);
        assertTrue(ChargedPunch.release(player, behind.getId()), "the full charge did not strike");
        assertEquals(TARGET_HEALTH, behind.getHealth(), DELTA);
        assertTrue(ahead.getHealth() < TARGET_HEALTH, "the swing along the look missed the mob ahead");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theFistDamageKeepsTheAttackMultipliers(GameTestHelper helper) {
        ServerPlayer player = human(helper, NEAR_STAND);
        AttackDamageService.refresh(player);
        AttributeInstance attack = assertNotNull(player.getAttribute(Attributes.ATTACK_DAMAGE),
                () -> "the player has no attack");
        assertTrue(attack.getValue() > attack.getBaseValue(), "the strength bonus is not on the attack");

        double plain = AttackDamageService.getFistDamage(player, SOME_POINTS);
        assertTrue(plain >= SOME_POINTS * PathStrengthData.ATTACK_PER_POINT, "the points are missing");
        attack.addTransientModifier(new AttributeModifier(Guzhenren.id("gametest_store_strength_total"), 0.5D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        assertEquals(plain * 1.5D, AttackDamageService.getFistDamage(player, SOME_POINTS), DELTA);
        attack.addTransientModifier(new AttributeModifier(Guzhenren.id("gametest_store_strength_weakness"), -100.0D,
                AttributeModifier.Operation.ADD_VALUE));
        assertEquals(0.0D, AttackDamageService.getFistDamage(player, 0), DELTA);
        helper.succeed();
    }

    private static GuItem.Refusal refusal(TendedGuItem item, ServerPlayer player, ItemStack gu) {
        return item.getBehavior(StoreStrengthBehavior.class).checkEffect(item, player, gu);
    }

    private static float fist(ServerPlayer player, int points) {
        return (float) AttackDamageService.getFistDamage(player, points);
    }

    private static int phantomPoints(ServerPlayer player) {
        PathStrengthData data = PathStrengthService.get(player);
        int points = 0;
        for (BeastPhantomEntity phantom : BeastPhantomEntity.findOwned(player)) {
            points += data.getBeastPoints(phantom.getFamily());
        }
        return points;
    }

    private static void assertFactor(ServerPlayer player, int ticks, float factor) {
        assertTrue(StoreStrength.start(player), "the charge did not start");
        GameTestPlayers.serverTicks(player, ticks);
        assertEquals(factor, StoreStrength.release(player), DELTA);
    }

    private static void assertOverload(ServerPlayer player, float healthBefore, int points, int capacity) {
        float recoil = (points - capacity) / (float) capacity * player.getMaxHealth() * 0.5F;
        assertTrue(recoil > 0.0F, "the strike does not pass the capacity");
        assertEquals(Math.max(2.0F, healthBefore - recoil), player.getHealth(), DELTA);
        DamageSource source = assertNotNull(player.getLastDamageSource(), () -> "no overload was recorded");
        assertTrue(source.is(ModDamageTypes.BODY_OVERLOAD), "the recoil is not the body overload");
        assertTrue(source.is(DamageTypeTags.NO_KNOCKBACK), "the body overload may knock back");
        assertTrue(player.getDeltaMovement().horizontalDistanceSqr() < SHOVE_SQR,
                "the body overload shoved its puncher");
    }

    private static List<QiWaveEntity> waves(ServerPlayer player) {
        return player.level().getEntitiesOfClass(QiWaveEntity.class, player.getBoundingBox().inflate(16.0D));
    }

    private static List<BeastChargeEntity> charges(ServerPlayer player) {
        return player.level().getEntitiesOfClass(BeastChargeEntity.class, player.getBoundingBox().inflate(16.0D));
    }

    private static void fillCharge(ServerPlayer player) {
        assertTrue(StoreStrength.start(player), "the charge did not start");
        GameTestPlayers.serverTicks(player, StoreStrength.CHARGE_TICKS);
        assertTrue(StoreStrength.getProgress(player) > 1.0F - DELTA, "the charge is not full");
    }

    private static void useGu(GameTestHelper helper, ServerPlayer player) {
        ItemStack gu = hold(player, InteractionHand.MAIN_HAND, ModItems.STORE_STRENGTH_GU_3.get(), GU_HUNGER);
        click(helper, player, gu);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        assertTrue(TimedEffects.hasBuff(player, ModEffects.STORE_STRENGTH_GU), "the Gu laid no buff");
    }

    private static void qiBuff(GameTestHelper helper, ServerPlayer player) {
        ItemStack gu = hold(player, InteractionHand.MAIN_HAND, ModItems.STRENGTH_QI_GU_3.get(), GU_HUNGER);
        click(helper, player, gu);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        assertTrue(TimedEffects.hasBuff(player, ModEffects.STRENGTH_QI_GU), "the Strength Qi Gu laid no buff");
    }

    private static ServerPlayer human(GameTestHelper helper, Vec3 stand) {
        ServerPlayer player = player(helper, stand);
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JUN, 6), "the jun was refused");
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.TEN_JIN, 2), "the ten jin was refused");
        assertEquals(JIN, StrengthService.getPermanentPoints(player), "the body does not hold 200 jin");
        useGu(helper, player);
        return player;
    }

    private static ServerPlayer fewJin(GameTestHelper helper, Vec3 stand) {
        ServerPlayer player = player(helper, stand);
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.TEN_JIN, 2), "the ten jin was refused");
        assertEquals(FEW_JIN, StrengthService.getPermanentPoints(player), "the body does not hold 20 jin");
        useGu(helper, player);
        return player;
    }

    private static ServerPlayer player(GameTestHelper helper, Vec3 stand) {
        GameTestPlayers.ground(helper);
        return GameTestGu.awakened(GameTestPlayers.survival(helper, "gzr-store-strength-gametest")
                .pastSpawnInvulnerability().at(stand), Rank.FIVE);
    }

    private static void assertBlock(GameTestHelper helper, BlockPos pos, Block block) {
        assertTrue(helper.getBlockState(pos).is(block),
                pos + " is " + helper.getBlockState(pos) + ", not " + block);
    }

    private static Villager villager(GameTestHelper helper, BlockPos at) {
        return villager(helper, at, TARGET_HEALTH);
    }

    private static Villager villager(GameTestHelper helper, BlockPos at, float health) {
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, at);
        villager.getAttribute(Attributes.MAX_HEALTH).setBaseValue(TARGET_HEALTH);
        villager.setHealth(health);
        return villager;
    }
}
