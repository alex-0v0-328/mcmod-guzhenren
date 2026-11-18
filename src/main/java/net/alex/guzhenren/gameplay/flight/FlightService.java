package net.alex.guzhenren.gameplay.flight;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家飞行授权等级查询与蛊虫飞行能力同步服务。
 * <ul>
 *   <li>更高速的二段飞行[Second-Stage Flight]巡航归 {@link SecondStageService} 管理；{@link #getGrant} 供双端共读（客户
 *       端在发送二段飞行切换意图前先用它做本地校验）。</li>
 *   <li>创造模式与宝黄天[Treasure Yellow Heaven]等飞行维度直接授予 {@link FlightGrant#SECOND_STAGE}；生存模式仅汇总实现
 *       {@link FlightContributor} 的活动药水效果并取最高档（{@code getGuGrant}）；死亡、旁观、骑乘或睡眠状态下统一返回
 *       {@link FlightGrant#NONE}。</li>
 *   <li>蛊虫飞行通过 {@link FlightAbility} 在 {@link #GU_FLIGHT_MODIFIER_ID} 下独立挂载临时修饰符，不改写会落盘的
 *       {@code Abilities.mayfly}，因此绝不会误回收游戏模式或锚定维度授予的飞行权限。</li>
 *   <li>{@link #tick} 每个服务端 tick 检查蛊虫飞行授权：有则挂载修饰符，无则撤销修饰符并在失去全部飞行来源时关闭飞行令
 *       生存玩家自然下落。</li>
 * </ul>
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
