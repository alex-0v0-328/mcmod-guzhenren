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
 * 自动生效行为：持有者受伤时自行生效，不可手动。
 * <ul>
 *   <li>自力更生蛊[Self Reliance Gu]、苦力蛊[Hardship Strength Gu]各带一个，{@link AutoUse} 开停；{@code applyEffect}
 *       施加流派效果至 {@link AutoUse#getEndTick}（窗口加持有兽种时长加成）。</li>
 *   <li>手动以 {@link Refusal#AUTO_EFFECT} 拒绝不付费，仅放行 {@link AutoUse}；功倍蛊[Amplify Effect Gu]不放大。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AutoUse
 * @since 1.0.0
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
