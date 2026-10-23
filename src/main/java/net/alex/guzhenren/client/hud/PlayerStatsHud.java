package net.alex.guzhenren.client.hud;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.AperturePressureService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The top-left HUD readout: the realm line, then one bar per pool.
 *
 * <p>Registered above the vanilla hotbar layer in
 * {@link net.alex.guzhenren.client.event.ClientEvents}. Draws the title line (realm + title + aptitude), then
 * bars in order: essence, distilled, soul, gap, lifespan/age and pressure text. Hidden with
 * {@code hideGui}, in spectator, and under F3. Every phrase comes from
 * {@link ModDisplayText} so the HUD and the info command cannot diverge.
 *
 * <p>The rows, their texts and the filled widths are a pure function of the aperture, body and soul attachments
 * plus the whole second of the pressure countdown, so {@link Layout} is rebuilt only when that {@link Key}
 * changes (the attachments are immutable records, replaced on each sync) and a quiet frame builds no text. A frame
 * draws through one {@link GuiBatch}: every bar's fills first, then all the text; no row's text lies under another
 * row's fill, so the order is invisible.
 *
 * @author Alex
 * @version 1.0.0
 * @see ModDisplayText
 * @since 1.0.0
 */

public final class PlayerStatsHud {

    public static final PlayerStatsHud INSTANCE = new PlayerStatsHud();

    private PlayerStatsHud() {}

    private static final int LEFT = 8;
    private static final int TOP = 8;
    private static final int BAR_WIDTH = 130;
    private static final int BAR_HEIGHT = 9;
    private static final int TEXT_HEIGHT = 9;
    private static final int ROW_GAP = 2;
    private static final int GROUP_GAP = 7;
    private static final long NO_COUNTDOWN = -1L;
    private @Nullable Key cachedKey;
    private Layout layout = new Layout(List.of(), List.of());

    /** Draws the readout for the frame; the loader's HUD layer calls it, see {@code ClientEvents}. */
    public void render(@NotNull GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui) return;

        if (player.isSpectator() || minecraft.getDebugOverlay().showDebugScreen()) return;

        Font font = minecraft.font;
        Layout current = getLayout(player);
        GuiBatch.draw(graphics, () -> draw(graphics, font, current));
    }

    private Layout getLayout(LocalPlayer player) {
        ApertureData data = ApertureService.get(player);
        BodyData body = BodyService.get(player);
        boolean counting = body.isExtreme() && isCountingDown(data.primary());
        long seconds = counting
                ? ModDisplayText.countdownSeconds(AperturePressureService.getRemainingTicks(player))
                : NO_COUNTDOWN;
        Key key = new Key(data, body, SoulService.get(player), seconds);
        if (!key.equals(cachedKey)) {
            layout = build(key);
            cachedKey = key;
        }
        return layout;
    }

    private static boolean isCountingDown(Aperture aperture) {
        return aperture.pressure() == Aperture.PRESSURE_COUNTDOWN_START && aperture.pressureDeadlineTick() > 0L;
    }

    private static Layout build(Key key) {
        ApertureData data = key.data();
        Aperture aperture = data.primary();
        List<Bar> bars = new ArrayList<>();
        List<Line> lines = new ArrayList<>();

        int y = TOP;
        lines.add(new Line(y, ModDisplayText.hudHeader(aperture, key.body())));
        y += TEXT_HEIGHT + ROW_GAP;

        if (data.hasAperture()) y = essenceBars(bars, y, data);

        bars.add(bar(y, key.soul().currentSoul(), key.soul().maxSoul(), ModPalette.SOUL));
        y += BAR_HEIGHT + GROUP_GAP;

        lines.add(new Line(y,
                Component.translatable("guzhenren.hud.lifespan", ModDisplayText.hudLifespan(key.body()))));
        y += TEXT_HEIGHT + ROW_GAP;

        if (key.body().isExtreme()) lines.add(new Line(y, pressure(aperture, key.seconds())));
        return new Layout(bars, lines);
    }

    private static int essenceBars(List<Bar> bars, int top, ApertureData data) {
        int y = top;
        for (int i = 0; i < data.count(); i++) {
            Aperture pool = data.get(i);
            bars.add(bar(y, pool.currentEssence(), pool.maxEssence(), ModPalette.APERTURE));
            y += BAR_HEIGHT + ROW_GAP;

            if (pool.distilledEssence() > 0L) {
                bars.add(bar(y, pool.distilledEssence(), pool.maxEssence(), ModPalette.DISTILLED_FILL));
                y += BAR_HEIGHT + ROW_GAP;
            }
        }
        return y;
    }

    private static Component pressure(Aperture aperture, long seconds) {
        return seconds != NO_COUNTDOWN
                ? Component.translatable("guzhenren.hud.aperture_pressure_cd", aperture.pressure(),
                ModDisplayText.clock(seconds))
                : Component.translatable("guzhenren.hud.aperture_pressure", aperture.pressure());
    }

    private static Bar bar(int y, long current, long max, int fill) {
        int track = BAR_WIDTH - 2;
        int filled = max <= 0L ? 0 : (int) Math.min(track, Math.max(Math.round(track * (current / (double) max)), 0L));
        return new Bar(y, filled, fill, ModDisplayText.pool(current, max));
    }

    private static void draw(GuiGraphics graphics, Font font, Layout layout) {
        int right = LEFT + BAR_WIDTH;
        for (Bar bar : layout.bars()) {
            int y = bar.y();
            graphics.fill(LEFT, y, right, y + BAR_HEIGHT, ModPalette.BAR_BORDER);
            graphics.fill(LEFT + 1, y + 1, right - 1, y + BAR_HEIGHT - 1, ModPalette.BAR_TRACK);
            if (bar.filled() > 0) {
                graphics.fill(LEFT + 1, y + 1, LEFT + 1 + bar.filled(), y + BAR_HEIGHT - 1, bar.fill());
            }
        }
        for (Line line : layout.lines()) {
            graphics.drawString(font, line.text(), LEFT, line.y(), ModPalette.TEXT, true);
        }
        for (Bar bar : layout.bars()) {
            graphics.drawString(font, bar.text(), LEFT + (BAR_WIDTH - font.width(bar.text())) / 2, bar.y() + 1,
                    ModPalette.TEXT, true);
        }
    }

    private record Key(ApertureData data, BodyData body, SoulData soul, long seconds) {}

    private record Bar(int y, int filled, int fill, String text) {}

    private record Line(int y, Component text) {}

    private record Layout(List<Bar> bars, List<Line> lines) {}
}
