package net.alex.guzhenren.client.hud;

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
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * The top-left HUD readout: the realm line, then one bar per pool.
 *
 * <p>Implements {@link net.minecraft.client.gui.LayeredDraw.Layer}; registered above
 * {@code VanillaGuiLayers.HOTBAR} in
 * {@link net.alex.guzhenren.client.event.ClientEvents}. Draws the title line (realm + title + aptitude), then
 * bars in order: essence, distilled, soul, gap, lifespan/age and pressure text. Hidden with
 * {@code hideGui}, in spectator, and under F3. Every phrase comes from
 * {@link ModDisplayText} so the HUD and the info command cannot diverge.
 *
 * @author Alex
 * @version 1.0.0
 * @see ModDisplayText
 * @since 1.0.0
 */

public final class PlayerStatsHud implements LayeredDraw.Layer {

    public static final PlayerStatsHud INSTANCE = new PlayerStatsHud();

    private PlayerStatsHud() {}

    private static final int LEFT = 8;
    private static final int TOP = 8;
    private static final int BAR_WIDTH = 130;
    private static final int BAR_HEIGHT = 9;
    private static final int TEXT_HEIGHT = 9;
    private static final int ROW_GAP = 2;
    private static final int GROUP_GAP = 7;

    @Override
    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui) return;

        if (player.isSpectator() || minecraft.getDebugOverlay().showDebugScreen()) return;

        Font font = minecraft.font;
        Aperture aperture = ApertureService.getAperture(player);
        ApertureData data = ApertureService.get(player);
        SoulData soul = SoulService.get(player);
        BodyData body = BodyService.get(player);

        int y = TOP;
        line(graphics, font, y, ModDisplayText.hudHeader(aperture, body));
        y += TEXT_HEIGHT + ROW_GAP;

        if (ApertureService.hasAperture(player)) y = essenceBars(graphics, font, y, data);

        bar(graphics, font, y, soul.currentSoul(), soul.maxSoul(), ModPalette.SOUL);
        y += BAR_HEIGHT + GROUP_GAP;

        line(graphics, font, y, Component.translatable("guzhenren.hud.lifespan", ModDisplayText.hudLifespan(body)));
        y += TEXT_HEIGHT + ROW_GAP;

        if (BodyService.isExtreme(player)) line(graphics, font, y, pressure(player, aperture));
    }

    private static int essenceBars(GuiGraphics graphics, Font font, int top, ApertureData data) {
        int y = top;
        for (int i = 0; i < data.count(); i++) {
            Aperture pool = data.get(i);
            bar(graphics, font, y, pool.currentEssence(), pool.maxEssence(), ModPalette.APERTURE);
            y += BAR_HEIGHT + ROW_GAP;

            if (pool.distilledEssence() > 0L) {
                bar(graphics, font, y, pool.distilledEssence(), pool.maxEssence(), ModPalette.DISTILLED_FILL);
                y += BAR_HEIGHT + ROW_GAP;
            }
        }
        return y;
    }

    private static Component pressure(LocalPlayer player, Aperture aperture) {
        return aperture.pressure() == Aperture.PRESSURE_COUNTDOWN_START && aperture.pressureDeadlineTick() > 0L
                ? Component.translatable("guzhenren.hud.aperture_pressure_cd", aperture.pressure(),
                ModDisplayText.countdown(AperturePressureService.getRemainingTicks(player)))
                : Component.translatable("guzhenren.hud.aperture_pressure", aperture.pressure());
    }

    private static void line(GuiGraphics graphics, Font font, int y, Component text) {
        graphics.drawString(font, text, LEFT, y, ModPalette.TEXT, true);
    }

    private static void bar(GuiGraphics graphics, Font font, int y, long current, long max, int fill) {
        int right = LEFT + BAR_WIDTH;
        graphics.fill(LEFT, y, right, y + BAR_HEIGHT, ModPalette.BAR_BORDER);
        graphics.fill(LEFT + 1, y + 1, right - 1, y + BAR_HEIGHT - 1, ModPalette.BAR_TRACK);

        int track = BAR_WIDTH - 2;
        int filled = max <= 0L ? 0 : Math.clamp(Math.round(track * (current / (double) max)), 0, track);
        if (filled > 0) {
            graphics.fill(LEFT + 1, y + 1, LEFT + 1 + filled, y + BAR_HEIGHT - 1, fill);
        }

        String text = ModDisplayText.pool(current, max);
        graphics.drawString(font, text, LEFT + (BAR_WIDTH - font.width(text)) / 2, y + 1, ModPalette.TEXT, true);
    }
}
