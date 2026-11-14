package net.alex.guzhenren.gameplay.body;

import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.jetbrains.annotations.NotNull;

/**
 * 不死（僵）的心跳步骤：{@link #tickTempZombie} 让到期临时僵尸恢复并投影为 {@code TEMP_ZOMBIE} 效果；{@link #pinHunger}
 * 保持不死之躯饥饿值满格。
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
