package net.alex.guzhenren.client.screen;

import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * The pools the refinement screen shows under its grid: essence [真元], distilled essence while there is any, and
 * soul [魂魄], each a {@code value / max} reading over a thin bar. All three ride the synced attachments, so none of
 * them needs a packet; a refinement spends essence and soul every second, and the bars show it.
 *
 * <p>Drawn from the screen's {@code renderLabels}, so coordinates are panel-relative. {@link #TRACK} is the empty
 * bar, shared with the screen's phase bar.
 *
 * @author Alex
 * @version 1.0.0
 * @see RefinementScreen
 * @since 1.0.0
 */

final class PoolBars {

    private PoolBars() {}

    static final int TRACK = 0x33000000;
    private static final int X = 18;
    private static final int WIDTH = 230;
    private static final int HEIGHT = 5;
    private static final int TOP = 156;
    private static final int STRIDE = 19;
    private static final String KEY = "guzhenren.menu.refinement.pool";

    static void draw(GuiGraphics graphics, Font font, Player player) {
        long maxEssence = ApertureEssenceService.getMaxEssence(player);
        int unit = 0;
        drawPool(graphics, font, unit++, ApertureEssenceService.getCurrentEssence(player), maxEssence,
                ModPalette.APERTURE);

        long distilled = ApertureEssenceService.getDistilledEssence(player);
        if (distilled > 0L) drawPool(graphics, font, unit++, distilled, maxEssence, ModPalette.DISTILLED_FILL);

        SoulData soul = SoulService.get(player);
        drawPool(graphics, font, unit, soul.currentSoul(), soul.maxSoul(), ModPalette.SOUL);
    }

    private static void drawPool(GuiGraphics graphics, Font font, int unit, long value, long max, int fill) {
        int y = TOP + unit * STRIDE;
        Component reading = Component.translatable(KEY, value, max);
        graphics.drawString(font, reading, X + WIDTH - font.width(reading), y, ModPalette.LEGEND_TEXT, false);

        int barY = y + font.lineHeight;
        graphics.fill(X, barY, X + WIDTH, barY + HEIGHT, TRACK);
        if (max <= 0L || value <= 0L) return;

        int filled = (int) Math.min(WIDTH, WIDTH * value / max);
        graphics.fill(X, barY, X + filled, barY + HEIGHT, fill);
    }
}
