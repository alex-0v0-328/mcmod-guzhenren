package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.display.ModDisplayText;
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
 * the vanilla air-level layer. Reads the held item's {@code getUseDuration} and
 * {@code chargeFraction} from {@link net.alex.guzhenren.item.GuItem}; if the item provides its own
 * fraction the bar reads that, otherwise it falls back to the vanilla remaining-ticks ratio. During the awakening
 * ritual [开窍仪式] it shows the aptitude bar instead: {@link AwakeningHud#getBarBase} climbs to the rolled base, gray
 * while filling and the default charge color once full, captioned like the aptitude line (八成七).
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

    private static final int AWAKENING_FILLING_COLOR = 0xFFCCCCCC;
    static final HudBar AWAKENING = HudBar.of("awakening", BAR_WIDTH, BAR_HEIGHT);

    static boolean isShowing(Minecraft minecraft, LocalPlayer player) {
        if (minecraft.options.hideGui || player.isSpectator()) return false;
        if (AwakeningHud.INSTANCE.getBarBase(player, 0.0F) >= 0) return true;
        if (!player.isUsingItem()) return false;

        ItemStack stack = player.getUseItem();
        return stack.getItem() instanceof GuItem gu && gu.getUseDuration(stack, player) > 0;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !isShowing(minecraft, player)) return;

        int x = (minecraft.getWindow().getGuiScaledWidth() - BAR_WIDTH) / 2;
        int y = getBarTop(minecraft);
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        int awakening = AwakeningHud.INSTANCE.getBarBase(player, partialTick);
        if (awakening >= 0) {
            int color = AwakeningHud.INSTANCE.isBarFull(player, partialTick) ? GuItem.CHARGE_COLOR_DEFAULT
                    : AWAKENING_FILLING_COLOR;
            draw(graphics, minecraft, AWAKENING, x, y, awakening / 100.0F, color,
                    ModDisplayText.formatBaseFraction(awakening));
            return;
        }

        ItemStack stack = player.getUseItem();
        GuItem gu = (GuItem) stack.getItem();
        int total = gu.getUseDuration(stack, player);
        int remaining = player.getUseItemRemainingTicks();
        Float own = gu.chargeFraction(stack, remaining);
        float progress = own != null ? own : 1.0F - remaining / (float) total;
        draw(graphics, minecraft, HOTBAR, x, y, progress, gu.chargeColor(stack, remaining),
                gu.chargeCaption(stack, remaining));
    }
}
