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
 * 玩家最大生命值临时 {@link AttributeModifier} 服务：汇总修为转数与铁骨（{@code Bone.getMaxHealthBonus}）的生命加成。
 * <ul>
 *   <li>转数加成取自 {@link ApertureService#getHealthRank}：仅认第一空窍（未开窍[Awakening]按凡人 20 点计，孤立第二空窍
 *       不增加生命上限）。</li>
 *   <li>由 {@link AttributeEvents} 在 {@code ApertureChangedEvent}（压力变更不触发）、{@code BodyTemperedEvent} 以及登
 *       录、克隆、重置时刷新。</li>
 *   <li>⚠ 必须使用 {@link TransientModifiers} 挂载临时修饰符；当最大生命值下调导致当前生命超出新上限时，
 *       {@link #refresh} 立即将当前生命下压至新上限（攻击力修饰无当前值，无需此步）。</li>
 *   <li>⚠ 原版在读档或克隆时先于本修饰符恢复生命值，会将高于 20 的生命截断为 20：{@link #carry} 在登出时将当前生命暂存
 *       至 {@code healthCarry}，{@link #restoreCarried} 在登录刷新上限后取回（仅当读档后生命恰好为 20 且暂存值更高时恢
 *       复）；该暂存值仅存活一次登出到登录（崩溃未触发登出则读档上限为 20），克隆与死亡重生的生命交接由
 *       {@code PlayerDataService.onClone} 负责。</li>
 * </ul>
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
