package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.gu.BoarGuEntity;
import net.alex.guzhenren.entity.gu.CrashGuEntity;
import net.alex.guzhenren.entity.gu.FleePlayerGoal;
import net.alex.guzhenren.entity.gu.HopeGuEntity;
import net.alex.guzhenren.entity.gu.HoverNearPlayerGoal;
import net.alex.guzhenren.entity.gu.LandRestGoal;
import net.alex.guzhenren.entity.gu.RestingFlyingGuEntity;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;

/**
 * 四种冲撞蛊[Crash Gu]实体。
 * <ul>
 *   <li>共用飞行生命周期，及野生飞行蛊各家族继承的防摔与玩家目标释放；{@link MockMode} 为目标运行中可切换的模拟玩家游戏
 *       模式。</li>
 *   <li>{@link #interruptedRestWithoutEscapeResumesFlight} 直驱真实目标生命周期，免随机逃跑点与相邻玩家把逃跑失败变成起
 *       飞；寻路可能失败，故不启逃离目标；威胁离开后普通飞行目标须无需重载或新攻击者即可用。</li>
 *   <li>{@link #requestedLandingTouchesDownBeforeResting} 需开天空：屏障顶正是地面搜索不得误作落点的遮挡，且对高度图遮
 *       住真实地表；两家族共用 {@link LandRestGoal}，自抵达半径外 5 格高出发，须在地表而非初越半径处开始休息；相邻玩家
 *       打断降落时测试重布置。</li>
 *   <li>{@link #landingRefusesWaterAndDamagingGround} 直驱目标：拒绝属目标自身决定，不容随机飞行与相邻玩家先移走
 *       蛊。</li>
 *   <li>{@link #landingFromFlightAltitudeDoesNotKill} 以超 3 格安全摔落距离的真实下降，为各野生飞行蛊家族守住
 *       {@code FlyingGuEntity} 摔落检查覆写；{@code NoAi} 排除目标时序；无覆写时物理路径（{@code move} →
 *       {@code checkFallDamage}）对 1 血小蛊造成 {@code ceil(distance - 3)} 伤害；{@code noAi} 跳过 {@code travel}，故
 *       直调其所用、内含 {@code checkFallDamage} 的 {@code Entity#move}。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BeetleGameTests {


    private BeetleGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void beetleTypesMapToFourCatchItems(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, true);
        for (Variant variant : variants()) {
            CrashGuEntity beetle = spawn(helper, variant.entityType(), CENTER_POS);
            List<Entity> added = GameTestWorld.entitiesAddedBy(helper, () -> helper.assertTrue(
                    beetle.interact(player, InteractionHand.MAIN_HAND).consumesAction(),
                    variant.entityType() + " interaction was not consumed"));
            helper.assertValueEqual(player.getInventory().countItem(variant.caughtItem()), 1,
                    variant.caughtItem() + " caught item missing");
            helper.assertTrue(!beetle.isAlive(), "caught beetle was not removed");
            helper.assertTrue(added.stream().noneMatch(ItemEntity.class::isInstance),
                    "capture spawned an item entity");
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void fullInventoryStillProducesCaughtItem(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, true);
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            inventory.setItem(slot, new ItemStack(Items.STONE, 64));
        }
        Variant variant = variants().getFirst();
        CrashGuEntity beetle = spawn(helper, variant.entityType(), CENTER_POS);
        helper.assertTrue(beetle.interact(player, InteractionHand.MAIN_HAND).consumesAction(),
                "full inventory capture was not consumed");
        helper.succeedWhen(() -> {
            helper.assertTrue(!beetle.isAlive(), "full inventory capture kept the beetle alive");
            helper.assertTrue(helper.getEntities(EntityType.ITEM).stream()
                    .anyMatch(item -> item.getItem().is(variant.caughtItem())),
                    "full inventory capture lost its item");
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void beetleDeathPlaysOutBeforeRemoval(GameTestHelper helper) {
        CrashGuEntity beetle = spawn(helper, variants().get(3).entityType(), CENTER_POS);
        GameTestWorld.Drops drops = GameTestWorld.recordDrops(helper, beetle.getBoundingBox().inflate(0.5D));
        helper.assertTrue(beetle.hurt(helper.getLevel().damageSources().generic(), 2.0F),
                "beetle did not accept lethal damage");
        helper.assertTrue(beetle.isDeadOrDying(), "lethal damage did not kill the beetle");
        helper.runAtTickTime(CrashGuEntity.DEATH_REMOVE_TICK - 2L,
                () -> helper.assertTrue(!beetle.isRemoved(), "beetle was removed before its death animation ended"));
        helper.runAtTickTime(CrashGuEntity.DEATH_REMOVE_TICK + 1L, () -> {
            helper.assertTrue(beetle.isRemoved(), "dead beetle was never removed");
            drops.stop().run();
            helper.assertTrue(drops.joined().stream().noneMatch(ItemEntity.class::isInstance),
                    "beetle death produced an item drop");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void beetleDeathProducesNoDrops(GameTestHelper helper) {
        for (Variant variant : variants()) {
            CrashGuEntity beetle = spawn(helper, variant.entityType(), CENTER_POS);
            List<Entity> added = GameTestWorld.entitiesAddedBy(helper,
                    () -> beetle.hurt(helper.getLevel().damageSources().generic(), 2.0F));
            helper.assertTrue(!beetle.isAlive(), "beetle death did not remove the entity");
            helper.assertTrue(added.stream().noneMatch(ItemEntity.class::isInstance),
                    "beetle death produced an item drop");
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void beetleRegistrationUsesSharedFlightShapeAndSpeed(GameTestHelper helper) {
        CrashGuEntity beetle = spawn(helper, variants().getFirst().entityType(), CENTER_POS);
        helper.assertValueEqual(beetle.getMaxHealth(), 1.0F, "beetle max health changed");
        helper.assertValueEqual(beetle.getBbWidth(), 0.4F, "beetle width changed");
        helper.assertValueEqual(beetle.getBbHeight(), 0.4F, "beetle height changed");
        helper.assertValueEqual(beetle.getAttributeValue(Attributes.FLYING_SPEED), 0.3D,
                "beetle flying speed changed");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void beetleFleeGoalUsesSixBlockThreatAndTenBlockRelease(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, true);
        CrashGuEntity beetle = spawn(helper, variants().getFirst().entityType(), CENTER_POS);
        movePlayerRelativeTo(player, beetle, 5.0D);
        FleePlayerGoal goal = new FleePlayerGoal(beetle);
        helper.assertTrue(goal.canUse(), "a threat inside six blocks did not start the flee goal");
        goal.start();

        movePlayerRelativeTo(player, beetle, 9.9D);
        helper.assertTrue(goal.canContinueToUse(), "flee goal stopped before the threat escaped ten blocks");
        movePlayerRelativeTo(player, beetle, 10.1D);
        helper.assertTrue(!goal.canContinueToUse(), "flee goal continued after the threat escaped ten blocks");
        goal.stop();

        movePlayerRelativeTo(player, beetle, 6.1D);
        helper.assertTrue(!new FleePlayerGoal(beetle).canUse(),
                "a threat outside six blocks started the flee goal");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void fleeGoalReleasesAPlayerWhoStopsBeingAThreat(GameTestHelper helper) {
        MockMode mode = new MockMode();
        ServerPlayer player = survivalMock(helper, true, mode);
        CrashGuEntity beetle = spawn(helper, variants().getFirst().entityType(), CENTER_POS);
        beetle.setNoAi(true);
        movePlayerRelativeTo(player, beetle, 5.0D);
        FleePlayerGoal goal = new FleePlayerGoal(beetle);
        helper.assertTrue(goal.canUse(), "a survival player inside six blocks did not start the flee goal");
        goal.start();
        mode.creative = true;
        helper.assertTrue(!goal.canContinueToUse(), "flee goal kept running from a player who switched to creative");
        goal.stop();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void hoverGoalReleasesAPlayerWhoTurnsSpectator(GameTestHelper helper) {
        MockMode mode = new MockMode();
        ServerPlayer player = survivalMock(helper, true, mode);
        HopeGuEntity hope = helper.spawn(ModEntityTypes.HOPE_GU_ENTITY.get(), CENTER_POS);
        hope.setNoAi(true);
        player.moveTo(hope.getX() + 5.0D, hope.getY(), hope.getZ(), 0.0F, 0.0F);
        HoverNearPlayerGoal goal = new HoverNearPlayerGoal(hope);
        helper.assertTrue(goal.canUse(), "an unawakened player inside twelve blocks did not start the hover goal");
        goal.start();
        mode.spectator = true;
        helper.assertTrue(!goal.canContinueToUse(), "hover goal kept following a player who turned spectator");
        goal.stop();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 200)
    public static void restingBeetleTakesOffWhenThreatened(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, true);
        CrashGuEntity beetle = spawn(helper, variants().getFirst().entityType(), CENTER_POS);
        movePlayerRelativeTo(player, beetle, 3.0D);
        beetle.beginResting();
        helper.succeedWhen(() -> helper.assertValueEqual(beetle.getPhase(),
                RestingFlyingGuEntity.FlightPhase.FLYING, "resting beetle did not take off for a nearby threat"));
    }

    @GameTest(template = "empty9x9x9")
    public static void interruptedRestWithoutEscapeResumesFlight(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper, true);
        List<RestingFlyingGuEntity> entities = List.of(
                helper.spawn(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), CENTER_POS),
                spawn(helper, variants().getFirst().entityType(), CENTER_POS));
        try {
            for (RestingFlyingGuEntity gu : entities) {
                gu.setNoAi(true);
                gu.setOnGround(true);
                LandRestGoal rest = new LandRestGoal(gu);
                rest.start();
                rest.tick();
                helper.assertValueEqual(gu.getPhase(), RestingFlyingGuEntity.FlightPhase.RESTING,
                        "normal landing did not enter resting");
                helper.assertTrue(!gu.isWantingToLand(), "normal rest retained a landing request");
                player.moveTo(gu.position().add(3.0D, 0.0D, 0.0D));
                helper.assertTrue(!rest.canContinueToUse(), "nearby threat did not interrupt rest");
                rest.stop();
                player.moveTo(gu.position().add(20.0D, 0.0D, 0.0D));
                helper.assertValueEqual(gu.getPhase(), RestingFlyingGuEntity.FlightPhase.FLYING,
                        "interrupted rest stayed stuck when escape did not start");
                helper.assertTrue(!gu.isWantingToLand(), "interrupted rest retained a landing request");
            }
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void restingGuTakesOffWhenItsGroundIsBroken(GameTestHelper helper) {
        helper.setBlock(CENTER_POS.below(), Blocks.STONE);
        BoarGuEntity gu = helper.spawn(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), CENTER_POS);
        gu.setNoAi(true);
        gu.setOnGround(true);
        LandRestGoal rest = new LandRestGoal(gu);
        rest.start();
        rest.tick();
        rest.tick();
        helper.assertValueEqual(gu.getPhase(), RestingFlyingGuEntity.FlightPhase.RESTING,
                "a Gu resting on intact ground did not stay at rest");
        helper.setBlock(CENTER_POS.below(), Blocks.AIR);
        rest.tick();
        helper.assertValueEqual(gu.getPhase(), RestingFlyingGuEntity.FlightPhase.FLYING,
                "a resting Gu stayed in the air after its ground was broken");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void restingGuBodyFollowsItsHeadTurnWhole(GameTestHelper helper) {
        BoarGuEntity gu = helper.spawn(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), CENTER_POS);
        gu.setNoAi(true);
        float turned = gu.yBodyRot + 90.0F;
        gu.setYHeadRot(turned);
        gu.tick();
        helper.assertTrue(Math.abs(Mth.wrapDegrees(gu.yBodyRot - turned)) < 1.0F,
                "the body lagged a 90 degree head turn, so a resting turn would not show: body " + gu.yBodyRot
                        + ", head " + turned);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 260)
    public static void savedBeetleResumesFlightLifecycleAfterReload(GameTestHelper helper) {
        Variant variant = variants().getFirst();
        helper.setBlock(CENTER_POS.below(), Blocks.STONE);
        CrashGuEntity original = spawn(helper, variant.entityType(), CENTER_POS);
        original.beginResting();
        CompoundTag saved = new CompoundTag();
        original.saveWithoutId(saved);
        original.discard();
        helper.assertTrue(!original.isAlive(), "saved source beetle was not removed");

        CrashGuEntity restored = spawn(helper, variant.entityType(), new BlockPos(6, 1, 6));
        restored.load(saved);
        helper.succeedWhen(() -> {
            helper.assertTrue(restored.isAlive(), "reloaded beetle was removed");
            helper.assertValueEqual(restored.getPhase(), RestingFlyingGuEntity.FlightPhase.FLYING,
                    "reloaded beetle did not resume and leave its saved landing lifecycle");
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 300)
    public static void requestedLandingTouchesDownBeforeResting(GameTestHelper helper) {
        GameTestWorld.openSky(helper);
        List<BlockPos> columns = List.of(new BlockPos(2, 6, 2), new BlockPos(6, 6, 6));
        List<RestingFlyingGuEntity> entities = List.of(
                helper.spawn(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), columns.get(0)),
                spawn(helper, variants().getFirst().entityType(), columns.get(1)));
        for (int i = 0; i < entities.size(); i++) {
            helper.setBlock(columns.get(i).atY(0), Blocks.STONE);
            entities.get(i).requestLanding();
        }
        helper.succeedWhen(() -> {
            for (int i = 0; i < entities.size(); i++) {
                RestingFlyingGuEntity gu = entities.get(i);
                if (gu.getPhase() == RestingFlyingGuEntity.FlightPhase.FLYING && !gu.isWantingToLand()) gu.requestLanding();
                helper.assertValueEqual(gu.getPhase(), RestingFlyingGuEntity.FlightPhase.RESTING,
                        gu.getType() + " never came to rest");
                double clearance = gu.getY() - helper.absolutePos(columns.get(i).atY(1)).getY();
                helper.assertTrue(clearance < 0.01D, gu.getType() + " rests " + clearance + " blocks above the ground");
            }
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void landingRefusesWaterAndDamagingGround(GameTestHelper helper) {
        GameTestWorld.openSky(helper);
        BlockPos pond = new BlockPos(2, 1, 2);
        helper.setBlock(pond.below(), Blocks.STONE);
        for (BlockPos wall : List.of(pond.north(), pond.south(), pond.east(), pond.west())) {
            helper.setBlock(wall, Blocks.STONE);
        }
        helper.setBlock(pond, Blocks.WATER);
        BlockPos magma = new BlockPos(6, 0, 6);
        helper.setBlock(magma, Blocks.MAGMA_BLOCK);
        List<RestingFlyingGuEntity> entities = List.of(
                helper.spawn(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), pond.atY(6)),
                spawn(helper, variants().getFirst().entityType(), magma.atY(6)));
        for (RestingFlyingGuEntity gu : entities) {
            gu.setNoAi(true);
            new LandRestGoal(gu).start();
            helper.assertValueEqual(gu.getPhase(), RestingFlyingGuEntity.FlightPhase.FLYING,
                    gu.getType() + " started landing on water or magma");
            helper.assertTrue(!gu.isWantingToLand(), gu.getType() + " kept its landing request over unsafe ground");
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void reloadedRestResumesWithoutReplayingTheLanding(GameTestHelper helper) {
        GameTestWorld.openSky(helper);
        helper.setBlock(CENTER_POS.below(), Blocks.STONE);
        BoarGuEntity original = helper.spawn(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), CENTER_POS);
        original.beginResting();
        CompoundTag saved = new CompoundTag();
        original.saveWithoutId(saved);
        original.discard();

        int[] landings = { 0 };
        BoarGuEntity restored = new BoarGuEntity(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), helper.getLevel(),
                ModItems.WHITE_BOAR_GU_1) {
            @Override
            protected void playLandingAnimation() { landings[0]++; }
        };
        restored.load(saved);
        helper.getLevel().addFreshEntity(restored);
        helper.runAfterDelay(10, () -> {
            helper.assertValueEqual(landings[0], 0, "reloaded rest replayed its landing animation");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void onlyAGroundedTakeoffPlaysTheLift(GameTestHelper helper) {
        int[] lifts = { 0 };
        BoarGuEntity gu = new BoarGuEntity(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), helper.getLevel(),
                ModItems.WHITE_BOAR_GU_1) {
            @Override
            protected void playLandingAnimation() {}

            @Override
            protected void playTakeoffAnimation() { lifts[0]++; }
        };
        gu.beginLanding();
        gu.takeOff();
        helper.assertValueEqual(lifts[0], 0,
                "an aborted landing replayed the lift from closed wing cases");
        gu.beginLanding();
        gu.beginResting();
        gu.takeOff();
        helper.assertValueEqual(lifts[0], 1, "a takeoff from rest did not play the lift");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 300)
    public static void landingFromFlightAltitudeDoesNotKill(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 0, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(6, 0, 6), Blocks.STONE);
        HopeGuEntity hope = helper.spawn(ModEntityTypes.HOPE_GU_ENTITY.get(), new BlockPos(2, 7, 2));
        BoarGuEntity boar = helper.spawn(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), new BlockPos(4, 7, 4));
        CrashGuEntity beetle =
                spawn(helper, ModEntityTypes.HORIZONTAL_CRASH_GU_ENTITY.get(), new BlockPos(6, 7, 6));
        hope.setNoAi(true);
        boar.setNoAi(true);
        beetle.setNoAi(true);
        double startY = hope.getY();
        helper.succeedWhen(() -> {
            for (Mob gu : List.of(hope, boar, beetle)) {
                if (!gu.onGround()) gu.move(MoverType.SELF, new Vec3(0.0D, -0.3D, 0.0D));
            }
            helper.assertTrue(hope.isAlive(), "hope Gu died landing from flight altitude");
            helper.assertTrue(boar.isAlive(), "boar died landing from flight altitude");
            helper.assertTrue(beetle.isAlive(), "beetle died landing from flight altitude");
            helper.assertTrue(hope.onGround() && boar.onGround() && beetle.onGround(),
                    "descent never finished: hope=" + probe(hope) + " boar=" + probe(boar)
                            + " beetle=" + probe(beetle));
            helper.assertTrue(hope.getY() < startY - 3.0D && boar.getY() < startY - 3.0D
                    && beetle.getY() < startY - 3.0D, "descent did not clear the safe-fall distance");
        });
    }

    private static String probe(Mob gu) {
        return "pos=" + gu.blockPosition() + " vel=" + gu.getDeltaMovement() + " noAi=" + gu.isNoAi()
                + " ticks=" + gu.tickCount;
    }

    private static List<Variant> variants() {
        return List.of(
                new Variant(ModEntityTypes.HORIZONTAL_CRASH_GU_ENTITY.get(), ModItems.HORIZONTAL_CRASH_GU_3.get()),
                new Variant(ModEntityTypes.VERTICAL_CRASH_GU_ENTITY.get(), ModItems.VERTICAL_CRASH_GU_3.get()),
                new Variant(ModEntityTypes.CHARGING_CRASH_GU_4_ENTITY.get(), ModItems.CHARGING_CRASH_GU_4.get()),
                new Variant(ModEntityTypes.CHARGING_CRASH_GU_5_ENTITY.get(), ModItems.CHARGING_CRASH_GU_5.get()));
    }

    private static CrashGuEntity spawn(GameTestHelper helper,
                                                   EntityType<CrashGuEntity> type, BlockPos pos) {
        return helper.spawn(type, pos);
    }

    private static void movePlayerRelativeTo(ServerPlayer player, CrashGuEntity beetle,
                                              double distance) {
        player.moveTo(beetle.getX() + distance, beetle.getY(), beetle.getZ(), 0.0F, 0.0F);
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private record Variant(EntityType<CrashGuEntity> entityType, Item caughtItem) {}

    private static final class MockMode {

        private boolean creative;
        private boolean spectator;
    }

    private static ServerPlayer survivalMock(GameTestHelper helper, boolean connect) {
        return survivalMock(helper, connect, new MockMode());
    }

    private static ServerPlayer survivalMock(GameTestHelper helper, boolean connect, MockMode mode) {
        GameTestPlayers.Mock mock = GameTestPlayers.survival(helper, "gzr-beetle-gametest")
                .creative(() -> mode.creative).spectator(() -> mode.spectator);
        if (!connect) return mock.unconnected().build();
        ServerPlayer player = mock.build();
        player.moveTo(helper.absoluteVec(new Vec3(4.5D, 1.5D, 4.5D)));
        player.setNoGravity(true);
        return player;
    }
}
