package net.alex.guzhenren.gameplay.flight;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.jetbrains.annotations.NotNull;

/**
 * 按具名来源授予的飞行，如蛊、锚定维度。各来源独立授撤，任一在即可飞。⚠ 调用方只按来源 ID 授予、撤销、查询；每个来源是
 * NeoForge 创造飞行属性上以来源 ID 命名的临时 {@code ADD_VALUE} 修饰符，不存档，重生即无。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class FlightAbility {

    private FlightAbility() {}

    public static boolean has(@NotNull Player player, @NotNull ResourceLocation source) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        return flight != null && flight.hasModifier(source);
    }

    /** 能否飞行；授权来自游戏模式、他模组或此处来源皆算。 */
    public static boolean canFly(@NotNull Player player) { return player.mayFly(); }

    /** 从 {@code source} 授飞；该来源已在则 {@code false}。 */
    public static boolean grant(@NotNull ServerPlayer player, @NotNull ResourceLocation source) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight == null || flight.hasModifier(source)) return false;
        flight.addTransientModifier(new AttributeModifier(source, 1.0D, AttributeModifier.Operation.ADD_VALUE));
        return true;
    }

    /** 撤销 {@code source} 的飞行；该来源不在则 {@code false}。 */
    public static boolean revoke(@NotNull ServerPlayer player, @NotNull ResourceLocation source) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        return flight != null && flight.removeModifier(source);
    }
}
