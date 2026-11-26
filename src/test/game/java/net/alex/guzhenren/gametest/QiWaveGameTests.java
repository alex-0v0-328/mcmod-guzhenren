package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.effect.timed.StrengthQiGuEffect;
import net.alex.guzhenren.entity.projectile.BeastChargeEntity;
import net.alex.guzhenren.entity.projectile.ProjectileHits;
import net.alex.guzhenren.entity.projectile.QiBurst;
import net.alex.guzhenren.entity.projectile.QiWaveEntity;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.strength.AreaDamage;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.BodyOverload;
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
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
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
 * 力气蛊[Strength Qi Gu]及其远程攻击测试，不含客户端瞄准（Epic Fight 锁定）。
 * <ul>
 *   <li>力气蛊施加增益 {@link StrengthQiGuEffect#DURATION_TICKS} 并投射图标；增益中主手空时向前方 6 格目标出远程攻击，爆开为
 *       {@code guzhenren:qi_wave} 伤害，归出拳者；单独花豕蛊[Flower Boar Gu]仍掷气波。</li>
 *   <li>上报（{@link QiWaveService#request}）与起招（{@link QiWaveService#onStrike}）在
 *       {@link QiWaveService#JOIN_TICKS} 内配对才出一发，哪方先到都行，各自落单或隔太久不出；
 *       {@link QiWaveService#fire} 不出手：无增益、手持物品、目标在触及距离内、在身后、是本人；方块挡路则气波熄灭。</li>
 *   <li>总伤害 = 按出手时结算力量点重算的空手拳伤 × 远程系数 0.5（{@link #shot}）：无力量 0.5，人力 70 斤 4.875，四转熊
 *       3.0，收尾暴击的熊（结算 80 点）5.5。人力与无可显形兽种者掷一枚气波带全部；永久兽力按兽种份额（点数 × 来源倍率）
 *       分给各道兽力冲击，没抽中时随机一只带全部；前几段不结算连招轮；冲击随弧线抬升，末段指向目标中心。</li>
 *   <li>爆开按溢出结算：锁定目标排首位吃整发，其余由近及远吃剩余（扣掉挨打者挨打前生命），至多
 *       {@link QiBurst#MAX_TARGETS} 个，免疫者不占名额，隔墙不伤。</li>
 *   <li>命中规则（{@link ProjectileHits}，月刃共用）：爆开越过被打倒者，波及出拳者自己的宠物与盔甲架，不波及旁观者；同队的
 *       非玩家照常中，而 {@code pvp=false} 与队伍关闭友伤时玩家仍被挡下；爆开打到出拳者自己时，护甲减伤后的实际伤害减半，
 *       别人打同一个人、同一发打别人都不减半，披甲后也是减伤结果的一半。</li>
 *   <li>收尾招化作远程攻击时，爆开半径随这一轮的暴击（掷前用 {@link ComboRound#rollsCrit(ServerPlayer, float)} 定下）：
 *       人力流暴击取范围伤害半径（含苦力放大），否则 3 格；兽力流恒为 3 格。仍是同一次气波溢出结算，不另放锥形：爆点
 *       后方 4 格的村民（碰撞箱距爆点 3.7 格）吃剩余。人力 1 斤半径 3.4 格不够，苦力 1 档 ×1.4 到 4.76 格够；苦力下没暴击
 *       仍是 3 格。</li>
 *   <li>过载反噬（{@link #recoil}）：四转承受力 60、最大生命 80（出拳者满血；五转 72 装得下人力 70 斤，验不出反噬），同一拳的几道弹射物共用一份记账，
 *       首次造成伤害时扣一次，没打中不扣。</li>
 *   <li>远程攻击与同一招的近战命中共用那一份记账：谁先造成伤害谁结算，另一方不再扣；{@link ComboRound#SHOT_TICKS} 后或
 *       新出招后，近战命中恢复按当前读数单独结算。</li>
 *   <li>锁定目标被同一拳先到的兽力冲击打死后，其余冲击在目标最后的中心爆开，由最近的目标吃整发。</li>
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
    private static final BlockPos BRAWLED = new BlockPos(7, 1, 1);
    private static final BlockPos CLOSE = new BlockPos(4, 1, 4);
    private static final BlockPos PAST_CLOSE = new BlockPos(4, 1, 8);
    private static final double PAST_DISTANCE = 3.7D;
    private static final Vec3 BURST_AT = new Vec3(4.5, 1.5, 4.5);
    private static final Vec3 BURST_HEADING = new Vec3(1.0, 0.0, 0.0);
    private static final BlockPos BURST_OPEN = new BlockPos(3, 1, 4);
    private static final BlockPos BURST_WALL = new BlockPos(5, 1, 4);
    private static final BlockPos BURST_HIDDEN = new BlockPos(6, 1, 4);
    private static final float BURST_DAMAGE = 10.0F;
    private static final double ARMOR = 20.0D;
    private static final int FLIGHT_TICKS = 12;
    private static final int BAG_SLOT = 9;
    private static final int HUNGER = 98;
    private static final int CAPACITY = 60;
    private static final int HUMAN_POINTS = 70;
    private static final int BEAR_POINTS = 40;
    private static final int BOAR_POINTS = 24;
    private static final int CRIT_FACTOR = 2;
    private static final int RANK_FIVE_TIER = 4;
    private static final int ALL_OUT_TICKS = 200;
    private static final float LAND = 0.0F;
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

    //region Punch [出拳]
    @GameTest(template = "empty9x9x9")
    public static void anOrbCarriesHalfTheFistToItsTarget(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        Villager villager = target(helper, AHEAD);
        Villager beside = target(helper, BESIDE);
        float expected = shot(0);
        assertEquals(expected, player.getAttributeValue(Attributes.ATTACK_DAMAGE) * QiWaveService.RANGED_FACTOR, DELTA);

        QiWaveEntity orb = assertInstance(shoot(player, villager), QiWaveEntity.class);
        assertEquals(expected, orb.getDamage(), DELTA);
        assertTrue(orb.getOwner() == player, "the orb is the puncher's");
        assertFalse(orb.shouldBeSaved(), "an orb is never written to a chunk");

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(expected, TARGET_HEALTH - villager.getHealth(), DELTA);
            assertEquals(TARGET_HEALTH, beside.getHealth(), DELTA);
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
    public static void aHumanPunchThrowsOneOrbWithTheWholePunchAndRecoilsOnceItLands(GameTestHelper helper) {
        ServerPlayer player = human(buffed(helper, STAND));
        Villager villager = target(helper, AHEAD);

        QiWaveEntity orb = assertInstance(shoot(player, villager), QiWaveEntity.class);
        assertEquals(shot(HUMAN_POINTS), orb.getDamage(), DELTA);
        assertEquals(player.getMaxHealth(), player.getHealth(), DELTA);

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(shot(HUMAN_POINTS), TARGET_HEALTH - villager.getHealth(), DELTA);
            assertEquals(player.getMaxHealth() - recoil(player, HUMAN_POINTS), player.getHealth(), DELTA);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aLoneBearChargeCarriesTheWholePunch(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        Villager villager = target(helper, AHEAD);

        BeastChargeEntity charge = assertInstance(shoot(player, villager), BeastChargeEntity.class);
        assertEquals(BeastStrengthFamily.BEAR, charge.getFamily(), "the charge is not the bear");
        assertEquals(shot(BEAR_POINTS), charge.getDamage(), DELTA);

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(shot(BEAR_POINTS), TARGET_HEALTH - villager.getHealth(), DELTA);
            assertEquals(player.getMaxHealth(), player.getHealth(), DELTA);
            assertTrue(charge.isRemoved(), "the charge lingered past its burst");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aCritBearCountsTwiceInThePunchAndOverloadsTheBody(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        Villager villager = target(helper, AHEAD);
        int settled = BEAR_POINTS * CRIT_FACTOR;

        draw(player, LAND);
        assertEquals(List.of(BeastStrengthFamily.BEAR), ComboRound.getCritFamilies(player),
                "the landed roll did not crit the bear");
        assertEquals(settled, StrengthService.getSettledPoints(player));
        BeastChargeEntity charge = assertInstance(finish(player, villager), BeastChargeEntity.class);
        assertEquals(shot(settled), charge.getDamage(), DELTA);
        assertTrue(ComboRound.isRanged(player), "the finisher did not mark its ranged attack");
        assertEquals(player.getMaxHealth(), player.getHealth(), DELTA);

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(shot(settled), TARGET_HEALTH - villager.getHealth(), DELTA);
            assertEquals(player.getMaxHealth() - recoil(player, settled), player.getHealth(), DELTA);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void underAllOutEffortTheCritBearStillOverloadsTheBody(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, ALL_OUT_TICKS, RANK_FIVE_TIER));
        Villager villager = target(helper, AHEAD);
        int settled = BEAR_POINTS * CRIT_FACTOR;
        assertEquals(CAPACITY, StrengthService.getCapacity(player), "All-Out Effort lifted the capacity");

        draw(player, MISS);
        assertEquals(List.of(BeastStrengthFamily.BEAR), ComboRound.getCritFamilies(player),
                "All-Out Effort did not crit the bear");
        BeastChargeEntity charge = assertInstance(finish(player, villager), BeastChargeEntity.class);
        assertEquals(shot(settled), charge.getDamage(), DELTA);

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(shot(settled), TARGET_HEALTH - villager.getHealth(), DELTA);
            assertEquals(player.getMaxHealth() - recoil(player, settled), player.getHealth(), DELTA);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void anUndrawnFinisherThrowsOneHeldFamilyWithTheWholePunch(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        PathStrengthService.grant(player, BeastStrength.WHITE_BOAR);
        Villager villager = target(helper, AHEAD);

        draw(player, MISS);
        assertTrue(ComboRound.getFamilies(player).isEmpty(), "a missed roll drew a family");
        BeastChargeEntity charge = assertInstance(finish(player, villager), BeastChargeEntity.class);
        assertTrue(List.of(BeastStrengthFamily.BEAR, BeastStrengthFamily.BOAR).contains(charge.getFamily()),
                "charged with a family not held: " + charge.getFamily());
        assertEquals(shot(BEAR_POINTS + BOAR_POINTS), charge.getDamage(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHardshipCritThrowsEveryDrawnFamilyByItsShareAndRecoilsOnce(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        Villager villager = target(helper, AHEAD);
        int settled = (BEAR_POINTS + BOAR_POINTS) * CRIT_FACTOR;

        critBearAndBoar(player);
        assertEquals(settled, StrengthService.getSettledPoints(player));
        List<Projectile> shots = QiWaveService.fire(player, villager.getId(), true);
        assertEquals(2, shots.size(), "the tier one crit did not throw both phantoms");
        List<BeastStrengthFamily> thrown = new ArrayList<>();
        float total = 0.0F;
        for (Projectile projectile : shots) {
            BeastChargeEntity charge = assertInstance(projectile, BeastChargeEntity.class);
            int points = charge.getFamily() == BeastStrengthFamily.BEAR ? BEAR_POINTS : BOAR_POINTS;
            assertEquals(shot(settled) * points / (BEAR_POINTS + BOAR_POINTS), charge.getDamage(), DELTA);
            thrown.add(charge.getFamily());
            total += charge.getDamage();
        }
        assertEquals(ComboRound.getFamilies(player), thrown, "the charges are not the drawn families");
        assertEquals(shot(settled), total, DELTA);

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(shot(settled), TARGET_HEALTH - villager.getHealth(), DELTA);
            assertEquals(player.getMaxHealth() - recoil(player, settled), player.getHealth(), DELTA);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void chargesWhoseTargetDiedBurstWhereItStoodOnTheNearest(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        Villager villager = target(helper, AHEAD);
        villager.setHealth(1.0F);
        Villager beside = target(helper, BESIDE);
        int settled = (BEAR_POINTS + BOAR_POINTS) * CRIT_FACTOR;

        critBearAndBoar(player);
        steadyArc(player.getRandom());
        List<Projectile> shots = QiWaveService.fire(player, villager.getId(), true);
        assertEquals(2, shots.size(), "the tier one crit did not throw both phantoms");

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(0.0D, villager.getHealth(), DELTA);
            assertEquals(shot(settled) - 1.0F, TARGET_HEALTH - beside.getHealth(), DELTA);
            for (Projectile projectile : shots) assertTrue(projectile.isRemoved(), "a charge lingered past its burst");
            assertEquals(player.getMaxHealth() - recoil(player, settled), player.getHealth(), DELTA);
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
    public static void anOpeningPunchSettlesNoRound(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, ALL_OUT_TICKS, RANK_FIVE_TIER));
        Villager villager = target(helper, AHEAD);

        ComboRound.beginStrike(player, false);
        BeastChargeEntity charge = assertInstance(shoot(player, villager), BeastChargeEntity.class);
        assertEquals(shot(BEAR_POINTS), charge.getDamage(), DELTA);
        assertTrue(ComboRound.getFamilies(player).isEmpty(), "an opening punch drew a family");
        assertTrue(ComboRound.getCritFamilies(player).isEmpty(), "an opening punch crit a family");
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
    public static void aBlockSnuffsTheOrbAndTheMissCostsNoHealth(GameTestHelper helper) {
        ServerPlayer player = human(buffed(helper, STAND));
        Villager villager = target(helper, AHEAD);
        for (int x = 3; x <= 5; x++) helper.setBlock(new BlockPos(x, 1, 4), Blocks.STONE);

        QiWaveEntity orb = assertInstance(shoot(player, villager), QiWaveEntity.class);
        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(TARGET_HEALTH, villager.getHealth(), DELTA);
            assertTrue(orb.isRemoved(), "the orb went through the wall");
            assertEquals(player.getMaxHealth(), player.getHealth(), DELTA);
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
    //endregion

    //region Overload [过载反噬]
    @GameTest(template = "empty9x9x9")
    public static void aFistThatLandsFirstSettlesTheMovesOverloadForItsOrb(GameTestHelper helper) {
        ServerPlayer player = human(buffed(helper, STAND));
        Villager villager = target(helper, AHEAD);
        Villager brawled = target(helper, BRAWLED);
        float left = player.getMaxHealth() - recoil(player, HUMAN_POINTS);

        ComboRound.beginStrike(player, false);
        assertInstance(shoot(player, villager), QiWaveEntity.class);
        assertTrue(Punches.land(player, brawled, 1.0F), "the fist did no damage");
        assertEquals(left, player.getHealth(), DELTA);

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(shot(HUMAN_POINTS), TARGET_HEALTH - villager.getHealth(), DELTA);
            assertEquals(left, player.getHealth(), DELTA);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aFistThatLandsAfterTheOrbBurstsRecoilsNoSecondTime(GameTestHelper helper) {
        ServerPlayer player = human(buffed(helper, STAND));
        Villager villager = target(helper, AHEAD);
        Villager brawled = target(helper, BRAWLED);
        float left = player.getMaxHealth() - recoil(player, HUMAN_POINTS);

        ComboRound.beginStrike(player, false);
        assertInstance(shoot(player, villager), QiWaveEntity.class);

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(shot(HUMAN_POINTS), TARGET_HEALTH - villager.getHealth(), DELTA);
            assertEquals(left, player.getHealth(), DELTA);
            assertTrue(Punches.land(player, brawled, 1.0F), "the fist did no damage");
            assertEquals(left, player.getHealth(), DELTA);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aFistPastTheShotsWindowSettlesItsOwnOverload(GameTestHelper helper) {
        ServerPlayer player = human(buffed(helper, STAND));
        Villager villager = target(helper, AHEAD);
        Villager brawled = target(helper, BRAWLED);
        float left = player.getMaxHealth() - recoil(player, HUMAN_POINTS);

        ComboRound.beginStrike(player, false);
        assertInstance(shoot(player, villager), QiWaveEntity.class);

        helper.runAfterDelay(ComboRound.SHOT_TICKS, () -> {
            assertEquals(left, player.getHealth(), DELTA);
            assertTrue(Punches.land(player, brawled, 1.0F), "the fist did no damage");
            assertEquals(left - recoil(player, HUMAN_POINTS), player.getHealth(), DELTA);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aNewMoveSettlesItsOwnOverload(GameTestHelper helper) {
        ServerPlayer player = human(buffed(helper, STAND));
        Villager villager = target(helper, AHEAD);
        Villager brawled = target(helper, BRAWLED);
        float left = player.getMaxHealth() - recoil(player, HUMAN_POINTS);

        ComboRound.beginStrike(player, false);
        assertInstance(shoot(player, villager), QiWaveEntity.class);

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(left, player.getHealth(), DELTA);
            ComboRound.beginStrike(player, false);
            assertTrue(Punches.land(player, brawled, 1.0F), "the fist did no damage");
            assertEquals(left - recoil(player, HUMAN_POINTS), player.getHealth(), DELTA);
            helper.succeed();
        });
    }
    //endregion

    //region Burst [爆开]
    @GameTest(template = "empty9x9x9")
    public static void theBurstFeedsItsLockedTargetFirstThenSpillsOutward(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        Villager closer = target(helper, BURST_OPEN);
        Villager locked = target(helper, BURST_HIDDEN);
        locked.setHealth(4.0F);

        List<LivingEntity> hit = QiBurst.burst(player, player, helper.absoluteVec(BURST_AT), BURST_HEADING, locked,
                BURST_DAMAGE, QiBurst.Spec.QI_WAVE);

        assertEquals(List.of(locked, closer), hit);
        assertEquals(0.0D, locked.getHealth(), DELTA);
        assertEquals(BURST_DAMAGE - 4.0F, TARGET_HEALTH - closer.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aBurstWithNoLockedTargetStartsAtTheNearest(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        Villager closer = target(helper, BURST_OPEN);
        Villager farther = target(helper, BURST_HIDDEN);

        List<LivingEntity> hit = QiBurst.burst(player, player, helper.absoluteVec(BURST_AT), BURST_HEADING, null,
                BURST_DAMAGE, QiBurst.Spec.QI_WAVE);

        assertEquals(List.of(closer), hit);
        assertEquals(BURST_DAMAGE, TARGET_HEALTH - closer.getHealth(), DELTA);
        assertEquals(TARGET_HEALTH, farther.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aBurstSparesWhatAWallHides(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        Villager open = target(helper, BURST_OPEN);
        open.setHealth(4.0F);
        Villager hidden = target(helper, BURST_HIDDEN);
        helper.setBlock(BURST_WALL, Blocks.STONE);
        helper.setBlock(BURST_WALL.above(), Blocks.STONE);

        List<LivingEntity> hit = QiBurst.burst(player, player, helper.absoluteVec(BURST_AT), BURST_HEADING, null,
                BURST_DAMAGE, new QiBurst.Spec(QiBurst.RADIUS, ModDamageTypes.QI_WAVE, false, null));

        assertEquals(List.of(open), hit);
        assertEquals(0.0D, open.getHealth(), DELTA);
        assertEquals(TARGET_HEALTH, hidden.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRefusingBodyTakesNoSlotAndNoDamageFromTheBurst(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        Villager refusing = target(helper, BURST_OPEN);
        refusing.setInvulnerable(true);
        Villager farther = target(helper, BURST_HIDDEN);

        List<LivingEntity> hit = QiBurst.burst(player, player, helper.absoluteVec(BURST_AT), BURST_HEADING, null,
                BURST_DAMAGE, QiBurst.Spec.QI_WAVE);

        assertEquals(List.of(farther), hit);
        assertEquals(TARGET_HEALTH, refusing.getHealth(), DELTA);
        assertEquals(BURST_DAMAGE, TARGET_HEALTH - farther.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theBurstStopsAtEightTargets(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        List<Pig> pigs = new ArrayList<>();
        for (int index = 0; index < QiBurst.MAX_TARGETS + 2; index++) {
            Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, BURST_OPEN);
            pig.setHealth(1.0F);
            pigs.add(pig);
        }

        List<LivingEntity> hit = QiBurst.burst(player, player, helper.absoluteVec(BURST_AT), BURST_HEADING, null,
                TARGET_HEALTH, QiBurst.Spec.QI_WAVE);

        assertEquals(QiBurst.MAX_TARGETS, hit.size());
        assertEquals(2, pigs.stream().filter(pig -> pig.getHealth() == 1.0F).count());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theShotSettlesItsOverloadOnceOnItsFirstDamage(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        QiBurst.Spec spec = new QiBurst.Spec(QiBurst.RADIUS, ModDamageTypes.QI_WAVE, false,
                new BodyOverload.Shot(HUMAN_POINTS, CAPACITY));
        Vec3 at = helper.absoluteVec(BURST_AT);

        assertTrue(QiBurst.burst(player, player, at, BURST_HEADING, null, BURST_DAMAGE, spec).isEmpty(),
                "a burst over empty ground hit something");
        assertEquals(player.getMaxHealth(), player.getHealth(), DELTA);

        Villager villager = target(helper, BURST_OPEN);
        assertEquals(List.of(villager), QiBurst.burst(player, player, at, BURST_HEADING, null, BURST_DAMAGE, spec));
        float left = player.getMaxHealth() - recoil(player, HUMAN_POINTS);
        assertEquals(left, player.getHealth(), DELTA);

        assertEquals(List.of(villager), QiBurst.burst(player, player, at, BURST_HEADING, null, BURST_DAMAGE, spec));
        assertEquals(left, player.getHealth(), DELTA);
        assertEquals(BURST_DAMAGE * 2.0F, TARGET_HEALTH - villager.getHealth(), DELTA);
        helper.succeed();
    }
    //endregion

    //region Hit rule [命中规则]
    @GameTest(template = "empty9x9x9")
    public static void aBurstSpillsOntoThePlayersOwnPet(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        Villager locked = target(helper, BURST_OPEN);
        locked.setHealth(4.0F);
        Wolf wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, BURST_HIDDEN);
        wolf.tame(player);
        float before = wolf.getHealth();

        List<LivingEntity> hit = QiBurst.burst(player, player, helper.absoluteVec(BURST_AT), BURST_HEADING, locked,
                BURST_DAMAGE, QiBurst.Spec.QI_WAVE);

        assertEquals(List.of(locked, wolf), hit);
        assertEquals(BURST_DAMAGE - 4.0F, before - wolf.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aBurstHitsAnArmorStand(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, BURST_OPEN);

        List<LivingEntity> hit = QiBurst.burst(player, player, helper.absoluteVec(BURST_AT), BURST_HEADING, null,
                BURST_DAMAGE, QiBurst.Spec.QI_WAVE);

        assertEquals(List.of(stand), hit);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aBurstSparesASpectatorAndSpillsPastIt(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        ServerPlayer watcher = GameTestPlayers.survival(helper, "gzr-qi-wave-spectator").spectator(() -> true)
                .pastSpawnInvulnerability().at(new Vec3(3.5, 1.0, 4.5));
        Villager farther = target(helper, BURST_HIDDEN);

        List<LivingEntity> hit = new ArrayList<>();
        GameTestPlayers.withPvp(helper, true, () -> hit.addAll(QiBurst.burst(player, player,
                helper.absoluteVec(BURST_AT), BURST_HEADING, null, BURST_DAMAGE, QiBurst.Spec.QI_WAVE)));

        assertEquals(List.of(farther), hit);
        assertEquals(watcher.getMaxHealth(), watcher.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aBurstHitsATeammateButPvpAndFriendlyFireOffStillShieldPlayers(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        Scoreboard scoreboard = helper.getLevel().getScoreboard();
        PlayerTeam team = scoreboard.addPlayerTeam("gzr-qi-wave-" + player.getUUID());
        Villager mate = target(helper, BURST_OPEN);
        ServerPlayer mateAvatar = GameTestPlayers.survival(helper, "gzr-qi-wave-mate").pastSpawnInvulnerability()
                .at(new Vec3(5.5, 1.0, 4.5));
        boolean pvp = helper.getLevel().getServer().isPvpAllowed();
        try {
            helper.getLevel().getServer().setPvpAllowed(true);
            scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(mate.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(mateAvatar.getScoreboardName(), team);
            Vec3 at = helper.absoluteVec(BURST_AT);

            team.setAllowFriendlyFire(false);
            List<LivingEntity> hit = QiBurst.burst(player, player, at, BURST_HEADING, mate, BURST_DAMAGE,
                    QiBurst.Spec.QI_WAVE);
            assertEquals(List.of(mate), hit, "a teammate that is not a player is no friendly-fire case");
            assertEquals(BURST_DAMAGE, TARGET_HEALTH - mate.getHealth(), DELTA);
            assertEquals(mateAvatar.getMaxHealth(), mateAvatar.getHealth(), DELTA);

            mate.setHealth(1.0F);
            hit = QiBurst.burst(player, player, at, BURST_HEADING, mate, BURST_DAMAGE, QiBurst.Spec.QI_WAVE);
            assertEquals(List.of(mate), hit, "friendly fire off did not shield a teammate player");
            assertEquals(mateAvatar.getMaxHealth(), mateAvatar.getHealth(), DELTA);

            team.setAllowFriendlyFire(true);
            mate.setHealth(1.0F);
            hit = QiBurst.burst(player, player, at, BURST_HEADING, mate, BURST_DAMAGE, QiBurst.Spec.QI_WAVE);
            assertEquals(List.of(mate, mateAvatar), hit, "friendly fire on lost the teammate player");

            scoreboard.removePlayerFromTeam(mateAvatar.getScoreboardName(), team);
            mateAvatar.setHealth(mateAvatar.getMaxHealth());
            helper.getLevel().getServer().setPvpAllowed(false);
            mate.setHealth(1.0F);
            hit = QiBurst.burst(player, player, at, BURST_HEADING, mate, BURST_DAMAGE, QiBurst.Spec.QI_WAVE);
            assertEquals(List.of(mate), hit, "pvp off did not shield a player");
            assertEquals(mateAvatar.getMaxHealth(), mateAvatar.getHealth(), DELTA);
        } finally {
            helper.getLevel().getServer().setPvpAllowed(pvp);
            scoreboard.removePlayerTeam(team);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aBurstThatCatchesTheCasterHalvesItAfterArmor(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        ServerPlayer other = GameTestPlayers.survival(helper, "gzr-qi-wave-other").pastSpawnInvulnerability()
                .at(new Vec3(1.5, 1.0, 1.5));
        Villager villager = target(helper, BURST_OPEN);
        QiWaveEntity orb = orbOf(helper, player);
        QiWaveEntity othersOrb = orbOf(helper, other);
        Vec3 onPlayer = player.getBoundingBox().getCenter();
        Vec3 onOther = other.getBoundingBox().getCenter();
        Vec3 onVillager = helper.absoluteVec(BURST_AT);

        GameTestPlayers.withPvp(helper, true, () -> {
            assertEquals(List.of(player), QiBurst.burst(player, orb, onPlayer, BURST_HEADING, player, BURST_DAMAGE,
                    QiBurst.Spec.QI_WAVE));
            assertEquals(BURST_DAMAGE * ProjectileHits.SELF_HIT_FACTOR, player.getMaxHealth() - player.getHealth(),
                    DELTA);
            assertEquals(List.of(other), QiBurst.burst(player, orb, onOther, BURST_HEADING, other, BURST_DAMAGE,
                    QiBurst.Spec.QI_WAVE));
            assertEquals(BURST_DAMAGE, other.getMaxHealth() - other.getHealth(), DELTA);
            assertEquals(List.of(player), QiBurst.burst(other, othersOrb, onPlayer, BURST_HEADING, player,
                    BURST_DAMAGE, QiBurst.Spec.QI_WAVE));
            assertEquals(BURST_DAMAGE * ProjectileHits.SELF_HIT_FACTOR + BURST_DAMAGE,
                    player.getMaxHealth() - player.getHealth(), DELTA);

            player.setHealth(player.getMaxHealth());
            for (LivingEntity armored : List.of(player, villager)) {
                armored.getAttribute(Attributes.ARMOR).setBaseValue(ARMOR);
            }
            QiBurst.burst(player, orb, onPlayer, BURST_HEADING, player, BURST_DAMAGE, QiBurst.Spec.QI_WAVE);
            QiBurst.burst(player, orb, onVillager, BURST_HEADING, villager, BURST_DAMAGE, QiBurst.Spec.QI_WAVE);
            float armoredVillager = TARGET_HEALTH - villager.getHealth();
            assertTrue(armoredVillager < BURST_DAMAGE, "the armor reduced nothing");
            assertEquals(armoredVillager * ProjectileHits.SELF_HIT_FACTOR,
                    player.getMaxHealth() - player.getHealth(), DELTA);
        });
        helper.succeed();
    }
    //endregion

    //region Area Damage [范围伤害]
    @GameTest(template = "empty9x9x9")
    public static void aHumanCritWidensTheBurstToTheAreaDamageRange(GameTestHelper helper) {
        ServerPlayer player = human(buffed(helper, STAND));
        Villager villager = target(helper, CLOSE);
        villager.setHealth(2.0F);
        Villager past = target(helper, PAST_CLOSE);
        assertTrue(AreaDamage.getRange(HUMAN_POINTS) > PAST_DISTANCE,
                "the area damage range stops short of the villager");

        crit(player, true);
        QiWaveEntity orb = assertInstance(finish(player, villager), QiWaveEntity.class);
        assertTrue(orb.isAlive(), "the orb did not fly");
        assertEquals(shot(HUMAN_POINTS), orb.getDamage(), DELTA);

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(0.0D, villager.getHealth(), DELTA);
            assertEquals(shot(HUMAN_POINTS) - 2.0F, TARGET_HEALTH - past.getHealth(), DELTA);
            DamageSource source = assertNotNull(past.getLastDamageSource(), () -> "the spill recorded no damage");
            assertTrue(source.is(ModDamageTypes.QI_WAVE), "the hit past the burst is not the burst's own spill");
            assertEquals(player.getMaxHealth() - recoil(player, HUMAN_POINTS), player.getHealth(), DELTA);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aHumanMissKeepsTheThreeBlockBurst(GameTestHelper helper) {
        ServerPlayer player = human(buffed(helper, STAND));
        Villager villager = target(helper, CLOSE);
        villager.setHealth(2.0F);
        Villager past = target(helper, PAST_CLOSE);
        assertTrue(QiBurst.RADIUS < PAST_DISTANCE, "the plain burst reaches the villager past it");

        crit(player, false);
        assertInstance(finish(player, villager), QiWaveEntity.class);

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(0.0D, villager.getHealth(), DELTA);
            assertEquals(TARGET_HEALTH, past.getHealth(), DELTA);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aBeastCritKeepsTheThreeBlockBurst(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        Villager villager = target(helper, CLOSE);
        villager.setHealth(1.0F);
        Villager past = target(helper, PAST_CLOSE);
        int settled = BEAR_POINTS * CRIT_FACTOR;

        crit(player, true);
        assertFalse(ComboRound.rollsArea(player), "a beast crit counted as an area damage");
        ComboRound.strike(player, LAND);
        assertEquals(List.of(BeastStrengthFamily.BEAR), ComboRound.getFamilies(player), "the crit drew no bear");
        steadyArc(player.getRandom());
        BeastChargeEntity charge = assertInstance(finish(player, villager), BeastChargeEntity.class);
        assertEquals(shot(settled), charge.getDamage(), DELTA);

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(0.0D, villager.getHealth(), DELTA);
            assertEquals(TARGET_HEALTH, past.getHealth(), DELTA);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void hardshipWidensAHumanCritsBurstByItsTier(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JIN, 1), "the jin was refused");
        hardship(player);
        assertEquals(1, AreaDamage.getWidenTier(player), "70% health is not widen tier one");
        assertTrue(AreaDamage.getRange(1) < PAST_DISTANCE && AreaDamage.getRange(1, 1) > PAST_DISTANCE,
                "the villager past does not sit between the plain and the widened range");
        Villager villager = target(helper, CLOSE);
        villager.setHealth(0.25F);
        Villager past = target(helper, PAST_CLOSE);

        crit(player, true);
        QiWaveEntity orb = assertInstance(finish(player, villager), QiWaveEntity.class);
        assertEquals(shot(1), orb.getDamage(), DELTA);

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(0.0D, villager.getHealth(), DELTA);
            assertEquals(shot(1) - 0.25F, TARGET_HEALTH - past.getHealth(), DELTA);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void underHardshipAHumanMissKeepsTheThreeBlockBurst(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JIN, 1), "the jin was refused");
        hardship(player);
        Villager villager = target(helper, CLOSE);
        villager.setHealth(0.25F);
        Villager past = target(helper, PAST_CLOSE);

        crit(player, false);
        assertInstance(finish(player, villager), QiWaveEntity.class);

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(0.0D, villager.getHealth(), DELTA);
            assertEquals(TARGET_HEALTH, past.getHealth(), DELTA);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void withoutHardshipAHumanCritKeepsThePlainAreaDamageRange(GameTestHelper helper) {
        ServerPlayer player = buffed(helper, STAND);
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JIN, 1), "the jin was refused");
        assertEquals(0, AreaDamage.getWidenTier(player), "the human branch widened without Hardship");
        Villager villager = target(helper, CLOSE);
        villager.setHealth(0.25F);
        Villager past = target(helper, PAST_CLOSE);

        crit(player, true);
        assertInstance(finish(player, villager), QiWaveEntity.class);

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(0.0D, villager.getHealth(), DELTA);
            assertEquals(TARGET_HEALTH, past.getHealth(), DELTA);
            helper.succeed();
        });
    }
    //endregion

    private static QiWaveEntity orbOf(GameTestHelper helper, ServerPlayer owner) {
        QiWaveEntity orb = new QiWaveEntity(ModEntityTypes.QI_WAVE.get(), helper.getLevel());
        orb.setOwner(owner);
        return orb;
    }

    private static float shot(int settledPoints) {
        return (1.0F + settledPoints * 0.125F) * 0.5F;
    }

    private static float recoil(ServerPlayer player, int settledPoints) {
        return (float) Math.max(0, settledPoints - CAPACITY) / CAPACITY * player.getMaxHealth() * 0.5F;
    }

    private static ServerPlayer human(ServerPlayer player) {
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JUN, 2), "the jun was refused");
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.TEN_JIN, 1), "the ten jin was refused");
        assertEquals(HUMAN_POINTS, StrengthService.getSettledPoints(player));
        return player;
    }

    private static void critBearAndBoar(ServerPlayer player) {
        PathStrengthService.grant(player, BeastStrength.BEAR);
        PathStrengthService.grant(player, BeastStrength.WHITE_BOAR);
        hardship(player);
        assertEquals(1, HardshipStrength.getTier(player), "70% health is not Hardship tier one");
        draw(player, LAND);
        assertEquals(2, ComboRound.getFamilies(player).size(), "the tier one crit drew other than two");
        assertEquals(2, ComboRound.getCritFamilies(player).size(), "a drawn family did not crit");
        player.setHealth(player.getMaxHealth());
    }

    private static void hardship(ServerPlayer player) {
        inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        player.setHealth(player.getMaxHealth() * 0.7F);
        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "Hardship Strength did not start");
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

    private static void crit(ServerPlayer player, boolean lands) {
        ComboRound.beginStrike(player, true);
        assertEquals(lands, ComboRound.rollsCrit(player, lands ? LAND : MISS),
                "the finisher's crit rolled the other way");
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
        ServerPlayer player = GameTestGu.awakened(GameTestPlayers.survival(helper, "gzr-qi-wave-gametest")
                .pastSpawnInvulnerability().at(stand), Rank.FOUR);
        assertEquals(CAPACITY, StrengthService.getCapacity(player));
        player.setHealth(player.getMaxHealth());
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
