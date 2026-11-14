package net.alex.guzhenren.gameplay.attribute;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

/**
 * 临时 {@link AttributeModifier} 的最大生命：转数份额加骨份额（{@code Bone.getMaxHealthBonus}）。
 * <ul>
 *   <li>转数份额取 {@code ApertureService.getHealthRank}：只看第一空窍[Aperture]，未开窍[Awakening]按凡人，
 *       单独第二空窍不加。</li>
 *   <li>{@link AttributeEvents} 于 {@code ApertureChangedEvent}（压力写入不发）、{@code BodyTemperedEvent} 及登录、
 *       克隆、重置时刷新（修饰不随克隆保留）。</li>
 *   <li>⚠ 修饰须临时（{@link TransientModifiers}）；降上限须同压当前生命，{@code AttackDamageService}
 *       不必（攻击无「当前值」）。</li>
 *   <li>⚠ 原版先于此修饰恢复生命（读档取存档值，克隆取旧值）并截到 20：{@link #carry} 登出记入 {@code HEALTH_CARRY}，
 *       {@link #restoreCarried} 登录刷新后取回，仅当读档恰 20 且携带值更高。</li>
 *   <li>携带值只活一次登出到登录：崩溃会话不写，读档至多 20，不取更早会话的生命；克隆由
 *       {@code PlayerDataService.onClone} 交接旧生命，重生满血。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.path.strength.AttackDamageService
 * @see ApertureService
 * @since 1.0.0
 */

public final class MaxHealthService {

    private MaxHealthService() {}

    public static final double VANILLA_MAX_HEALTH = 20.0D;
    private static final ResourceLocation MODIFIER_ID =
            Guzhenren.id("rank_max_health");

    public static void refresh(@NotNull ServerPlayer player) {
        int target = ApertureService.getHealthRank(player).getMaxHealth();
        double bonus = (target > 0 ? target - VANILLA_MAX_HEALTH : 0.0D)
                + BodyService.getBone(player).getMaxHealthBonus();
        TransientModifiers.swap(player, Attributes.MAX_HEALTH, MODIFIER_ID, bonus);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    public static void carry(@NotNull ServerPlayer player) {
        PlayerData.get(player).setHealthCarry(player.getHealth());
    }

    public static void restoreCarried(@NotNull ServerPlayer player) {
        Float carried = PlayerData.get(player).takeHealthCarry();
        if (carried != null && carried > VANILLA_MAX_HEALTH && player.getHealth() == (float) VANILLA_MAX_HEALTH) {
            player.setHealth(carried);
        }
    }
}
