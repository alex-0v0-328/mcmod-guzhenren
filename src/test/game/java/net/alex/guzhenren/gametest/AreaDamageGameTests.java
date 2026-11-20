package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.strength.AreaDamage;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.ComboRound;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
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
 * 活玩家的范围伤害[Area Damage]：人力钧力流暴击的身前锥形溢出。
 * <ul>
 *   <li>出手场景：场景西缘朝东，人力 88 斤全额发挥 88 点，空手拳伤 {@link #PUNCH}，锥长约 6.75 格；暴击率 5%，收尾招
 *       掷骰前用 {@link ComboRound#rollsCrit(ServerPlayer, float)} 定下中否（{@link #LAND}、{@link #MISS}）。</li>
 *   <li>只有人力流收尾招暴击才出锥形，挥空也出；兽力流收尾招暴击只出兽影、不出锥形；采矿模式、持剑、拳套、副手持剑都
 *       不出。</li>
 *   <li>溢出结算：目标由近及远吃剩余伤害，每命中一个扣掉其挨打前生命（不看护甲），扣完或满 8 个为止，免疫者不占名额
 *       也不扣剩余；没有主目标时最近者吃整拳，收尾招打中主目标后只剩 {@link ComboRound#getSpill}。</li>
 *   <li>过载反噬：五转满血（承受力 56）持同样 88 点，锥形打中时结算一次（{@link #recoil}），这一招近战已结算过则不重
 *       复，锥形没打中则不扣。</li>
 *   <li>放大：五转人力 1 斤在场景中央朝东，空手拳伤 {@link #LIGHT_PUNCH}，锥长 3.4 格；苦力 5 档（生命 10%）时半径
 *       ×3（10.2 格）、半角 72.5°，57° 侧前方的猪被扫到，正侧方与身后不扫；兽力流与未开苦力不放大。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AreaDamageGameTests {

    private AreaDamageGameTests() {}

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
    private static final Vec3 CENTER_FLANK = new Vec3(6.5D, 1.0D, 7.4D);
    private static final Vec3 CENTER_SIDE = new Vec3(4.5D, 1.0D, 7.5D);
    private static final Vec3 CENTER_BACK = new Vec3(1.5D, 1.0D, 4.5D);
    private static final float LAND = 0.0F;
    private static final float MISS = 0.99F;
    private static final int STRONG_POINTS = 88;
    private static final int CAPACITY = 56;
    private static final float PUNCH = 12.0F;
    private static final float LIGHT_PUNCH = 1.125F;
    private static final float PIG_HEALTH = 10.0F;
    private static final float TIER_FIVE_HEALTH = 0.1F;
    private static final int BAG_SLOT = 9;
    private static final int HUNGER = 98;
    private static final double DELTA = 0.001D;

    //region Trigger [触发]
    @GameTest(template = "empty9x9x9")
    public static void aHumanCritSpillsDownTheConeAhead(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        Pig near = idlePig(helper, NEAR, 4.0F);
        Pig far = idlePig(helper, FAR);
        Pig third = idlePig(helper, THIRD);
        List<Pig> missed = List.of(idlePig(helper, BEHIND), idlePig(helper, ASIDE));
        ServerPlayerPatch patch = fighting(player);

        GameTestEpicFight.actionStarted(patch, Animations.FIST_AUTO3);
        assertTrue(ComboRound.rollsCrit(player, LAND), "a 0 roll missed a 5% crit");
        assertTrue(ComboRound.rollsCrit(player, MISS), "the round rolled its crit a second time");
        assertTrue(ComboRound.rollsArea(player), "a human crit did not count as an area damage");
        GameTestEpicFight.attackPhaseEnded(patch, Animations.FIST_AUTO3, false);

        assertEquals(0.0D, near.getHealth(), DELTA);
        assertEquals(PIG_HEALTH - (PUNCH - 4.0F), far.getHealth(), DELTA);
        assertTrue(far.getDeltaMovement().x > 0.0D, () -> "the area damage shoved " + far.getDeltaMovement());
        assertEquals(PIG_HEALTH, third.getHealth(), DELTA);
        for (Pig pig : missed) {
            assertEquals(PIG_HEALTH, pig.getHealth(), DELTA);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aMissedCritReleasesNothing(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        Pig pig = idlePig(helper, NEAR);

        strike(fighting(player), Animations.FIST_AUTO3, false, false);

        assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void onlyAClosedFistFinisherReleases(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
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
        assertTrue(pig.getHealth() < pig.getMaxHealth(), "a fist dash crit released nothing");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aBeastFinisherOpensNoConeEvenOnACrit(GameTestHelper helper) {
        ServerPlayer player = beasts(helper);
        Pig pig = idlePig(helper, NEAR);
        ServerPlayerPatch patch = fighting(player);

        GameTestEpicFight.actionStarted(patch, Animations.FIST_AUTO3);
        assertTrue(ComboRound.rollsCrit(player, LAND), "the beast finisher missed a sure roll");
        assertFalse(ComboRound.rollsArea(player), "a beast crit counted as an area damage");
        GameTestEpicFight.attackPhaseEnded(patch, Animations.FIST_AUTO3, false);

        assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);
        assertTrue(pig.getDeltaMovement().lengthSqr() < 1.0E-6D, "a beast finisher shoved the body");
        helper.succeed();
    }
    //endregion

    //region Cone [锥形]
    @GameTest(template = "empty9x9x9")
    public static void theConeSparesTriedTargetsPetsAndArmorStands(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        Pig tried = idlePig(helper, NEAR);
        Wolf wolf = helper.spawn(EntityType.WOLF, NEAR);
        wolf.setNoAi(true);
        wolf.tame(player);
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, NEAR);
        Pig open = idlePig(helper, FAR);

        List<LivingEntity> landed = AreaDamage.release(player, EAST, List.of(tried));

        assertEquals(List.of(open), landed);
        assertEquals(tried.getMaxHealth(), tried.getHealth(), DELTA);
        assertEquals(wolf.getMaxHealth(), wolf.getHealth(), DELTA);
        assertTrue(stand.isAlive(), "the area damage broke an armor stand");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theFistsTriedTargetsAreSpared(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
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
        assertEquals(STRONG_POINTS, AreaDamage.getPoints(player));
        assertEquals(0, AreaDamage.getWidenTier(player), "a body off Hardship widened its cone");
        Pig inside = idlePig(helper, THIRD);
        Pig outside = idlePig(helper, BEYOND);

        assertEquals(List.of(inside), AreaDamage.release(player, EAST, List.of()));
        assertEquals(0.0D, inside.getHealth(), DELTA);
        assertEquals(PIG_HEALTH, outside.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aWallStopsTheCone(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(3, y, 4), Blocks.STONE);
        Pig pig = idlePig(helper, FAR);

        assertTrue(AreaDamage.release(player, EAST, List.of()).isEmpty(), "the area damage went through a wall");
        assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRefusedHitShovesNothing(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        Pig pig = idlePig(helper, NEAR);
        pig.setInvulnerable(true);

        assertTrue(AreaDamage.release(player, EAST, List.of()).isEmpty(), "an invulnerable body counted as landed");
        assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);
        assertTrue(pig.getDeltaMovement().lengthSqr() < 1.0E-6D, "a refused hit still shoved the body");
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

        assertEquals(List.of(near), AreaDamage.release(player, EAST, List.of()));

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

        assertEquals(List.of(near, far), AreaDamage.release(player, EAST, List.of()));

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

        assertEquals(List.of(armored, far), AreaDamage.release(player, EAST, List.of()));

        assertTrue(armored.isAlive() && armored.getHealth() < PIG_HEALTH,
                () -> "the armored pig is at " + armored.getHealth());
        assertEquals(PIG_HEALTH - (PUNCH - PIG_HEALTH), far.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void refusingBodiesTakeNoSlotAndNoDamage(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        for (int index = 0; index < AreaDamage.MAX_TARGETS; index++) idlePig(helper, NEAR).setInvulnerable(true);
        Pig open = idlePig(helper, FAR, 40.0F);

        assertEquals(List.of(open), AreaDamage.release(player, EAST, List.of()));
        assertEquals(40.0F - PUNCH, open.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theConeStopsAtEightTargets(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        List<Pig> pigs = new ArrayList<>();
        for (int index = 0; index < AreaDamage.MAX_TARGETS + 2; index++) pigs.add(idlePig(helper, NEAR, 1.0F));

        assertEquals(AreaDamage.MAX_TARGETS, AreaDamage.release(player, EAST, List.of()).size());
        assertEquals(2, pigs.stream().filter(pig -> pig.getHealth() == 1.0F).count());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aLandedFinisherLeavesTheConeItsSpill(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        Pig main = idlePig(helper, NEAR, 4.0F);
        Pig open = idlePig(helper, FAR);

        ComboRound.beginStrike(player, true);
        assertFalse(ComboRound.rollsCrit(player, MISS), "a 99% roll landed a 5% crit");
        assertTrue(Punches.land(player, main, PUNCH), "the finisher did no damage");
        assertEquals(PUNCH - 4.0F, ComboRound.getSpill(player, PUNCH), DELTA);
        List<LivingEntity> landed = AreaDamage.release(player, EAST, List.of(main));

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
        assertFalse(ComboRound.rollsCrit(player, MISS), "a 99% roll landed a 5% crit");
        assertTrue(Punches.land(player, main, PUNCH), "the finisher did no damage");
        assertEquals(PUNCH - 20.0F, ComboRound.getSpill(player, PUNCH), DELTA);

        assertTrue(AreaDamage.release(player, EAST, List.of(main)).isEmpty(), "a spent finisher still spilled");
        assertEquals(PIG_HEALTH, open.getHealth(), DELTA);
        assertTrue(open.getDeltaMovement().lengthSqr() < 1.0E-6D, "a spent finisher still shoved the body");
        helper.succeed();
    }
    //endregion

    //region Overload [过载反噬]
    @GameTest(template = "empty9x9x9")
    public static void aWhiffedFinisherWhoseConeLandsRecoilsOnce(GameTestHelper helper) {
        ServerPlayer player = overloaded(helper, "gzr-area-damage-cone-recoil");
        Pig pig = idlePig(helper, NEAR, 40.0F);

        ComboRound.beginStrike(player, true);
        assertEquals(List.of(pig), AreaDamage.release(player, EAST, List.of()));

        assertEquals(40.0F - PUNCH, pig.getHealth(), DELTA);
        assertEquals(player.getMaxHealth() - recoil(player), player.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aConeBehindALandedFinisherRecoilsNoSecondTime(GameTestHelper helper) {
        ServerPlayer player = overloaded(helper, "gzr-area-damage-melee-recoil");
        Pig main = idlePig(helper, NEAR, 4.0F);
        Pig open = idlePig(helper, FAR);
        float left = player.getMaxHealth() - recoil(player);

        ComboRound.beginStrike(player, true);
        assertFalse(ComboRound.rollsCrit(player, MISS), "a 99% roll landed a 5% crit");
        assertTrue(Punches.land(player, main, PUNCH), "the finisher did no damage");
        assertEquals(left, player.getHealth(), DELTA);
        assertEquals(List.of(open), AreaDamage.release(player, EAST, List.of(main)));

        assertEquals(PIG_HEALTH - (PUNCH - 4.0F), open.getHealth(), DELTA);
        assertEquals(left, player.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aConeThatLandsOnNothingCostsNoHealth(GameTestHelper helper) {
        ServerPlayer player = overloaded(helper, "gzr-area-damage-no-recoil");
        Pig behind = idlePig(helper, BEHIND);

        ComboRound.beginStrike(player, true);
        assertTrue(AreaDamage.release(player, EAST, List.of()).isEmpty(), "the cone landed behind the fist");

        assertEquals(PIG_HEALTH, behind.getHealth(), DELTA);
        assertEquals(player.getMaxHealth(), player.getHealth(), DELTA);
        helper.succeed();
    }
    //endregion

    //region Widen [放大]
    @GameTest(template = "empty9x9x9")
    public static void hardshipWidensTheHumanConeByItsTier(GameTestHelper helper) {
        ServerPlayer player = tierFive(light(helper, "gzr-area-damage-widened"));
        assertEquals(5, AreaDamage.getWidenTier(player), "tier five Hardship did not widen the human branch");
        assertEquals(LIGHT_PUNCH, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);
        Pig flank = idlePig(helper, CENTER_FLANK, 0.5F);
        Pig far = idlePig(helper, CENTER_FAR);
        Pig side = idlePig(helper, CENTER_SIDE);
        Pig back = idlePig(helper, CENTER_BACK);

        assertEquals(List.of(flank, far), AreaDamage.release(player, EAST, List.of()));

        assertEquals(0.0D, flank.getHealth(), DELTA);
        assertEquals(PIG_HEALTH - (LIGHT_PUNCH - 0.5F), far.getHealth(), DELTA);
        assertEquals(PIG_HEALTH, side.getHealth(), DELTA);
        assertEquals(PIG_HEALTH, back.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theWidenTierIsTheHardshipTierOfTheHumanBranch(GameTestHelper helper) {
        ServerPlayer player = hardship(light(helper, "gzr-area-damage-tiers"));
        float max = player.getMaxHealth();
        float[] health = { 0.7F, 0.5F, 0.35F, 0.2F, TIER_FIVE_HEALTH, 0.8F };
        int[] tier = { 1, 2, 3, 4, 5, 0 };
        for (int index = 0; index < health.length; index++) {
            player.setHealth(max * health[index]);
            assertEquals(tier[index], HardshipStrength.getTier(player), "Hardship tier at " + health[index]);
            assertEquals(tier[index], AreaDamage.getWidenTier(player), "widen tier at " + health[index]);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void withoutHardshipTheHumanBranchKeepsItsCone(GameTestHelper helper) {
        ServerPlayer player = light(helper, "gzr-area-damage-narrow");
        player.setHealth(player.getMaxHealth() * TIER_FIVE_HEALTH);
        assertEquals(0, AreaDamage.getWidenTier(player), "the human branch widened without Hardship");
        Pig flank = idlePig(helper, CENTER_FLANK);
        Pig far = idlePig(helper, CENTER_FAR);
        Pig ahead = idlePig(helper, CENTER_AHEAD);

        assertEquals(List.of(ahead), AreaDamage.release(player, EAST, List.of()));

        assertEquals(PIG_HEALTH, flank.getHealth(), DELTA);
        assertEquals(PIG_HEALTH, far.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void hardshipWidensNothingForTheBeastBranch(GameTestHelper helper) {
        ServerPlayer player = centered(helper, "gzr-area-damage-beast-hardship");
        assertTrue(PathStrengthService.grant(player, BeastStrength.WHITE_BOAR), "the white boar was refused");
        tierFive(player);
        assertEquals(0, AreaDamage.getWidenTier(player), "Hardship widened the beast branch");
        Pig flank = idlePig(helper, CENTER_FLANK);
        Pig far = idlePig(helper, CENTER_FAR);

        assertEquals(List.of(far), AreaDamage.release(player, EAST, List.of()));

        assertEquals(PIG_HEALTH, flank.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHardshipHumanCritOpensTheWidenedCone(GameTestHelper helper) {
        ServerPlayer player = tierFive(light(helper, "gzr-area-damage-widened-crit"));
        Pig flank = idlePig(helper, CENTER_FLANK);

        strike(fighting(player), Animations.FIST_AUTO3, false, true);

        assertEquals(PIG_HEALTH - LIGHT_PUNCH, flank.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHardshipHumanWhoseCritMissesOpensNoCone(GameTestHelper helper) {
        ServerPlayer player = tierFive(light(helper, "gzr-area-damage-widened-miss"));
        Pig ahead = idlePig(helper, CENTER_AHEAD);
        Pig flank = idlePig(helper, CENTER_FLANK);

        strike(fighting(player), Animations.FIST_AUTO3, false, false);

        assertEquals(PIG_HEALTH, ahead.getHealth(), DELTA);
        assertEquals(PIG_HEALTH, flank.getHealth(), DELTA);
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
        ServerPlayer player = patch.getOriginal();
        if (ComboRound.isFinishing(player)) {
            assertEquals(lands, ComboRound.rollsCrit(player, lands ? LAND : MISS), "the crit rolled the other way");
        }
        GameTestEpicFight.attackPhaseEnded(patch, animation, terminated);
    }

    private static ServerPlayer strong(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-area-damage-gametest").pastSpawnInvulnerability()
                .at(WEST);
        player.setYRot(-90.0F);
        return human(player);
    }

    private static ServerPlayer beasts(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-area-damage-beast").pastSpawnInvulnerability()
                .at(WEST);
        player.setYRot(-90.0F);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        PathStrengthService.grant(player, BeastStrength.WHITE_BOAR);
        PathStrengthService.grant(player, BeastStrength.BLACK_BOAR);
        assertEquals(STRONG_POINTS, AreaDamage.getPoints(player));
        return player;
    }

    private static ServerPlayer human(ServerPlayer player) {
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.TEN_JIN, 8), "the ten jin was refused");
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JIN, 8), "the jin was refused");
        assertEquals(STRONG_POINTS, AreaDamage.getPoints(player));
        return player;
    }

    private static ServerPlayer overloaded(GameTestHelper helper, String name) {
        ground(helper);
        ServerPlayer player = GameTestGu.awakened(GameTestPlayers.survival(helper, name).pastSpawnInvulnerability()
                .at(WEST), Rank.FIVE);
        player.setYRot(-90.0F);
        human(player);
        player.setHealth(player.getMaxHealth());
        assertEquals(CAPACITY, StrengthService.getCapacity(player));
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
        assertEquals(1, AreaDamage.getPoints(player));
        return player;
    }

    private static ServerPlayer hardship(ServerPlayer player) {
        inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        player.setHealth(player.getMaxHealth() * 0.7F);
        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "Hardship Strength did not start");
        return player;
    }

    private static ServerPlayer tierFive(ServerPlayer player) {
        hardship(player);
        player.setHealth(player.getMaxHealth() * TIER_FIVE_HEALTH);
        assertEquals(5, HardshipStrength.getTier(player), "10% health is not Hardship tier five");
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
