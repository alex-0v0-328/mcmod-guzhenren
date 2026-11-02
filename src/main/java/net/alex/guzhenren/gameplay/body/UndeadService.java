package net.alex.guzhenren.gameplay.body;

import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.jetbrains.annotations.NotNull;

/**
 * The heartbeat steps of the undead [僵]. {@link #tickTempZombie} lets an expired temp zombie recover and
 * projects the state as the {@code TEMP_ZOMBIE} effect; {@link #pinHunger} keeps an undead body's hunger full.
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyService
 * @since 1.0.0
 */

public final class UndeadService {

    private UndeadService() {}

    private static final int FULL_HUNGER = 20;

    public static void tickTempZombie(@NotNull ServerPlayer player) {
        if (BodyService.isTempZombie(player) && BodyService.hasTempZombieRunOut(player)) {
            BodyService.removePhysique(player, Physique.TEMP_ZOMBIE);
        }
        BodyService.projectMarker(player, ModEffects.TEMP_ZOMBIE, BodyService.isTempZombie(player),
                BodyService.getTempZombieTicksLeft(player));
    }

    public static void pinHunger(@NotNull ServerPlayer player) {
        if (!BodyService.isUndead(player)) return;

        FoodData food = player.getFoodData();
        food.setFoodLevel(FULL_HUNGER);
        food.setSaturation(FULL_HUNGER);
        food.setExhaustion(0.0F);
    }
}
