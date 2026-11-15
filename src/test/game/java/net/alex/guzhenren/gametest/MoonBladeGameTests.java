package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.effect.BleedingEffect;
import net.alex.guzhenren.entity.projectile.MoonBladeEntity;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveService;
import net.alex.guzhenren.item.gu.mortal.path.moon.MoonBladeBehavior;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 月刃与流血测试。
 * <ul>
 *   <li>月光蛊[Moonlight Gu]、月芒蛊[Moonglow Gu]、血月蛊[Blood Moon Gu]的月刃对无甲目标打登记伤害，类型
 *       {@code guzhenren:moon_blade}，归射手；仅血月刃留流血 {@link MoonBladeEntity#BLEEDING_TICKS}，每跳 1 点、
 *       无视护甲，类型 {@code guzhenren:bleeding}；牛奶只解流血，不解本模组其他效果。</li>
 *   <li>未命中月刃 {@link MoonBladeEntity#LIFETIME_TICKS} 后消失、不存档；测试中悬停，因场景墙挡真实飞行。</li>
 *   <li>带目标的 {@code MoonBladeEntity.shoot} 弯向视线外 2 格静止目标，也中视线上目标，追横移目标，被中间墙挡下；
 *       无目标月刃直飞越过，不锁定、不倾斜。</li>
 *   <li>服务端校验客户端目标（{@code KillingMoveService.resolveTarget}）：收前方活体；拒身后、本人、无实体 id。</li>
 *   <li>目标为射手南方 3 格的无意识村民；射手朝南（yaw 0），经真实 {@link MoonBladeBehavior} 发射；月刃每 tick 飞 1.5
 *       格，数 tick 内命中，检查前先等 {@link #FLIGHT_TICKS}；不覆盖杀招[Killing Move]系数。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MoonBladeGameTests {

    private static final Vec3 STAND = new Vec3(4.5, 1.0, 4.5);
    private static final BlockPos TARGET = new BlockPos(4, 1, 7);
    /** 视线东侧 2 格：直飞月刃（宽 0.8）打不中，锁定月刃必须命中。 */
    private static final BlockPos OFF_LINE_TARGET = new BlockPos(6, 1, 7);
    private static final int FLIGHT_TICKS = 8;
    private static final float TARGET_HEALTH = 200.0F;
    private static final double EPSILON = 1.0E-6D;

    private MoonBladeGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void moonlightBladeTakesSixAndLeavesNoBleeding(GameTestHelper helper) {
        shootAtTarget(helper, ModItems.MOONLIGHT_GU_1.get(), 6.0F, false);
    }

    @GameTest(template = "empty9x9x9")
    public static void moonglowBladeTakesEighteenAndLeavesNoBleeding(GameTestHelper helper) {
        shootAtTarget(helper, ModItems.MOONGLOW_GU_2.get(), 18.0F, false);
    }

    @GameTest(template = "empty9x9x9")
    public static void bloodMoonBladeTakesFiftyFourAndLeavesBleedingForFiveSeconds(GameTestHelper helper) {
        shootAtTarget(helper, ModItems.BLOOD_MOON_GU_3.get(), 54.0F, true);
    }

    @GameTest(template = "empty9x9x9")
    public static void bleedingPulsesOnTheSecondAndNotBetween(GameTestHelper helper) {
        BleedingEffect bleeding = ModEffects.BLEEDING.get();

        assertTrue(bleeding.isPulseTick(Ticks.SECOND), "a whole second left pulses");
        assertTrue(bleeding.isPulseTick(5 * Ticks.SECOND));
        assertFalse(bleeding.isPulseTick(Ticks.SECOND - 1), "between seconds is quiet");
        assertFalse(bleeding.isPulseTick(Ticks.SECOND + 1));
        assertEquals(1.0, BleedingEffect.DAMAGE_PER_PULSE, 0.0);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void bleedingPulseTakesOnePointPastArmor(GameTestHelper helper) {
        Villager bare = target(helper);
        Villager armored = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(2, 1, 7));
        armored.getAttribute(Attributes.MAX_HEALTH).setBaseValue(TARGET_HEALTH);
        armored.setHealth(TARGET_HEALTH);
        armored.getAttribute(Attributes.ARMOR).setBaseValue(20.0);
        armored.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(12.0);
        assertTrue(armored.getArmorValue() >= 20, "the fixture really wears armor");

        for (Villager villager : List.of(bare, armored)) {
            ModEffects.BLEEDING.get().applyEffectTick(villager, 0);
            assertEquals(1.0, TARGET_HEALTH - villager.getHealth(), 0.0001);
            DamageSource source = assertNotNull(villager.getLastDamageSource(), () -> "no damage was recorded");
            assertTrue(source.is(ModDamageTypes.BLEEDING), "the pulse is Bleeding damage");
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void bleedingPulsesThreeTimesInThreeSeconds(GameTestHelper helper) {
        Villager villager = target(helper);
        villager.addEffect(ModEffects.instance(ModEffects.BLEEDING, 3 * Ticks.SECOND + 1));

        helper.runAfterDelay(2L * Ticks.SECOND + 10, () -> {
            float lost = TARGET_HEALTH - villager.getHealth();
            assertEquals(3.0, lost, 0.0001);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void milkCuresBleedingButNotTheModsOtherEffects(GameTestHelper helper) {
        Villager villager = target(helper);
        villager.addEffect(ModEffects.instance(ModEffects.BLEEDING, 600));
        villager.addEffect(ModEffects.instance(ModEffects.FLOWER_BOAR_GU, 600));

        villager.removeEffectsCuredBy(EffectCures.MILK);

        assertFalse(villager.hasEffect(ModEffects.BLEEDING), "milk clears Bleeding");
        assertTrue(villager.hasEffect(ModEffects.FLOWER_BOAR_GU), "milk spares the mod's other effects");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aMissedBladeVanishesAfterItsLifetimeAndIsNeverSaved(GameTestHelper helper) {
        ServerPlayer player = shooter(helper);
        MoonBladeEntity.shoot(player, MoonBladeEntity.Variant.MOON, 6.0F);
        MoonBladeEntity blade = blades(helper, player).getFirst();
        blade.setDeltaMovement(Vec3.ZERO);
        assertFalse(blade.shouldBeSaved(), "a blade is never written to a chunk");

        helper.runAfterDelay(MoonBladeEntity.LIFETIME_TICKS - 4L, () -> {
            assertFalse(blade.isRemoved(), "still alive before its lifetime ends");
            helper.runAfterDelay(8L, () -> {
                assertTrue(blade.isRemoved(), "gone after its lifetime");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aLockedBladeCurvesToAStillTargetOffTheLine(GameTestHelper helper) {
        lockedShotLands(helper, target(helper, OFF_LINE_TARGET));
    }

    @GameTest(template = "empty9x9x9")
    public static void aLockedBladeHitsAStillTargetOnTheLine(GameTestHelper helper) {
        lockedShotLands(helper, target(helper));
    }

    @GameTest(template = "empty9x9x9")
    public static void aLockedBladeFollowsATargetWalkingSideways(GameTestHelper helper) {
        ServerPlayer player = shooter(helper, new Vec3(1.5, 1.0, 1.5));
        Villager villager = target(helper, new BlockPos(1, 1, 7));
        MoonBladeEntity.shoot(player, MoonBladeEntity.Variant.MOON, 6.0F, villager);
        MoonBladeEntity blade = blades(helper, player).getFirst();
        assertTrue(blade.isLocked(), "the blade is locked on");
        helper.onEachTick(() -> {
            if (!blade.isRemoved()) villager.setPos(villager.getX() + 0.5D, villager.getY(), villager.getZ());
        });

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertTrue(villager.getX() > helper.absoluteVec(new Vec3(2.5D, 0.0D, 0.0D)).x,
                    "the target really walked off the line while the blade flew");
            assertEquals(6.0, TARGET_HEALTH - villager.getHealth(), 0.0001);
            assertTrue(blade.isRemoved(), "the blade ends on the hit");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void anUnlockedBladeFliesStraightPastATargetOffTheLine(GameTestHelper helper) {
        ServerPlayer player = shooter(helper);
        Villager villager = target(helper, OFF_LINE_TARGET);
        MoonBladeEntity.shoot(player, MoonBladeEntity.Variant.MOON, 6.0F);
        MoonBladeEntity blade = blades(helper, player).getFirst();
        double x = blade.getX();
        assertFalse(blade.isLocked(), "no target, no lock");
        assertNull(blade.getArc());
        assertEquals(0.0, blade.getBankDegrees(), 0.0);

        helper.runAfterDelay(2L, () -> {
            assertTrue(Math.abs(blade.getX() - x) < EPSILON, "a straight blade keeps its line");
            assertTrue(blade.getZ() > player.getZ() + 2.0D, "and flies on");
            helper.runAfterDelay(FLIGHT_TICKS - 2L, () -> {
                assertEquals(TARGET_HEALTH, villager.getHealth(), 0.0);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aWallStopsALockedBlade(GameTestHelper helper) {
        for (int x = 1; x <= 7; x++) {
            for (int y = 1; y <= 4; y++) helper.setBlock(new BlockPos(x, y, 6), Blocks.STONE);
        }
        ServerPlayer player = shooter(helper);
        Villager villager = target(helper);
        MoonBladeEntity.shoot(player, MoonBladeEntity.Variant.MOON, 6.0F, villager);
        MoonBladeEntity blade = blades(helper, player).getFirst();
        assertTrue(blade.isLocked(), "the blade is locked on");

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(TARGET_HEALTH, villager.getHealth(), 0.0);
            assertTrue(blade.isRemoved(), "the blade ended on the wall");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void theServerTakesATargetAheadAndRefusesOneBehindOrItself(GameTestHelper helper) {
        ServerPlayer player = shooter(helper);
        Villager ahead = target(helper, OFF_LINE_TARGET);
        Villager behind = target(helper, new BlockPos(4, 1, 1));

        assertTrue(KillingMoveService.resolveTarget(player, ahead.getId()) == ahead, "a target ahead is taken");
        assertNull(KillingMoveService.resolveTarget(player, behind.getId()));
        assertNull(KillingMoveService.resolveTarget(player, player.getId()));
        assertNull(KillingMoveService.resolveTarget(player, KillingMoveService.NO_TARGET));
        assertNull(KillingMoveService.resolveTarget(player, Integer.MAX_VALUE));
        helper.succeed();
    }

    private static void lockedShotLands(GameTestHelper helper, Villager villager) {
        ServerPlayer player = shooter(helper);
        MoonBladeEntity.shoot(player, MoonBladeEntity.Variant.MOON, 6.0F, villager);
        MoonBladeEntity blade = blades(helper, player).getFirst();
        assertTrue(blade.isLocked(), "the blade is locked on");
        assertNotNull(blade.getArc(), () -> "a locked blade flies an arc");
        assertTrue(blade.getBankDegrees() != 0.0F, "a locked blade banks");
        assertTrue(blade.getLifetimeTicks() <= FLIGHT_TICKS, "a shot this short lands within the wait");

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(6.0, TARGET_HEALTH - villager.getHealth(), 0.0001);
            DamageSource source = assertNotNull(villager.getLastDamageSource(), () -> "no damage was recorded");
            assertTrue(source.is(ModDamageTypes.MOON_BLADE), "the hit is Moon Blade damage");
            assertTrue(blade.isRemoved(), "the blade ends on the hit");
            helper.succeed();
        });
    }

    private static void shootAtTarget(GameTestHelper helper, Item item, float damage,
                                      boolean bleeds) {
        ServerPlayer player = shooter(helper);
        Villager villager = target(helper);
        MortalGuItem gu = (MortalGuItem) item;
        MoonBladeBehavior behavior = gu.getBehavior(MoonBladeBehavior.class);
        assertNotNull(behavior, () -> item + " carries no moon blade behavior");
        assertEquals(bleeds, behavior.variant().bleeds(), "the variant matches the Gu");
        assertEquals(damage, behavior.damage(), 0.0);

        behavior.applyEffect(gu, player, new ItemStack(item));

        helper.runAfterDelay(FLIGHT_TICKS, () -> {
            assertEquals(damage, TARGET_HEALTH - villager.getHealth(), 0.0001);
            DamageSource source = assertNotNull(villager.getLastDamageSource(), () -> "no damage was recorded");
            assertTrue(source.is(ModDamageTypes.MOON_BLADE), "the hit is Moon Blade damage");
            assertTrue(source.getEntity() == player, "the shooter owns the hit");
            assertTrue(source.getDirectEntity() instanceof MoonBladeEntity, "the blade is the direct cause");
            MobEffectInstance bleeding = villager.getEffect(ModEffects.BLEEDING);
            if (bleeds) {
                assertNotNull(bleeding, () -> "the Blood Moon Blade leaves Bleeding");
                assertTrue(bleeding.getDuration() <= MoonBladeEntity.BLEEDING_TICKS
                        && bleeding.getDuration() > MoonBladeEntity.BLEEDING_TICKS - FLIGHT_TICKS - 1,
                        "Bleeding lasts five seconds from the hit");
            } else {
                assertFalse(villager.hasEffect(ModEffects.BLEEDING), "a plain blade leaves no Bleeding");
            }
            assertTrue(blades(helper, player).isEmpty(), "the blade ends on the hit");
            helper.succeed();
        });
    }

    private static List<MoonBladeEntity> blades(GameTestHelper helper, ServerPlayer player) {
        // Neighboring tests sit 14 blocks apart, well inside the radius: only this shooter's blades are ours.
        return helper.getLevel().getEntitiesOfClass(MoonBladeEntity.class, player.getBoundingBox().inflate(40.0),
                blade -> blade.getOwner() == player);
    }

    private static Villager target(GameTestHelper helper) { return target(helper, TARGET); }

    private static Villager target(GameTestHelper helper, BlockPos at) {
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, at);
        villager.getAttribute(Attributes.MAX_HEALTH).setBaseValue(TARGET_HEALTH);
        villager.setHealth(TARGET_HEALTH);
        return villager;
    }

    private static ServerPlayer shooter(GameTestHelper helper) { return shooter(helper, STAND); }

    private static ServerPlayer shooter(GameTestHelper helper, Vec3 stand) {
        return GameTestPlayers.survival(helper, "gzr-moon-blade-mock").at(stand);
    }
}
