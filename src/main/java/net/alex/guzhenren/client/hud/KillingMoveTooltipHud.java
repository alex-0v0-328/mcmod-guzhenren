package net.alex.guzhenren.client.hud;

import java.util.List;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.item.gu.killingmove.KillingMove;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveService;
import net.alex.guzhenren.network.payload.KillingMoveTooltipPayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The Killing Move Tooltip [杀招提示]: after a killing move [杀招] lands, a panel at the screen's left edge, centered
 * vertically, names the move and gives each Gu it used, main first, one row: its icon, then rank, path and name, and
 * under them its hunger as a bar drawn like vanilla's durability bar (green full, red empty) with {@code x/y}. Rows
 * show the Gu as {@code KillingMoveTooltipPayload} carried them, right after the cast, never live values; a Gu the
 * cast killed draws gray with an empty bar. Registered above the hotbar, its notice handler set at client setup
 * ({@code ClientEvents}).
 *
 * <p>The motion runs on the wall clock: it slides in over {@link #SLIDE_IN_MS}, easing out, stays until
 * {@link #SHOWN_MS} after the cast, then slides out over {@link #SLIDE_OUT_MS}, easing in, while the panel and text
 * fade; the icons only slide. A cast while it shows swaps the rows and restarts the clock without sliding again, and
 * one while it slides out turns it back from where it is ({@link #getEnterElapsed}).
 *
 * <p>The panel is {@link #MIN_WIDTH} GUI pixels wide, wider only when a row or the title needs it.
 *
 * @author Alex
 * @version 1.0.0
 * @see KillingMoveService#drive
 * @since 1.0.0
 */

public final class KillingMoveTooltipHud {

    public static final KillingMoveTooltipHud INSTANCE = new KillingMoveTooltipHud();

    private KillingMoveTooltipHud() {}

    static final long SLIDE_IN_MS = 150L;
    static final long SHOWN_MS = 5_000L;
    static final long SLIDE_OUT_MS = 250L;
    private static final int LEFT = 4;
    private static final int MIN_WIDTH = 100;
    private static final int HEADER_HEIGHT = 13;
    private static final int ROW_HEIGHT = 22;
    private static final int TEXT_X = 25;
    private static final int BAR_WIDTH = 40;
    private static final int BACKGROUND = 0xC0101018;
    private static final int ACCENT = 0xFF9FC5FF;
    private static final int RULE = 0x30FFFFFF;
    private static final int SPENT_ICON = 0x90101018;
    private static final int MIN_TEXT_ALPHA = 4;
    private @Nullable KillingMoveTooltipPayload shown;
    private long castAt;
    private long enteredAt;

    public void show(@NotNull KillingMoveTooltipPayload notice) { show(notice, Util.getMillis()); }

    void show(KillingMoveTooltipPayload notice, long now) {
        float offset = shown == null ? 1.0F : getOffset(now - enteredAt, now - castAt);
        enteredAt = now - getEnterElapsed(offset);
        castAt = now;
        shown = notice;
    }

    public void clear() { shown = null; }

    /** How far the panel sits off screen: 0 in place, 1 fully hidden. */
    static float getOffset(long sinceEnter, long sinceCast) {
        float entering = 1.0F - clamp(sinceEnter / (float) SLIDE_IN_MS);
        float leaving = clamp((sinceCast - SHOWN_MS) / (float) SLIDE_OUT_MS);
        return Math.max(entering * entering * entering, leaving * leaving * leaving);
    }

    /** The time into the slide-in that leaves the panel at {@code offset}, so a turn back never jumps. */
    static long getEnterElapsed(float offset) {
        return Math.round((1.0D - Math.cbrt(clamp(offset))) * SLIDE_IN_MS);
    }

    static float getAlpha(long sinceCast) { return 1.0F - clamp((sinceCast - SHOWN_MS) / (float) SLIDE_OUT_MS); }

    static boolean isDone(long sinceCast) { return sinceCast >= SHOWN_MS + SLIDE_OUT_MS; }

    private static float clamp(float value) { return Math.clamp(value, 0.0F, 1.0F); }

    public void render(@NotNull GuiGraphics graphics) {
        KillingMoveTooltipPayload notice = shown;
        if (notice == null) return;
        long now = Util.getMillis();
        if (isDone(now - castAt)) {
            shown = null;
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.player.isSpectator()) return;

        Font font = minecraft.font;
        List<KillingMoveService.Used> used = notice.used();
        Component title = getTitle(notice.move());
        int width = MIN_WIDTH;
        width = Math.max(width, 6 + font.width(title) + 4);
        for (KillingMoveService.Used gu : used) width = Math.max(width, TEXT_X + font.width(getLine(gu.stack())) + 4);
        int height = HEADER_HEIGHT + used.size() * ROW_HEIGHT + 2;
        int x = LEFT - Math.round((LEFT + width) * getOffset(now - enteredAt, now - castAt));
        int y = (graphics.guiHeight() - height) / 2;
        int alpha = Math.round(getAlpha(now - castAt) * 255.0F);

        graphics.fill(x, y, x + width, y + height, fade(BACKGROUND, alpha));
        graphics.fill(x, y, x + 2, y + height, fade(ACCENT, alpha));
        graphics.fill(x + 6, y + HEADER_HEIGHT - 1, x + width - 4, y + HEADER_HEIGHT, fade(RULE, alpha));
        if (alpha >= MIN_TEXT_ALPHA) graphics.drawString(font, title, x + 6, y + 3, fade(ModPalette.TEXT, alpha), true);
        for (int i = 0; i < used.size(); i++) {
            drawRow(graphics, font, used.get(i), x, y + HEADER_HEIGHT + 1 + i * ROW_HEIGHT, alpha);
        }
    }

    private static void drawRow(GuiGraphics graphics, Font font, KillingMoveService.Used gu, int x, int y,
                                int alpha) {
        ItemStack stack = gu.stack();
        graphics.renderItem(stack, x + 6, y + 3);
        if (gu.spent()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 200.0F);
            graphics.fill(x + 6, y + 3, x + 22, y + 19, SPENT_ICON);
            graphics.pose().popPose();
        }
        if (alpha < MIN_TEXT_ALPHA) return;

        int text = fade(gu.spent() ? ModPalette.TEXT_DEAD : ModPalette.TEXT, alpha);
        graphics.drawString(font, getLine(stack), x + TEXT_X, y + 2, text, true);
        if (!(stack.getItem() instanceof TendedGuItem tended) || tended.getSpec().getMaxHunger() <= 0) return;

        int max = tended.getSpec().getMaxHunger();
        int hunger = gu.spent() ? 0 : Math.min(max, TendedGuItem.getState(stack).hunger());
        float fraction = hunger / (float) max;
        int barY = y + 15;
        graphics.fill(x + TEXT_X, barY, x + TEXT_X + BAR_WIDTH, barY + 2, fade(0xFF000000, alpha));
        int filled = Math.round(BAR_WIDTH * fraction);
        if (filled > 0) {
            graphics.fill(x + TEXT_X, barY, x + TEXT_X + filled, barY + 1,
                    fade(0xFF000000 | Mth.hsvToRgb(fraction / 3.0F, 1.0F, 1.0F), alpha));
        }
        graphics.drawString(font, hunger + "/" + max, x + TEXT_X + BAR_WIDTH + 3, y + 12, text, true);
    }

    private static Component getTitle(String id) {
        KillingMove move = KillingMove.byId(id);
        return move == null ? Component.literal(id) : Component.translatable(move.getTranslationKey());
    }

    private static Component getLine(ItemStack stack) {
        MutableComponent line = Component.empty();
        if (stack.getItem() instanceof MortalGuItem gu) {
            line.append(Component.translatable(gu.getRank().getTranslationKey())).append(" ")
                    .append(Component.translatable(gu.getPath().getTranslationKey())).append(" ");
        } else if (stack.getItem() instanceof GuItem gu) {
            line.append(Component.translatable(gu.getPath().getTranslationKey())).append(" ");
        }
        return line.append(stack.getHoverName());
    }

    private static int fade(int argb, int alpha) {
        return FastColor.ARGB32.color(FastColor.ARGB32.alpha(argb) * alpha / 255, argb);
    }
}
