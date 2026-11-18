package net.alex.guzhenren.item.gu.mortal.path.moon;

import net.alex.guzhenren.entity.projectile.MoonBladeEntity;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 月刃发射行为：月光蛊[Moonlight Gu]、月芒蛊[Moonglow Gu]与血月蛊[Blood Moon Gu]共用，各自在注册时注入弹射物变体与基础
 * 伤害。
 * <ul>
 *   <li>沿视线发射一枚对应变体的 {@link MoonBladeEntity}；若当前正由 {@link KillingMoveService} 驱动且带有复验通过的锁
 *       定目标，则改沿弧线追踪该目标。</li>
 *   <li>在杀招[Killing Move]驱动下伤害乘以 {@link KillingMoveService#getDamageFactor}（非杀招驱动时倍率为 1）；月刃为瞬
 *       发攻击，不设额外生效门槛，也不挂载持续流派效果。</li>
 * </ul>
 */

public record MoonBladeBehavior(MoonBladeEntity.Variant variant, float damage) implements GuBehavior {

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        MoonBladeEntity.shoot(player, variant, damage * KillingMoveService.getDamageFactor(player, gu),
                KillingMoveService.getAimedTarget(player));
    }
}
