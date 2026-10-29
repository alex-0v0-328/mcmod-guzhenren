package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.ModPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared layout for the bars drawn over the hotbar: geometry, the baseline that armor and mounts move,
 * and the three-fill draw with its caption that every such bar reuses.
 *
 * <p>Package-private abstract base for {@link net.alex.guzhenren.client.hud.ChargeHud},
 * {@link net.alex.guzhenren.client.hud.NourishHud} and {@link net.alex.guzhenren.client.hud.MugwortShieldHud}.
 * The {@code barTop} method rides vanilla's held-item-name baseline so armor and a mount's health push the bar up
 * instead of hiding it. All drawing is {@code g.fill} plus the caption, no textures, in one {@link GuiBatch}. Each
 * bar's {@link #render} draws it for the frame, called from the loader's HUD layer ({@code ClientEvents}).
 *
 * <p>Several bars can be up at once (a charged Gu held while nourishing under a shield): {@link #getStackShift}
 * lifts each later one clear of the one below's caption, so they stack instead of overlapping.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.hud.ChargeHud
 * @see net.alex.guzhenren.client.hud.NourishHud
 * @since 1.0.0
 */

abstract class HotbarHud {

    static final int BAR_WIDTH = 182;
    static final int BAR_HEIGHT = 5;
    static final int TEXT_GAP = 3;
    static final int NAME_GAP = 4;
    static final int MIN_SHIFT = 59;
    static final int CREATIVE_LIFT = 14;
    static final int STACK_GAP = 3;

    public abstract void render(@NotNull GuiGraphics graphics);

    static int getBarTop(Minecraft minecraft) {
        Gui gui = minecraft.gui;
        int shift = Math.max(Math.max(gui.leftHeight, gui.rightHeight), MIN_SHIFT);
        int baseline = minecraft.getWindow().getGuiScaledHeight() - shift;
        if (minecraft.gameMode != null && !minecraft.gameMode.canHurtPlayer()) baseline += CREATIVE_LIFT;
        return baseline - NAME_GAP - BAR_HEIGHT;
    }

    static int getStackShift(Minecraft minecraft) {
        return minecraft.font.lineHeight + TEXT_GAP + BAR_HEIGHT + STACK_GAP;
    }

    static void draw(GuiGraphics g, Minecraft mc, int x, int y, float fraction, int fill, @Nullable Component label) {
        GuiBatch.draw(g, () -> {
            g.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, ModPalette.BAR_BORDER);
            g.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, ModPalette.BAR_TRACK);
            g.fill(x, y, x + Math.round(BAR_WIDTH * Mth.clamp(fraction, 0.0F, 1.0F)), y + BAR_HEIGHT, fill);
            if (label != null) {
                g.drawString(mc.font, label, x + (BAR_WIDTH - mc.font.width(label)) / 2,
                        y - TEXT_GAP - mc.font.lineHeight, ModPalette.TEXT, true);
            }
        });
    }
}
