package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.SpriteBatch;
import net.minecraft.resources.ResourceLocation;

/**
 * A HUD progress bar drawn from two GUI sprites, {@code hud/<name>_bar_background} and {@code hud/<name>_bar_fill}:
 * a frame one pixel larger than the fill on every side, and a grayscale fill tinted with the bar's data color.
 *
 * <p>Both sprites draw unscaled. A part-full bar shows the fill's leftmost {@code filled} columns instead of squeezing
 * the whole sprite, so the marks drawn into a fill (the awakening bar's fifths, the nourish bar's tenths) stay lined up
 * with the frame's.
 *
 * <p>⚠ {@link #width} and {@link #height} are the fill sprite's size; {@code HudBarTexturesTest} pins the PNGs to the
 * layouts that build these.
 *
 * @author Alex
 * @version 1.0.0
 * @see HotbarHud
 * @see PlayerStatsHud
 * @since 1.0.0
 */

record HudBar(ResourceLocation background, ResourceLocation fill, int width, int height) {

    static HudBar of(String name, int width, int height) {
        return new HudBar(Guzhenren.id("hud/" + name + "_bar_background"), Guzhenren.id("hud/" + name + "_bar_fill"),
                width, height);
    }

    /** Draws the bar with its fill's top-left corner at ({@code x}, {@code y}); {@code filled} runs 0 to width. */
    void draw(SpriteBatch sprites, int x, int y, int filled, int tint) {
        sprites.blit(background, x - 1, y - 1, width + 2, height + 2);
        sprites.blitLeft(fill, width, height, x, y, Math.min(filled, width), tint);
    }
}
