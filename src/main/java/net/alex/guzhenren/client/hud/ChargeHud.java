package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.item.GuItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The bar drawn over the hotbar while a charged Gu use is running.
 *
 * <p>Extends {@link net.alex.guzhenren.client.hud.HotbarHud}. Registered above
 * the vanilla air-level layer. Reads the held item's {@code chargeDuration} and
 * {@code chargeFraction} from {@link net.alex.guzhenren.item.GuItem}; if the item provides its own
 * fraction the bar reads that, otherwise it falls back to the vanilla remaining-ticks ratio.
 *
 * <p>{@link #isShowing} is the one test of "a charged Gu use is running", also read by {@link NourishHud} to stack
 * its bar above this one.
 *
 * <p>⚠ Any Gu whose use has a duration gets this for free, so adding a charged Gu needs no change here.
 * Whenever a leaf class's caption reads {@code a / b}, that class owes a {@code chargeFraction} over
 * the same pair.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ChargeHud extends HotbarHud {

    public static final ChargeHud INSTANCE = new ChargeHud();

    private ChargeHud() {}

    static boolean isShowing(Minecraft minecraft, LocalPlayer player) {
        if (minecraft.options.hideGui || player.isSpectator() || !player.isUsingItem()) return false;

        ItemStack stack = player.getUseItem();
        return stack.getItem() instanceof GuItem gu && gu.chargeDuration(stack, player) > 0;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !isShowing(minecraft, player)) return;

        ItemStack stack = player.getUseItem();
        GuItem gu = (GuItem) stack.getItem();
        int total = gu.chargeDuration(stack, player);
        int remaining = player.getUseItemRemainingTicks();
        Float own = gu.chargeFraction(stack, remaining);
        float progress = own != null ? own : 1.0F - remaining / (float) total;

        int x = (minecraft.getWindow().getGuiScaledWidth() - BAR_WIDTH) / 2;
        int y = getBarTop(minecraft);

        draw(graphics, minecraft, x, y, progress, gu.chargeColor(stack, remaining), gu.chargeCaption(stack, remaining));
    }
}
