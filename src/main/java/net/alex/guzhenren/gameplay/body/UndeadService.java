package net.alex.guzhenren.gameplay.body;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.path.qi.PathQiService;
import net.alex.guzhenren.gameplay.path.qi.QiKind;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.jetbrains.annotations.NotNull;

/**
 * The heartbeat steps of the undead [僵] and of Death Qi [死气]. {@link #tickHalfZombie} turns a
 * half-zombie holding Death Qi into a zombie, lets an expired one recover, and projects the state as the
 * {@code HALF_ZOMBIE} effect; {@link #pinHunger} keeps an undead body's hunger full; {@link #tickDeathQi}
 * burns lifespan [寿元] and floors health on the living while Death Qi is held.
 *
 * <p>⚠ {@link #DEATH_QI_YEAR_INTERVAL_TICKS} is 120 = 6 × 20 -- it must be a multiple of the heartbeat's
 * 20, or the burning silently stops. A {@code MobEffect} has no expiry hook, so the burning runs here, on
 * the heartbeat, and the {@code DEATH_QI} effect is only its projection.
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyService
 * @see PathQiService
 * @since 1.0.0
 */

public final class UndeadService {

    private UndeadService() {}

    public static final int DEATH_QI_YEAR_INTERVAL_TICKS = 6 * Ticks.SECOND;
    public static final long DEATH_QI_YEARS_PER_INTERVAL = 1L;
    public static final float DEATH_QI_HEALTH_FLOOR = 2.0F;
    public static final float DEATH_QI_HEALTH_PER_HEARTBEAT = 1.0F;
    private static final int FULL_HUNGER = 20;

    public static void tickHalfZombie(@NotNull ServerPlayer player) {
        if (BodyService.isHalfZombie(player)) {
            if (PathQiService.getCurrent(player, QiKind.DEATH) > 0L) {
                BodyService.turnZombie(player, BodyService.get(player).zombieTier());
            } else if (BodyService.hasHalfZombieRunOut(player)) {
                BodyService.removePhysique(player, Physique.HALF_ZOMBIE);
            }
        }
        projectHalfZombie(player);
    }

    private static void projectHalfZombie(ServerPlayer player) {
        if (!BodyService.isHalfZombie(player)) {
            if (player.hasEffect(ModEffects.HALF_ZOMBIE)) player.removeEffect(ModEffects.HALF_ZOMBIE);
            return;
        }
        player.addEffect(ModEffects.instance(ModEffects.HALF_ZOMBIE,
                Math.max(1, (int) BodyService.getHalfZombieTicksLeft(player))));
    }

    public static void pinHunger(@NotNull ServerPlayer player) {
        if (!BodyService.isUndead(player)) return;

        FoodData food = player.getFoodData();
        food.setFoodLevel(FULL_HUNGER);
        food.setSaturation(FULL_HUNGER);
        food.setExhaustion(0.0F);
    }

    public static void tickDeathQi(@NotNull ServerPlayer player) {
        if (!player.hasEffect(ModEffects.DEATH_QI) || BodyService.isZombie(player)) return;

        if (player.tickCount % DEATH_QI_YEAR_INTERVAL_TICKS == 0) {
            BodyService.drainByDeathQi(player, DEATH_QI_YEARS_PER_INTERVAL);
        }
        if (player.getHealth() > DEATH_QI_HEALTH_FLOOR) {
            player.setHealth(Math.max(DEATH_QI_HEALTH_FLOOR, player.getHealth() - DEATH_QI_HEALTH_PER_HEARTBEAT));
        }
    }
}
