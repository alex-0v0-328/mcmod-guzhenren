package net.alex.guzhenren.item.gu.mortal.soul;

import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.OneShotGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The Guts Gu [胆识蛊]: a one-shot soul Gu that raises the soul cap [魂魄上限] by ten.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.OneShotGuItem}. The apply delegates to
 * {@link SoulService#addMax}; no gate is needed because a
 * cap raise is always legal and never stacks past what the service clamps.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.OneShotGuItem
 * @since 1.0.0
 */

public class GutsGuItem extends OneShotGuItem {

    private static final int SOUL_BONUS = 10;

    public GutsGuItem(Properties properties, GuSpec spec) {
        super(properties, spec);
    }

    @Override
    protected int useApply(ServerPlayer player, ItemStack stack) {
        SoulService.addMax(player, SOUL_BONUS);
        return 1;
    }
}
