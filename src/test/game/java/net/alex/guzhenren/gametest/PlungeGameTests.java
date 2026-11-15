package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.alex.guzhenren.gameplay.plunge.PlungeService;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static net.alex.guzhenren.gametest.GameTestPlayers.place;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 服务端坠击测试。
 * <ul>
 *   <li>覆盖谁能发起、发起清除什么、落地留下什么与伤害、什么提前结束；坑里 TNT 被点燃是保留特性。</li>
 *   <li>模拟玩家不跑物理，手动代替下落：在 {@link #TOP} 发起（保留已有下落距离），移到 {@link #dirtFloor} 上的
 *       {@link #FEET}，标着地、清零下落距离，再调 {@link PlungeService#tick}，同原版收到客户端落地。</li>
 *   <li>最小坑每条射线都破泥土，必成坑；{@link #TOP} 带 2 格下落：共落 5 格，原版伤害 2，坠击 6，坑威力略大于 2。</li>
 *   <li>客户端部分（潜行加 Alt、本地保持、落地停止）与真实下落速度由客户端验收。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlungeGameTests {
    private static final Vec3 TOP = new Vec3(4.5D, 4.0D, 4.5D);
    private static final Vec3 FEET = new Vec3(4.5D, 1.0D, 4.5D);
    private static final BlockPos BELOW = new BlockPos(4, 0, 4);
    private static final float PRIOR_FALL = 2.0F;
    private static final float FIVE_BLOCK_PLUNGE_DAMAGE = 6.0F;
    private static final double BASE_GRAVITY = 0.08D;

    private PlungeGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void onlyAPlayerInTheAirMayStart(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        player.setOnGround(true);
        assertFalse(PlungeService.start(player), "a player on the ground started a plunge");

        player.setOnGround(false);
        assertTrue(PlungeService.start(player), "a player in the air was refused");
        assertTrue(PlungeService.isActive(player), "no plunge mark after the start");
        assertFalse(PlungeService.start(player), "a second start plunged twice");

        ServerPlayer spectator = plunger(helper).spectator(() -> true).at(TOP);
        assertFalse(PlungeService.start(spectator), "a spectator started a plunge");
        player.discard();
        spectator.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theStartDropsFlightAndLevitationAndOnlyFallsStraightDown(GameTestHelper helper) {
        ServerPlayer player = plunger(helper).creative(() -> true).at(TOP);
        player.getAbilities().flying = true;
        player.setSprinting(true);
        assertTrue(SecondStageService.start(player), "the creative flier could not enter the second stage");
        player.addEffect(ModEffects.instance(MobEffects.LEVITATION, 200));
        player.addEffect(ModEffects.instance(MobEffects.SLOW_FALLING, 200));
        player.setDeltaMovement(1.0D, 0.5D, -1.0D);

        assertTrue(PlungeService.start(player), "the creative flier was refused");

        assertFalse(player.getAbilities().flying, "the plunge kept flying");
        assertFalse(SecondStageService.isActive(player), "the plunge kept the second stage");
        assertFalse(player.hasEffect(MobEffects.LEVITATION), "the plunge kept levitation");
        assertFalse(player.hasEffect(MobEffects.SLOW_FALLING), "the plunge kept slow falling");
        assertEquals(0.0D, player.getDeltaMovement().length(), 1.0E-9D);
        assertEquals(BASE_GRAVITY * PlungeService.GRAVITY_MULTIPLIER, player.getAttributeValue(Attributes.GRAVITY),
                1.0E-9D);
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void flightCannotResumeMidPlunge(GameTestHelper helper) {
        ServerPlayer player = plunger(helper).creative(() -> true).at(TOP);
        PlungeService.start(player);

        player.getAbilities().flying = true;
        PlungeService.tick(player);

        assertFalse(player.getAbilities().flying, "flight came back mid-plunge");
        assertTrue(PlungeService.isActive(player), "turning flight on called the plunge off");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theLandingCratersAndTriplesTheWholeFall(GameTestHelper helper) {
        dirtFloor(helper);
        ServerPlayer player = plungeFromTheTop(helper);
        float before = player.getHealth();

        land(player, helper);

        assertFalse(PlungeService.isActive(player), "the plunge outlived the landing");
        assertEquals(BASE_GRAVITY, player.getAttributeValue(Attributes.GRAVITY), 1.0E-9D);
        assertEquals(before - FIVE_BLOCK_PLUNGE_DAMAGE, player.getHealth(), 0.001D);
        assertTrue(helper.getBlockState(BELOW).isAir(), "the landing left no crater");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aDropUnderFourBlocksLeavesNothing(GameTestHelper helper) {
        dirtFloor(helper);
        ServerPlayer player = hurtableAtTop(helper);
        PlungeService.start(player);
        float before = player.getHealth();

        land(player, helper);

        assertFalse(PlungeService.isActive(player), "the plunge outlived the landing");
        assertEquals(before, player.getHealth(), 0.001D);
        assertTrue(helper.getBlockState(BELOW).is(Blocks.DIRT), "a three-block drop left a crater");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void threeAndAHalfBlocksHurtWithoutACrater(GameTestHelper helper) {
        dirtFloor(helper);
        ServerPlayer player = hurtableAtTop(helper);
        player.fallDistance = 0.5F;
        PlungeService.start(player);
        float before = player.getHealth();

        land(player, helper);

        assertEquals(before - 3.0F, player.getHealth(), 0.001D);
        assertTrue(helper.getBlockState(BELOW).is(Blocks.DIRT), "a three-and-a-half-block drop left a crater");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theServerStepLandingWaitsForTheClient(GameTestHelper helper) {
        dirtFloor(helper);
        ServerPlayer player = plungeFromTheTop(helper);
        float before = player.getHealth();
        place(player, helper, FEET);
        player.setOnGround(true);
        player.fallDistance = PRIOR_FALL + 3.0F;

        PlungeService.tick(player);

        assertTrue(PlungeService.isActive(player), "the plunge landed before the client's fall was handled");
        assertEquals(before, player.getHealth(), 0.001D);
        assertTrue(helper.getBlockState(BELOW).is(Blocks.DIRT), "the server's own step left a crater");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theFallIsCappedAtEightBlocksATick(GameTestHelper helper) {
        ServerPlayer player = plungeFromTheTop(helper);
        player.setDeltaMovement(0.0D, -20.0D, 0.0D);

        PlungeService.tick(player);

        assertEquals(-PlungeService.MAX_FALL_SPEED, player.getDeltaMovement().y, 1.0E-9D);
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aLadderEndsThePlunge(GameTestHelper helper) {
        ServerPlayer player = plungeFromTheTop(helper);
        float before = player.getHealth();
        helper.setBlock(new BlockPos(4, 1, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.LADDER.defaultBlockState()
                .setValue(LadderBlock.FACING, Direction.NORTH));
        place(player, helper, new Vec3(4.5D, 1.2D, 4.5D));

        PlungeService.tick(player);

        assertFalse(PlungeService.isActive(player), "the plunge outlived the ladder");
        assertEquals(BASE_GRAVITY, player.getAttributeValue(Attributes.GRAVITY), 1.0E-9D);
        assertEquals(before, player.getHealth(), 0.001D);
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void deathEndsThePlunge(GameTestHelper helper) {
        ServerPlayer player = plungeFromTheTop(helper);

        player.setHealth(0.0F);
        PlungeService.tick(player);

        assertFalse(PlungeService.isActive(player), "the plunge outlived its player");
        assertEquals(BASE_GRAVITY, player.getAttributeValue(Attributes.GRAVITY), 1.0E-9D);
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void griefingOffKeepsTheGroundButStillHurts(GameTestHelper helper) {
        dirtFloor(helper);
        ServerPlayer player = plungeFromTheTop(helper);
        float before = player.getHealth();
        GameRules.BooleanValue griefing = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean was = griefing.get();
        try {
            griefing.set(false, helper.getLevel().getServer());
            land(player, helper);
        } finally {
            griefing.set(was, helper.getLevel().getServer());
        }

        assertTrue(helper.getBlockState(BELOW).is(Blocks.DIRT), "mobGriefing off still left a crater");
        assertEquals(before - FIVE_BLOCK_PLUNGE_DAMAGE, player.getHealth(), 0.001D);
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theCraterHurtsNoBystanderAndBurnsNoItem(GameTestHelper helper) {
        dirtFloor(helper);
        Pig pig = helper.spawn(EntityType.PIG, new Vec3(6.0D, 1.0D, 4.5D));
        float pigHealth = pig.getHealth();
        Vec3 itemAt = helper.absoluteVec(new Vec3(3.0D, 1.0D, 4.5D));
        ItemEntity item = new ItemEntity(helper.getLevel(), itemAt.x, itemAt.y, itemAt.z,
                new ItemStack(Items.STICK));
        helper.getLevel().addFreshEntity(item);
        ServerPlayer player = plungeFromTheTop(helper);

        land(player, helper);

        assertEquals(pigHealth, pig.getHealth(), 0.001D);
        assertTrue(pig.getDeltaMovement().lengthSqr() > 1.0E-4D, "the crater did not knock the pig back");
        assertTrue(item.isAlive(), "the crater destroyed a dropped item");
        assertEquals(0.0D, player.getDeltaMovement().length(), 1.0E-9D);
        player.discard();
        item.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theCraterPrimesTntInIt(GameTestHelper helper) {
        dirtFloor(helper);
        BlockPos tnt = BELOW.east();
        helper.setBlock(tnt, Blocks.TNT);
        ServerPlayer player = plungeFromTheTop(helper);

        land(player, helper);

        List<PrimedTnt> primed = helper.getLevel().getEntitiesOfClass(PrimedTnt.class,
                new AABB(helper.absolutePos(tnt)).inflate(2.0D));
        assertTrue(helper.getBlockState(tnt).isAir(), "the crater kept the TNT block");
        assertEquals(1, primed.size(), "primed TNT in the crater");
        primed.forEach(PrimedTnt::discard);
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void vanillaFallDamageIsReplacedMidPlunge(GameTestHelper helper) {
        ServerPlayer player = plungeFromTheTop(helper);
        float before = player.getHealth();

        assertFalse(player.causeFallDamage(20.0F, 1.0F, player.damageSources().fall()),
                "vanilla's fall damage landed mid-plunge");
        assertEquals(before, player.getHealth(), 0.001D);
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void mountingEndsThePlungeWithNothingLeft(GameTestHelper helper) {
        dirtFloor(helper);
        ServerPlayer player = plungeFromTheTop(helper);
        float before = player.getHealth();
        Minecart cart = EntityType.MINECART.create(helper.getLevel());
        assertTrue(cart != null, "minecart not created");
        Vec3 position = helper.absoluteVec(FEET);
        cart.moveTo(position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(cart);
        assertTrue(player.startRiding(cart, true), "the player could not board the minecart");

        PlungeService.tick(player);

        assertFalse(PlungeService.isActive(player), "the plunge outlived the mount");
        assertEquals(BASE_GRAVITY, player.getAttributeValue(Attributes.GRAVITY), 1.0E-9D);
        assertEquals(before, player.getHealth(), 0.001D);
        assertTrue(helper.getBlockState(BELOW).is(Blocks.DIRT), "an interrupted plunge left a crater");
        player.stopRiding();
        player.discard();
        cart.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aDroppedMarkTakesTheGravityWithIt(GameTestHelper helper) {
        ServerPlayer player = plungeFromTheTop(helper);

        PlayerData.get(player).setPlungeTopY(null);
        PlungeService.tick(player);

        assertEquals(BASE_GRAVITY, player.getAttributeValue(Attributes.GRAVITY), 1.0E-9D);
        player.discard();
        helper.succeed();
    }

    private static GameTestPlayers.Mock plunger(GameTestHelper helper) {
        return GameTestPlayers.survival(helper, "gzr-plunge-gametest");
    }

    private static ServerPlayer survival(GameTestHelper helper) {
        return plunger(helper).at(TOP);
    }

    private static ServerPlayer hurtableAtTop(GameTestHelper helper) {
        return plunger(helper).pastSpawnInvulnerability().at(TOP);
    }

    private static ServerPlayer plungeFromTheTop(GameTestHelper helper) {
        ServerPlayer player = hurtableAtTop(helper);
        player.fallDistance = PRIOR_FALL;
        assertTrue(PlungeService.start(player), "the plunge did not start");
        return player;
    }

    private static void land(ServerPlayer player, GameTestHelper helper) {
        place(player, helper, FEET);
        player.setOnGround(true);
        player.fallDistance = 0.0F;
        PlungeService.tick(player);
    }

    private static void dirtFloor(GameTestHelper helper) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.DIRT);
        }
    }
}
