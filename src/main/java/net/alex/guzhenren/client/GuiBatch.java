package net.alex.guzhenren.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The one place the mod batches GUI drawing.
 *
 * <p>Every {@code GuiGraphics.fill} and {@code drawString} ends in its own flush, so a panel of fifty fills is
 * fifty draw calls. {@link #draw} runs a block through vanilla's {@code drawManaged}, the same call the vanilla HUD
 * uses, which holds the flushes back until the block ends. A batch still ends whenever the render type changes, so
 * a block draws all its fills first and all its text after, which is only allowed where text sits on top of the
 * fills in the first place.
 *
 * <p>⚠ Never nest: the inner call clears the managed flag when it returns, and the outer block's remaining draws
 * flush one by one again. {@code drawManaged} is deprecated in the pinned NeoForge sources; this is the one call
 * site that suppresses the warning.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class GuiBatch {

    private GuiBatch() {}

    @SuppressWarnings("deprecation")
    public static void draw(GuiGraphics graphics, Runnable block) { graphics.drawManaged(block); }
}
