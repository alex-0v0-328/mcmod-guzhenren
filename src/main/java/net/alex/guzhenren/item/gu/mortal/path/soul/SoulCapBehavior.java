package net.alex.guzhenren.item.gu.mortal.path.soul;

import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 胆识蛊[Guts Gu]的行为：魂魄上限加定值，不加当前魂魄；无需门槛，由 {@link SoulService#addMax} 钳制。
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.OneShotGuItem
 * @since 1.0.0
 */

public record SoulCapBehavior(int bonus) implements GuBehavior {

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        SoulService.addMax(player, bonus);
    }
}
