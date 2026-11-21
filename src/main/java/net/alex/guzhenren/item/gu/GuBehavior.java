package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.tending.ConsumedGuItem;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 凡蛊（{@link MortalGuItem}）的单条可组合行为接口：封装生效门槛、实际效果及对宿主通用规则的放宽条件。
 * <ul>
 *   <li>接口方法全部提供默认空实现，具体行为仅覆写自身关心的钩子；每只凡蛊由生命周期宿主
 *       （{@link OneShotGuItem}、{@link TendedGuItem} 或 {@link ConsumedGuItem}）与注册时按序传入的行为列表组合而成：宿
 *       主统一负责炼化、补食、真元与饱食[Hunger]结算及冷却，行为仅通过本接口钩子调整规则。</li>
 *   <li>宿主按注册顺序轮询行为列表：{@link #checkEffect} 返回首个非空拒绝原因；{@link #applyEffect} 依次执行全部行
 *       为；{@link #canBeAmplified} 与 {@link #canUseDuringEffectCooldown} 只要任一行为返回 {@code true} 即成
 *       立；{@link #getHungerCostMultiplier} 取各行为返回的最大倍率；{@link #defersForcedDeath} 允许需要蛊虫在持续期间
 *       存活的行为将强行催动[Forced Use]致死推迟到轮次结束时再自行结算；{@link #tickDropped} 在蛊虫作为掉落物
 *       时逐刻调用，可将其移除；{@link #canFitInsideContainerItems} 任一行为返回 {@code false}，蛊虫就装不进收纳袋、
 *       潜影盒等物品容器。</li>
 *   <li>⚠ 行为实现必须保持无状态以便跨多只蛊虫复用：物品栈状态存于数据组件，玩家状态存于各领域服务；行为自身仅持有固定
 *       时长或等级常量，通用数值通过宿主读取 {@link GuProperties}；外部代码统一通过 {@link MortalGuItem#getBehavior} 查
 *       询行为，不按物品子类判断。</li>
 * </ul>
 */

public interface GuBehavior {

    default @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) { return null; }

    default void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {}

    default boolean canBeAmplified() { return false; }

    default boolean canUseDuringEffectCooldown() { return false; }

    default int getHungerCostMultiplier(MortalGuItem gu, Player player, ItemStack stack) { return 1; }

    default boolean defersForcedDeath(MortalGuItem gu, ServerPlayer player, ItemStack stack) { return false; }

    default void tickDropped(MortalGuItem gu, ItemEntity entity) {}

    default boolean canFitInsideContainerItems(MortalGuItem gu) { return true; }
}
