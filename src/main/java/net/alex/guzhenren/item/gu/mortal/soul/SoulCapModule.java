package net.alex.guzhenren.item.gu.mortal.soul;

import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The Guts Gu's [胆识蛊] module: raises the soul cap [魂魄上限] by a fixed amount, never the current soul.
 *
 * <p>The effect delegates to {@link SoulService#addMax}; no gate is needed because a cap raise is always legal and
 * never stacks past what the service clamps.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.OneShotGuItem
 * @since 1.0.0
 */

public record SoulCapModule(int bonus) implements GuModule {

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        SoulService.addMax(player, bonus);
    }
}
