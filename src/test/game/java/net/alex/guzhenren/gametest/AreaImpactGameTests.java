package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.path.strength.AreaImpact;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.registry.effect.ModEffects;
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
import static net.alex.guzhenren.gametest.GameTestPlayers.ground;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 活玩家的范围冲击（场景西缘朝东）。
 * <ul>
 *   <li>覆盖扇形命中与放过、运行中效果定的几率、Epic Fight 触发（仿其起招时与拳击命中窗口关闭时投递；只有连招轮收尾招
 *       放）。</li>
 *   <li>身体持熊与两猪，即全力以赴所需兽力[Beast Strength]虚影流；加满者另开全力以赴与力气蛊[Strength Qi Gu]，几率
 *       50%。</li>
 *   <li>{@link #nextRoll} 播种定下次掷骰中否，触发测试投递前都调。</li>
 *   <li>远程拳爆点的范围冲击是以爆点为心的球，四面都打，从球心判视线；身体两猪一熊、未开窍，发挥 16 点，球半径 4.6 格。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AreaImpactGameTests {

    private AreaImpactGameTests() {}

    private static final Vec3 WEST = new Vec3(1.5D, 1.0D, 4.5D);
    private static final Vec3 NEAR = new Vec3(3.5D, 1.0D, 4.5D);
    private static final Vec3 FAR = new Vec3(5.5D, 1.0D, 4.5D);
    private static final Vec3 BEHIND = new Vec3(0.5D, 1.0D, 4.5D);
    private static final Vec3 BEYOND = new Vec3(7.5D, 1.0D, 4.5D);
    private static final Vec3 ASIDE = new Vec3(2.0D, 1.0D, 8.0D);
    private static final Vec3 EAST = new Vec3(1.0D, 0.0D, 0.0D);
    private static final Vec3 SPHERE = new Vec3(5.0D, 1.5D, 4.5D);
    private static final int STRONG_POINTS = 16;
    private static final int EFFECT_TICKS = 600;
    private static final int RANK_FIVE_TIER = 4;
    private static final double DELTA = 0.001D;

    @GameTest(template = "empty9x9x9")
    public static void aFistStrikeHitsTheConeAhead(GameTestHelper helper) {
        ServerPlayer player = charged(helper);
        Pig near = idlePig(helper, NEAR);
        Pig far = idlePig(helper, FAR);
        List<Pig> missed = List.of(idlePig(helper, BEHIND), idlePig(helper, BEYOND), idlePig(helper, ASIDE));
        float damage = AreaImpact.getDamage(player.getAttributeValue(Attributes.ATTACK_DAMAGE));

        strike(fighting(player), Animations.FIST_AUTO3, false, true);

        assertEquals(near.getMaxHealth() - damage, near.getHealth(), DELTA);
        assertEquals(far.getMaxHealth() - damage, far.getHealth(), DELTA);
        assertTrue(near.getDeltaMovement().x > 0.0D, () -> "the impact shoved " + near.getDeltaMovement());
        for (Pig pig : missed) {
            assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);
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
    public static void aBurstHitsTheSphereAroundItAndSeesFromItsCenter(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        assertEquals(STRONG_POINTS, AreaImpact.getPoints(player));
        for (int y = 1; y <= 2; y++) helper.setBlock(new BlockPos(7, y, 4), Blocks.STONE);
        Pig north = idlePig(helper, new Vec3(5.0D, 1.0D, 1.5D));
        Pig south = idlePig(helper, new Vec3(5.0D, 1.0D, 7.5D));
        Pig spared = idlePig(helper, new Vec3(6.0D, 1.0D, 4.5D));
        Pig walled = idlePig(helper, new Vec3(8.0D, 1.0D, 4.5D));
        Pig past = idlePig(helper, new Vec3(8.5D, 1.0D, 8.0D));
        float damage = AreaImpact.getDamage(player.getAttributeValue(Attributes.ATTACK_DAMAGE));

        List<LivingEntity> landed = AreaImpact.burst(player, helper.absoluteVec(SPHERE), List.of(spared));

        assertEquals(2, landed.size());
        assertTrue(Set.copyOf(landed).containsAll(List.of(north, south)), () -> "the sphere landed on " + landed);
        assertEquals(north.getMaxHealth() - damage, north.getHealth(), DELTA);
        assertTrue(north.getDeltaMovement().z < 0.0D, () -> "the burst shoved " + north.getDeltaMovement());
        assertTrue(south.getDeltaMovement().z > 0.0D, () -> "the burst shoved " + south.getDeltaMovement());
        for (Pig pig : List.of(spared, walled, past)) {
            assertEquals(pig.getMaxHealth(), pig.getHealth(), DELTA);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void refusingBodiesLeaveTheCapToTheNext(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        for (int index = 0; index < AreaImpact.MAX_TARGETS; index++) idlePig(helper, NEAR).setInvulnerable(true);
        Pig open = idlePig(helper, FAR);

        assertEquals(List.of(open), AreaImpact.release(player, EAST, List.of()));
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

    @GameTest(template = "empty9x9x9")
    public static void theImpactStopsAtEightTargets(GameTestHelper helper) {
        ServerPlayer player = strong(helper);
        List<Pig> pigs = new ArrayList<>();
        for (int index = 0; index < AreaImpact.MAX_TARGETS + 2; index++) pigs.add(idlePig(helper, NEAR));

        assertEquals(AreaImpact.MAX_TARGETS, AreaImpact.release(player, EAST, List.of()).size());
        assertEquals(2, pigs.stream().filter(pig -> pig.getHealth() == pig.getMaxHealth()).count());
        helper.succeed();
    }

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
}
