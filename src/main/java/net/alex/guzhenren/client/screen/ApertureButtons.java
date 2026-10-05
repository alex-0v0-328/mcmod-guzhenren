package net.alex.guzhenren.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.network.payload.ImpactApertureWallPayload;
import net.alex.guzhenren.network.payload.NourishAperturePayload;
import net.alex.guzhenren.network.payload.OpenApertureStoragePayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The buttons under each aperture [空窍] in the B panel: nourish [温养空窍] (or flush [冲刷窍壁] for the first
 * aperture once it can strike the wall), then storage [空窍存储]. {@link #stack} decides which buttons an
 * aperture shows, {@link #draw} paints one and {@link #click} sends its payload; where a stack sits is the
 * panel's business.
 *
 * <p>The nourish button shows the run's progress as a fill while it is the running target, and a click on
 * it then cancels; starting a run closes the panel. A button whose action cannot be afforded is drawn dead.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerInfoScreen
 * @since 1.0.0
 */

final class ApertureButtons {

    private ApertureButtons() {}

    static final int NOURISH = 0;
    static final int IMPACT = 1;
    static final int STORAGE = 2;
    private static final int PROGRESS = 0x804FC3F7;
    private static final String KEY_NOURISH = "guzhenren.screen.nourish";
    private static final String KEY_NOURISH_STOP = "guzhenren.screen.nourish_stop";
    private static final String KEY_NOURISH_SECOND = "guzhenren.screen.nourish_second";
    private static final String KEY_IMPACT = "guzhenren.screen.impact";
    private static final String KEY_STORAGE = "guzhenren.screen.button.storage";

    record StackButton(int aperture, int kind, String key, int top) {}

    record ColumnButton(int aperture, int kind, String key, int x0, int x1, int top) {}

    static List<StackButton> stack(LocalPlayer player, int aperture) {
        List<StackButton> buttons = new ArrayList<>();
        int step = PlayerInfoScreen.BTN_H + PlayerInfoScreen.BTN_GAP;
        if (aperture == ApertureData.PRIMARY && ApertureNourishService.canImpact(player)) {
            buttons.add(new StackButton(aperture, IMPACT, KEY_IMPACT, 0));
            return withStorage(aperture, buttons, step);
        }
        if (!ApertureNourishService.isAtCeiling(player, aperture)) {
            String key = isRunning(player, aperture) ? KEY_NOURISH_STOP
                    : ApertureService.getAperture(player, aperture).second() ? KEY_NOURISH_SECOND : KEY_NOURISH;
            buttons.add(new StackButton(aperture, NOURISH, key, 0));
            return withStorage(aperture, buttons, step);
        }
        return withStorage(aperture, buttons, 0);
    }

    static void draw(GuiGraphics graphics, Font font, LocalPlayer player, ColumnButton button, int mouseX,
                     int mouseY, int accent) {
        boolean hover = inBox(mouseX, mouseY, button.x0(), button.x1(), button.top());
        int x0 = button.x0();
        int x1 = button.x1();
        int top = button.top();
        int bottom = top + PlayerInfoScreen.BTN_H;
        switch (button.kind()) {
            case IMPACT -> {
                int strike = !ApertureNourishService.canAffordImpact(player) ? ModPalette.BUTTON_DEAD
                        : hover ? ModPalette.BUTTON_HOVER : ModPalette.BUTTON_IDLE;
                graphics.fill(x0, top, x1, bottom, strike);
                graphics.renderOutline(x0, top, x1 - x0, PlayerInfoScreen.BTN_H, accent);
            }
            case NOURISH -> {
                boolean running = isRunning(player, button.aperture());
                int fill = running ? ModPalette.BUTTON_HOVER
                        : ApertureNourishService.canNourish(player, button.aperture()) ? ModPalette.BUTTON_IDLE
                        : ModPalette.BUTTON_DEAD;
                graphics.fill(x0, top, x1, bottom, fill);
                if (running) {
                    int done = x0 + Math.round((x1 - x0) * ApertureNourishService.getFraction(player, button.aperture()));
                    graphics.fill(x0, top, done, bottom, PROGRESS);
                }
                graphics.renderOutline(x0, top, x1 - x0, PlayerInfoScreen.BTN_H,
                        running ? accent : ModPalette.BORDER);
            }
            default -> {
                graphics.fill(x0, top, x1, bottom, hover ? ModPalette.BUTTON_HOVER : ModPalette.BUTTON_IDLE);
                graphics.renderOutline(x0, top, x1 - x0, PlayerInfoScreen.BTN_H, ModPalette.BORDER);
            }
        }
        Component text = Component.translatable(button.key());
        graphics.drawString(font, text, x0 + (x1 - x0 - font.width(text)) / 2,
                top + (PlayerInfoScreen.BTN_H - font.lineHeight) / 2, ModPalette.TEXT, false);
    }

    static boolean click(LocalPlayer player, ColumnButton button) {
        switch (button.kind()) {
            case IMPACT -> PacketDistributor.sendToServer(ImpactApertureWallPayload.INSTANCE);
            case NOURISH -> {
                if (isRunning(player, button.aperture())) {
                    PacketDistributor.sendToServer(new NourishAperturePayload(
                            NourishAperturePayload.Action.CANCEL, ApertureNourishService.getTargetIndex(player)));
                } else if (ApertureNourishService.canNourish(player, button.aperture())) {
                    PacketDistributor.sendToServer(new NourishAperturePayload(
                            NourishAperturePayload.Action.START, button.aperture()));
                    return true;
                }
            }
            default -> PacketDistributor.sendToServer(new OpenApertureStoragePayload(button.aperture()));
        }
        return false;
    }

    static boolean inBox(double mouseX, double mouseY, int x0, int x1, int top) {
        return mouseX >= x0 && mouseX < x1 && mouseY >= top && mouseY < top + PlayerInfoScreen.BTN_H;
    }

    private static boolean isRunning(LocalPlayer player, int aperture) {
        return ApertureNourishService.isCultivating(player) && ApertureNourishService.getTargetIndex(player) == aperture;
    }

    private static List<StackButton> withStorage(int aperture, List<StackButton> buttons, int top) {
        buttons.add(new StackButton(aperture, STORAGE, KEY_STORAGE, top));
        return buttons;
    }
}
