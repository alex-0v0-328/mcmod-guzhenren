package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.WildBoarEntity;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;
import static net.alex.guzhenren.gametest.GameTestPlayers.PLAYER_MAX_HEALTH;
import static net.alex.guzhenren.gametest.GameTestPlayers.ground;
import static net.alex.guzhenren.gametest.GameTestPlayers.place;

/**
 * 野猪中立 AI、服务端攻击、死亡周期测试。{@link #wildBoarClearsTargetOutsideFollowRange}：脱战野猪可能已恢复吃草。
 * <ul>
 *   <li>击杀掉落取击杀期间新增实体（{@link #killForDrops}），因扫描测试范围会算进飘入的他测物品与经验球，
 *       漏掉飘出的本测经验球。</li>
 *   <li>{@link #assertNoLaterDrops} 只记野猪身体范围，只数猪肉与经验：重复掉落会作为独立战利品出现在此，
 *       相邻测试掉落仍可能从边缘进入。</li>
 *   <li>经验球价值（{@link #experience}）读存档的 {@code Count}：发放时等值经验球会合并。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WildBoarGameTests {

    private WildBoarGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void wildBoarDoesNotAttackFromProximity(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 2.0D);
        WildBoarEntity boar = spawn(helper, CENTER_POS);
        helper.onEachTick(() -> {
            boar.getNavigation().stop();
            boar.setDeltaMovement(Vec3.ZERO);
        });

        helper.runAtTickTime(50L, () -> {
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH,
                    "neutral proximity damaged player");
            helper.assertTrue(boar.getTarget() == null, "neutral proximity assigned a target");
            helper.assertTrue(boar.getAction() == WildBoarEntity.Action.IDLE
                            || boar.getAction() == WildBoarEntity.Action.GRAZE,
                    "neutral proximity started an attack: " + boar.getAction());
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void wildBoarDoesNotAttackOnPeaceful(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 2.0D);
        WildBoarEntity boar = spawn(helper, CENTER_POS);
        Difficulty previous = helper.getLevel().getDifficulty();
        helper.getLevel().getServer().setDifficulty(Difficulty.PEACEFUL, true);
        try {
            boar.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.assertTrue(boar.getTarget() == null, "peaceful wild boar assigned a revenge target");
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH,
                    "peaceful revenge damaged player");
        } finally {
            helper.getLevel().getServer().setDifficulty(previous, true);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void wildBoarRevengeTargetsOnlyHurtIndividual(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 0.5D);
        WildBoarEntity victim = spawn(helper, new BlockPos(4, 1, 4));
        WildBoarEntity bystander = spawn(helper, new BlockPos(2, 1, 4));
        victim.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.onEachTick(() -> {
            victim.getNavigation().stop();
            bystander.getNavigation().stop();
        });

        helper.runAtTickTime(10L, () -> {
            helper.assertTrue(victim.getTarget() == player,
                    "hurt wild boar did not target its attacker");
            helper.assertTrue(bystander.getTarget() == null,
                    "nearby wild boar joined another boar's revenge target");
            helper.assertTrue(victim.getAction() == WildBoarEntity.Action.ALERT
                            || victim.getAction() == WildBoarEntity.Action.ATTACK_CHARGE
                            || victim.getAction() == WildBoarEntity.Action.ATTACK_TOSS,
                    "hurt wild boar did not enter its revenge action: " + victim.getAction());
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void wildBoarProjectileDamageIdentifiesShooter(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 0.5D);
        WildBoarEntity boar = spawn(helper, CENTER_POS);
        Arrow arrow = GameTestWorld.arrow(helper, player);
        arrow.setOwner(player);
        helper.assertTrue(boar.hurt(helper.getLevel().damageSources().arrow(arrow, player), 1.0F),
                "projectile did not damage wild boar");
        helper.onEachTick(() -> boar.getNavigation().stop());

        helper.runAtTickTime(8L, () -> {
            helper.assertTrue(boar.getTarget() == player,
                    "projectile damage did not assign the projectile shooter");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 140)
    public static void wildBoarClearsTargetOutsideFollowRange(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 0.5D);
        WildBoarEntity boar = spawn(helper, CENTER_POS);
        boar.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.runAtTickTime(1L, () -> place(player, helper, new Vec3(4.5D, 1.0D, -14.0D)));
        helper.runAtTickTime(70L, () -> {
            helper.assertTrue(boar.getTarget() == null,
                    "wild boar kept a target outside its follow range");
            helper.assertTrue(boar.getAction() == WildBoarEntity.Action.IDLE
                            || boar.getAction() == WildBoarEntity.Action.GRAZE,
                    "wild boar kept an attack after its target became invalid: " + boar.getAction());
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void wildBoarChargeHitsOnceDuringChargeWindow(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 7.5D, 4.5D);
        WildBoarEntity boar = spawn(helper, new BlockPos(1, 1, 4));
        boar.setTarget(player);
        boar.tryStartAction(WildBoarEntity.Action.ATTACK_CHARGE);
        float initialHealth = player.getHealth();
        int[] hitTick = { -1 };
        helper.onEachTick(() -> {
            if (hitTick[0] < 0 && player.getHealth() < initialHealth) {
                hitTick[0] = (int)helper.getTick();
                place(player, helper, new Vec3(8.5D, 1.0D, 0.5D));
                player.setDeltaMovement(Vec3.ZERO);
            }
        });
        helper.runAtTickTime(5L, () -> {
            helper.assertValueEqual(player.getHealth(), initialHealth,
                    "charge damaged before its six tick wind-up");
            helper.assertValueEqual(boar.getAction(), WildBoarEntity.Action.ATTACK_CHARGE,
                    "charge left its action during wind-up");
            helper.assertTrue(boar.getActionTicks() >= 5, "charge wind-up tick counter reset");
        });
        helper.runAtTickTime(30L, () -> {
            helper.assertTrue(hitTick[0] >= 6 && hitTick[0] <= 16,
                    "charge hit outside ticks 6-16: " + hitTick[0]);
            helper.assertValueEqual(player.getHealth(), 12.0F,
                    "charge damage or repeated hit count");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void wildBoarTossHitsAtNineTicks(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 2.5D);
        WildBoarEntity boar = spawn(helper, CENTER_POS);
        boar.setTarget(player);
        boar.tryStartAction(WildBoarEntity.Action.ATTACK_TOSS);
        float initialHealth = player.getHealth();
        int[] hitTick = { -1 };
        helper.onEachTick(() -> {
            if (hitTick[0] < 0) {
                if (player.getHealth() < initialHealth) {
                    hitTick[0] = (int)boar.getActionTicks();
                } else {
                    place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
                    player.setDeltaMovement(Vec3.ZERO);
                }
            }
        });
        helper.runAtTickTime(8L, () -> helper.assertValueEqual(player.getHealth(), initialHealth,
                "toss damaged before its 0.45 second hit point"));
        helper.runAtTickTime(20L, () -> {
            helper.assertValueEqual(hitTick[0], 9, "toss action hit tick");
            helper.assertValueEqual(player.getHealth(), 14.0F, "toss damage");
            helper.assertTrue(player.getDeltaMovement().y() > 0.0D,
                    "toss did not apply upward knockback");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void wildBoarChargeStopsAtWall(GameTestHelper helper) {
        ground(helper);
        for (int z = 3; z <= 5; z++) {
            helper.setBlock(new BlockPos(4, 1, z), Blocks.STONE);
            helper.setBlock(new BlockPos(4, 2, z), Blocks.STONE);
        }
        ServerPlayer player = survivalMock(helper, 7.5D, 4.5D);
        WildBoarEntity boar = spawn(helper, new BlockPos(1, 1, 4));
        boar.setTarget(player);
        boar.tryStartAction(WildBoarEntity.Action.ATTACK_CHARGE);
        helper.onEachTick(() -> player.setDeltaMovement(Vec3.ZERO));
        helper.runAtTickTime(35L, () -> {
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH,
                    "charge hit through a solid wall");
            helper.assertTrue(boar.getAction() != WildBoarEntity.Action.ATTACK_CHARGE,
                    "charge remained active after colliding with a wall");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void wildBoarTossHonorsCanceledHurt(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 2.5D);
        player.setInvulnerable(true);
        WildBoarEntity boar = spawn(helper, CENTER_POS);
        boar.setTarget(player);
        boar.tryStartAction(WildBoarEntity.Action.ATTACK_TOSS);
        helper.onEachTick(() -> {
            place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
            player.setDeltaMovement(Vec3.ZERO);
        });
        helper.runAtTickTime(20L, () -> {
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH,
                    "canceled toss still damaged target");
            helper.assertTrue(player.getDeltaMovement().lengthSqr() < 1.0E-6D,
                    "canceled toss still applied knockback");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void wildBoarTossHonorsKnockbackResistance(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 2.5D);
        player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        WildBoarEntity boar = spawn(helper, CENTER_POS);
        boar.setTarget(player);
        boar.tryStartAction(WildBoarEntity.Action.ATTACK_TOSS);
        helper.onEachTick(() -> {
            if (player.getHealth() >= PLAYER_MAX_HEALTH) {
                place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
                player.setDeltaMovement(Vec3.ZERO);
            }
        });
        helper.runAtTickTime(20L, () -> {
            helper.assertValueEqual(player.getHealth(), 14.0F,
                    "knockback resistant target did not take toss damage");
            helper.assertTrue(player.getDeltaMovement().lengthSqr() < 1.0E-6D,
                    "toss ignored full knockback resistance");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void wildBoarReloadClearsActionKeepsCooldown(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 2.5D);
        WildBoarEntity original = spawn(helper, CENTER_POS);
        original.setTarget(player);
        original.tryStartAction(WildBoarEntity.Action.ATTACK_CHARGE);
        int chargeCooldown = original.getChargeCooldown();
        CompoundTag saved = new CompoundTag();
        original.saveWithoutId(saved);
        original.discard();

        WildBoarEntity restored = spawn(helper, new BlockPos(6, 1, 6));
        restored.load(saved);
        helper.assertValueEqual(restored.getAction(), WildBoarEntity.Action.IDLE,
                "reloaded wild boar kept a transient action");
        helper.assertValueEqual(restored.getActionTicks(), 0L, "reloaded wild boar action ticks");
        helper.assertTrue(restored.getTarget() == null, "reloaded wild boar kept its target");
        helper.assertTrue(restored.getDeltaMovement().horizontalDistanceSqr() < 1.0E-6D,
                "reloaded wild boar kept charge movement");
        helper.assertValueEqual(restored.getChargeCooldown(), chargeCooldown,
                "reloaded wild boar lost charge cooldown");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void wildBoarDeathPersistsUntilThirtyTwoTicks(GameTestHelper helper) {
        ground(helper);
        WildBoarEntity boar = spawn(helper, CENTER_POS);
        boar.tryStartAction(WildBoarEntity.Action.ATTACK_CHARGE);
        helper.assertTrue(boar.hurt(helper.getLevel().damageSources().generic(), Float.MAX_VALUE),
                "wild boar did not accept lethal damage");
        helper.assertValueEqual(boar.getAction(), WildBoarEntity.Action.DEATH,
                "lethal damage did not enter death action");
        helper.runAtTickTime(28L, () -> {
            helper.assertTrue(!boar.isRemoved(), "wild boar was removed before death tick 32");
            helper.assertTrue(boar.deathTime < 32, "wild boar death timer passed 32 too early");
            helper.assertValueEqual(boar.getAction(), WildBoarEntity.Action.DEATH,
                    "death action was interrupted");
        });
        helper.runAtTickTime(32L, () -> {
            helper.assertTrue(boar.isRemoved(), "wild boar remained after death tick 32");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void wildBoarDropsRawCookedLootingLootAndExperienceOnce(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 8.0D);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        GameTestWorld.enchantLooting(helper, sword, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);

        WildBoarEntity raw = spawn(helper, new BlockPos(1, 1, 2));
        WildBoarEntity cooked = spawn(helper, new BlockPos(7, 1, 2));
        List<Entity> rawDrops = killForDrops(helper, player, raw);
        cooked.igniteForSeconds(100);
        List<Entity> cookedDrops = killForDrops(helper, player, cooked);
        assertDeathDrops(helper, rawDrops, Items.PORKCHOP, "raw");
        assertDeathDrops(helper, cookedDrops, Items.COOKED_PORKCHOP, "cooked");
        GameTestWorld.Drops rawLater = GameTestWorld.recordDrops(helper, raw.getBoundingBox().inflate(0.5D));
        GameTestWorld.Drops cookedLater = GameTestWorld.recordDrops(helper, cooked.getBoundingBox().inflate(0.5D));

        helper.runAtTickTime(45L, () -> {
            rawLater.stop().run();
            cookedLater.stop().run();
            assertNoLaterDrops(helper, rawLater);
            assertNoLaterDrops(helper, cookedLater);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void wildBoarChasesAfterOneAlert(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 7.5D, 4.5D);
        WildBoarEntity boar = spawn(helper, new BlockPos(1, 1, 4));
        CompoundTag cooldown = new CompoundTag();
        boar.saveWithoutId(cooldown);
        cooldown.putInt("WildBoarChargeCooldown", 100);
        boar.load(cooldown);
        boar.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        Vec3 start = boar.position();
        int alertSequence = boar.getActionSequence();
        helper.runAtTickTime(14L, () -> helper.assertValueEqual(boar.getAction(),
                WildBoarEntity.Action.ALERT, "initial alert was skipped"));
        helper.runAtTickTime(50L, () -> {
            helper.assertTrue(boar.position().distanceTo(start) > 1.0D, "retaliating boar did not chase");
            helper.assertTrue(boar.getAction() != WildBoarEntity.Action.ALERT
                    && boar.getActionSequence() > alertSequence, "alert replayed instead of chasing");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void wildBoarChargeKeepsRecoveryAnimationAndCooldown(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 4.5D);
        WildBoarEntity boar = spawn(helper, new BlockPos(1, 1, 4));
        boar.setTarget(player);
        helper.assertTrue(boar.tryStartAction(WildBoarEntity.Action.ATTACK_CHARGE), "charge did not start");
        helper.assertValueEqual(boar.getChargeCooldown(), 100,
                "charge cooldown did not begin immediately");
        helper.runAtTickTime(18L, () -> {
            helper.assertValueEqual(player.getHealth(), 12.0F, "charge did not hit once");
            helper.assertValueEqual(boar.getAction(), WildBoarEntity.Action.ATTACK_CHARGE,
                    "hit skipped the charge recovery animation");
            helper.assertTrue(!boar.tryStartAction(WildBoarEntity.Action.ATTACK_TOSS), "overlapping attack accepted");
        });
        helper.runAtTickTime(28L, () -> {
            helper.assertTrue(!boar.tryStartAction(WildBoarEntity.Action.ATTACK_TOSS), "recovery gap was bypassed");
            helper.assertTrue(boar.getChargeCooldown() > 60, "charge cooldown drained too quickly");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void wildBoarChargeStopsBeforeCliff(GameTestHelper helper) {
        ground(helper);
        for (int x = 4; x < 7; x++) {
            for (int z = 0; z < 9; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.AIR);
        }
        ServerPlayer player = survivalMock(helper, 7.5D, 4.5D);
        WildBoarEntity boar = spawn(helper, new BlockPos(1, 1, 4));
        boar.setTarget(player);
        boar.tryStartAction(WildBoarEntity.Action.ATTACK_CHARGE);
        helper.runAtTickTime(20L, () -> {
            helper.assertTrue(boar.getX() + boar.getBbWidth() / 2.0D
                    <= helper.absoluteVec(new Vec3(4, 1, 4)).x, "charge crossed the cliff edge");
            helper.assertValueEqual(player.getHealth(), 20.0F, "charge hit across a cliff");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 140)
    public static void wildBoarLosesOccludedTargetAndTossCannotHitThroughWall(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 2.5D);
        WildBoarEntity boar = spawn(helper, CENTER_POS);
        boar.setTarget(player);
        boar.tryStartAction(WildBoarEntity.Action.ATTACK_TOSS);
        for (int x = 0; x < 9; x++) {
            for (int y = 1; y < 4; y++) helper.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
        }
        helper.onEachTick(() -> {
            boar.moveTo(helper.absoluteVec(new Vec3(4.5D, 1.0D, 4.5D)));
            boar.getNavigation().stop();
            boar.setDeltaMovement(Vec3.ZERO);
        });
        helper.runAtTickTime(20L, () -> helper.assertValueEqual(player.getHealth(), 20.0F,
                "toss ignored line of sight"));
        helper.runAtTickTime(90L, () -> helper.assertTrue(boar.getTarget() == player,
                "target cleared before five seconds of lost sight"));
        helper.runAtTickTime(105L, () -> {
            helper.assertTrue(boar.getTarget() == null, "occluded target never cleared");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void wildBoarDeathCancelsPendingHit(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 2.5D);
        WildBoarEntity boar = spawn(helper, CENTER_POS);
        boar.setTarget(player);
        boar.tryStartAction(WildBoarEntity.Action.ATTACK_TOSS);
        helper.runAtTickTime(5L, () -> boar.hurt(helper.getLevel().damageSources().generic(), Float.MAX_VALUE));
        helper.runAtTickTime(18L, () -> {
            helper.assertValueEqual(player.getHealth(), 20.0F, "dead boar delivered a pending hit");
            helper.assertValueEqual(boar.getAction(), WildBoarEntity.Action.DEATH,
                    "death animation was overridden");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void wildBoarGrazeIsInterruptedWithoutChangingGrass(GameTestHelper helper) {
        ground(helper);
        helper.setBlock(CENTER_POS.below(), Blocks.GRASS_BLOCK);
        WildBoarEntity boar = spawn(helper, CENTER_POS);
        boar.tryStartAction(WildBoarEntity.Action.GRAZE);
        helper.runAtTickTime(25L, () -> {
            helper.assertValueEqual(boar.getAction(), WildBoarEntity.Action.GRAZE, "graze ended early");
            ServerPlayer player = survivalMock(helper, 4.5D, 2.5D);
            boar.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.assertValueEqual(boar.getAction(), WildBoarEntity.Action.ALERT,
                    "hurt did not interrupt grazing");
            helper.assertBlockState(CENTER_POS.below(), state -> state.is(Blocks.GRASS_BLOCK),
                    () -> "grazing changed grass");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void wildBoarLethalChargeRetainsRecovery(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper).build();
        player.setHealth(1.0F);
        place(player, helper, new Vec3(4.5D, 1.0D, 4.5D));
        WildBoarEntity boar = spawn(helper, new BlockPos(1, 1, 4));
        boar.setTarget(player);
        helper.assertTrue(boar.tryStartAction(WildBoarEntity.Action.ATTACK_CHARGE), "charge did not start");
        helper.runAtTickTime(18L, () -> {
            helper.assertTrue(!player.isAlive(), "charge was not lethal");
            helper.assertTrue(boar.getTarget() == null, "dead target retained");
            helper.assertValueEqual(boar.getAction(), WildBoarEntity.Action.ATTACK_CHARGE,
                    "lethal hit truncated recovery animation");
        });
        helper.runAtTickTime(25L, () -> {
            helper.assertTrue(!boar.getAction().isAttack(), "charge did not finish");
            helper.succeed();
        });
    }

    private static WildBoarEntity spawn(GameTestHelper helper, BlockPos pos) {
        return helper.spawn(ModEntityTypes.WILD_BOAR.get(), pos);
    }

    private static List<Entity> killForDrops(GameTestHelper helper, ServerPlayer player, WildBoarEntity boar) {
        return GameTestWorld.entitiesAddedBy(helper,
                () -> boar.hurt(helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE));
    }

    private static void assertDeathDrops(GameTestHelper helper, List<Entity> drops, Item meat, String kind) {
        helper.assertTrue(drops.stream().allMatch(entity -> entity instanceof ItemEntity item && item.getItem().is(meat)
                        || entity instanceof ExperienceOrb),
                kind + " wild boar dropped an unexpected item: " + describe(helper, drops));
        int meatCount = drops.stream().mapToInt(entity -> entity instanceof ItemEntity item
                ? item.getItem().getCount() : 0).sum();
        helper.assertTrue(meatCount >= 1 && meatCount <= 6,
                kind + " porkchop count outside base plus looting range: " + meatCount);
        int experience = drops.stream().mapToInt(entity -> entity instanceof ExperienceOrb orb ? experience(orb) : 0)
                .sum();
        helper.assertTrue(experience >= 1 && experience <= 3,
                kind + " wild boar experience reward outside one-to-three: " + experience);
    }

    private static void assertNoLaterDrops(GameTestHelper helper, GameTestWorld.Drops later) {
        List<Entity> duplicates = later.joined().stream().filter(entity -> entity instanceof ExperienceOrb
                || entity instanceof ItemEntity item
                && (item.getItem().is(Items.PORKCHOP) || item.getItem().is(Items.COOKED_PORKCHOP))).toList();
        helper.assertTrue(duplicates.isEmpty(),
                "wild boar loot or experience was duplicated after death animation: " + describe(helper, duplicates));
    }

    private static int experience(ExperienceOrb orb) {
        return orb.getValue() * orb.saveWithoutId(new CompoundTag()).getInt("Count");
    }

    private static String describe(GameTestHelper helper, List<Entity> entities) {
        return entities.stream().map(entity -> entity.getType().toShortString()
                + (entity instanceof ItemEntity item ? " " + item.getItem() : "")
                + (entity instanceof ExperienceOrb orb ? " " + experience(orb) : "")
                + " at " + helper.relativeVec(entity.position()) + " age " + entity.tickCount).toList().toString();
    }

    /** 站在地面、位于场景相对坐标 {@code x, z} 的生存玩家。 */
    private static ServerPlayer survivalMock(GameTestHelper helper, double x, double z) {
        return survivalMock(helper).at(new Vec3(x, 1.0D, z));
    }

    private static GameTestPlayers.Mock survivalMock(GameTestHelper helper) {
        return GameTestPlayers.survival(helper, "gzr-wild-boar-gametest").pastSpawnInvulnerability();
    }
}
