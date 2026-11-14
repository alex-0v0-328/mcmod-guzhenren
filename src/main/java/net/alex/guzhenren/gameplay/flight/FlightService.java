package net.alex.guzhenren.gameplay.flight;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 飞行授权等级（{@link FlightGrant}）与蛊给的飞行。
 * <ul>
 *   <li>更快的二段飞行[Second-Stage Flight]归 {@link SecondStageService}；{@link #getGrant} 两端共用，客户端发意图前先
 *       问。</li>
 *   <li>创造模式与宝黄天[Treasure Yellow Heaven]里任何人可飞二段；生存仅靠实现 {@link FlightContributor} 的生效效果，
 *       取最强（{@link #getGuGrant}）；旁观、骑乘、睡觉、死亡者无。飞行即原版创造飞行：双击跳跃开关，疾跑翻倍。</li>
 *   <li>蛊飞行是 {@link FlightAbility} 来源（{@link #GU_FLIGHT_MODIFIER_ID}），不直接写会存档、游戏模式共用的
 *       {@code Abilities#mayfly}，绝不收回游戏模式或锚定维度给的飞行。</li>
 *   <li>{@link #tick}（{@link FlightEvents}，每服务端 tick）有蛊授权即授予，无则撤销，无其他来源时关飞，效果到期生存玩
 *       家下落；创造与宝黄天自带飞行。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see FlightGrant
 * @see SecondStageService
 * @since 1.0.0
 */

public final class FlightService {

    private FlightService() {}

    private static final ResourceLocation GU_FLIGHT_MODIFIER_ID = Guzhenren.id("gu_flight");

    public static @NotNull FlightGrant getGrant(@NotNull Player player) {
        if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isSleeping()) {
            return FlightGrant.NONE;
        }
        if (player.isCreative() || ModDimensions.isFlightGranted(player.level().dimension())) {
            return FlightGrant.SECOND_STAGE;
        }
        return getGuGrant(player);
    }

    private static @NotNull FlightGrant getGuGrant(@NotNull Player player) {
        FlightGrant grant = FlightGrant.NONE;
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (instance.getEffect().value() instanceof FlightContributor contributor) {
                grant = FlightGrant.getStrongest(grant, contributor.getFlightGrant(instance.getAmplifier()));
            }
        }
        return grant;
    }

    public static void tick(@NotNull ServerPlayer player) {
        boolean granted = getGuGrant(player).canFly();
        if (granted == FlightAbility.has(player, GU_FLIGHT_MODIFIER_ID)) return;

        if (granted) {
            FlightAbility.grant(player, GU_FLIGHT_MODIFIER_ID);
            return;
        }
        FlightAbility.revoke(player, GU_FLIGHT_MODIFIER_ID);
        if (!FlightAbility.canFly(player)) player.getAbilities().flying = false;
        player.onUpdateAbilities();
    }
}
