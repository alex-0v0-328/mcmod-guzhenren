package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.strength.AreaImpact;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.ComboRound;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.registry.entries.EpicFightItems;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import static net.alex.guzhenren.gametest.GameTestGu.inSlot;
import static net.alex.guzhenren.gametest.GameTestPlayers.ground;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 活玩家的范围冲击[Area Impact]锥形与溢出结算。
 * <ul>
 *   <li>兽力场景：场景西缘朝东，身体持熊与两猪共 88 点（全额发挥），空手拳伤 {@link #PUNCH}，锥长约 6.75 格；加满者另开
 *       全力以赴与力气蛊[Strength Qi Gu]，几率 50%。</li>
 *   <li>溢出结算：目标由近及远吃剩余伤害，每命中一个扣掉其挨打前生命（不看护甲），扣完或满 8 个为止，免疫者不占名额
 *       也不扣剩余；没有主目标时最近者吃整拳，收尾招打中主目标后只剩 {@link ComboRound#getSpill}。</li>
 *   <li>过载反噬：五转满血（最大生命 100、承受力 56）持同样 88 点，锥形冲击打中时结算一次（{@link #recoil}），这一招近战
 *       已结算过则不重复，锥形没打中则不扣。</li>
 *   <li>Epic Fight 触发仿其起招时与拳击命中窗口关闭时投递，只有连招轮收尾招放；{@link #nextRoll} 播种定下次掷骰中否，触
 *       发测试投递前都调。</li>
 *   <li>放大场景：五转玩家在场景中央朝东，人力 1 斤，空手拳伤 {@link #LIGHT_PUNCH}，锥长 3.4 格；苦力运行中的人力钧力流
 *       必定触发，锥长 5.1 格、半角 135°，兽力流与未开苦力不放大。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AreaImpactGameTests {

    private AreaImpactGameTests() {}

    private static final Vec3 WEST = new Vec3(1.5D, 1.0D, 4.5D);
    private static final Vec3 NEAR = new Vec3(3.5D, 1.0D, 4.5D);
    private static final Vec3 FAR = new Vec3(5.5D, 1.0D, 4.5D);
    private static final Vec3 THIRD = new Vec3(7.5D, 1.0D, 4.5D);
    private static final Vec3 BEHIND = new Vec3(0.5D, 1.0D, 4.5D);
    private static final Vec3 BEYOND = new Vec3(8.5D, 1.0D, 4.5D);
    private static final Vec3 ASIDE = new Vec3(2.0D, 1.0D, 8.0D);
    private static final Vec3 EAST = new Vec3(1.0D, 0.0D, 0.0D);
    private static final Vec3 CENTER = new Vec3(4.5D, 1.0D, 4.5D);
    private static final Vec3 CENTER_AHEAD = new Vec3(6.5D, 1.0D, 4.5D);
    private static final Vec3 CENTER_FAR = new Vec3(8.5D, 1.0D, 4.5D);
    private static final Vec3 CENTER_SIDE = new Vec3(4.5D, 1.0D, 7.5D);
    private static final Vec3 CENTER_BACK = new Vec3(1.5D, 1.0D, 4.5D);
    private static final int STRONG_POINTS = 88;
    private static final int CAPACITY = 56;
    private static final float PUNCH = 12.0F;
    private static final float LIGHT_PUNCH = 1.125F;
    private static final float PIG_HEALTH = 10.0F;
    private static final int EFFECT_TICKS = 600;
    private static final int RANK_FIVE_TIER = 4;
    private static final int BAG_SLOT = 9;
    private static final int HUNGER = 98;
    private static final double DELTA = 0.001D;

    //region Cone [锥形]
    @GameTest(template = "empty9x9x9")
    public static void aFistStrikeSpillsDownTheConeAhead(GameTestHelper helper) {
        ServerPlayer player = charged(helper);
        Pig near = idlePig(helper, NEAR, 4.0F);
        Pig far = idlePig(helper, FAR);
        Pig third = idlePig(helper, THIRD);
        List<Pig> missed = List.of(idlePig(helper, BEHIND), idlePig(helper, ASIDE));

        strike(fighting(player), Animations.FIST_AUTO3, false, true);

        assertEquals(0.0D, near.getHealth(), DELTA);
        assertEquals(PIG_HEALTH - (PUNCH - 4.0F), far.getHealth(), DELTA);
        assertTrue(far.getDeltaMovement().x > 0.0D, () -> "the impact shoved " + far.getDeltaMovement());
        assertEquals(PIG_HEALTH, third.getHealth(), DELTA);
        for (Pig pig : missed) {
            assertEquals(PIG_HEALTH, pig.getHealth(), DELTA);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aMissedRollReleasesNothing(GameTestHelper helper) {
        ServerPlayer player = charged(helper);
        Pig pig = idlePig(helper, NEAR);

        strike(fighting(player), Animations.FIST_AUTO3, false, false);

        assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void onlyAClosedFistFinisherReleases(GameTestHelper helper) {
        ServerPlayer player = charged(helper);
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        assertTrue(patch != null, "Epic Fight player patch missing");
        Pig pig = idlePig(helper, NEAR);

        patch.toVanillaMode(false);
        strike(patch, Animations.FIST_AUTO3, false, true);
        assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);

        patch.toEpicFightMode(false);
        strike(patch, Animations.FIST_AUTO1, false, true);
        assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);
        strike(patch, Animations.FIST_AUTO2, false, true);
        assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);
        strike(patch, Animations.FIST_AUTO3, true, true);
        assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);
        strike(patch, Animations.RELENTLESS_COMBO, false, true);
        assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        strike(patch, Animations.FIST_AUTO3, false, true);
        assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EpicFightItems.GLOVE.get()));
        strike(patch, Animations.FIST_AUTO3, false, true);
        assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.IRON_SWORD));
        assertTrue(patch.isMirrorMode(), "a bare main hand beside an offhand sword is not mirror mode");
        strike(patch, Animations.FIST_AUTO3, false, true);
        assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);

        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        strike(patch, Animations.FIST_DASH, false, true);
        assertTrue(pig.getHealth() < pig.getMaxHealth(), "a fist dash attack released nothing");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void withoutEitherEffectNothingIsReleased(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        Pig pig = idlePig(helper, NEAR);

        strike(fighting(player), Animations.FIST_AUTO3, false, true);

        assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theChanceFollowsTheRunningEffects(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        assertEquals(0.0D, AreaImpact.getChance(player));
        player.addEffect(ModEffects.instance(ModEffects.STRENGTH_QI_GU, EFFECT_TICKS, 0));
        assertEquals(AreaImpact.STRENGTH_QI_CHANCE, AreaImpact.getChance(player));
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        assertEquals(AreaImpact.BOTH_CHANCE, AreaImpact.getChance(player));
        player.removeEffect(ModEffects.STRENGTH_QI_GU);
        assertEquals(AreaImpact.ALL_OUT_CHANCE, AreaImpact.getChance(player));

        ServerPlayer branchless = GameTestPlayers.survival(helper, "gzr-area-impact-branchless").at(BEHIND);
        branchless.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        assertEquals(0.0D, AreaImpact.getChance(branchless));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theImpactSparesTriedTargetsPetsAndArmorStands(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        Pig tried = idlePig(helper, NEAR);
        Wolf wolf = helper.spawn(EntityType.WOLF, NEAR);
        wolf.setNoAi(true);
        wolf.tame(player);
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, NEAR);
        Pig open = idlePig(helper, FAR);

        List<LivingEntity> landed = AreaImpact.release(player, EAST, List.of(tried));

        assertEquals(List.of(open), landed);
        assertEquals(tried.getMaxHealth(), tried.getHealth(), DELTA);
        assertEquals(wolf.getMaxHealth(), wolf.getHealth(), DELTA);
        assertTrue(stand.isAlive(), "the impact broke an armor stand");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theFistsTriedTargetsAreSpared(GameTestHelper helper) {
        ServerPlayer player = charged(helper);
        ServerPlayerPatch patch = fighting(player);
        Pig tried = idlePig(helper, NEAR);
        Pig open = idlePig(helper, FAR);
        patch.getCurrentlyAttackTriedEntities().add(tried);
        assertTrue(patch.getCurrentlyAttackTriedEntities().contains(tried), "the tried list did not keep the pig");

        strike(patch, Animations.FIST_AUTO3, false, true);

        assertEquals(tried.getMaxHealth(), tried.getHealth(), DELTA);
        assertTrue(open.getHealth() < open.getMaxHealth(), "the untried pig was not hit");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theConeStopsAtItsRange(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        assertEquals(STRONG_POINTS, AreaImpact.getPoints(player));
        Pig inside = idlePig(helper, THIRD);
        Pig outside = idlePig(helper, BEYOND);

        assertEquals(List.of(inside), AreaImpact.release(player, EAST, List.of()));
        assertEquals(0.0D, inside.getHealth(), DELTA);
        assertEquals(PIG_HEALTH, outside.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aWallStopsTheImpact(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(3, y, 4), Blocks.STONE);
        Pig pig = idlePig(helper, FAR);

        assertTrue(AreaImpact.release(player, EAST, List.of()).isEmpty(), "the impact went through a wall");
        assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRefusedImpactShovesNothing(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        Pig pig = idlePig(helper, NEAR);
        pig.setInvulnerable(true);

        assertTrue(AreaImpact.release(player, EAST, List.of()).isEmpty(), "an invulnerable body counted as landed");
        assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);
        assertTrue(pig.getDeltaMovement().lengthSqr() < 1.0E-6D, "a refused impact still shoved the body");
        helper.succeed();
    }
    //endregion

    //region Spill [溢出]
    @GameTest(template = "empty9x9x9")
    public static void withNoMainTargetTheNearestTakesTheWholePunch(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        assertEquals(PUNCH, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);
        Pig near = idlePig(helper, NEAR, 40.0F);
        Pig far = idlePig(helper, FAR);

        assertEquals(List.of(near), AreaImpact.release(player, EAST, List.of()));

        assertEquals(40.0F - PUNCH, near.getHealth(), DELTA);
        assertEquals(PIG_HEALTH, far.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theSpillRunsOutOnTheHealthItMeets(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        Pig near = idlePig(helper, NEAR, 4.0F);
        Pig far = idlePig(helper, FAR);
        Pig third = idlePig(helper, THIRD);

        assertEquals(List.of(near, far), AreaImpact.release(player, EAST, List.of()));

        assertEquals(0.0D, near.getHealth(), DELTA);
        assertEquals(PIG_HEALTH - (PUNCH - 4.0F), far.getHealth(), DELTA);
        assertEquals(PIG_HEALTH, third.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theSpillLosesHealthBeforeTheHitWhateverTheArmor(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        Pig armored = idlePig(helper, NEAR);
        armored.getAttribute(Attributes.ARMOR).setBaseValue(20.0D);
        Pig far = idlePig(helper, FAR);

        assertEquals(List.of(armored, far), AreaImpact.release(player, EAST, List.of()));

        assertTrue(armored.isAlive() && armored.getHealth() < PIG_HEALTH,
                () -> "the armored pig is at " + armored.getHealth());
        assertEquals(PIG_HEALTH - (PUNCH - PIG_HEALTH), far.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void refusingBodiesTakeNoSlotAndNoDamage(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        for (int index = 0; index < AreaImpact.MAX_TARGETS; index++) idlePig(helper, NEAR).setInvulnerable(true);
        Pig open = idlePig(helper, FAR, 40.0F);

        assertEquals(List.of(open), AreaImpact.release(player, EAST, List.of()));
        assertEquals(40.0F - PUNCH, open.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theImpactStopsAtEightTargets(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        List<Pig> pigs = new ArrayList<>();
        for (int index = 0; index < AreaImpact.MAX_TARGETS + 2; index++) pigs.add(idlePig(helper, NEAR, 1.0F));

        assertEquals(AreaImpact.MAX_TARGETS, AreaImpact.release(player, EAST, List.of()).size());
        assertEquals(2, pigs.stream().filter(pig -> pig.getHealth() == 1.0F).count());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aLandedFinisherLeavesTheConeItsSpill(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        Pig main = idlePig(helper, NEAR, 4.0F);
        Pig open = idlePig(helper, FAR);

        ComboRound.beginStrike(player, true);
        nextRoll(player.getRandom(), ComboRound.CHANCE, false);
        assertTrue(main.hurt(player.damageSources().playerAttack(player), PUNCH), "the finisher did no damage");
        assertEquals(PUNCH - 4.0F, ComboRound.getSpill(player, PUNCH), DELTA);
        List<LivingEntity> landed = AreaImpact.release(player, EAST, List.of(main));

        assertEquals(List.of(open), landed);
        assertEquals(PIG_HEALTH - (PUNCH - 4.0F), open.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aFinisherSpentOnItsTargetHurtsNoOneElse(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        Pig main = idlePig(helper, NEAR, 20.0F);
        Pig open = idlePig(helper, FAR);

        ComboRound.beginStrike(player, true);
        nextRoll(player.getRandom(), ComboRound.CHANCE, false);
        assertTrue(main.hurt(player.damageSources().playerAttack(player), PUNCH), "the finisher did no damage");
        assertEquals(PUNCH - 20.0F, ComboRound.getSpill(player, PUNCH), DELTA);

        assertTrue(AreaImpact.release(player, EAST, List.of(main)).isEmpty(), "a spent finisher still spilled");
        assertEquals(PIG_HEALTH, open.getHealth(), DELTA);
        assertTrue(open.getDeltaMovement().lengthSqr() < 1.0E-6D, "a spent finisher still shoved the body");
        helper.succeed();
    }
    //endregion

    //region Overload [过载反噬]
    @GameTest(template = "empty9x9x9")
    public static void aWhiffedFinisherWhoseConeLandsRecoilsOnce(GameTestHelper helper) {
        ServerPlayer player = overloaded(helper, "gzr-area-impact-cone-recoil");
        Pig pig = idlePig(helper, NEAR, 40.0F);

        ComboRound.beginStrike(player, true);
        assertEquals(List.of(pig), AreaImpact.release(player, EAST, List.of()));

        assertEquals(40.0F - PUNCH, pig.getHealth(), DELTA);
        assertEquals(player.getMaxHealth() - recoil(player), player.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aConeBehindALandedFinisherRecoilsNoSecondTime(GameTestHelper helper) {
        ServerPlayer player = overloaded(helper, "gzr-area-impact-melee-recoil");
        Pig main = idlePig(helper, NEAR, 4.0F);
        Pig open = idlePig(helper, FAR);
        float left = player.getMaxHealth() - recoil(player);

        ComboRound.beginStrike(player, true);
        nextRoll(player.getRandom(), ComboRound.CHANCE, false);
        assertTrue(main.hurt(player.damageSources().playerAttack(player), PUNCH), "the finisher did no damage");
        assertEquals(left, player.getHealth(), DELTA);
        assertEquals(List.of(open), AreaImpact.release(player, EAST, List.of(main)));

        assertEquals(PIG_HEALTH - (PUNCH - 4.0F), open.getHealth(), DELTA);
        assertEquals(left, player.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aConeThatLandsOnNothingCostsNoHealth(GameTestHelper helper) {
        ServerPlayer player = overloaded(helper, "gzr-area-impact-no-recoil");
        Pig behind = idlePig(helper, BEHIND);

        ComboRound.beginStrike(player, true);
        assertTrue(AreaImpact.release(player, EAST, List.of()).isEmpty(), "the cone landed behind the fist");

        assertEquals(PIG_HEALTH, behind.getHealth(), DELTA);
        assertEquals(player.getMaxHealth(), player.getHealth(), DELTA);
        helper.succeed();
    }
    //endregion

    //region Widened [放大]
    @GameTest(template = "empty9x9x9")
    public static void hardshipWidensTheHumanBranchsCone(GameTestHelper helper) {
        ServerPlayer player = hardship(light(helper, "gzr-area-impact-widened"));
        assertTrue(AreaImpact.isWidened(player), "Hardship did not widen the human branch");
        assertEquals(LIGHT_PUNCH, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);
        Pig side = idlePig(helper, CENTER_SIDE, 0.5F);
        Pig far = idlePig(helper, CENTER_FAR);
        Pig back = idlePig(helper, CENTER_BACK);

        assertEquals(List.of(side, far), AreaImpact.release(player, EAST, List.of()));

        assertEquals(0.0D, side.getHealth(), DELTA);
        assertEquals(PIG_HEALTH - (LIGHT_PUNCH - 0.5F), far.getHealth(), DELTA);
        assertEquals(PIG_HEALTH, back.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void hardshipAlwaysReleasesForTheHumanBranch(GameTestHelper helper) {
        ServerPlayer player = hardship(light(helper, "gzr-area-impact-always"));
        assertEquals(0.0D, AreaImpact.getChance(player));
        nextRoll(player.getRandom(), AreaImpact.BOTH_CHANCE, false);
        assertTrue(AreaImpact.rolls(player, player.getRandom()), "a missed roll kept the widened impact in");
        Pig pig = idlePig(helper, CENTER_AHEAD);

        strike(fighting(player), Animations.FIST_AUTO3, false, false);

        assertEquals(PIG_HEALTH - LIGHT_PUNCH, pig.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void withoutHardshipTheHumanBranchKeepsItsCone(GameTestHelper helper) {
        ServerPlayer player = light(helper, "gzr-area-impact-narrow");
        assertFalse(AreaImpact.isWidened(player), "the human branch widened without Hardship");
        nextRoll(player.getRandom(), AreaImpact.BOTH_CHANCE, true);
        assertFalse(AreaImpact.rolls(player, player.getRandom()), "the human branch released with no chance");
        Pig side = idlePig(helper, CENTER_SIDE);
        Pig far = idlePig(helper, CENTER_FAR);
        Pig ahead = idlePig(helper, CENTER_AHEAD);

        assertEquals(List.of(ahead), AreaImpact.release(player, EAST, List.of()));

        assertEquals(PIG_HEALTH, side.getHealth(), DELTA);
        assertEquals(PIG_HEALTH, far.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void hardshipWidensNothingForTheBeastBranch(GameTestHelper helper) {
        ServerPlayer player = centered(helper, "gzr-area-impact-beast-hardship");
        assertTrue(PathStrengthService.grant(player, BeastStrength.WHITE_BOAR), "the white boar was refused");
        hardship(player);
        assertFalse(AreaImpact.isWidened(player), "Hardship widened the beast branch");
        nextRoll(player.getRandom(), AreaImpact.BOTH_CHANCE, true);
        assertFalse(AreaImpact.rolls(player, player.getRandom()), "Hardship released for the beast branch");
        Pig side = idlePig(helper, CENTER_SIDE);
        Pig far = idlePig(helper, CENTER_FAR);

        assertEquals(List.of(far), AreaImpact.release(player, EAST, List.of()));

        assertEquals(PIG_HEALTH, side.getHealth(), DELTA);
        helper.succeed();
    }
    //endregion

    static void nextRoll(RandomSource random, float chance, boolean lands) {
        for (long seed = 0L; ; seed++) {
            random.setSeed(seed);
            if ((random.nextFloat() < chance) == lands) {
                random.setSeed(seed);
                return;
            }
        }
    }

    private static void strike(ServerPlayerPatch patch,
                               AnimationManager.AnimationAccessor<? extends AttackAnimation> animation,
                               boolean terminated, boolean lands) {
        GameTestEpicFight.actionStarted(patch, animation);
        nextRoll(patch.getOriginal().getRandom(), AreaImpact.BOTH_CHANCE, lands);
        GameTestEpicFight.attackPhaseEnded(patch, animation, terminated);
    }

    private static ServerPlayer strong(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-area-impact-gametest").pastSpawnInvulnerability()
                .at(WEST);
        player.setYRot(-90.0F);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        PathStrengthService.grant(player, BeastStrength.WHITE_BOAR);
        PathStrengthService.grant(player, BeastStrength.BLACK_BOAR);
        return player;
    }

    private static ServerPlayer charged(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        player.addEffect(ModEffects.instance(ModEffects.STRENGTH_QI_GU, EFFECT_TICKS, 0));
        assertEquals(AreaImpact.BOTH_CHANCE, AreaImpact.getChance(player));
        return player;
    }

    private static ServerPlayer overloaded(GameTestHelper helper, String name) {
        ground(helper);
        ServerPlayer player = GameTestGu.awakened(GameTestPlayers.survival(helper, name).pastSpawnInvulnerability()
                .at(WEST), Rank.FIVE);
        player.setYRot(-90.0F);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        PathStrengthService.grant(player, BeastStrength.WHITE_BOAR);
        PathStrengthService.grant(player, BeastStrength.BLACK_BOAR);
        player.setHealth(player.getMaxHealth());
        assertEquals(CAPACITY, StrengthService.getCapacity(player));
        assertEquals(STRONG_POINTS, AreaImpact.getPoints(player));
        return player;
    }

    private static float recoil(ServerPlayer player) {
        return (float) (STRONG_POINTS - CAPACITY) / CAPACITY * player.getMaxHealth() * 0.5F;
    }

    private static ServerPlayer centered(GameTestHelper helper, String name) {
        ground(helper);
        ServerPlayer player = GameTestGu.awakened(GameTestPlayers.survival(helper, name).pastSpawnInvulnerability()
                .at(CENTER), Rank.FIVE);
        player.setYRot(-90.0F);
        return player;
    }

    private static ServerPlayer light(GameTestHelper helper, String name) {
        ServerPlayer player = centered(helper, name);
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JIN, 1), "the jin was refused");
        assertEquals(1, AreaImpact.getPoints(player));
        return player;
    }

    private static ServerPlayer hardship(ServerPlayer player) {
        inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        player.setHealth(player.getMaxHealth() * 0.7F);
        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "Hardship Strength did not start");
        return player;
    }

    private static ServerPlayerPatch fighting(ServerPlayer player) {
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        assertTrue(patch != null, "Epic Fight player patch missing");
        patch.toEpicFightMode(false);
        return patch;
    }

    private static Pig idlePig(GameTestHelper helper, Vec3 at) {
        Pig pig = helper.spawn(EntityType.PIG, at);
        pig.setNoAi(true);
        pig.setNoGravity(true);
        pig.setDeltaMovement(Vec3.ZERO);
        return pig;
    }

    private static Pig idlePig(GameTestHelper helper, Vec3 at, float health) {
        Pig pig = idlePig(helper, at);
        pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(health, PIG_HEALTH));
        pig.setHealth(health);
        return pig;
    }
}
