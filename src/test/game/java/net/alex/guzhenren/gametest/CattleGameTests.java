package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.CattleEntity;
import net.alex.guzhenren.entity.beast.CattleSpecies;
import net.alex.guzhenren.entity.beast.ChargerEntity;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;
import static net.alex.guzhenren.gametest.GameTestPlayers.PLAYER_MAX_HEALTH;
import static net.alex.guzhenren.gametest.GameTestPlayers.ground;
import static net.alex.guzhenren.gametest.GameTestPlayers.place;

/**
 * 牛：经 {@link ChargerEntity} 与野猪共用的反击性情、各物种冲撞与顶飞伤害、牛的掉落、共用死亡生命周期。冲撞型野兽机制
 * （墙、悬崖、遮挡、击退抗性、重载）仍归 {@code WildBoarGameTests}。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CattleGameTests {

    private CattleGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void cattleDoNotAttackFromProximity(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 2.0D);
        CattleEntity ox = spawn(helper, CattleSpecies.WATER_BUFFALO, CENTER_POS);
        helper.onEachTick(() -> {
            ox.getNavigation().stop();
            ox.setDeltaMovement(Vec3.ZERO);
        });
        helper.runAtTickTime(50L, () -> {
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH, "a neutral water buffalo damaged the player");
            helper.assertTrue(ox.getTarget() == null, "a water buffalo targeted a bystander");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void cattleAlertThenChaseOnlyTheirAttacker(GameTestHelper helper) {
        ground(helper);
        ServerPlayer attacker = survivalMock(helper, 4.5D, 2.5D);
        ServerPlayer bystander = survivalMock(helper, 1.5D, 4.5D);
        CattleEntity ox = spawn(helper, CattleSpecies.YELLOW_OX, CENTER_POS);
        helper.assertTrue(ox.hurt(helper.getLevel().damageSources().playerAttack(attacker), 1.0F),
                "the yellow ox did not accept damage");
        helper.assertTrue(ox.getTarget() == attacker, "the yellow ox did not target its attacker");
        helper.assertValueEqual(ox.getAction(), ChargerEntity.Action.ALERT, "a first hit did not play the alert");
        helper.runAtTickTime(ChargerEntity.ALERT_ACTION_TICKS + 2L, () -> {
            helper.assertTrue(ox.getTarget() == attacker, "the yellow ox lost its attacker after the alert");
            helper.assertTrue(ox.getTarget() != bystander, "the yellow ox turned on a bystander");
            helper.assertTrue(ox.getAction() != ChargerEntity.Action.ALERT, "the alert did not end");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 80)
    public static void waterBuffaloChargeHitsOnceForEightDuringTheChargeWindow(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 7.5D, 4.5D);
        CattleEntity ox = spawn(helper, CattleSpecies.WATER_BUFFALO, new BlockPos(1, 1, 4));
        ox.setTarget(player);
        ox.tryStartAction(ChargerEntity.Action.ATTACK_CHARGE);
        float initialHealth = player.getHealth();
        int[] hitTick = { -1 };
        helper.onEachTick(() -> {
            if (hitTick[0] < 0 && player.getHealth() < initialHealth) {
                hitTick[0] = (int) helper.getTick();
                place(player, helper, new Vec3(8.5D, 1.0D, 0.5D));
                player.setDeltaMovement(Vec3.ZERO);
            }
        });
        helper.runAtTickTime(5L, () -> helper.assertValueEqual(player.getHealth(), initialHealth,
                "the charge damaged before its six tick wind-up"));
        helper.runAtTickTime(30L, () -> {
            helper.assertTrue(hitTick[0] >= ChargerEntity.CHARGE_WINDUP_TICKS
                    && hitTick[0] <= ChargerEntity.CHARGE_WINDOW_END_TICKS, "charge hit outside ticks 6-16: " + hitTick[0]);
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH - 8.0F, "water buffalo charge damage");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void yellowOxTossHitsAtNineTicksForFour(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 2.5D);
        CattleEntity ox = spawn(helper, CattleSpecies.YELLOW_OX, CENTER_POS);
        ox.setTarget(player);
        ox.tryStartAction(ChargerEntity.Action.ATTACK_TOSS);
        float initialHealth = player.getHealth();
        int[] hitTick = { -1 };
        helper.onEachTick(() -> {
            if (hitTick[0] < 0) {
                if (player.getHealth() < initialHealth) {
                    hitTick[0] = (int) ox.getActionTicks();
                } else {
                    place(player, helper, new Vec3(4.5D, 1.0D, 2.5D));
                    player.setDeltaMovement(Vec3.ZERO);
                }
            }
        });
        helper.runAtTickTime(20L, () -> {
            helper.assertValueEqual(hitTick[0], ChargerEntity.TOSS_HIT_TICK, "toss hit tick");
            helper.assertValueEqual(player.getHealth(), PLAYER_MAX_HEALTH - 4.0F, "yellow ox toss damage");
            helper.assertTrue(player.getDeltaMovement().y() > 0.0D, "the toss did not lift the player");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void cattleReloadClearsActionKeepsCooldown(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 2.5D);
        CattleEntity original = spawn(helper, CattleSpecies.AZURE_OX, CENTER_POS);
        original.setTarget(player);
        original.tryStartAction(ChargerEntity.Action.ATTACK_CHARGE);
        int chargeCooldown = original.getChargeCooldown();
        CompoundTag saved = new CompoundTag();
        original.saveWithoutId(saved);
        original.discard();

        CattleEntity restored = spawn(helper, CattleSpecies.AZURE_OX, new BlockPos(6, 1, 6));
        restored.load(saved);
        helper.assertValueEqual(restored.getAction(), ChargerEntity.Action.IDLE, "a reloaded azure ox kept its action");
        helper.assertTrue(restored.getTarget() == null, "a reloaded azure ox kept its target");
        helper.assertValueEqual(restored.getChargeCooldown(), chargeCooldown, "a reloaded azure ox lost its cooldown");
        helper.assertTrue(chargeCooldown > 0, "the charge set no cooldown");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void cattleDeathHoldsThirtyTwoTicksAndDropsBeefLeatherAndExperienceOnce(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = survivalMock(helper, 4.5D, 8.0D);
        CattleEntity ox = spawn(helper, CattleSpecies.WATER_BUFFALO, CENTER_POS);
        List<Entity> drops = GameTestWorld.entitiesAddedBy(helper,
                () -> ox.hurt(helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE));
        helper.assertValueEqual(ox.getAction(), ChargerEntity.Action.DEATH, "lethal damage did not enter death");
        helper.assertTrue(drops.stream().allMatch(entity -> entity instanceof ExperienceOrb
                        || entity instanceof ItemEntity item
                        && (item.getItem().is(Items.BEEF) || item.getItem().is(Items.LEATHER))),
                "a water buffalo dropped an unexpected item: " + drops);
        int beef = drops.stream().mapToInt(entity -> entity instanceof ItemEntity item && item.getItem().is(Items.BEEF)
                ? item.getItem().getCount() : 0).sum();
        helper.assertTrue(beef >= 1 && beef <= 3, "beef count outside one-to-three: " + beef);
        helper.assertTrue(drops.stream().anyMatch(entity -> entity instanceof ExperienceOrb), "no experience dropped");
        GameTestWorld.Drops later = GameTestWorld.recordDrops(helper, ox.getBoundingBox().inflate(0.5D));
        helper.runAtTickTime(ChargerEntity.DEATH_REMOVE_TICK - 4L, () -> {
            helper.assertTrue(!ox.isRemoved(), "the water buffalo was removed before death tick 32");
            helper.assertValueEqual(ox.getAction(), ChargerEntity.Action.DEATH, "death was interrupted");
        });
        helper.runAtTickTime(ChargerEntity.DEATH_REMOVE_TICK + 4L, () -> {
            helper.assertTrue(ox.isRemoved(), "the water buffalo remained after death tick 32");
            later.stop().run();
            helper.assertTrue(later.joined().stream().noneMatch(entity -> entity instanceof ExperienceOrb
                    || entity instanceof ItemEntity), "cattle loot was duplicated after the death animation");
            helper.succeed();
        });
    }

    private static CattleEntity spawn(GameTestHelper helper, CattleSpecies species, BlockPos pos) {
        EntityType<CattleEntity> type = switch (species) {
            case YELLOW_OX -> ModEntityTypes.YELLOW_OX.get();
            case WATER_BUFFALO -> ModEntityTypes.WATER_BUFFALO.get();
            case AZURE_OX -> ModEntityTypes.AZURE_OX.get();
        };
        return helper.spawn(type, pos);
    }

    /** 站在场景相对坐标 {@code x, z} 地面上的生存模式玩家。 */
    private static ServerPlayer survivalMock(GameTestHelper helper, double x, double z) {
        return GameTestPlayers.survival(helper, "gzr-cattle-gametest").pastSpawnInvulnerability()
                .at(new Vec3(x, 1.0D, z));
    }
}
