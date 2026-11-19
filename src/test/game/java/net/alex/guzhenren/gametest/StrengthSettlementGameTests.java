package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.display.InfoModel;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.body.ShieldLayer;
import net.alex.guzhenren.gameplay.body.ShieldService;
import net.alex.guzhenren.gameplay.body.ShieldType;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.ComboRound;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.BodyOverload;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.SprintSpeedService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthSnapshot;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.alex.guzhenren.item.gu.mortal.path.strength.AllOutEffortBehavior;
import net.alex.guzhenren.item.gu.mortal.path.strength.BeastStrengthBehavior;
import net.alex.guzhenren.item.gu.mortal.path.strength.HumanStrengthBehavior;
import net.alex.guzhenren.item.gu.mortal.path.strength.StoreStrengthBehavior;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.registry.entries.EpicFightItems;
import static net.alex.guzhenren.gametest.GameTestApertures.awakenedMortal;
import static net.alex.guzhenren.gametest.GameTestGu.click;
import static net.alex.guzhenren.gametest.GameTestGu.hunger;
import static net.alex.guzhenren.gametest.GameTestGu.refined;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertSame;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 在线玩家力道结算测试。
 * <ul>
 *   <li>常态承受力随转、体质、炼体变化；力量点全额进攻击力、不按承受力截断；攻击加成只给空手，持剑或 Epic Fight 拳套
 *       无。</li>
 *   <li>全力以赴蛊[All-Out Effort Gu]按档位提升当前承受力，只作用于已入分支（含生效中的限时兽力）的肉身；全力以赴、
 *       蓄力、惯力的催动门槛同认限时兽力。</li>
 *   <li>过载反噬：结算力量点超出当前承受力的部分，每次出手在伤害落到目标后结算一次（同一招后续命中、被无敌帧吞掉
 *       或被盾牌完全挡下的命中与挥空不扣，挡下也不占这一招的名额），最低留 2 血；事后超限（脱皮、换体质、指令）力
 *       量不消失、同规则反噬。</li>
 *   <li>过载反噬按公式实扣：皮肤固定减伤、肉身护盾、抗性效果与保护附魔都不抵（护盾点数不动）。</li>
 *   <li>准入：永久兽力[Beast Strength]与人力共用常态承受力，拒绝不扣费，增幅与全力以赴不占不抬，指令不过准入，百钧
 *       之力凡人灌不进；棕熊本力蛊[Brown Bear Innate Strength Gu]替换熊或全新授予都到 96 点，之后拒熊；石龟承受力；
 *       驰马疾跑及其收尾解放后虚影显示期间的额外步行与疾跑速度。</li>
 *   <li>B 面板力量点行列总力量点、当前与常态承受力、超出点数。</li>
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
    private static final int SHIELD_RAISE_TICKS = 8;
    private static final int GU_HUNGER = 12;
    private static final String NEEDS_BRANCH = "guzhenren.item.failed.all_out_needs_branch";
    private static final String HUMAN_CAPACITY = "guzhenren.item.failed.human_strength_capacity";
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
    public static void skinWornAndShedMoveTheCapacityAndNeverTheAttack(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        assertEquals(16, StrengthService.getCapacity(player));
        assertEquals(40, StrengthService.getSettledPoints(player), "a mortal's bear was cut to the capacity");
        assertEquals(24, overload(player));
        assertEquals(base + 40 * PathStrengthData.ATTACK_PER_POINT, player.getAttributeValue(Attributes.ATTACK_DAMAGE),
                DELTA);

        BodyService.wearSkin(player, Skin.IRON_SKIN_4, 0);
        assertEquals(16 + 108, StrengthService.getCapacity(player), "the worn skin did not move the capacity");
        assertEquals(0, overload(player), "the worn skin did not carry the bear");
        assertEquals(base + 40 * PathStrengthData.ATTACK_PER_POINT, player.getAttributeValue(Attributes.ATTACK_DAMAGE),
                DELTA);

        BodyService.shedSkin(player);
        assertEquals(16, StrengthService.getCapacity(player), "the shed skin kept its capacity");
        assertEquals(24, overload(player));
        assertEquals(base + 40 * PathStrengthData.ATTACK_PER_POINT, player.getAttributeValue(Attributes.ATTACK_DAMAGE),
                DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void allOutEffortLiftsTheCapacityByTheGuTier(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        PathStrengthService.grant(player, BeastStrength.BEAR);

        assertEquals(40, StrengthService.getStrengthPoints(player));
        assertEquals(40, StrengthService.getSettledPoints(player), "the capacity cut the settled points");
        assertEquals(24, overload(player));
        assertEquals(base + 5.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);

        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            assertEquals(48, StrengthService.getCapacity(player), "rank five All-Out did not add 200%");
            assertEquals(16, StrengthService.getNormalCapacity(player), "the lift moved the normal capacity");
            assertEquals(40, StrengthService.getSettledPoints(player), "the lift moved the settled points");
            assertEquals(0, overload(player), "the lifted capacity did not carry the bear");
            assertEquals(base + 5.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);

            player.removeEffect(ModEffects.ALL_OUT_EFFORT_GU);
            helper.runAfterDelay(SETTLE_TICKS, () -> {
                assertEquals(16, StrengthService.getCapacity(player), "the capacity stayed lifted");
                assertEquals(24, overload(player), "the overload did not come back");
                assertEquals(base + 5.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);
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
            assertEquals(104, StrengthService.getSettledPoints(player), "the points past the lift were cut");
            assertEquals(80, overload(player), "the lift carried more than its 50%");
            assertEquals(16, StrengthService.getCapacity(bare), "a body without a branch was lifted");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aTimedBeastIsBranchEnoughForAllOutEffortAndTheChargingGu(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        ApertureService.setRank(player, Rank.ONE);
        List<Refusal> bare = List.of(
                assertNotNull(check(player, ModItems.ALL_OUT_EFFORT_GU_3.get(), AllOutEffortBehavior.class),
                        () -> "All-Out Effort took a body with no branch"),
                assertNotNull(check(player, ModItems.STORE_STRENGTH_GU_3.get(), StoreStrengthBehavior.class),
                        () -> "Store Strength took a body with no branch"),
                assertNotNull(check(player, ModItems.HABITUAL_STRENGTH_GU_4.get(), StoreStrengthBehavior.class),
                        () -> "Habitual Strength took a body with no branch"));
        for (Refusal refusal : bare) assertEquals(NEEDS_BRANCH, refusal.key());
        assertSame(StrengthPathBranch.NORMAL, PathStrengthService.getHeldBranch(player));

        TimedEffects.grantBuff(player, (TendedGuItem) ModItems.FLOWER_BOAR_GU_1.get(), ModEffects.FLOWER_BOAR_GU, 0,
                EFFECT_TICKS, Amplification.NONE);
        assertSame(StrengthPathBranch.BEAST_STRENGTH_PHANTOM, PathStrengthService.getHeldBranch(player));
        assertEquals(0, StrengthService.getPermanentPoints(player), "the timed boar counted as permanent");
        assertNull(check(player, ModItems.ALL_OUT_EFFORT_GU_3.get(), AllOutEffortBehavior.class));
        assertNull(check(player, ModItems.STORE_STRENGTH_GU_3.get(), StoreStrengthBehavior.class));
        assertNull(check(player, ModItems.HABITUAL_STRENGTH_GU_4.get(), StoreStrengthBehavior.class));

        assertEquals(24, StrengthService.getCapacity(player), "the timed boar moved the capacity");
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        assertEquals(72, StrengthService.getCapacity(player), "the lift did not read the timed boar as a branch");

        PathEffectService.removeType(player, ModPathEffectTypes.BUFF);
        assertEquals(24, StrengthService.getCapacity(player), "the lift outlived the timed boar");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHitUnderAllOutEffortRecoilsOnlyPastTheLiftedCapacity(GameTestHelper helper) {
        ServerPlayer player = fighter(helper, "gzr-strength-gametest");
        PathStrengthService.grant(player, BeastStrength.BEAR);
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        float max = player.getMaxHealth();
        player.setHealth(max);

        assertEquals(48, StrengthService.getCapacity(player));
        strike(helper, player, new BlockPos(4, 1, 2));
        assertEquals(max, player.getHealth(), DELTA);

        player.removeEffect(ModEffects.ALL_OUT_EFFORT_GU);
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS));
        assertEquals(24, StrengthService.getCapacity(player));
        strike(helper, player, new BlockPos(4, 1, 6));
        assertEquals(max - 16.0F / 24.0F * max * BodyOverload.RECOIL_RATIO, player.getHealth(), DELTA);
        ComboRound.clear(player);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theStrengthBonusCountsOnlyForAFist(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        assertTrue(AttackDamageService.isFistHand(player), "an empty hand is not a fist");
        assertEquals(base + 5.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        assertFalse(AttackDamageService.isFistHand(player), "a sword counted as a fist");
        AttackDamageService.refresh(player);
        assertEquals(0.0D, AttackDamageService.getStrengthBonus(player), DELTA);
        double sword = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        assertTrue(Math.abs(sword - base - 5.0D) > DELTA, "the strength bonus stayed on with a sword in hand");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EpicFightItems.GLOVE.get()));
        assertFalse(AttackDamageService.isFistHand(player), "a glove counted as a bare fist");
        AttackDamageService.refresh(player);
        assertEquals(0.0D, AttackDamageService.getStrengthBonus(player), DELTA);

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        AttackDamageService.refresh(player);
        assertEquals(base + 5.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aNewBeastMustFitUnderTheNormalCapacity(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        ApertureService.setRank(player, Rank.ONE);
        assertTrue(StrengthService.canAdmit(player, BeastStrength.WHITE_BOAR), "rank one refused its boar");
        assertFalse(StrengthService.canAdmit(player, BeastStrength.BEAR), "rank one admitted a bear");

        TimedEffects.grantBuff(player, (TendedGuItem) ModItems.FLOWER_BOAR_GU_1.get(), ModEffects.FLOWER_BOAR_GU, 0,
                EFFECT_TICKS, Amplification.NONE);
        assertTrue(StrengthService.canAdmit(player, BeastStrength.WHITE_BOAR), "a timed boar took a permanent seat");
        PathStrengthService.grant(player, BeastStrength.WHITE_BOAR);
        assertFalse(StrengthService.canAdmit(player, BeastStrength.BLACK_BOAR), "a full rank one admitted a second boar");

        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            assertEquals(72, StrengthService.getCapacity(player));
            assertFalse(StrengthService.canAdmit(player, BeastStrength.BLACK_BOAR), "the All-Out lift admitted a beast");

            BodyService.wearSkin(player, Skin.IRON_SKIN_3, 0);
            assertTrue(StrengthService.canAdmit(player, BeastStrength.BLACK_BOAR), "iron skin three did not make room");
            assertFalse(StrengthService.canAdmit(player, BeastStrength.BEAR), "24 + 40 passed 60");
            assertTrue(PathStrengthService.grant(player, BeastStrength.BEAR), "a command was held to the admission");
            assertEquals(64, StrengthService.getPermanentPoints(player));
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aNewHumanLayerMustFitUnderTheNormalCapacityAndARefusalPaysNothing(GameTestHelper helper) {
        ServerPlayer player = GameTestGu.awakened(
                GameTestPlayers.survival(helper, "gzr-human-admission-gametest").onGround(), Rank.ONE);
        assertEquals(24, StrengthService.getNormalCapacity(player));
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.TEN_JIN, 2), "the ten jin was refused");
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JIN, 3), "the jin was refused");
        assertEquals(23, StrengthService.getPermanentPoints(player));
        assertNull(check(player, ModItems.JIN_STRENGTH_GU_1.get(), HumanStrengthBehavior.class));
        assertTrue(StrengthService.canAdmit(player, HumanStrength.JIN), "23 + 1 did not fit 24");
        assertHumanCapacity(check(player, ModItems.TENS_JIN_STRENGTH_GU_2.get(), HumanStrengthBehavior.class));

        assertTrue(PathStrengthService.addHumanStrength(player, HumanStrength.JIN, 1), "the last jin was refused");
        assertFalse(StrengthService.canAdmit(player, HumanStrength.JIN), "24 + 1 passed 24");
        assertHumanCapacity(check(player, ModItems.JIN_STRENGTH_GU_1.get(), HumanStrengthBehavior.class));

        ItemStack stack = refined(ModItems.JIN_STRENGTH_GU_1.get(), GU_HUNGER);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        long essence = ApertureEssenceService.getSpendable(player);
        click(helper, player, stack);
        assertEquals(4, PathStrengthService.getHumanStrength(player, HumanStrength.JIN), "the refused use wrote");
        assertEquals(essence, ApertureEssenceService.getSpendable(player), "the refused use spent essence");
        assertEquals(GU_HUNGER, hunger(stack), "the refused use fed on the Gu");
        assertSame(stack, player.getMainHandItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        assertEquals(72, StrengthService.getCapacity(player));
        assertHumanCapacity(check(player, ModItems.JIN_STRENGTH_GU_1.get(), HumanStrengthBehavior.class));
        player.removeEffect(ModEffects.ALL_OUT_EFFORT_GU);

        BodyService.wearSkin(player, Skin.IRON_SKIN_3, 0);
        assertNull(check(player, ModItems.JIN_STRENGTH_GU_1.get(), HumanStrengthBehavior.class));
        assertNull(check(player, ModItems.TENS_JIN_STRENGTH_GU_2.get(), HumanStrengthBehavior.class));
        assertNull(check(player, ModItems.JUN_STRENGTH_GU_3.get(), HumanStrengthBehavior.class));
        assertHumanCapacity(check(player, ModItems.TENS_JUN_STRENGTH_GU_4.get(), HumanStrengthBehavior.class));

        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.TEN_JIN, 9),
                "a command was held to the admission");
        assertEquals(94, StrengthService.getPermanentPoints(player));
        assertEquals(94, StrengthService.getSettledPoints(player), "the layers past the capacity were cut");
        assertEquals(34, overload(player));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHundredJunFitsNoMortalBody(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        ApertureService.setRank(player, Rank.FIVE);
        assertHumanCapacity(check(player, ModItems.TENS_JUN_STRENGTH_GU_4.get(), HumanStrengthBehavior.class));
        assertHumanCapacity(check(player, ModItems.HUNDREDS_JUN_STRENGTH_GU_5.get(), HumanStrengthBehavior.class));

        BodyService.setExtremePhysique(player, ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL);
        BodyService.wearSkin(player, Skin.IRON_SKIN_4, 0);
        BodyService.temperTendon(player, Tendon.STEEL_TENDON_4);
        BodyService.temperBone(player, Bone.IRON_BONE_4);
        assertEquals(844, StrengthService.getNormalCapacity(player), "the largest mortal body is not 844");
        assertNull(check(player, ModItems.TENS_JUN_STRENGTH_GU_4.get(), HumanStrengthBehavior.class));
        assertHumanCapacity(check(player, ModItems.HUNDREDS_JUN_STRENGTH_GU_5.get(), HumanStrengthBehavior.class));
        assertFalse(StrengthService.canAdmit(player, HumanStrength.HUNDRED_JUN), "a mortal admitted a hundred jun");

        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.HUNDRED_JUN, 1),
                "a command was held to the admission");
        assertEquals(3000, StrengthService.getSettledPoints(player));
        assertEquals(3000 - 844, overload(player));
        helper.succeed();
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

    private static void assertHumanCapacity(@Nullable Refusal refusal) {
        assertEquals(HUMAN_CAPACITY, assertNotNull(refusal, () -> "a layer past the capacity was allowed").key());
    }

    private static <M extends GuBehavior> @Nullable Refusal check(ServerPlayer player, Item item, Class<M> type) {
        MortalGuItem gu = (MortalGuItem) item;
        M behavior = assertNotNull(gu.getBehavior(type), () -> item + " lacks its " + type.getSimpleName());
        return behavior.checkEffect(gu, player, new ItemStack(item));
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
        assertEquals(48, StrengthService.getSettledPoints(player));
        assertEquals(0, overload(player));

        assertTrue(StrengthService.canAdmit(player, BeastStrength.TIGER), "the turtle's room refused the tiger");
        PathStrengthService.grant(player, BeastStrength.TIGER);
        assertTrue(StrengthService.canAdmit(player, BeastStrength.GALLOPING_HORSE), "176 refused 48 + 64 + 48");
        PathStrengthService.grant(player, BeastStrength.GALLOPING_HORSE);
        assertEquals(160, StrengthService.getSettledPoints(player));
        assertEquals(0, overload(player), "the turtle's room did not seat tiger and horse");
        assertFalse(StrengthService.canAdmit(player, BeastStrength.BEAR), "a full turtle body admitted a bear");

        PathStrengthService.revoke(player, BeastStrength.STONE_TURTLE);
        assertEquals(48, StrengthService.getNormalCapacity(player), "the turtle's capacity outlived it");
        assertEquals(112, StrengthService.getSettledPoints(player), "the tiger and horse were cut to the capacity");
        assertEquals(64, overload(player));
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
    public static void aReleasedHorseSpeedsTheWalkAndTheSprintWhileItsPhantomShows(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        double walk = player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        PathStrengthService.grant(player, BeastStrength.GALLOPING_HORSE);
        ComboRound.beginStrike(player, true);
        ComboRound.strike(player, 0.0F);
        ComboRound.complete(player, false);
        SprintSpeedService.refresh(player);
        assertEquals(walk * 1.5D, player.getAttributeValue(Attributes.MOVEMENT_SPEED), DELTA);

        player.setSprinting(true);
        SprintSpeedService.refresh(player);
        assertEquals(walk * 2.3D, player.getAttributeValue(Attributes.MOVEMENT_SPEED), DELTA);
        player.setSprinting(false);

        helper.runAfterDelay(ComboRound.SHOWN_TICKS + 1, () -> {
            SprintSpeedService.refresh(player);
            assertEquals(walk, player.getAttributeValue(Attributes.MOVEMENT_SPEED), DELTA);
            ComboRound.clear(player);
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
    public static void eachStrikePaysTheBodyOverloadOnceDownToOneHeart(GameTestHelper helper) {
        ServerPlayer player = fighter(helper, "gzr-overload-gametest");
        ApertureService.awaken(player, 80);
        ApertureService.setRank(player, Rank.FIVE);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        TimedEffects.grantBuff(player, (TendedGuItem) ModItems.FLOWER_BOAR_GU_1.get(), ModEffects.FLOWER_BOAR_GU, 0,
                EFFECT_TICKS, Amplification.NONE);

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            assertEquals(56, StrengthService.getCapacity(player), "the boar moved the capacity");
            assertEquals(64, StrengthService.getSettledPoints(player), "the overload was not brought to bear");
            assertEquals(8, overload(player));

            float max = player.getMaxHealth();
            float recoil = 8.0F / 56.0F * max * BodyOverload.RECOIL_RATIO;
            player.setHealth(max);
            ComboRound.beginStrike(player, false);
            player.swing(InteractionHand.MAIN_HAND);
            assertEquals(max, player.getHealth(), DELTA);

            Pig first = punch(helper, player, new BlockPos(2, 1, 2));
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
            assertEquals(max - recoil, player.getHealth(), DELTA);

            ComboRound.beginStrike(player, false);
            first.hurt(player.damageSources().playerAttack(player), 1.0F);
            assertEquals(max - recoil, player.getHealth(), DELTA);

            strike(helper, player, new BlockPos(2, 1, 6));
            assertEquals(max - 2.0F * recoil, player.getHealth(), DELTA);

            player.setHealth(3.0F);
            strike(helper, player, new BlockPos(6, 1, 6));
            assertEquals(BodyOverload.HEALTH_FLOOR, player.getHealth(), DELTA);
            strike(helper, player, new BlockPos(4, 1, 6));
            assertEquals(BodyOverload.HEALTH_FLOOR, player.getHealth(), DELTA);

            player.setHealth(max);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
            strike(helper, player, new BlockPos(4, 1, 2));
            assertEquals(max, player.getHealth(), DELTA);
            ComboRound.clear(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aPunchWithNoNewStrikePaysAgainOnceTheGateRunsOut(GameTestHelper helper) {
        ServerPlayer player = fighter(helper, "gzr-overload-gate-gametest");
        PathStrengthService.grant(player, BeastStrength.BEAR);
        float max = player.getMaxHealth();
        float recoil = 24.0F / 16.0F * max * BodyOverload.RECOIL_RATIO;
        player.setHealth(max);

        punch(helper, player, new BlockPos(2, 1, 2));
        assertEquals(max - recoil, player.getHealth(), DELTA);
        punch(helper, player, new BlockPos(6, 1, 2));
        assertEquals(max - recoil, player.getHealth(), DELTA);

        helper.runAfterDelay(ComboRound.OVERLOAD_GATE_TICKS, () -> {
            player.setHealth(max);
            punch(helper, player, new BlockPos(2, 1, 6));
            assertEquals(max - recoil, player.getHealth(), DELTA);
            ComboRound.clear(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aPunchAShieldStopsPaysNothingAndLeavesTheStrikeItsRecoil(GameTestHelper helper) {
        ServerPlayer player = fighter(helper, "gzr-overload-shield-gametest");
        PathStrengthService.grant(player, BeastStrength.BEAR);
        Pig guard = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 2));
        guard.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        face(guard, player);
        guard.startUsingItem(InteractionHand.OFF_HAND);

        helper.runAfterDelay(SHIELD_RAISE_TICKS, () -> {
            face(guard, player);
            assertTrue(guard.isBlocking(), "the shield is not raised");
            float max = player.getMaxHealth();
            float recoil = 24.0F / 16.0F * max * BodyOverload.RECOIL_RATIO;
            float guarded = guard.getHealth();
            player.setHealth(max);

            ComboRound.beginStrike(player, false);
            assertFalse(guard.hurt(player.damageSources().playerAttack(player), 1.0F), "the shield let the punch in");
            assertEquals(guarded, guard.getHealth(), DELTA);
            assertEquals(max, player.getHealth(), DELTA);

            punch(helper, player, new BlockPos(4, 1, 6));
            assertEquals(max - recoil, player.getHealth(), DELTA);
            punch(helper, player, new BlockPos(2, 1, 6));
            assertEquals(max - recoil, player.getHealth(), DELTA);
            ComboRound.clear(player);
            helper.succeed();
        });
    }

    private static void face(Pig guard, ServerPlayer player) {
        Vec3 toward = player.position().subtract(guard.position());
        float yaw = (float) (Mth.atan2(toward.z, toward.x) * Mth.RAD_TO_DEG) - 90.0F;
        guard.setYRot(yaw);
        guard.setYHeadRot(yaw);
        guard.setYBodyRot(yaw);
    }

    @GameTest(template = "empty9x9x9")
    public static void aRecoilSmallerThanTheIronSkinsCutStillLandsInFull(GameTestHelper helper) {
        ServerPlayer player = fighter(helper, "gzr-overload-skin-gametest");
        ApertureService.awaken(player, 80);
        ApertureService.setRank(player, Rank.FIVE);
        BodyService.wearSkin(player, Skin.IRON_SKIN_4, 0);
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JUN, 6), "the jun was refused");
        assertEquals(164, StrengthService.getCapacity(player));
        assertEquals(16, overload(player));

        float max = player.getMaxHealth();
        float recoil = 16.0F / 164.0F * max * BodyOverload.RECOIL_RATIO;
        assertEquals(6, Skin.IRON_SKIN_4.getDamageReduction());
        assertTrue(recoil < Skin.IRON_SKIN_4.getDamageReduction(), "the recoil is no smaller than the skin's cut");
        player.setHealth(max);
        player.hurt(player.damageSources().generic(), 5.0F);
        assertEquals(max, player.getHealth(), DELTA);

        player.invulnerableTime = 0;
        strike(helper, player, new BlockPos(2, 1, 2));
        assertEquals(max - recoil, player.getHealth(), DELTA);
        DamageSource source = assertNotNull(player.getLastDamageSource(), () -> "the recoil left no damage source");
        assertTrue(source.is(ModDamageTypes.BODY_OVERLOAD), "the last damage is not the body overload");
        assertTrue(source.is(DamageTypeTags.BYPASSES_EFFECTS), "the body overload stops at effects");
        assertTrue(source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS), "the body overload stops at enchantments");
        ComboRound.clear(player);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theRecoilTakesHealthAndLeavesTheBodyShieldAlone(GameTestHelper helper) {
        ServerPlayer player = fighter(helper, "gzr-overload-shield-layer-gametest");
        PathStrengthService.grant(player, BeastStrength.BEAR);
        ShieldService.raise(player, ShieldType.HEAVENLY_MUGWORT, 16.0F, EFFECT_TICKS);
        float max = player.getMaxHealth();
        float recoil = 24.0F / 16.0F * max * BodyOverload.RECOIL_RATIO;
        player.setHealth(max);
        player.hurt(player.damageSources().generic(), 5.0F);
        assertEquals(max, player.getHealth(), DELTA);
        assertEquals(11.0D, shieldPoints(player), DELTA);

        player.invulnerableTime = 0;
        strike(helper, player, new BlockPos(2, 1, 2));
        assertEquals(max - recoil, player.getHealth(), DELTA);
        assertEquals(11.0D, shieldPoints(player), DELTA);
        assertTrue(ShieldService.isUp(player), "the recoil broke the shield");
        ComboRound.clear(player);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void resistanceAndProtectionDoNotLessenTheRecoil(GameTestHelper helper) {
        ServerPlayer player = fighter(helper, "gzr-overload-resistance-gametest");
        PathStrengthService.grant(player, BeastStrength.BEAR);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_TICKS, 4));
        ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chestplate.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.PROTECTION), 4);
        player.setItemSlot(EquipmentSlot.CHEST, chestplate);
        float max = player.getMaxHealth();
        float recoil = 24.0F / 16.0F * max * BodyOverload.RECOIL_RATIO;
        player.setHealth(max);
        player.hurt(player.damageSources().magic(), 5.0F);
        assertEquals(max, player.getHealth(), DELTA);

        player.invulnerableTime = 0;
        strike(helper, player, new BlockPos(2, 1, 2));
        assertEquals(max - recoil, player.getHealth(), DELTA);
        ComboRound.clear(player);
        helper.succeed();
    }

    private static float shieldPoints(ServerPlayer player) {
        ShieldLayer layer = ShieldService.find(player, ShieldType.HEAVENLY_MUGWORT);
        return layer == null ? 0.0F : layer.points();
    }

    @GameTest(template = "empty9x9x9")
    public static void strengthLeftPastTheCapacityStaysAndRecoilsTheSame(GameTestHelper helper) {
        ServerPlayer player = fighter(helper, "gzr-overload-left-gametest");
        ApertureService.awaken(player, 80);
        ApertureService.setRank(player, Rank.ONE);
        BodyService.wearSkin(player, Skin.IRON_SKIN_4, 0);
        assertTrue(StrengthService.canAdmit(player, BeastStrength.BEAR), "iron skin four refused the bear");
        PathStrengthService.grant(player, BeastStrength.BEAR);
        player.setHealth(player.getMaxHealth());
        strike(helper, player, new BlockPos(2, 1, 2));
        assertEquals(player.getMaxHealth(), player.getHealth(), DELTA);

        BodyService.shedSkin(player);
        assertEquals(24, StrengthService.getCapacity(player));
        assertEquals(40, StrengthService.getSettledPoints(player), "the strength left with the skin");
        float shed = player.getMaxHealth();
        player.setHealth(shed);
        strike(helper, player, new BlockPos(6, 1, 2));
        assertEquals(shed - 16.0F / 24.0F * shed * BodyOverload.RECOIL_RATIO, player.getHealth(), DELTA);

        BodyService.setExtremePhysique(player, ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL);
        assertEquals(96, StrengthService.getCapacity(player));
        float martial = player.getMaxHealth();
        player.setHealth(martial);
        strike(helper, player, new BlockPos(2, 1, 6));
        assertEquals(martial, player.getHealth(), DELTA);

        BodyService.setExtremePhysique(player, ExtremePhysique.NONE);
        assertEquals(24, StrengthService.getCapacity(player));
        assertEquals(40, StrengthService.getSettledPoints(player), "the strength left with the physique");
        float plain = player.getMaxHealth();
        player.setHealth(plain);
        strike(helper, player, new BlockPos(6, 1, 6));
        assertEquals(plain - 16.0F / 24.0F * plain * BodyOverload.RECOIL_RATIO, player.getHealth(), DELTA);

        assertFalse(StrengthService.canAdmit(player, BeastStrength.TIGER), "40 + 64 passed 24");
        assertTrue(PathStrengthService.grant(player, BeastStrength.TIGER), "a command was held to the admission");
        assertEquals(104, StrengthService.getSettledPoints(player));
        assertEquals(80, overload(player));
        player.setHealth(plain);
        strike(helper, player, new BlockPos(4, 1, 6));
        assertEquals(BodyOverload.HEALTH_FLOOR, player.getHealth(), DELTA);
        ComboRound.clear(player);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void thePanelRowListsTheTotalBothCapacitiesAndThePointsOver(GameTestHelper helper) {
        ServerPlayer bare = mortal(helper);
        assertTrue(bodyRows(bare).stream().noneMatch(InfoModel.CapacityRow.class::isInstance),
                "a body with no strength lists a strength row");

        ServerPlayer player = awakenedMortal(helper);
        ApertureService.setRank(player, Rank.FIVE);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        assertTrue(bodyRows(player).contains(new InfoModel.CapacityRow(40, 56, 56, 0)), () -> "" + bodyRows(player));

        TimedEffects.grantBuff(player, (TendedGuItem) ModItems.FLOWER_BOAR_GU_1.get(), ModEffects.FLOWER_BOAR_GU, 0,
                EFFECT_TICKS, Amplification.NONE);
        assertTrue(bodyRows(player).contains(new InfoModel.CapacityRow(64, 56, 56, 8)), () -> "" + bodyRows(player));

        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS));
        assertTrue(bodyRows(player).contains(new InfoModel.CapacityRow(64, 84, 56, 0)), () -> "" + bodyRows(player));
        helper.succeed();
    }

    private static List<InfoModel.Entry> bodyRows(ServerPlayer player) {
        return InfoModel.buildBodyRows(player).stream().map(InfoModel.Row::entry).toList();
    }

    private static int overload(ServerPlayer player) {
        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        return snapshot.overloadPoints(StrengthService.getSettledPoints(player, snapshot));
    }

    private static void strike(GameTestHelper helper, ServerPlayer player, BlockPos at) {
        ComboRound.beginStrike(player, false);
        punch(helper, player, at);
    }

    private static Pig punch(GameTestHelper helper, ServerPlayer player, BlockPos at) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, at);
        assertTrue(pig.hurt(player.damageSources().playerAttack(player), 1.0F), "the punch did no damage");
        return pig;
    }

    private static ServerPlayer fighter(GameTestHelper helper, String name) {
        return GameTestPlayers.survival(helper, name).pastSpawnInvulnerability().onGround();
    }

    private static ServerPlayer mortal(GameTestHelper helper) {
        return GameTestApertures.mortal(helper);
    }
}
