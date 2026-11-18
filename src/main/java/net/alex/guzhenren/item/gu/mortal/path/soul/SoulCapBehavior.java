package net.alex.guzhenren.item.gu.mortal.path.soul;

import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 胆识蛊[Guts Gu]行为：供一次性消耗蛊（{@link net.alex.guzhenren.item.gu.OneShotGuItem}）通过
 * {@link SoulService#addMax} 永久增加定额魂魄上限（不恢复当前魂魄值），无额外催动门槛，上限边界由服务层统一钳制。
 */

public record SoulCapBehavior(int bonus) implements GuBehavior {

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        SoulService.addMax(player, bonus);
    }
}
