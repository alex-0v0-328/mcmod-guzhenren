package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.BeastPhantomEntity;
import net.alex.guzhenren.entity.projectile.BeastChargeEntity;
import net.alex.guzhenren.entity.projectile.QiWaveEntity;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.BeastChargeDamage;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.BodyOverload;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StoreStrength;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthSnapshot;
import net.alex.guzhenren.gameplay.path.timed.TimedBuff;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
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
 *   <li>无永久兽力流或人力流分支则拒绝不扣费，有则施加增益；未蓄满松手不出拳不扣费；手持剑或 Epic Fight 拳套、
 *       增益消失则拒绝并丢弃蓄力；全力以赴蛊[All-Out Effort Gu]缩短蓄力。</li>
 *   <li>人力钧力流蓄满（200 斤）朝锁定目标打水平扇形：目标全额，扇形内一半，扇形外无伤；冲击反噬把出拳者打到生命下限，
 *       不推动；苦力蛊[Hardship Strength Gu]把扇形扩到身侧；熊蓄满（40 点）计 480 点并显示熊虚影。</li>
 *   <li>蓄满的扇形崩裂身前地形：人力 200 点破到石头（硬度 1.5）、不破深板岩；熊 480 点破到深板岩（3），不破圆石深板岩
 *       与黑曜石，也不挖脚下地面。</li>
 *   <li>力气蛊[Strength Qi Gu]增益中目标在触及距离外：人力流放一个追踪气波，兽力流放一发兽力冲击，按解放熊自身 480
 *       点计（120.0），不打近战；追踪气波超时就地爆开；拳伤害保留攻击力乘数，不低于 0。</li>
 *   <li>惯力蛊[Habitual Strength Gu]增益下起蓄带可移动标记，蓄力期间疾跑被清掉；取消、出拳、丢弃都撤；出拳结果与蓄力蛊相同。</li>
 *   <li>出拳者站石头地面朝南（yaw 0），已过出生无敌，反噬可生效；身后目标 id 不保留，挥击沿视线；目标为
 *       {@link #TARGET_HEALTH} 生命的无意识村民；蓄力用 {@link GameTestPlayers#serverTicks} 推进。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StoreStrengthGameTests {

    private static final Vec3 NEAR_STAND = new Vec3(4.5, 1.0, 2.5);
    private static final Vec3 FAR_STAND = new Vec3(4.5, 1.0, 0.5);
    private static final BlockPos LOCKED = new BlockPos(4, 1, 6);
    private static final BlockPos IN_FAN = new BlockPos(6, 1, 5);
    private static final BlockPos SIDE = new BlockPos(8, 1, 2);
    private static final BlockPos BEHIND = new BlockPos(4, 1, 0);
    private static final BlockPos PAST_REACH = new BlockPos(4, 1, 7);
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
    private static final int BEAR_POINTS = 40;
    private static final int BEAR_MAX_POINTS = 480;
    private static final int RANK_FIVE_TIER = 4;
    private static final int ALL_OUT_TICKS = 40;
    private static final int SOME_TICKS = 50;
    private static final int NO_TARGET = -1;
    private static final int OUT_OF_TIME_TICKS = 1000;
    private static final double SHOVE_SQR = 1.0E-6D;
    private static final float TARGET_HEALTH = 1000.0F;
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
    public static void letGoBeforeFullItStrikesNothingAndCostsNothing(GameTestHelper helper) {
        ServerPlayer player = human(helper, NEAR_STAND);
        Villager locked = villager(helper, LOCKED);

        assertTrue(StoreStrength.start(player), "the charge did not start");
        GameTestPlayers.serverTicks(player, SOME_TICKS);
        float health = player.getHealth();
        assertTrue(StoreStrength.isCharging(player), "the charge stopped on its own");
        float progress = StoreStrength.getProgress(player);
        assertTrue(progress > 0.5F && progress < 1.0F, "the charge is not part way: " + progress);

        assertFalse(ChargedPunch.release(player, locked.getId()), "a short charge struck");
        assertFalse(StoreStrength.isCharging(player), "a release left the charge standing");
        assertEquals(0.0F, StoreStrength.getProgress(player), DELTA);
        assertEquals(TARGET_HEALTH, locked.getHealth(), DELTA);
        assertEquals(health, player.getHealth(), DELTA);
        assertTrue(player.getLastDamageSource() == null, "a short charge paid a backlash");

        assertTrue(StoreStrength.start(player), "the second charge did not start");
        assertFalse(ChargedPunch.release(player, locked.getId()), "an empty charge struck");
        assertEquals(TARGET_HEALTH, locked.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aFullHumanChargeStrikesTheFanAndPaysTheBacklash(GameTestHelper helper) {
        ServerPlayer player = human(helper, NEAR_STAND);
        Villager locked = villager(helper, LOCKED);
        Villager inFan = villager(helper, IN_FAN);
        Villager outside = villager(helper, SIDE);

        fillCharge(player);
        float health = player.getHealth();
        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        assertEquals(JIN, snapshot.totalPoints(), "the body does not hold 200 points");
        assertTrue(snapshot.capacity() < JIN, "the capacity holds the whole strength: " + snapshot.capacity());
        float expected = (float) AttackDamageService.getFistDamage(player, JIN);
        float backlash = BodyOverload.getRecoil(JIN - snapshot.capacity(), snapshot.capacity(), player.getMaxHealth());

        assertTrue(ChargedPunch.release(player, locked.getId()), "the full charge did not strike");

        assertEquals(expected, TARGET_HEALTH - locked.getHealth(), DELTA);
        assertEquals(expected * ChargedPunch.SECONDARY_RATIO, TARGET_HEALTH - inFan.getHealth(), DELTA);
        assertEquals(TARGET_HEALTH, outside.getHealth(), DELTA);
        DamageSource hit = assertNotNull(locked.getLastDamageSource(), () -> "the locked target recorded no damage");
        assertTrue(hit.is(ModDamageTypes.CHARGED_PUNCH), "the hit is not a charged punch");
        assertTrue(hit.getEntity() == player, "the hit names another than the puncher");
        assertTrue(BeastPhantomEntity.findOwned(player).isEmpty(), "the human path showed a phantom");
        assertBacklash(player, health, backlash);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aFullBearChargeBringsFourHundredEightyPointsAndShowsThePhantom(GameTestHelper helper) {
        ServerPlayer player = player(helper, NEAR_STAND);
        assertTrue(PathStrengthService.grant(player, BeastStrength.BEAR), "the bear was refused");
        useGu(helper, player);
        Villager locked = villager(helper, LOCKED);

        fillCharge(player);
        float health = player.getHealth();
        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        assertEquals(BEAR_POINTS, snapshot.totalPoints(), "the body does not hold the bear's 40 points");
        int max = StoreStrength.getMaxPoints(snapshot.totalPoints(), BEAR_POINTS);
        assertEquals(BEAR_MAX_POINTS, max, "the released bear does not count 12 times");
        float expected = (float) AttackDamageService.getFistDamage(player, BEAR_MAX_POINTS);
        float backlash = BodyOverload.getRecoil(max - snapshot.capacity(), snapshot.capacity(), player.getMaxHealth());

        assertTrue(ChargedPunch.release(player, locked.getId()), "the full charge did not strike");

        assertEquals(expected, TARGET_HEALTH - locked.getHealth(), DELTA);
        assertTrue(expected > AttackDamageService.getFistDamage(player, BEAR_POINTS) + 1.0D,
                "the strike is no harder than the plain fist");
        assertTrue(BeastPhantomEntity.findOwned(player, BeastStrengthFamily.BEAR) != null,
                "the bear phantom did not show");
        assertBacklash(player, health, backlash);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aFullBearChargeShattersTheFanUpToHardnessThree(GameTestHelper helper) {
        ServerPlayer player = player(helper, NEAR_STAND);
        assertTrue(PathStrengthService.grant(player, BeastStrength.BEAR), "the bear was refused");
        useGu(helper, player);
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
    public static void aFullHumanChargeShattersStoneButNotDeepslate(GameTestHelper helper) {
        ServerPlayer player = human(helper, NEAR_STAND);
        Villager locked = villager(helper, LOCKED);
        helper.setBlock(FAN_FEET, Blocks.STONE);
        helper.setBlock(FAN_RIGHT, Blocks.DEEPSLATE);

        fillCharge(player);
        assertTrue(ChargedPunch.release(player, locked.getId()), "the full charge did not strike");

        assertBlock(helper, FAN_FEET, Blocks.AIR);
        assertBlock(helper, FAN_RIGHT, Blocks.DEEPSLATE);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void hardshipWidensTheHumanFanToTheSidesButNotBehind(GameTestHelper helper) {
        ServerPlayer player = human(helper, NEAR_STAND);
        Villager locked = villager(helper, LOCKED);
        Villager side = villager(helper, SIDE);
        Villager behind = villager(helper, BEHIND);
        inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);

        fillCharge(player);
        player.setHealth(player.getMaxHealth() * 0.7F);
        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "Hardship Strength did not start");
        float expected = (float) AttackDamageService.getFistDamage(player, JIN);

        assertTrue(ChargedPunch.release(player, locked.getId()), "the full charge did not strike");

        assertEquals(expected, TARGET_HEALTH - locked.getHealth(), DELTA);
        assertEquals(expected * ChargedPunch.SECONDARY_RATIO, TARGET_HEALTH - side.getHealth(), DELTA);
        assertEquals(TARGET_HEALTH, behind.getHealth(), DELTA);
        assertTrue(player.getHealth() >= BodyOverload.HEALTH_FLOOR, "the backlash went under the floor");
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
        float expected = (float) AttackDamageService.getFistDamage(player, JIN);
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
    public static void withTheQiBuffAFarTargetTakesOneTrackingQiWave(GameTestHelper helper) {
        ServerPlayer player = human(helper, FAR_STAND);
        Villager far = villager(helper, PAST_REACH);
        Villager close = villager(helper, CLOSE);

        fillCharge(player);
        assertTrue(ChargedPunch.release(player, far.getId()), "the full charge did not strike");
        assertTrue(waves(player).isEmpty(), "a Qi Wave flew without the Strength Qi Gu's buff");
        assertTrue(far.getHealth() < TARGET_HEALTH, "the 200-point fan (8.2 blocks) missed the target 7 out");
        assertTrue(close.getHealth() < TARGET_HEALTH, "the melee swing hit nothing without the buff");
        far.setHealth(TARGET_HEALTH);
        close.setHealth(TARGET_HEALTH);

        qiBuff(helper, player);
        player.setHealth(player.getMaxHealth());
        fillCharge(player);
        float expected = (float) AttackDamageService.getFistDamage(player, JIN);
        assertTrue(ChargedPunch.release(player, far.getId()), "the second full charge did not strike");

        List<QiWaveEntity> waves = waves(player);
        assertEquals(1, waves.size(), "the ranged strike did not throw one Qi Wave");
        assertEquals(expected, waves.getFirst().getDamage(), DELTA);
        assertTrue(waves.getFirst().getOwner() == player, "the wave is not the puncher's");
        assertEquals(TARGET_HEALTH, far.getHealth(), DELTA);
        assertEquals(TARGET_HEALTH, close.getHealth(), DELTA);
        assertTrue(charges(player).isEmpty(), "the human path threw a Beast Charge");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aTrackingQiWaveOutOfTimeBurstsWhereItIs(GameTestHelper helper) {
        ServerPlayer player = human(helper, FAR_STAND);
        qiBuff(helper, player);
        Villager far = villager(helper, PAST_REACH);
        Villager beside = villager(helper, BESIDE_THROW);

        fillCharge(player);
        assertTrue(ChargedPunch.release(player, far.getId()), "the full charge did not strike");
        List<QiWaveEntity> waves = waves(player);
        assertEquals(1, waves.size(), "the ranged strike did not throw one Qi Wave");
        QiWaveEntity wave = waves.getFirst();
        wave.tickCount = OUT_OF_TIME_TICKS;
        wave.tick();
        assertTrue(wave.isRemoved(), "the out-of-time wave lingered");
        assertTrue(beside.getHealth() < TARGET_HEALTH, "the out-of-time wave burst nowhere");
        assertEquals(TARGET_HEALTH, far.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void withTheQiBuffABeastThrowsOneBeastChargeAtAFarTarget(GameTestHelper helper) {
        ServerPlayer player = player(helper, FAR_STAND);
        assertTrue(PathStrengthService.grant(player, BeastStrength.BEAR), "the bear was refused");
        useGu(helper, player);
        qiBuff(helper, player);
        Villager far = villager(helper, PAST_REACH);
        Villager close = villager(helper, CLOSE);

        fillCharge(player);
        float expected = (float) (BEAR_MAX_POINTS * BeastChargeDamage.ATTACK_PER_POINT);
        assertTrue(ChargedPunch.release(player, far.getId()), "the full charge did not strike");

        List<BeastChargeEntity> charges = charges(player);
        assertEquals(1, charges.size(), "the ranged strike did not throw one Beast Charge");
        assertEquals(BeastStrengthFamily.BEAR, charges.getFirst().getFamily(), "the charge is not the bear");
        assertEquals(expected, charges.getFirst().getDamage(), DELTA);
        assertTrue(waves(player).isEmpty(), "the beast path threw a Qi Wave");
        assertEquals(TARGET_HEALTH, far.getHealth(), DELTA);
        assertEquals(TARGET_HEALTH, close.getHealth(), DELTA);
        assertTrue(BeastPhantomEntity.findOwned(player, BeastStrengthFamily.BEAR) != null,
                "the bear phantom did not show");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void allOutEffortShortensTheCharge(GameTestHelper helper) {
        assertEquals(StoreStrength.CHARGE_TICKS, StoreStrength.getChargeTicks(StrengthService.NO_ALL_OUT));
        assertEquals(56, StoreStrength.getChargeTicks(StrengthService.ALL_OUT_LOWEST_TIER));
        assertEquals(48, StoreStrength.getChargeTicks(StrengthService.ALL_OUT_LOWEST_TIER + 1));
        assertEquals(ALL_OUT_TICKS, StoreStrength.getChargeTicks(StrengthService.ALL_OUT_LOWEST_TIER + 2));

        ServerPlayer player = human(helper, NEAR_STAND);
        assertTrue(StoreStrength.start(player), "the charge did not start");
        GameTestPlayers.serverTicks(player, ALL_OUT_TICKS);
        assertFalse(ChargedPunch.release(player, NO_TARGET), "40 ticks filled the charge with no All-Out Effort");

        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, 2 * StoreStrength.CHARGE_TICKS,
                RANK_FIVE_TIER));
        assertEquals(RANK_FIVE_TIER, StrengthService.getAllOutTier(player), "the rank five tier is not read");
        assertTrue(StoreStrength.start(player), "the charge did not start under All-Out Effort");
        GameTestPlayers.serverTicks(player, ALL_OUT_TICKS - 10);
        assertTrue(StoreStrength.getProgress(player) < 1.0F - 2.0F / ALL_OUT_TICKS, "the charge is already full");
        assertFalse(ChargedPunch.release(player, NO_TARGET), "30 ticks filled the rank five charge");

        assertTrue(StoreStrength.start(player), "the charge did not restart");
        GameTestPlayers.serverTicks(player, ALL_OUT_TICKS);
        assertEquals(1.0F, StoreStrength.getProgress(player), DELTA);
        assertTrue(ChargedPunch.release(player, NO_TARGET), "40 ticks did not fill the rank five charge");
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

        double plain = AttackDamageService.getFistDamage(player, BEAR_MAX_POINTS);
        assertTrue(plain >= BEAR_MAX_POINTS * PathStrengthData.ATTACK_PER_POINT, "the points are missing");
        attack.addTransientModifier(new AttributeModifier(Guzhenren.id("gametest_store_strength_total"), 0.5D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        assertEquals(plain * 1.5D, AttackDamageService.getFistDamage(player, BEAR_MAX_POINTS), DELTA);
        attack.addTransientModifier(new AttributeModifier(Guzhenren.id("gametest_store_strength_weakness"), -100.0D,
                AttributeModifier.Operation.ADD_VALUE));
        assertEquals(0.0D, AttackDamageService.getFistDamage(player, 0), DELTA);
        helper.succeed();
    }

    private static GuItem.Refusal refusal(TendedGuItem item, ServerPlayer player, ItemStack gu) {
        return item.getBehavior(StoreStrengthBehavior.class).checkEffect(item, player, gu);
    }

    private static void assertBacklash(ServerPlayer player, float healthBefore, float recoil) {
        float expected = healthBefore - BodyOverload.getDamage(recoil, healthBefore);
        assertEquals(expected, player.getHealth(), DELTA);
        assertTrue(player.getHealth() >= BodyOverload.HEALTH_FLOOR, "the backlash went under the floor");
        assertTrue(player.getHealth() < healthBefore, "the strike paid no backlash");
        DamageSource source = assertNotNull(player.getLastDamageSource(), () -> "no backlash was recorded");
        assertTrue(source.is(ModDamageTypes.IMPACT_BACKLASH), "the recoil is not the impact backlash");
        assertTrue(source.is(DamageTypeTags.NO_KNOCKBACK), "the impact backlash may knock back");
        assertTrue(player.getDeltaMovement().horizontalDistanceSqr() < SHOVE_SQR,
                "the impact backlash shoved its puncher");
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
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, at);
        villager.getAttribute(Attributes.MAX_HEALTH).setBaseValue(TARGET_HEALTH);
        villager.setHealth(TARGET_HEALTH);
        return villager;
    }
}
