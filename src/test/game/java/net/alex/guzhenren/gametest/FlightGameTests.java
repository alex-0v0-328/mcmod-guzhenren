package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.flight.FlightAbility;
import net.alex.guzhenren.gameplay.flight.FlightGrant;
import net.alex.guzhenren.gameplay.flight.FlightService;
import net.alex.guzhenren.gameplay.flight.SecondStageCruise;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER;
import static net.alex.guzhenren.gametest.GameTestPlayers.ground;
import static net.alex.guzhenren.gametest.GameTestPlayers.place;

/**
 * 服务端飞行与二段飞行[Second-Stage Flight]：谁能进第二段、何事结束、停在何处。
 * <ul>
 *   <li>模拟玩家创造标志每次重读，可中途撤销；{@code Abilities#mayfly} 默认 {@code false}（测试另设除外），代表游戏模式
 *       自带飞行；飞行、疾跑手动设，代表客户端状态。</li>
 *   <li>撞毁即一次 {@code SecondStageService#travel} 步进撞上不足巡航速度距离的墙、地或顶。</li>
 *   <li>蛊途径（{@code FlightContributor} 效果）暂无效果可驱，规则（取最强授权）归纯测试 {@code FlightGrantTest}；
 *       GameTest 服务端无宝黄天[Treasure Yellow Heaven]维度（见 {@link TreasureYellowHeavenGameTests}）。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FlightGameTests {
    private static final Vec3 WEST = new Vec3(1.5D, 1.0D, 4.5D);
    private static final Vec3 BODY = new Vec3(3.0D, 1.0D, 4.5D);
    private static final Vec3 ALOFT = new Vec3(4.5D, 2.5D, 4.5D);
    private static final int WALL_X = 6;
    private static final int FLOOR_Y = 0;
    private static final int CEILING_Y = 4;
    private static final BlockPos STRUCK = new BlockPos(WALL_X, 1, 4);
    private static final BlockPos STRUCK_ASIDE = new BlockPos(WALL_X, 1, 0);
    private static final BlockPos BELOW = new BlockPos(4, FLOOR_Y, 4);
    private static final BlockPos ABOVE = new BlockPos(4, CEILING_Y, 4);
    private static final double CRASH_SPEED = 2.0D;
    private static final int ENTRY_COAST_TICKS = 5;

    private FlightGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void creativeSprintFlightEntersTheSecondStage(GameTestHelper helper) {
        ServerPlayer player = sprintFlying(creative(helper, new boolean[] { true }));

        helper.assertTrue(FlightService.getGrant(player) == FlightGrant.SECOND_STAGE,
                "creative must grant the second stage");
        helper.assertTrue(SecondStageService.start(player), "a sprint-flying creative player was refused");
        helper.assertTrue(SecondStageService.isActive(player), "second-stage mark missing after the entry");
        helper.assertTrue(!SecondStageService.start(player), "a second start entered twice");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void enteringNeedsFlightAndSprint(GameTestHelper helper) {
        ServerPlayer player = creative(helper, new boolean[] { true });

        player.setSprinting(true);
        helper.assertTrue(!SecondStageService.start(player), "a player on foot entered the second stage");
        player.getAbilities().flying = true;
        player.setSprinting(false);
        helper.assertTrue(!SecondStageService.start(player), "a hovering player entered the second stage");
        SecondStageService.toggle(player);
        helper.assertTrue(!SecondStageService.isActive(player), "a toggle entered without a sprint");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void survivalWithoutGuCannotEnter(GameTestHelper helper) {
        ServerPlayer player = sprintFlying(survival(helper));

        helper.assertTrue(FlightService.getGrant(player) == FlightGrant.NONE, "survival without a Gu got a grant");
        helper.assertTrue(!SecondStageService.start(player), "survival without a Gu entered the second stage");
        helper.assertTrue(!FlightAbility.canFly(player), "a survival player without a Gu may fly");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void spectatorCannotEnter(GameTestHelper helper) {
        ServerPlayer player = sprintFlying(GameTestPlayers.survival(helper, "gzr-flight-gametest")
                .creative(() -> true).spectator(() -> true).build());

        helper.assertTrue(!SecondStageService.start(player), "a spectator entered the second stage");
        helper.assertTrue(!SecondStageService.isActive(player), "a spectator carries the second-stage mark");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void riderCannotEnter(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = sprintFlying(creative(helper, new boolean[] { true }));
        Minecart cart = EntityType.MINECART.create(helper.getLevel());
        helper.assertTrue(cart != null, "minecart not created");
        Vec3 position = helper.absoluteVec(CENTER);
        cart.moveTo(position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(cart);
        helper.assertTrue(player.startRiding(cart, true), "the player could not board the minecart");

        helper.assertTrue(!SecondStageService.start(player), "a rider entered the second stage");
        player.stopRiding();
        player.discard();
        cart.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void stoppingTheSprintCoastsThenEndsTheStage(GameTestHelper helper) {
        ServerPlayer player = sprintFlying(creative(helper, new boolean[] { true }));
        SecondStageService.start(player);

        player.setSprinting(false);
        for (int tick = 1; tick < ENTRY_COAST_TICKS; tick++) SecondStageService.tick(player);
        helper.assertTrue(SecondStageService.isActive(player), "the second stage ended before the throttle ran dry");
        SecondStageService.tick(player);

        helper.assertTrue(!SecondStageService.isActive(player), "the second stage outlived the drained throttle");
        helper.assertTrue(PlayerData.get(player).getSecondStageThrottle() == 0.0D, "the throttle outlived the stage");
        helper.assertTrue(player.getAbilities().flying, "leaving the second stage ended the flight");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void entryStartsTheThrottleAtSprintFlight(GameTestHelper helper) {
        ServerPlayer player = sprintFlying(creative(helper, new boolean[] { true }));
        SecondStageService.start(player);

        double entry = PlayerData.get(player).getSecondStageThrottle();
        helper.assertTrue(Math.abs(entry - SecondStageCruise.ENTRY_THROTTLE) < 1.0E-9D,
                "an entry set the throttle to " + entry);
        SecondStageService.tick(player);
        double sprinted = PlayerData.get(player).getSecondStageThrottle();
        helper.assertTrue(Math.abs(sprinted - entry - 1.0D / SecondStageCruise.RAMP_TICKS) < 1.0E-9D,
                "a sprinting tick moved the throttle to " + sprinted);
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void cruiseFollowsTheLook(GameTestHelper helper) {
        ServerPlayer player = facingEast(sprintFlying(creative(helper, new boolean[] { true })));
        helper.assertTrue(!SecondStageService.travel(player), "travel took over outside the stage");
        SecondStageService.start(player);
        PlayerData.get(player).setSecondStageThrottle(1.0D);
        double startX = player.getX();

        helper.assertTrue(SecondStageService.travel(player), "travel did not take over in the stage");

        double step = SecondStageCruise.STEERING
                * SecondStageCruise.getTopSpeed(player.getAbilities().getFlyingSpeed());
        helper.assertTrue(Math.abs(player.getDeltaMovement().x - step) < 1.0E-6D
                        && Math.abs(player.getDeltaMovement().y) < 1.0E-6D
                        && Math.abs(player.getDeltaMovement().z) < 1.0E-6D,
                "the cruise velocity was " + player.getDeltaMovement());
        helper.assertTrue(Math.abs(player.getX() - startX - step) < 1.0E-6D, "the cruise did not move the player");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void wallCrashHurtsAndEndsTheStage(GameTestHelper helper) {
        wall(helper, Blocks.STONE);
        ServerPlayer player = facingEast(sprintFlying(GameTestPlayers.survival(helper, "gzr-flight-gametest")
                .creative(() -> true).pastSpawnInvulnerability().build()));
        place(player, helper, CENTER);
        player.setYRot(-90.0F);
        SecondStageService.start(player);
        cruiseAt(player, CRASH_SPEED);

        SecondStageService.travel(player);

        float expected = player.getMaxHealth() - SecondStageCruise.getWallDamage(CRASH_SPEED);
        helper.assertTrue(Math.abs(player.getHealth() - expected) < 1.0E-3F,
                "the crash left " + player.getHealth() + " health, expected " + expected);
        helper.assertTrue(!SecondStageService.isActive(player), "the crash did not end the stage");
        helper.assertTrue(player.getDeltaMovement().equals(Vec3.ZERO), "the crash did not stop the flier");
        helper.assertTrue(player.getAbilities().flying, "the crash ended the flight");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void crashCratersTheWall(GameTestHelper helper) {
        wall(helper, Blocks.DIRT);
        ServerPlayer player = facingEast(sprintFlying(creative(helper, new boolean[] { true })));
        SecondStageService.start(player);
        cruiseAt(player, CRASH_SPEED);

        SecondStageService.travel(player);

        helper.assertTrue(helper.getBlockState(STRUCK).isAir(), "the crash left the wall whole");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void griefingOffKeepsTheWallButStillHurts(GameTestHelper helper) {
        wall(helper, Blocks.DIRT);
        ServerPlayer player = facingEast(sprintFlying(GameTestPlayers.survival(helper, "gzr-flight-gametest")
                .creative(() -> true).pastSpawnInvulnerability().build()));
        place(player, helper, CENTER);
        player.setYRot(-90.0F);
        SecondStageService.start(player);
        cruiseAt(player, CRASH_SPEED);
        GameRules.BooleanValue griefing = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean was = griefing.get();
        try {
            griefing.set(false, helper.getLevel().getServer());
            SecondStageService.travel(player);
        } finally {
            griefing.set(was, helper.getLevel().getServer());
        }

        helper.assertTrue(helper.getBlockState(STRUCK).is(Blocks.DIRT), "mobGriefing off still cratered the wall");
        helper.assertTrue(player.getHealth() < player.getMaxHealth(), "mobGriefing off spared the wall damage");
        helper.assertTrue(!SecondStageService.isActive(player), "the crash did not end the stage");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void diveCrashCratersTheFloorAndEndsTheStage(GameTestHelper helper) {
        layer(helper, FLOOR_Y, Blocks.DIRT);
        ServerPlayer player = sprintFlying(hurtable(helper));
        place(player, helper, ALOFT);
        player.setXRot(90.0F);
        SecondStageService.start(player);
        cruiseAt(player, new Vec3(0.0D, -CRASH_SPEED, 0.0D));

        SecondStageService.travel(player);

        float expected = player.getMaxHealth() - SecondStageCruise.getWallDamage(CRASH_SPEED);
        helper.assertTrue(Math.abs(player.getHealth() - expected) < 1.0E-3F,
                "the dive left " + player.getHealth() + " health, expected " + expected);
        helper.assertTrue(!SecondStageService.isActive(player), "the dive did not end the stage");
        helper.assertTrue(player.getDeltaMovement().equals(Vec3.ZERO), "the dive did not stop the flier");
        helper.assertTrue(helper.getBlockState(BELOW).isAir(), "the dive left the floor whole");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void climbCrashCratersTheCeiling(GameTestHelper helper) {
        layer(helper, CEILING_Y, Blocks.DIRT);
        ServerPlayer player = sprintFlying(creative(helper, new boolean[] { true }));
        player.setXRot(-90.0F);
        SecondStageService.start(player);
        cruiseAt(player, new Vec3(0.0D, CRASH_SPEED, 0.0D));

        SecondStageService.travel(player);

        helper.assertTrue(!SecondStageService.isActive(player), "the climb did not end the stage");
        helper.assertTrue(helper.getBlockState(ABOVE).isAir(), "the climb left the ceiling whole");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void fastFlightShattersGlassAndKeepsTheStage(GameTestHelper helper) {
        wall(helper, Blocks.GLASS);
        ServerPlayer player = facingEast(sprintFlying(hurtable(helper)));
        SecondStageService.start(player);
        cruiseAt(player, CRASH_SPEED);

        SecondStageService.travel(player);

        helper.assertTrue(helper.getBlockState(STRUCK).isAir(), "the glass in the path stayed whole");
        helper.assertTrue(helper.getBlockState(STRUCK_ASIDE).is(Blocks.GLASS), "glass off the path broke");
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "the glass hurt the flier");
        helper.assertTrue(SecondStageService.isActive(player), "the glass ended the stage");
        helper.assertTrue(Math.abs(player.getDeltaMovement().x - CRASH_SPEED) < 1.0E-6D, "the glass slowed the flier");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void slowFlightShattersNothing(GameTestHelper helper) {
        wall(helper, Blocks.GLASS);
        ServerPlayer player = facingEast(sprintFlying(hurtable(helper)));
        SecondStageService.start(player);
        cruiseAt(player, SecondStageCruise.BARRIER_SPEED - 0.1D);

        SecondStageService.travel(player);

        helper.assertTrue(helper.getBlockState(STRUCK).is(Blocks.GLASS), "a flight below the barrier broke glass");
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "stopping against glass hurt the flier");
        helper.assertTrue(SecondStageService.isActive(player), "stopping against glass ended the stage");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void griefingOffKeepsTheGlassAndSparesTheFlier(GameTestHelper helper) {
        wall(helper, Blocks.GLASS);
        ServerPlayer player = facingEast(sprintFlying(hurtable(helper)));
        SecondStageService.start(player);
        cruiseAt(player, CRASH_SPEED);
        GameRules.BooleanValue griefing = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean was = griefing.get();
        try {
            griefing.set(false, helper.getLevel().getServer());
            SecondStageService.travel(player);
        } finally {
            griefing.set(was, helper.getLevel().getServer());
        }

        helper.assertTrue(helper.getBlockState(STRUCK).is(Blocks.GLASS), "mobGriefing off still shattered the glass");
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "stopping against glass hurt the flier");
        helper.assertTrue(SecondStageService.isActive(player), "stopping against glass counted as a crash");
        helper.assertTrue(PlayerData.get(player).getSecondStageThrottle() > 0.0D,
                "stopping against glass emptied the throttle");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void ramHurtsAndShovesOncePerSecond(GameTestHelper helper) {
        ServerPlayer player = facingEast(sprintFlying(creative(helper, new boolean[] { true })));
        place(player, helper, WEST);
        player.setYRot(-90.0F);
        Pig pig = idlePig(helper);
        SecondStageService.start(player);
        cruiseAt(player, CRASH_SPEED);

        SecondStageService.travel(player);

        float expected = pig.getMaxHealth() - SecondStageCruise.getRamDamage(CRASH_SPEED);
        helper.assertTrue(Math.abs(pig.getHealth() - expected) < 1.0E-3F,
                "the ram left " + pig.getHealth() + " health, expected " + expected);
        Vec3 push = pig.getDeltaMovement();
        helper.assertTrue(Math.abs(push.x - SecondStageCruise.RAM_KNOCKBACK) < 1.0E-6D
                && Math.abs(push.y - SecondStageCruise.RAM_LIFT) < 1.0E-6D, "the ram shoved " + push);
        helper.assertTrue(Math.abs(player.getDeltaMovement().x - CRASH_SPEED) < 1.0E-6D, "the ram slowed the flier");

        pig.invulnerableTime = 0;
        place(player, helper, WEST);
        player.setYRot(-90.0F);
        cruiseAt(player, CRASH_SPEED);
        SecondStageService.travel(player);
        helper.assertTrue(Math.abs(pig.getHealth() - expected) < 1.0E-3F, "the same body was rammed twice");
        player.discard();
        pig.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void ramSparesOwnPets(GameTestHelper helper) {
        ServerPlayer player = facingEast(sprintFlying(creative(helper, new boolean[] { true })));
        place(player, helper, WEST);
        player.setYRot(-90.0F);
        Wolf wolf = helper.spawn(EntityType.WOLF, BODY);
        wolf.setNoAi(true);
        wolf.tame(player);
        SecondStageService.start(player);
        cruiseAt(player, CRASH_SPEED);

        SecondStageService.travel(player);

        helper.assertTrue(wolf.getHealth() == wolf.getMaxHealth(), "the flier rammed its own wolf");
        helper.assertTrue(wolf.getDeltaMovement().lengthSqr() < 1.0E-6D, "the flier shoved its own wolf");
        player.discard();
        wolf.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void refusedRamLeavesTheBodyInPlace(GameTestHelper helper) {
        ServerPlayer player = facingEast(sprintFlying(creative(helper, new boolean[] { true })));
        place(player, helper, WEST);
        player.setYRot(-90.0F);
        Pig pig = idlePig(helper);
        pig.setInvulnerable(true);
        SecondStageService.start(player);
        cruiseAt(player, CRASH_SPEED);

        SecondStageService.travel(player);

        helper.assertTrue(pig.getHealth() == pig.getMaxHealth(), "an invulnerable body took the ram");
        helper.assertTrue(pig.getDeltaMovement().lengthSqr() < 1.0E-6D, "a refused ram still shoved the body");
        player.discard();
        pig.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void slowFlightRamsNothing(GameTestHelper helper) {
        ServerPlayer player = facingEast(sprintFlying(creative(helper, new boolean[] { true })));
        place(player, helper, WEST);
        player.setYRot(-90.0F);
        Pig pig = idlePig(helper);
        SecondStageService.start(player);
        cruiseAt(player, SecondStageCruise.BARRIER_SPEED - 0.1D);

        SecondStageService.travel(player);

        helper.assertTrue(pig.getHealth() == pig.getMaxHealth(), "a flight below the barrier rammed");
        player.discard();
        pig.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void landingEndsTheStage(GameTestHelper helper) {
        ServerPlayer player = sprintFlying(creative(helper, new boolean[] { true }));
        SecondStageService.start(player);

        player.getAbilities().flying = false;
        SecondStageService.tick(player);

        helper.assertTrue(!SecondStageService.isActive(player), "the second stage outlived the flight");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void secondToggleReturnsToSprintFlight(GameTestHelper helper) {
        ServerPlayer player = sprintFlying(creative(helper, new boolean[] { true }));

        SecondStageService.toggle(player);
        helper.assertTrue(SecondStageService.isActive(player), "the first toggle did not enter");
        SecondStageService.toggle(player);
        SecondStageService.tick(player);

        helper.assertTrue(!SecondStageService.isActive(player), "the second toggle did not leave");
        helper.assertTrue(player.getAbilities().flying && player.isSprinting(), "leaving dropped the sprint flight");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void everyEntryStartsAgainAfterAStop(GameTestHelper helper) {
        ServerPlayer player = sprintFlying(creative(helper, new boolean[] { true }));
        SecondStageService.start(player);
        player.setSprinting(false);
        for (int tick = 0; tick < ENTRY_COAST_TICKS; tick++) SecondStageService.tick(player);

        player.setSprinting(true);
        helper.assertTrue(SecondStageService.start(player), "a stopped player could not enter again");
        helper.assertTrue(SecondStageService.isActive(player), "the second entry left no mark");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void losingTheGrantEndsTheStage(GameTestHelper helper) {
        boolean[] creative = { true };
        ServerPlayer player = sprintFlying(creative(helper, creative));
        SecondStageService.start(player);

        creative[0] = false;
        SecondStageService.tick(player);

        helper.assertTrue(!SecondStageService.isActive(player), "the second stage outlived its grant");
        player.discard();
        helper.succeed();
    }

    private static ServerPlayer facingEast(ServerPlayer player) {
        player.setYRot(-90.0F);
        player.setXRot(0.0F);
        return player;
    }

    private static void cruiseAt(ServerPlayer player, double speed) { cruiseAt(player, new Vec3(speed, 0.0D, 0.0D)); }

    private static void cruiseAt(ServerPlayer player, Vec3 velocity) {
        PlayerData.get(player).setSecondStageThrottle(
                velocity.length() / SecondStageCruise.getTopSpeed(player.getAbilities().getFlyingSpeed()));
        player.setDeltaMovement(velocity);
    }

    private static void wall(GameTestHelper helper, Block block) {
        for (int y = 1; y < 4; y++) {
            for (int z = 0; z < 9; z++) helper.setBlock(new BlockPos(WALL_X, y, z), block);
        }
    }

    private static void layer(GameTestHelper helper, int y, Block block) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) helper.setBlock(new BlockPos(x, y, z), block);
        }
    }

    private static ServerPlayer hurtable(GameTestHelper helper) {
        return GameTestPlayers.survival(helper, "gzr-flight-gametest").creative(() -> true)
                .pastSpawnInvulnerability().at(CENTER);
    }

    private static Pig idlePig(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityType.PIG, BODY);
        pig.setNoAi(true);
        pig.setNoGravity(true);
        pig.setDeltaMovement(Vec3.ZERO);
        return pig;
    }

    private static ServerPlayer sprintFlying(ServerPlayer player) {
        player.getAbilities().flying = true;
        player.setSprinting(true);
        return player;
    }

    private static ServerPlayer survival(GameTestHelper helper) {
        return GameTestPlayers.survival(helper, "gzr-flight-gametest").at(CENTER);
    }

    private static ServerPlayer creative(GameTestHelper helper, boolean[] creative) {
        return GameTestPlayers.survival(helper, "gzr-flight-gametest").creative(() -> creative[0]).at(CENTER);
    }
}
