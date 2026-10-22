package net.alex.guzhenren.item.gu.mortal.heaven;

import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.OneShotGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * A one-shot Gu that grants lifespan [寿元], rolled between the bounds given at registration.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.OneShotGuItem}; the roll window is the only thing the
 * constructor carries, and the service write goes through
 * {@link BodyService#addLifespan}. Each of the four rungs
 * carries its own range, and the name is the promise -- the roll only decides the tail.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.OneShotGuItem
 * @since 1.0.0
 */

public class LifespanGuItem extends OneShotGuItem {

    private static final String MSG_GAINED = "guzhenren.item.gu.lifespan_gained";
    private final int minYears;
    private final int maxYears;

    public LifespanGuItem(Properties properties, int minYears, int maxYears, GuSpec spec) {
        super(properties, spec);
        this.minYears = minYears;
        this.maxYears = maxYears;
    }

    @Override
    protected int useApply(ServerPlayer player, ItemStack stack) {
        int years = minYears + player.getRandom().nextInt(maxYears - minYears + 1);
        BodyService.addLifespan(player, years);
        inform(player, MSG_GAINED, years);
        return 1;
    }
}
