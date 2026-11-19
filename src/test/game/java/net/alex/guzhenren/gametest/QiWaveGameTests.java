package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.effect.timed.StrengthQiGuEffect;
import net.alex.guzhenren.entity.projectile.BeastChargeEntity;
import net.alex.guzhenren.entity.projectile.QiBurst;
import net.alex.guzhenren.entity.projectile.QiWaveEntity;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.strength.AreaImpact;
import net.alex.guzhenren.gameplay.path.strength.BeastChargeDamage;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.ComboRound;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.path.qi.QiWaveService;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestGu.click;
import static net.alex.guzhenren.gametest.GameTestGu.hold;
import static net.alex.guzhenren.gametest.GameTestGu.inSlot;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 力气蛊[Strength Qi Gu]及其远程拳测试，不含客户端瞄准（Epic Fight 锁定）。
 * <ul>
 *   <li>力气蛊施加增益 {@link StrengthQiGuEffect#DURATION_TICKS} 并投射图标；增益中主手空时，人力钧力流肉身向前方 6
 *       格目标出拳，掷一个气波，爆开为 {@code guzhenren:qi_wave} 伤害，归出拳者；目标受拳的攻击伤害，旁边村民一半，
 *       自己的狼无伤；单独花豕蛊[Flower Boar Gu]仍掷气波。</li>
 *   <li>上报（{@link QiWaveService#request}）与起招（{@link QiWaveService#onStrike}）在
 *       {@link QiWaveService#JOIN_TICKS} 内配对才出一发，哪方先到都行，各自落单或隔太久不出；
 *       {@link QiWaveService#fire} 不出手：无增益、手持物品、目标在触及距离内、在身后、是本人；方块挡路则气波熄灭。</li>
 *   <li>永久兽力[Beast Strength]掷本族兽力冲击，按该族所加力量计伤：五转熊 10.0，收尾解放 14.0（120 点被承受力截断），
 *       五转全力以赴蛊[All-Out Effort Gu]下收尾解放 30.0；苦力连击两发各留本族份额；前几段不结算连招轮；冲击随弧线
 *       抬升，末段指向目标中心。</li>
 *   <li>范围冲击在收尾时于出拳者随机源上掷一次，掷前设种（{@code AreaImpactGameTests.nextRoll}）；掷中则爆开时以爆点
 *       为心放球：伤及爆点后方 4 格、爆开半径外的村民，气波那次另伤及爆点正东 4 格、与飞行方向垂直的村民，未中则其无
 *       伤；气波出拳者另练人力 200 斤（发挥 56 点，球半径 6 格），熊冲击发挥 40 点（球半径 5.5 格）。</li>
 *   <li>冲击弧线在出拳者随机源上掷出；{@link #steadyArc} 给该随机源设种，使后续 {@link #ARC_DRAWS} 次取值都低于
 *       {@link #LOW_DRAW}，无论哪几次掷弧线：弯曲至多 0.24、峰值后移，冲击在 {@link #FLIGHT_TICKS} 内落到目标。</li>
 *   <li>出拳者在北缘朝南（yaw 0）；目标为 {@link #TARGET_HEALTH} 生命的无意识村民。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QiWaveGameTests {

    private static final Vec3 STAND = new Vec3(4.5, 1.0, 0.5);
    private static final BlockPos AHEAD = new BlockPos(4, 1, 7);
    private static final BlockPos BESIDE = new BlockPos(6, 1, 7);
    private static final BlockPos PET = new BlockPos(2, 1, 7);
    private static final BlockPos CLOSE = new BlockPos(4, 1, 4);
    private static final BlockPos PAST_CLOSE = new BlockPos(4, 1, 8);
    private static final BlockPos ASIDE_CLOSE = new BlockPos(8, 1, 4);
    private static final Vec3 BURST_AT = new Vec3(4.5, 1.5, 4.5);
    private static final BlockPos BURST_OPEN = new BlockPos(3, 1, 4);
    private static final BlockPos BURST_WALL = new BlockPos(5, 1, 4);
    private static final BlockPos BURST_HIDDEN = new BlockPos(6, 1, 4);
    private static final float BURST_DAMAGE = 10.0F;
    private static final int FLIGHT_TICKS = 12;
    private static final int BAG_SLOT = 9;
    private static final int HUNGER = 98;
    private static final int BEAR_POINTS = 40;
    private static final int BOAR_POINTS = 24;
    private static final int RANK_FIVE_TIER = 4;
    private static final int ALL_OUT_TICKS = 200;
    private static final float BEAR_DAMAGE = 10.0F;
    private static final float RELEASED_BEAR_DAMAGE = 14.0F;
    private static final float ALL_OUT_BEAR_DAMAGE = 30.0F;
    private static final float MISS = 0.99F;
    private static final int ARC_DRAWS = 8;
    private static final float LOW_DRAW = 0.3F;
    private static final float TARGET_HEALTH = 200.0F;
    private static final double DELTA = 1.0E-4D;
    private static final double ANGLE_DELTA = 1.0E-3D;

    private QiWaveGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void theGuLaysItsBuffForThirtySecondsAndProjectsItsIcon(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);

        assertTrue(TimedEffects.hasBuff(player, ModEffects.STRENGTH_QI_GU), "no buff was laid");
        assertEquals(StrengthQiGuEffect.DURATION_TICKS,
                TimedEffects.getBuffTicksLeft(player, ModEffects.STRENGTH_QI_GU));
        assertTrue(player.hasEffect(ModEffects.STRENGTH_QI_GU), "the icon was not projected");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void anOrbBurstsFullOnTheTargetHalfBesideItAndSparesThePet(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        Villager villager = target(helper, AHEAD);
        Villager beside = target(helper, BESIDE);
        Wolf wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, PET);
        wolf.tame(player);
        float wolfHealth = wolf.getHealth();
        float expected = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);

        Projectile shot = shoot(player, villager);
        QiWaveEntity orb = assertInstance(shot, QiWaveEntity.class);
        assertEquals(expected, orb.getDamage(), DELTA);
        assertTrue(orb.getOwner() == player, "the orb is the puncher's");
        assertFalse(orb.shouldBeSaved(), "an orb is never written to a chunk");

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(expected, TARGET_HEALTH - villager.getHealth(), DELTA);
            assertEquals(expected * QiBurst.SECONDARY_RATIO, TARGET_HEALTH - beside.getHealth(), DELTA);
            assertEquals(wolfHealth, wolf.getHealth(), DELTA);
            DamageSource source = assertNotNull(villager.getLastDamageSource(), () -> "no damage was recorded");
            assertTrue(source.is(ModDamageTypes.QI_WAVE), "the hit is a Qi Wave");
            assertTrue(source.getEntity() == player, "the hit names the puncher");
            assertTrue(orb.isRemoved(), "the orb outlived its burst");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aRangedPunchFiresOnlyWhenPairedWithAStrike(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        Villager villager = target(helper, AHEAD);

        none(QiWaveService.request(player, villager.getId()));
        one(QiWaveService.onStrike(player, false), "a strike after the report threw nothing");
        none(QiWaveService.onStrike(player, false));
        one(QiWaveService.request(player, villager.getId()), "a report after the strike threw nothing");
        none(QiWaveService.request(player, villager.getId()));
        none(QiWaveService.request(player, villager.getId()));
        one(QiWaveService.onStrike(player, false), "a mashed report threw other than once");
        none(QiWaveService.request(player, villager.getId()));
        helper.runAfterDelay(QiWaveService.JOIN_TICKS + 1, () -> {
            none(QiWaveService.onStrike(player, false));
            none(QiWaveService.request(player, Integer.MAX_VALUE));
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aBearChargeHitsForTheBearsOwnPoints(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        Villager villager = target(helper, AHEAD);

        BeastChargeEntity charge = assertInstance(shoot(player, villager), BeastChargeEntity.class);
        assertEquals(BeastStrengthFamily.BEAR, charge.getFamily(), "the charge is not the bear");
        assertEquals(BEAR_DAMAGE, charge.getDamage(), DELTA);
        assertEquals(BEAR_POINTS * BeastChargeDamage.ATTACK_PER_POINT, charge.getDamage(), DELTA);

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(charge.getDamage(), TARGET_HEALTH - villager.getHealth(), DELTA);
            assertTrue(charge.isRemoved(), "the charge lingered past its burst");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aReleasedBearChargeTriplesItsPointsCutToTheCapacity(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        Villager villager = target(helper, AHEAD);

        draw(player, 0.0F);
        assertEquals(List.of(BeastStrengthFamily.BEAR), ComboRound.getFullyReleased(player),
                "the landed roll did not release the bear");
        assertTrue(StrengthService.getCapacity(player) < BEAR_POINTS * StrengthService.FULL_RELEASE_FACTOR,
                "the capacity does not cut the released bear");
        BeastChargeEntity charge = assertInstance(finish(player, villager), BeastChargeEntity.class);
        assertEquals(RELEASED_BEAR_DAMAGE, charge.getDamage(), DELTA);
        assertEquals(StrengthService.getCapacity(player) * BeastChargeDamage.ATTACK_PER_POINT, charge.getDamage(),
                DELTA);
        assertTrue(ComboRound.isRanged(player), "the finisher did not mark its ranged punch");

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(charge.getDamage(), TARGET_HEALTH - villager.getHealth(), DELTA);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void underAllOutEffortTheReleasedBearHitsForItsWholeTripledPoints(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, ALL_OUT_TICKS, RANK_FIVE_TIER));
        Villager villager = target(helper, AHEAD);

        draw(player, MISS);
        assertEquals(List.of(BeastStrengthFamily.BEAR), ComboRound.getFullyReleased(player),
                "All-Out Effort did not release the bear");
        BeastChargeEntity charge = assertInstance(finish(player, villager), BeastChargeEntity.class);
        assertEquals(ALL_OUT_BEAR_DAMAGE, charge.getDamage(), DELTA);
        assertEquals(BEAR_POINTS * StrengthService.FULL_RELEASE_FACTOR * BeastChargeDamage.ATTACK_PER_POINT,
                charge.getDamage(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aStreakThrowsEveryDrawnFamilyEachForItsOwnPoints(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        PathStrengthService.grant(player, BeastStrength.WHITE_BOAR);
        inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        player.setHealth(player.getMaxHealth() * 0.7F);
        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "Hardship Strength did not start");
        Villager villager = target(helper, AHEAD);

        draw(player, MISS);
        assertEquals(1, ComboRound.getFamilies(player).size(), "the streak's first round drew other than one");
        ComboRound.complete(player, false);
        draw(player, MISS);
        assertEquals(2, ComboRound.getFamilies(player).size(), "the streak's second round drew other than two");
        assertTrue(ComboRound.getFullyReleased(player).isEmpty(), "a missed roll released a family");
        double share = Math.min(1.0D, (double) StrengthService.getCapacity(player) / (BEAR_POINTS + BOAR_POINTS));
        List<Projectile> shots = QiWaveService.fire(player, villager.getId(), true);
        assertEquals(2, shots.size(), "the streak's second round did not throw both phantoms");
        List<BeastStrengthFamily> thrown = new ArrayList<>();
        float total = 0.0F;
        for (Projectile shot : shots) {
            BeastChargeEntity charge = assertInstance(shot, BeastChargeEntity.class);
            int points = charge.getFamily() == BeastStrengthFamily.BEAR ? BEAR_POINTS : BOAR_POINTS;
            assertEquals(points * share * BeastChargeDamage.ATTACK_PER_POINT, charge.getDamage(), DELTA);
            thrown.add(charge.getFamily());
            total += charge.getDamage();
        }
        assertEquals(ComboRound.getFamilies(player), thrown, "the charges are not the drawn families");

        float landed = total;
        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(landed, TARGET_HEALTH - villager.getHealth(), DELTA);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aChargeRearsOnItsLiftAndPointsAtTheTargetLast(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        PathStrengthService.grant(player, BeastStrength.WHITE_BOAR);
        Villager villager = target(helper, AHEAD);

        BeastChargeEntity charge = assertInstance(shoot(player, villager), BeastChargeEntity.class);
        assertTrue(charge.getXRot() < 0.0F, "the charge did not rear on the arc's lift: pitch " + charge.getXRot());
        Vec3[] last = new Vec3[3];
        helper.onEachTick(() -> {
            if (charge.isRemoved()) return;
            last[0] = charge.position();
            last[1] = new Vec3(charge.getXRot(), charge.getYRot(), 0.0D);
            last[2] = villager.getBoundingBox().getCenter();
        });
        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertTrue(charge.isRemoved(), "the charge lingered past its burst");
            Vec3 toTarget = last[2].subtract(last[0]);
            double pitch = Math.toDegrees(Math.atan2(-toTarget.y, toTarget.horizontalDistance()));
            double yaw = Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
            assertEquals(pitch, last[1].x, ANGLE_DELTA);
            assertEquals(yaw, last[1].y, ANGLE_DELTA);
            assertTrue(last[1].x > 0.0F, "the last stretch does not dive at the target: pitch " + last[1].x);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aLandedBurstRollOpensTheSphereAroundTheBurst(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JUN, 6), "the jun was refused");
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.TEN_JIN, 2), "the ten jin was refused");
        Villager villager = target(helper, CLOSE);
        Villager past = target(helper, PAST_CLOSE);
        Villager aside = target(helper, ASIDE_CLOSE);
        float expected = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);

        impact(player, true);
        QiWaveEntity orb = assertInstance(finish(player, villager), QiWaveEntity.class);
        assertTrue(orb.isAlive(), "the orb did not fly");

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(expected, TARGET_HEALTH - villager.getHealth(), DELTA);
            assertEquals(AreaImpact.getDamage(expected), TARGET_HEALTH - past.getHealth(), DELTA);
            assertEquals(AreaImpact.getDamage(expected), TARGET_HEALTH - aside.getHealth(), DELTA);
            DamageSource source = assertNotNull(past.getLastDamageSource(), () -> "the sphere recorded no damage");
            assertTrue(source.is(ModDamageTypes.AREA_IMPACT), "the hit past the burst is not the area impact");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aBurstSparesWhatAWallHides(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        Villager open = target(helper, BURST_OPEN);
        Villager hidden = target(helper, BURST_HIDDEN);
        helper.setBlock(BURST_WALL, Blocks.STONE);
        helper.setBlock(BURST_WALL.above(), Blocks.STONE);

        QiBurst.burst(player, player, helper.absoluteVec(BURST_AT), new Vec3(1.0, 0.0, 0.0), null, BURST_DAMAGE,
                new QiBurst.Spec(QiBurst.RADIUS, ModDamageTypes.QI_WAVE, false));

        assertEquals(BURST_DAMAGE * QiBurst.SECONDARY_RATIO, TARGET_HEALTH - open.getHealth(), DELTA);
        assertEquals(TARGET_HEALTH, hidden.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aMissedBurstRollOpensNoSphere(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        Villager villager = target(helper, CLOSE);
        Villager past = target(helper, PAST_CLOSE);
        float expected = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);

        impact(player, false);
        assertInstance(finish(player, villager), QiWaveEntity.class);

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(expected, TARGET_HEALTH - villager.getHealth(), DELTA);
            assertEquals(TARGET_HEALTH, past.getHealth(), DELTA);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aChargeBurstOpensTheSphereWhereItLands(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        Villager villager = target(helper, CLOSE);
        Villager past = target(helper, PAST_CLOSE);

        impact(player, true);
        ComboRound.strike(player, MISS);
        steadyArc(player.getRandom());
        BeastChargeEntity charge = assertInstance(finish(player, villager), BeastChargeEntity.class);

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(charge.getDamage(), TARGET_HEALTH - villager.getHealth(), DELTA);
            assertTrue(past.getHealth() < TARGET_HEALTH, "the sphere missed the villager past the burst");
            DamageSource source = assertNotNull(past.getLastDamageSource(), () -> "the sphere recorded no damage");
            assertTrue(source.is(ModDamageTypes.AREA_IMPACT), "the hit past the burst is not the area impact");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void anOpeningPunchSettlesNoRound(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, ALL_OUT_TICKS, RANK_FIVE_TIER));
        Villager villager = target(helper, AHEAD);

        ComboRound.beginStrike(player, false);
        BeastChargeEntity charge = assertInstance(shoot(player, villager), BeastChargeEntity.class);
        assertEquals(BEAR_DAMAGE, charge.getDamage(), DELTA);
        assertTrue(ComboRound.getFamilies(player).isEmpty(), "an opening punch drew a family");
        assertTrue(ComboRound.getFullyReleased(player).isEmpty(), "an opening punch released a family");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aFlowerBoarAloneThrowsAnOrb(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        TimedEffects.grantBuff(player, (TendedGuItem) ModItems.FLOWER_BOAR_GU_1.get(), ModEffects.FLOWER_BOAR_GU, 0,
                600L, Amplification.NONE);
        Villager villager = target(helper, AHEAD);

        assertInstance(shoot(player, villager), QiWaveEntity.class);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aBlockSnuffsTheOrb(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        Villager villager = target(helper, AHEAD);
        for (int x = 3; x <= 5; x++) helper.setBlock(new BlockPos(x, 1, 4), Blocks.STONE);

        QiWaveEntity orb = assertInstance(shoot(player, villager), QiWaveEntity.class);
        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(TARGET_HEALTH, villager.getHealth(), DELTA);
            assertTrue(orb.isRemoved(), "the orb went through the wall");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void nothingWithoutTheBuffOrWithAnItemInHand(GameTestHelper helper) {
        ServerPlayer bare = GameTestGu.awakened(GameTestPlayers.survival(helper, "gzr-qi-wave-bare").at(STAND),
                Rank.FIVE);
        Villager villager = target(helper, AHEAD);
        none(QiWaveService.fire(bare, villager.getId(), false));

        ServerPlayer armed = buffed(helper, STAND);
        armed.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        none(QiWaveService.fire(armed, villager.getId(), false));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void nothingInsideTheReachBehindOrAtItself(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, new Vec3(4.5, 1.0, 4.5));
        Villager near = target(helper, new BlockPos(4, 1, 6));
        Villager behind = target(helper, new BlockPos(4, 1, 0));

        ComboRound.beginStrike(player, true);
        none(QiWaveService.fire(player, near.getId(), true));
        none(QiWaveService.fire(player, behind.getId(), true));
        none(QiWaveService.fire(player, player.getId(), true));
        none(QiWaveService.fire(player, Integer.MAX_VALUE, true));
        assertFalse(ComboRound.isRanged(player), "a refused punch took over the finisher");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aFarTargetStillTakesAPunch(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, new Vec3(0.5, 1.0, 0.5));
        Villager far = target(helper, new BlockPos(8, 1, 8));
        assertTrue(QiWaveService.getBoxDistance(player, far) > 8.0D, "the target is not past 8 blocks");
        one(QiWaveService.fire(player, far.getId(), false), "nothing at a far target");
        helper.succeed();
    }

    private static Projectile shoot(ServerPlayer player, Villager target) {
        return one(QiWaveService.fire(player, target.getId(), false), "nothing was thrown");
    }

    private static Projectile finish(ServerPlayer player, Villager target) {
        if (!ComboRound.isFinishing(player)) ComboRound.beginStrike(player, true);
        return one(QiWaveService.fire(player, target.getId(), true), "the finisher threw nothing");
    }

    private static void draw(ServerPlayer player, float roll) {
        ComboRound.beginStrike(player, true);
        ComboRound.strike(player, roll);
    }

    private static void impact(ServerPlayer player, boolean lands) {
        ComboRound.beginStrike(player, true);
        AreaImpactGameTests.nextRoll(player.getRandom(), AreaImpact.STRENGTH_QI_CHANCE, lands);
        assertEquals(lands, ComboRound.rollsImpact(player), "the finisher's area impact rolled the other way");
    }

    private static void steadyArc(RandomSource random) {
        for (long seed = 0L; ; seed++) {
            random.setSeed(seed);
            boolean low = true;
            for (int i = 0; i < ARC_DRAWS && low; i++) low = random.nextFloat() < LOW_DRAW;
            if (low) {
                random.setSeed(seed);
                return;
            }
        }
    }

    private static Projectile one(List<Projectile> shots, String message) {
        assertEquals(1, shots.size(), message);
        return shots.getFirst();
    }

    private static void none(List<Projectile> shots) {
        assertTrue(shots.isEmpty(), "threw " + shots);
    }

    private static <T> T assertInstance(Object value, Class<T> type) {
        assertTrue(type.isInstance(value), "expected a " + type.getSimpleName() + ", got " + value);
        return type.cast(value);
    }

    private static ServerPlayer buffed(GameTestHelper helper, Vec3 stand) {
        ServerPlayer player = GameTestGu.awakened(GameTestPlayers.survival(helper, "gzr-qi-wave-gametest").at(stand),
                Rank.FIVE);
        ItemStack gu = hold(player, InteractionHand.MAIN_HAND, ModItems.STRENGTH_QI_GU_3.get(), 50);
        click(helper, player, gu);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return player;
    }

    private static Villager target(GameTestHelper helper, BlockPos at) {
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, at);
        villager.getAttribute(Attributes.MAX_HEALTH).setBaseValue(TARGET_HEALTH);
        villager.setHealth(TARGET_HEALTH);
        return villager;
    }
}
