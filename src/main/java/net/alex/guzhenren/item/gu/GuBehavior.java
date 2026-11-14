package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.tending.ConsumedGuItem;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 蛊虫行为：蛊的一项作为，含门槛、效果与所放宽的限制。
 * <ul>
 *   <li>皆为默认方法，只答所改；蛊 = 生命周期宿主 {@link OneShotGuItem}/{@link TendedGuItem}/{@link ConsumedGuItem} +
 *       注册时按序给的行为；宿主管炼化、补食、真元[Essence] 与饱食[Hunger] 账单、冷却，行为只经下列限制放宽。</li>
 *   <li>宿主按序询问：{@link #checkEffect} 取首个拒绝；{@link #applyEffect} 全执行；{@link #canBeAmplified} 与
 *       {@link #canUseDuringEffectCooldown} 任一即成立；饱食倍率取最大；{@link #defersForcedDeath} 让需蛊存活的行为把强
 *       行催动[Forced Use] 的蛊留到轮末再自行杀死。</li>
 *   <li>⚠ 行为无状态、可多蛊共享：堆状态存堆上，玩家状态存服务；自带时长、等级，共用数值经宿主读 {@link GuProperties}；
 *       他处经 {@link MortalGuItem#getBehavior} 问，不看类。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see MortalGuItem
 * @since 1.0.0
 */

public interface GuBehavior {

    default @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) { return null; }

    default void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {}

    default boolean canBeAmplified() { return false; }

    default boolean canUseDuringEffectCooldown() { return false; }

    default int getHungerCostMultiplier(MortalGuItem gu, Player player, ItemStack stack) { return 1; }

    default boolean defersForcedDeath(MortalGuItem gu, ServerPlayer player, ItemStack stack) { return false; }
}
