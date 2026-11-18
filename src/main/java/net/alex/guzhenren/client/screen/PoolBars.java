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
 * 炼蛊界面网格下方的资源池进度条组：依次绘制真元、精炼真元（仅大于 0 时显示）与魂魄三条细进度条并在上方标注
 * {@code value / max} 读数；随同步附件实时更新而无需额外发包，使炼蛊每秒的资源消耗即时可见；在
 * {@code renderLabels} 中以相对面板坐标绘制，三条各自通过 {@link Widgets#drawBar} 使用独立 {@link SpriteBatch}，
 * 读数文本组件仅在当前值或上限变化时重建缓存。
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
