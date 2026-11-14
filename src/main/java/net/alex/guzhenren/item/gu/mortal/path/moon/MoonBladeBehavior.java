package net.alex.guzhenren.item.gu.mortal.path.moon;

import net.alex.guzhenren.entity.projectile.MoonBladeEntity;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 月刃行为：月光蛊[Moonlight Gu]、月芒蛊[Moonglow Gu]、血月蛊[Blood Moon Gu]共用，伤害各定。
 * <ul>
 *   <li>沿视线发一枚本变体月刃；{@link KillingMoveService} 有锁定目标时弧线追踪（仅杀招驱动时有）。</li>
 *   <li>杀招[Killing Move]驱动时伤害乘杀招倍率（否则 1）；不拒绝、不留流派效果：月刃瞬发。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see MoonBladeEntity
 * @since 1.0.0
 */

public record MoonBladeBehavior(MoonBladeEntity.Variant variant, float damage) implements GuBehavior {

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        MoonBladeEntity.shoot(player, variant, damage * KillingMoveService.getDamageFactor(player, gu),
                KillingMoveService.getAimedTarget(player));
    }
}
