package net.alex.guzhenren.gameplay.body;

import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.jetbrains.annotations.NotNull;

/**
 * 死灵体质（僵尸与临时僵尸化）的心跳维护服务：{@link #tickTempZombie} 在临时僵尸化到期时移除该体质并同步
 * {@code TEMP_ZOMBIE} 状态栏投影效果；{@link #pinHunger} 将死灵体质玩家的饥饿值与饱和度锁满并清零消耗度。
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
