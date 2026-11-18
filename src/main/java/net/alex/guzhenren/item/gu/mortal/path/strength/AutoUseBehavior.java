package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 受伤自动触发的凡蛊行为接口：当持有者生命值低于阈值时由 {@link AutoUse} 自动催动，不支持手动右键催动。
 * <ul>
 *   <li>自力更生蛊[Self Reliance Gu]与苦力蛊[Hardship Strength Gu]实现本接口，由 {@link AutoUse} 统一控制开启与中止；
 *       {@link #applyEffect} 将对应的流派效果挂载至 {@link AutoUse#getEndTick}（含玩家所持永久兽力的时长加成）。</li>
 *   <li>玩家手动右键催动时，{@link #checkEffect} 统一返回 {@link Refusal#AUTO_EFFECT} 拒绝且不扣费，仅在
 *       {@link AutoUse#isStarting} 上下文内放行；自动催动行为不受功倍蛊[Amplify Effect Gu]增幅。</li>
 * </ul>
 */

public interface AutoUseBehavior extends GuBehavior {

    @NotNull PathEffectType<?> getStateType();

    boolean canStart(@NotNull ServerPlayer player);

    int getDurationSeconds();

    @Override
    default @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        return AutoUse.isStarting(player) ? null : new Refusal(Refusal.AUTO_EFFECT);
    }
}
