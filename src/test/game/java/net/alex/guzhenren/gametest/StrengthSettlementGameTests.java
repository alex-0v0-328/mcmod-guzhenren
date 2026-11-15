package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.BodyOverload;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.SprintSpeedService;
import net.alex.guzhenren.gameplay.path.strength.StrengthRound;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.alex.guzhenren.item.gu.mortal.path.strength.BeastStrengthBehavior;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.registry.entries.EpicFightItems;
import static net.alex.guzhenren.gametest.GameTestApertures.awakenedMortal;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 在线玩家力道结算测试。
 * <ul>
 *   <li>常态承受力随转、体质、炼体变化，可用点数随之进攻击力；攻击加成只给空手，持剑或 Epic Fight 拳套无。</li>
 *   <li>全力以赴蛊[All-Out Effort Gu]按档位提升承受力，只作用于有分支的肉身，出拳不付负荷；限时兽力超出承受力则每拳付肉
 *       体过载，最低一颗心。</li>
 *   <li>新永久兽力[Beast Strength]准入门槛；棕熊本力蛊[Brown Bear Innate Strength Gu]替换熊或全新授予都到 96 点，
 *       之后拒熊；石龟承受力；驰马疾跑及其解放轮额外步行与疾跑速度。</li>
 *   <li>效果驱动攻击力写入是服务端任务，检查等 2 tick；模拟玩家不 tick，需时手调 {@code PlayerTickEvents}
 *       换手刷新。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StrengthSettlementGameTests {

    private StrengthSettlementGameTests() {}

    private static final int EFFECT_TICKS = 600;
    private static final int RANK_FIVE_TIER = 4;
    private static final int SETTLE_TICKS = 2;
    private static final double DELTA = 0.001D;

    @GameTest(template = "empty9x9x9")
    public static void capacityFollowsRankAndPhysique(GameTestHelper helper) {
        ServerPlayer mortal = mortal(helper);
        assertEquals(StrengthService.MORTAL_CAPACITY, StrengthService.getCapacity(mortal), "mortal capacity");
        assertEquals(16, StrengthService.getCapacity(mortal), "mortal capacity is not 16");

        ServerPlayer player = awakenedMortal(helper);
        Rank[] ranks = { Rank.ONE, Rank.TWO, Rank.THREE, Rank.FOUR, Rank.FIVE };
        for (int index = 0; index < ranks.length; index++) {
            ApertureService.setRank(player, ranks[index]);
            int expected = 16 + 8 * (index + 1);
            assertEquals(expected, StrengthService.getBaseCapacity(ranks[index], ExtremePhysique.NONE),
                    "base capacity at " + ranks[index]);
            assertEquals(expected, StrengthService.getCapacity(player), "capacity at " + ranks[index]);
        }

        ApertureService.setRank(player, Rank.TWO);
        BodyService.setExtremePhysique(player, ExtremePhysique.VERDANT_GREAT_SUN);
        assertEquals(32 * 2, StrengthService.getCapacity(player), "a ten-extreme body did not double the capacity");
        BodyService.setExtremePhysique(player, ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL);
        assertEquals(32 * 4, StrengthService.getCapacity(player), "the true martial body did not quadruple it");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void temperingAddsToCapacityOutsideThePhysiqueMultiplier(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        ApertureService.setRank(player, Rank.TWO);
        BodyService.setExtremePhysique(player, ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL);
        int base = 32 * 4;
        assertEquals(base, StrengthService.getCapacity(player));

        BodyService.temperTendon(player, Tendon.STEEL_TENDON_3);
        assertEquals(base + Tendon.STEEL_TENDON_3.getCapacityBonus(), StrengthService.getCapacity(player),
                "tendons were multiplied or missed");
        BodyService.temperBone(player, Bone.IRON_BONE_4);
        assertEquals(base + Tendon.STEEL_TENDON_3.getCapacityBonus() + Bone.IRON_BONE_4.getCapacityBonus(),
                StrengthService.getCapacity(player), "bones were multiplied or missed");
        BodyService.wearSkin(player, Skin.IRON_SKIN_4, 0);
        assertEquals(base + Tendon.STEEL_TENDON_3.getCapacityBonus() + Bone.IRON_BONE_4.getCapacityBonus() + 108,
                StrengthService.getCapacity(player), "the skin was multiplied or missed");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void skinWornAndShedMoveCapacityAndAttackAtOnce(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        assertEquals(16, StrengthService.getCapacity(player));
        assertEquals(base + 16 * PathStrengthData.ATTACK_PER_POINT, player.getAttributeValue(Attributes.ATTACK_DAMAGE),
                DELTA);

        BodyService.wearSkin(player, Skin.IRON_SKIN_4, 0);
        assertEquals(16 + 108, StrengthService.getCapacity(player), "the worn skin did not move the capacity");
        assertEquals(base + 40 * PathStrengthData.ATTACK_PER_POINT, player.getAttributeValue(Attributes.ATTACK_DAMAGE),
                DELTA);

        BodyService.shedSkin(player);
        assertEquals(16, StrengthService.getCapacity(player), "the shed skin kept its capacity");
        assertEquals(base + 16 * PathStrengthData.ATTACK_PER_POINT, player.getAttributeValue(Attributes.ATTACK_DAMAGE),
                DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void allOutEffortLiftsTheCapacityByTheGuTier(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        PathStrengthService.grant(player, BeastStrength.BEAR);

        assertEquals(40, StrengthService.getStrengthPoints(player));
        assertEquals(16, StrengthService.getUsablePoints(player), "usable points passed the capacity");
        assertEquals(base + 2.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);

        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            assertEquals(48, StrengthService.getCapacity(player), "rank five All-Out did not add 200%");
            assertEquals(16, StrengthService.getNormalCapacity(player), "the lift moved the normal capacity");
            assertEquals(40, StrengthService.getUsablePoints(player), "the lifted capacity did not carry the bear");
            assertEquals(base + 5.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);

            player.removeEffect(ModEffects.ALL_OUT_EFFORT_GU);
            helper.runAfterDelay(SETTLE_TICKS, () -> {
                assertEquals(16, StrengthService.getUsablePoints(player), "the points stayed lifted");
                assertEquals(16, StrengthService.getCapacity(player), "the capacity stayed lifted");
                assertEquals(base + 2.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void allOutEffortCapsAtItsTierAndNeedsABranch(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        ServerPlayer bare = GameTestPlayers.survival(helper, "gzr-strength-bare-gametest").build();
        PathStrengthService.grant(player, BeastStrength.BEAR);
        PathStrengthService.grant(player, BeastStrength.TIGER);
        assertEquals(104, StrengthService.getStrengthPoints(player));

        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS));
        bare.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            assertEquals(24, StrengthService.getCapacity(player), "a command's tier zero did not read as rank three");
            assertEquals(24, StrengthService.getUsablePoints(player), "the lift carried more than its 50%");
            assertEquals(16, StrengthService.getCapacity(bare), "a body without a branch was lifted");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aHitUnderAllOutEffortCostsNoLoad(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-strength-gametest").pastSpawnInvulnerability()
                .onGround();
        PathStrengthService.grant(player, BeastStrength.BEAR);
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS));
        player.setHealth(20.0F);

        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 2));
        assertTrue(pig.hurt(player.damageSources().playerAttack(player), 1.0F), "the hit did no damage");
        assertEquals(20.0D, player.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theStrengthBonusCountsOnlyForAFist(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        assertTrue(AttackDamageService.isFistHand(player), "an empty hand is not a fist");
        assertEquals(base + 2.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        assertFalse(AttackDamageService.isFistHand(player), "a sword counted as a fist");
        AttackDamageService.refresh(player);
        assertEquals(0.0D, AttackDamageService.getStrengthBonus(player), DELTA);
        double sword = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        assertTrue(Math.abs(sword - base - 2.0D) > DELTA, "the strength bonus stayed on with a sword in hand");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EpicFightItems.GLOVE.get()));
        assertFalse(AttackDamageService.isFistHand(player), "a glove counted as a bare fist");
        AttackDamageService.refresh(player);
        assertEquals(0.0D, AttackDamageService.getStrengthBonus(player), DELTA);

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        AttackDamageService.refresh(player);
        assertEquals(base + 2.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aNewBeastMustFitUnderTheNormalCapacity(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        ApertureService.setRank(player, Rank.ONE);
        assertTrue(StrengthService.canAdmit(player, BeastStrength.WHITE_BOAR), "rank one refused its boar");
        assertFalse(StrengthService.canAdmit(player, BeastStrength.BEAR), "rank one admitted a bear");

        PathStrengthService.grant(player, BeastStrength.WHITE_BOAR);
        assertFalse(StrengthService.canAdmit(player, BeastStrength.BLACK_BOAR), "a full rank one admitted a second boar");

        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            assertEquals(72, StrengthService.getCapacity(player));
            assertFalse(StrengthService.canAdmit(player, BeastStrength.BLACK_BOAR), "the All-Out lift admitted a beast");

            BodyService.wearSkin(player, Skin.IRON_SKIN_3, 0);
            assertTrue(StrengthService.canAdmit(player, BeastStrength.BLACK_BOAR), "iron skin three did not make room");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void theBrownBearGuReplacesTheBearOrComesFreshToTheSameNinetySix(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        ApertureService.setRank(player, Rank.ONE);
        BodyService.wearSkin(player, Skin.IRON_SKIN_4, 0);
        PathStrengthService.grant(player, BeastStrength.WHITE_BOAR);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        assertEquals(132, StrengthService.getNormalCapacity(player));
        assertNull(checkBeast(player, ModItems.BROWN_BEAR_INNATE_STRENGTH_GU_3.get()));
        applyBeast(player, ModItems.BROWN_BEAR_INNATE_STRENGTH_GU_3.get());
        PathStrengthData upgraded = PathStrengthService.get(player);
        assertFalse(upgraded.has(BeastStrength.BEAR), "the bear stayed beside its innate strength");
        assertEquals(96, upgraded.getBeastPoints(BeastStrengthFamily.BEAR));
        assertEquals(120, upgraded.getBeastPoints(), "the boar did not stay beside the upgrade");
        assertHeld(checkBeast(player, ModItems.BEAR_STRENGTH_GU_1.get()));
        assertHeld(checkBeast(player, ModItems.BROWN_BEAR_INNATE_STRENGTH_GU_3.get()));
        assertEquals("guzhenren.item.failed.beast_strength_capacity",
                assertNotNull(checkBeast(player, ModItems.BLACK_BOAR_GU_1.get()), () -> "144 passed 132").key(),
                "the white boar kept out the black");

        PathStrengthService.revoke(player, BeastStrength.BROWN_BEAR);
        assertNull(checkBeast(player, ModItems.BROWN_BEAR_INNATE_STRENGTH_GU_3.get()));
        applyBeast(player, ModItems.BROWN_BEAR_INNATE_STRENGTH_GU_3.get());
        assertEquals(upgraded, PathStrengthService.get(player), "the fresh grant differs from the upgrade");

        PathStrengthService.clear(player);
        PathStrengthService.grant(player, BeastStrength.TIGER);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        assertEquals("guzhenren.item.failed.beast_strength_capacity",
                assertNotNull(checkBeast(player, ModItems.BROWN_BEAR_INNATE_STRENGTH_GU_3.get()),
                        () -> "64 + 96 passed 132").key());
        helper.succeed();
    }

    private static @Nullable Refusal checkBeast(ServerPlayer player, Item item) {
        MortalGuItem gu = (MortalGuItem) item;
        BeastStrengthBehavior behavior = assertNotNull(gu.getBehavior(BeastStrengthBehavior.class),
                () -> item + " lacks its beast strength");
        return behavior.checkEffect(gu, player, new ItemStack(item));
    }

    private static void applyBeast(ServerPlayer player, Item item) {
        MortalGuItem gu = (MortalGuItem) item;
        assertNotNull(gu.getBehavior(BeastStrengthBehavior.class), () -> item + " lacks its beast strength")
                .applyEffect(gu, player, new ItemStack(item));
    }

    private static void assertHeld(@Nullable Refusal refusal) {
        assertEquals("guzhenren.item.failed.beast_strength_held",
                assertNotNull(refusal, () -> "a covered beast was allowed").key());
    }

    @GameTest(template = "empty9x9x9")
    public static void theStoneTurtleNeedsRoomForItselfThenCarriesTheRest(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        ApertureService.setRank(player, Rank.THREE);
        assertFalse(StrengthService.canAdmit(player, BeastStrength.STONE_TURTLE), "rank three bare admitted the turtle");

        ApertureService.setRank(player, Rank.FOUR);
        assertTrue(StrengthService.canAdmit(player, BeastStrength.STONE_TURTLE), "rank four bare refused the turtle");
        PathStrengthService.grant(player, BeastStrength.STONE_TURTLE);
        assertEquals(48 + 128, StrengthService.getNormalCapacity(player), "the turtle carried no capacity");
        assertEquals(48, StrengthService.getUsablePoints(player));

        PathStrengthService.grant(player, BeastStrength.TIGER);
        PathStrengthService.grant(player, BeastStrength.GALLOPING_HORSE);
        assertEquals(160, StrengthService.getUsablePoints(player), "the turtle's room did not seat tiger and horse");
        assertFalse(StrengthService.canAdmit(player, BeastStrength.BEAR), "a full turtle body admitted a bear");

        PathStrengthService.revoke(player, BeastStrength.STONE_TURTLE);
        assertEquals(48, StrengthService.getNormalCapacity(player), "the turtle's capacity outlived it");
        assertEquals(48, StrengthService.getUsablePoints(player), "the tiger and horse were not cut to the capacity");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theGallopingHorseSpeedsOnlyTheSprint(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        double walk = player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        PathStrengthService.grant(player, BeastStrength.GALLOPING_HORSE);
        SprintSpeedService.refresh(player);
        assertEquals(walk, player.getAttributeValue(Attributes.MOVEMENT_SPEED), DELTA);

        player.setSprinting(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        SprintSpeedService.refresh(player);
        assertEquals(walk * 1.8D, player.getAttributeValue(Attributes.MOVEMENT_SPEED), DELTA);

        PathStrengthService.revoke(player, BeastStrength.GALLOPING_HORSE);
        SprintSpeedService.refresh(player);
        assertEquals(walk * SprintSpeedService.VANILLA_SPRINT_FACTOR,
                player.getAttributeValue(Attributes.MOVEMENT_SPEED), DELTA);
        player.setSprinting(false);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void aReleasedHorseSpeedsTheWalkAndTheSprintForItsRound(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        double walk = player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        PathStrengthService.grant(player, BeastStrength.GALLOPING_HORSE);
        StrengthRound.open(player, 0.0F);
        SprintSpeedService.refresh(player);
        assertEquals(walk * 1.5D, player.getAttributeValue(Attributes.MOVEMENT_SPEED), DELTA);

        player.setSprinting(true);
        SprintSpeedService.refresh(player);
        assertEquals(walk * 2.3D, player.getAttributeValue(Attributes.MOVEMENT_SPEED), DELTA);
        player.setSprinting(false);

        helper.runAfterDelay(StrengthRound.ROUND_TICKS + 1, () -> {
            SprintSpeedService.refresh(player);
            assertEquals(walk, player.getAttributeValue(Attributes.MOVEMENT_SPEED), DELTA);
            StrengthRound.clear(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aJadeSkinAddsNoCapacity(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);

        BodyService.wearSkin(player, Skin.WHITE_JADE_SKIN, EFFECT_TICKS);
        assertEquals(16, StrengthService.getCapacity(player), "a timed skin moved the capacity");
        assertEquals(base, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);

        BodyService.shedSkin(player);
        assertEquals(16, StrengthService.getCapacity(player));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void healingIsNotCappedByHardshipStrength(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        player.addEffect(ModEffects.instance(ModEffects.HARDSHIP_STRENGTH_GU, EFFECT_TICKS));

        player.setHealth(2.0F);
        player.heal(15.0F);
        assertEquals(17.0D, player.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void eachFistHitPaysTheBodyOverloadDownToOneHeart(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-overload-gametest").pastSpawnInvulnerability()
                .onGround();
        ApertureService.awaken(player, 80);
        ApertureService.setRank(player, Rank.FIVE);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        TimedEffects.grantBuff(player, (TendedGuItem) ModItems.FLOWER_BOAR_GU_1.get(), ModEffects.FLOWER_BOAR_GU, 0,
                EFFECT_TICKS, Amplification.NONE);

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            assertEquals(56, StrengthService.getCapacity(player), "the boar moved the capacity");
            assertEquals(8, StrengthService.snapshot(player).overloadPoints());
            assertEquals(64, StrengthService.getUsablePoints(player), "the overload was not brought to bear");

            float max = player.getMaxHealth();
            float recoil = 8.0F / 56.0F * max * BodyOverload.RECOIL_RATIO;
            player.setHealth(max);
            punch(helper, player, new BlockPos(2, 1, 2));
            assertEquals(max - recoil, player.getHealth(), DELTA);
            DamageSource source = player.getLastDamageSource();
            assertTrue(source != null && source.is(ModDamageTypes.BODY_OVERLOAD), "the recoil is not a body overload");
            assertTrue(source.is(DamageTypeTags.BYPASSES_ARMOR), "the body overload stops at armor");
            assertTrue(source.getEntity() == null, "the body overload counted as an attack");
            assertTrue(source.is(DamageTypeTags.NO_KNOCKBACK), "the body overload may knock back");
            assertTrue(player.getDeltaMovement().horizontalDistanceSqr() < 1.0E-6D,
                    "the body overload shoved its puncher");
            assertEquals(0, player.invulnerableTime, "the recoil left hurt frames on the attacker");

            punch(helper, player, new BlockPos(6, 1, 2));
            assertEquals(max - 2.0F * recoil, player.getHealth(), DELTA);

            player.setHealth(3.0F);
            punch(helper, player, new BlockPos(2, 1, 6));
            assertEquals(BodyOverload.HEALTH_FLOOR, player.getHealth(), DELTA);
            punch(helper, player, new BlockPos(6, 1, 6));
            assertEquals(BodyOverload.HEALTH_FLOOR, player.getHealth(), DELTA);

            player.setHealth(max);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
            punch(helper, player, new BlockPos(4, 1, 6));
            assertEquals(max, player.getHealth(), DELTA);
            helper.succeed();
        });
    }

    private static void punch(GameTestHelper helper, ServerPlayer player, BlockPos at) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, at);
        assertTrue(pig.hurt(player.damageSources().playerAttack(player), 1.0F), "the punch did no damage");
    }

    private static ServerPlayer mortal(GameTestHelper helper) {
        return GameTestApertures.mortal(helper);
    }
}
