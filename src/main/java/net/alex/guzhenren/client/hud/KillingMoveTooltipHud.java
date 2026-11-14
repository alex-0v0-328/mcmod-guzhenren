package net.alex.guzhenren.client.hud;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
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
 * 杀招[Killing Move]命中后屏幕左缘垂直居中的提示面板。
 * <ul>
 *   <li>标题为招名；所用蛊每只一行、主蛊在前：图标、转数、道[Path]、名称，下附耐久条式饱食[Hunger]条（满绿空红）与
 *       {@code x/y}；内容是 {@code KillingMoveTooltipPayload} 施放即刻的快照，施放中耗死的蛊画灰、条空。</li>
 *   <li>注册在快捷栏上，通知处理器在客户端初始化时设（{@code ClientEvents}）；宽 {@link #MIN_WIDTH} GUI 像素，
 *       不够才加宽；按墙钟 {@link #SLIDE_IN_MS} 缓出滑入，停至施放后 {@link #SHOWN_MS}，{@link #SLIDE_OUT_MS}
 *       缓入滑出并淡出（图标只滑）；滑出中再施放则原位折回（{@link #getEnterElapsed}），显示中再施放则换行、重计时、
 *       不重滑。</li>
 * </ul>
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
    private @Nullable Panel panel;
    private long castAt;
    private long enteredAt;

    public void show(@NotNull KillingMoveTooltipPayload notice) { show(notice, Util.getMillis()); }

    void show(KillingMoveTooltipPayload notice, long now) {
        float offset = shown == null ? 1.0F : getOffset(now - enteredAt, now - castAt);
        enteredAt = now - getEnterElapsed(offset);
        castAt = now;
        shown = notice;
    }

    public void clear() {
        shown = null;
        panel = null;
    }

    /** 面板移出屏幕的比例：0 在原位，1 完全隐藏。 */
    static float getOffset(long sinceEnter, long sinceCast) {
        float entering = 1.0F - clamp(sinceEnter / (float) SLIDE_IN_MS);
        float leaving = clamp((sinceCast - SHOWN_MS) / (float) SLIDE_OUT_MS);
        return Math.max(entering * entering * entering, leaving * leaving * leaving);
    }

    /** 滑入进行到面板恰在 {@code offset} 处的时长，折回时不跳变。 */
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
            clear();
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.player.isSpectator()) return;

        Font font = minecraft.font;
        Panel current = panel;
        if (current == null || current.notice() != notice) {
            current = Panel.of(notice, font);
            panel = current;
        }
        int width = current.width();
        int height = HEADER_HEIGHT + current.rows().size() * ROW_HEIGHT + 2;
        int x = LEFT - Math.round((LEFT + width) * getOffset(now - enteredAt, now - castAt));
        int y = (graphics.guiHeight() - height) / 2;
        int alpha = Math.round(getAlpha(now - castAt) * 255.0F);

        graphics.fill(x, y, x + width, y + height, fade(BACKGROUND, alpha));
        graphics.fill(x, y, x + 2, y + height, fade(ACCENT, alpha));
        graphics.fill(x + 6, y + HEADER_HEIGHT - 1, x + width - 4, y + HEADER_HEIGHT, fade(RULE, alpha));
        if (alpha >= MIN_TEXT_ALPHA) {
            graphics.drawString(font, current.title(), x + 6, y + 3, fade(ModPalette.TEXT, alpha), true);
        }
        for (int i = 0; i < current.rows().size(); i++) {
            drawRow(graphics, font, current.rows().get(i), x, y + HEADER_HEIGHT + 1 + i * ROW_HEIGHT, alpha);
        }
    }

    private static void drawRow(GuiGraphics graphics, Font font, Row row, int x, int y, int alpha) {
        graphics.renderItem(row.stack(), x + 6, y + 3);
        if (row.spent()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 200.0F);
            graphics.fill(x + 6, y + 3, x + 22, y + 19, SPENT_ICON);
            graphics.pose().popPose();
        }
        if (alpha < MIN_TEXT_ALPHA) return;

        int text = fade(row.spent() ? ModPalette.TEXT_DEAD : ModPalette.TEXT, alpha);
        graphics.drawString(font, row.line(), x + TEXT_X, y + 2, text, true);
        if (row.counter() == null) return;

        int barY = y + 15;
        graphics.fill(x + TEXT_X, barY, x + TEXT_X + BAR_WIDTH, barY + 2, fade(0xFF000000, alpha));
        int filled = Math.round(BAR_WIDTH * row.fraction());
        if (filled > 0) {
            graphics.fill(x + TEXT_X, barY, x + TEXT_X + filled, barY + 1,
                    fade(0xFF000000 | Mth.hsvToRgb(row.fraction() / 3.0F, 1.0F, 1.0F), alpha));
        }
        graphics.drawString(font, row.counter(), x + TEXT_X + BAR_WIDTH + 3, y + 12, text, true);
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

    /** 一条通知的文字与宽度，首帧测量一次，不逐帧测。 */
    private record Panel(KillingMoveTooltipPayload notice, Component title, List<Row> rows, int width) {

        static Panel of(KillingMoveTooltipPayload notice, Font font) {
            Component title = getTitle(notice.move());
            int width = Math.max(MIN_WIDTH, 6 + font.width(title) + 4);
            List<Row> rows = new ArrayList<>(notice.used().size());
            for (KillingMoveService.Used gu : notice.used()) {
                Row row = Row.of(gu);
                rows.add(row);
                width = Math.max(width, TEXT_X + font.width(row.line()) + 4);
            }
            return new Panel(notice, title, rows, width);
        }
    }

    /** {@code counter} 为 {@code x/y} 形式的饱食[Hunger]；无饱食的蛊为 null，不画条。 */
    private record Row(ItemStack stack, boolean spent, Component line, float fraction, @Nullable String counter) {

        static Row of(KillingMoveService.Used gu) {
            ItemStack stack = gu.stack();
            Component line = getLine(stack);
            if (!(stack.getItem() instanceof TendedGuItem tended) || tended.getGuProperties().getMaxHunger() <= 0) {
                return new Row(stack, gu.spent(), line, 0.0F, null);
            }
            int max = tended.getGuProperties().getMaxHunger();
            int hunger = gu.spent() ? 0 : Math.min(max, TendedGuItem.getState(stack).hunger());
            return new Row(stack, gu.spent(), line, hunger / (float) max, hunger + "/" + max);
        }
    }

    private static int fade(int argb, int alpha) {
        return FastColor.ARGB32.color(FastColor.ARGB32.alpha(argb) * alpha / 255, argb);
    }
}
