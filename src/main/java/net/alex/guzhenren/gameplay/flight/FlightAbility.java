package net.alex.guzhenren.gameplay.flight;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.jetbrains.annotations.NotNull;

/**
 * 按具名来源（{@link ResourceLocation}，如蛊虫效果、锚定维度）独立授予与撤销创造式飞行的底层能力门面。
 * <ul>
 *   <li>多个来源互不覆盖，只要任一来源存在（或游戏模式、其他模组授权）即可飞行（{@link #canFly}）。</li>
 *   <li>⚠ 调用方只能按自身来源 ID 调用 {@link #grant}、{@link #revoke} 与 {@link #has}；每个来源底层对应
 *       {@link NeoForgeMod#CREATIVE_FLIGHT} 属性上同 ID 的临时 {@code ADD_VALUE} 修饰符，不落盘且死亡重生后自动清
 *       零。</li>
 * </ul>
 */

public final class FlightAbility {

    private FlightAbility() {}

    public static boolean has(@NotNull Player player, @NotNull ResourceLocation source) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        return flight != null && flight.hasModifier(source);
    }

    public static boolean canFly(@NotNull Player player) { return player.mayFly(); }

    public static boolean grant(@NotNull ServerPlayer player, @NotNull ResourceLocation source) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight == null || flight.hasModifier(source)) return false;
        flight.addTransientModifier(new AttributeModifier(source, 1.0D, AttributeModifier.Operation.ADD_VALUE));
        return true;
    }

    public static boolean revoke(@NotNull ServerPlayer player, @NotNull ResourceLocation source) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        return flight != null && flight.removeModifier(source);
    }
}
