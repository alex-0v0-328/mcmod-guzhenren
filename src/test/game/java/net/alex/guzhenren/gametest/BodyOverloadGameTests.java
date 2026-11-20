package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.LastHurtView;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.flight.SecondStageCruise;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BodyOverload;
import net.alex.guzhenren.gameplay.path.strength.ComboRound;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 过载反噬[Body Overload]结算边界的四条回归。
 * <ul>
 *   <li>① 收尾招打中举盾目标：溢出量按扣除格挡后的护甲前伤害算，完全挡下记 0，不再记整拳。</li>
 *   <li>② 只有出拳伤害结算过载反噬：荆棘反伤与二段飞行冲撞（以 {@code player_attack} 撞人）都不扣血，随后的真出拳照扣
 *       （排除反噬对该玩家整体失效的假通过）。</li>
 *   <li>③ 反噬扣血前后同时复原受击无敌帧与 {@code lastHurt}：先吃 5 点伤害，反噬后无敌帧内再吃 6 点只掉差额 1 点。</li>
 *   <li>④ 被伤害吸收完全吃掉的拳：没伤到生命也算打中，照扣过载反噬（Alex 定）。</li>
 *   <li>凡人持白豕 24 点（承受力 12，超出 12）：反噬为最大生命的 50%；五转人力 90 点（承受力 72，超出 18）用于 ③。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BodyOverloadGameTests {

    private BodyOverloadGameTests() {}

    private static final Vec3 WEST = new Vec3(1.5D, 1.0D, 4.5D);
    private static final Vec3 RAMMED = new Vec3(3.0D, 1.0D, 4.5D);
    private static final double CRASH_SPEED = 2.0D;
    private static final int SHIELD_RAISE_TICKS = 8;
    private static final int ABSORPTION_TICKS = 600;
    private static final int ABSORPTION_LEVEL = 4;
    private static final float MISS = 0.99F;
    private static final float FIRST_HIT = 5.0F;
    private static final float SECOND_HIT = 6.0F;
    private static final double DELTA = 0.001D;

    //region ① Shield [举盾]
    @GameTest(template = "empty9x9x9")
    public static void aFinisherAShieldStopsLeavesNoSpill(GameTestHelper helper) {
        ServerPlayer player = boar(helper, "gzr-overload-legacy-shield");
        Pig guard = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 2));
        guard.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        face(guard, player);
        guard.startUsingItem(InteractionHand.OFF_HAND);

        helper.runAfterDelay(SHIELD_RAISE_TICKS, () -> {
            face(guard, player);
            assertTrue(guard.isBlocking(), "the shield is not raised");
            float guarded = guard.getHealth();

            ComboRound.beginStrike(player, true);
            assertFalse(ComboRound.rollsCrit(player, MISS), "a 99% roll landed a 5% crit");
            assertFalse(Punches.land(player, guard, 12.0F), "the shield let the punch in");
            assertEquals(guarded, guard.getHealth(), DELTA);
            assertEquals(0.0D, ComboRound.getSpill(player, 12.0F), DELTA);
            ComboRound.clear(player);
            helper.succeed();
        });
    }
    //endregion

    //region ② Source [伤害来源]
    @GameTest(template = "empty9x9x9")
    public static void thornsPayNoOverloadButTheNextPunchDoes(GameTestHelper helper) {
        ServerPlayer player = boar(helper, "gzr-overload-legacy-thorns");
        float max = player.getMaxHealth();
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 1, 2));

        ComboRound.beginStrike(player, false);
        assertTrue(pig.hurt(player.damageSources().thorns(player), 1.0F), "the thorns did no damage");
        assertEquals(max, player.getHealth(), DELTA);

        ComboRound.beginStrike(player, false);
        Pig punched = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 1, 2));
        assertTrue(Punches.land(player, punched, 1.0F), "the punch did no damage");
        assertEquals(max - recoil(max, 12, 12), player.getHealth(), DELTA);
        ComboRound.clear(player);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aSecondStageRamPaysNoOverload(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-overload-legacy-ram").creative(() -> true)
                .pastSpawnInvulnerability().at(WEST);
        player.setYRot(-90.0F);
        player.setXRot(0.0F);
        PathStrengthService.grant(player, BeastStrength.WHITE_BOAR);
        assertTrue(StrengthService.getSettledPoints(player) > StrengthService.getCapacity(player),
                "the boar does not pass the mortal capacity");
        float max = player.getMaxHealth();
        player.setHealth(max);
        Pig pig = helper.spawn(EntityType.PIG, RAMMED);
        pig.setNoAi(true);
        pig.setNoGravity(true);
        pig.setDeltaMovement(Vec3.ZERO);
        player.getAbilities().flying = true;
        player.setSprinting(true);
        assertTrue(SecondStageService.start(player), "the flier did not enter the second stage");
        PlayerData.get(player).setSecondStageThrottle(
                CRASH_SPEED / SecondStageCruise.getTopSpeed(player.getAbilities().getFlyingSpeed()));
        player.setDeltaMovement(new Vec3(CRASH_SPEED, 0.0D, 0.0D));

        SecondStageService.travel(player);

        assertTrue(pig.getHealth() < pig.getMaxHealth(), "the ram missed the pig");
        assertEquals(max, player.getHealth(), DELTA);

        ComboRound.beginStrike(player, false);
        Pig punched = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 1, 7));
        assertTrue(Punches.land(player, punched, 1.0F), "the punch did no damage");
        assertEquals(max - recoil(max, 12, 12), player.getHealth(), DELTA);
        ComboRound.clear(player);
        player.discard();
        helper.succeed();
    }
    //endregion

    //region ③ Last hurt [受击基准]
    @GameTest(template = "empty9x9x9")
    public static void theRecoilRestoresTheHurtFramesAndTheLastHurt(GameTestHelper helper) {
        ServerPlayer player = GameTestGu.awakened(GameTestPlayers.survival(helper, "gzr-overload-legacy-last-hurt")
                .pastSpawnInvulnerability().onGround(), Rank.FIVE);
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JUN, 3), "the jun was refused");
        assertEquals(72, StrengthService.getCapacity(player));
        float max = player.getMaxHealth();
        float recoil = recoil(max, 18, 72);
        assertTrue(Math.abs(recoil - FIRST_HIT) > 0.5F, "the recoil equals the first hit; the test proves nothing");
        player.setHealth(max);

        assertTrue(player.hurt(player.damageSources().generic(), FIRST_HIT), "the first hit did not land");
        int frames = player.invulnerableTime;
        assertTrue(frames > 10, "the first hit left no hurt frames");
        ComboRound.beginStrike(player, false);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 1, 2));
        assertTrue(Punches.land(player, pig, 1.0F), "the punch did no damage");

        assertEquals(max - FIRST_HIT - recoil, player.getHealth(), DELTA);
        assertEquals(frames, player.invulnerableTime);
        assertEquals(FIRST_HIT, ((LastHurtView) player).guzhenren$getLastHurt(), DELTA);

        float before = player.getHealth();
        assertTrue(player.hurt(player.damageSources().generic(), SECOND_HIT), "the second hit did not land");
        assertEquals(before - (SECOND_HIT - FIRST_HIT), player.getHealth(), DELTA);
        ComboRound.clear(player);
        helper.succeed();
    }
    //endregion

    //region ④ Absorption [伤害吸收]
    @GameTest(template = "empty9x9x9")
    public static void aPunchTheAbsorptionSwallowsStillPaysTheOverload(GameTestHelper helper) {
        ServerPlayer player = boar(helper, "gzr-overload-legacy-absorption");
        float max = player.getMaxHealth();
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 1, 2));
        pig.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ABSORPTION_TICKS, ABSORPTION_LEVEL));
        float absorption = pig.getAbsorptionAmount();
        assertTrue(absorption > 1.0F, "the pig took no absorption");
        float health = pig.getHealth();

        ComboRound.beginStrike(player, false);
        Punches.land(player, pig, 1.0F);

        assertEquals(health, pig.getHealth(), DELTA);
        assertEquals(absorption - 1.0F, pig.getAbsorptionAmount(), DELTA);
        assertEquals(max - recoil(max, 12, 12), player.getHealth(), DELTA);
        ComboRound.clear(player);
        helper.succeed();
    }
    //endregion

    private static float recoil(float maxHealth, int overload, int capacity) {
        return (float) overload / capacity * maxHealth * BodyOverload.RECOIL_RATIO;
    }

    private static ServerPlayer boar(GameTestHelper helper, String name) {
        ServerPlayer player = GameTestPlayers.survival(helper, name).pastSpawnInvulnerability().onGround();
        PathStrengthService.grant(player, BeastStrength.WHITE_BOAR);
        assertEquals(12, StrengthService.getCapacity(player));
        assertEquals(24, StrengthService.getSettledPoints(player));
        player.setHealth(player.getMaxHealth());
        return player;
    }

    private static void face(Pig guard, ServerPlayer player) {
        Vec3 toward = player.position().subtract(guard.position());
        float yaw = (float) (Mth.atan2(toward.z, toward.x) * Mth.RAD_TO_DEG) - 90.0F;
        guard.setYRot(yaw);
        guard.setYHeadRot(yaw);
        guard.setYBodyRot(yaw);
    }
}
