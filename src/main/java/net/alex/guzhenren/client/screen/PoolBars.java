package net.alex.guzhenren.client.screen;

import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * 炼蛊界面网格下方的资源池条。真元[Essence]、精炼真元（大于 0 才显示）、魂魄三条细条，上方各一行 {@code value / max}；
 * 随同步附件更新、不发包，炼蛊每秒消耗真元和魂魄即时可见。从 {@code renderLabels} 画，坐标相对面板；条用
 * {@link Widgets#drawBar}，各一个 {@link SpriteBatch}；读数各自缓存，值或上限变才重建。
 *
 * @author Alex
 * @version 1.0.0
 * @see RefinementScreen
 * @since 1.0.0
 */

final class PoolBars {

    private PoolBars() {}

    private static final int X = 18;
    private static final int WIDTH = 230;
    private static final int HEIGHT = 6;
    private static final int TOP = 156;
    private static final int STRIDE = 19;
    private static final String KEY = "guzhenren.menu.refinement.pool";
    private static final int UNITS = 3;
    private static final Reading[] READINGS = new Reading[UNITS];

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
        Component reading = formatReading(unit, value, max);
        graphics.drawString(font, reading, X + WIDTH - font.width(reading), y, ModPalette.LEGEND_TEXT, false);

        int inner = WIDTH - 2;
        int filled = max <= 0L || value <= 0L ? 0 : (int) Math.min(inner, inner * value / max);
        SpriteBatch.draw(graphics, sprites -> Widgets.drawBar(sprites, X, y + font.lineHeight, WIDTH, HEIGHT, filled,
                fill));
    }

    private static Component formatReading(int unit, long value, long max) {
        Reading cached = READINGS[unit];
        if (cached == null || cached.value() != value || cached.max() != max) {
            cached = new Reading(value, max, Component.translatable(KEY, value, max));
            READINGS[unit] = cached;
        }
        return cached.text();
    }

    private record Reading(long value, long max, Component text) {}
}
