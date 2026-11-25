package net.alex.guzhenren.item.gu.mortal.path.soul;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderMountain;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 胆识蛊[Guts Gu]行为：供一次性消耗蛊（{@link net.alex.guzhenren.item.gu.OneShotGuItem}）通过
 * {@link SoulService#addMax} 永久增加定额魂魄上限（不恢复当前魂魄值），上限边界由服务层统一钳制。
 * <ul>
 *   <li>{@link #checkEffect} 只放行荡魂山[Soulsunder Mountain]范围内的催动；范围外或未生成荡魂山（未装 Gu World）时拒
 *       绝，物品保留。范围外的胆识蛊另由 {@link SoulsunderMountain} 每秒清除，这里的拒绝只挡住清除前那一拍。</li>
 *   <li>{@link #tickDropped} 每秒一次：掉落物在 {@link SoulsunderMountain#dissolvesAt} 判定为范围外时直接移除；
 *       {@link #canFitInsideContainerItems} 不让它进收纳袋、潜影盒，免得带出山。</li>
 *   <li>荡魂山相关的三处都先过 {@link SoulsunderMountain#isGutsGu}：本行为若复用到别的蛊，只加魂魄上限，不受山约束。
 *       </li>
 *   <li>⚠ 客户端不知道山心，门槛只对 {@link ServerPlayer} 校验，客户端直接放行，由服务端拒绝。</li>
 * </ul>
 */

public record SoulCapBehavior(int bonus) implements GuBehavior {

    public static final String FAILED_OUTSIDE = "guzhenren.item.failed.outside_soulsunder";

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer server) || !SoulsunderMountain.isGutsGu(stack)) return null;
        return SoulsunderMountain.isInside(server) ? null : new Refusal(FAILED_OUTSIDE);
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        SoulService.addMax(player, bonus);
    }

    @Override
    public void tickDropped(MortalGuItem gu, ItemEntity entity) {
        if (entity.tickCount % Ticks.SECOND != 0 || !(entity.level() instanceof ServerLevel level)) return;
        if (SoulsunderMountain.isGutsGu(entity.getItem()) && SoulsunderMountain.dissolvesAt(level, entity)) {
            entity.discard();
        }
    }

    @Override
    public boolean canFitInsideContainerItems(MortalGuItem gu) {
        return !SoulsunderMountain.isGutsGu(gu.getDefaultInstance());
    }
}
