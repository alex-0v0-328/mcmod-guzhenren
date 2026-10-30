package net.alex.guzhenren.item.gu.mortal.heaven;

import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The Lifespan Gu's [寿蛊] module: grants lifespan [寿元], rolled between the bounds given at registration.
 *
 * <p>The service write goes through {@link BodyService#addLifespan}. Each of the four rungs carries its own range,
 * and the name is the promise -- the roll only decides the tail.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.OneShotGuItem
 * @since 1.0.0
 */

public record LifespanModule(int minYears, int maxYears) implements GuModule {

    private static final String MSG_GAINED = "guzhenren.item.gu.lifespan_gained";

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        int years = minYears + player.getRandom().nextInt(maxYears - minYears + 1);
        BodyService.addLifespan(player, years);
        ActionBar.inform(player, MSG_GAINED, years);
    }
}
