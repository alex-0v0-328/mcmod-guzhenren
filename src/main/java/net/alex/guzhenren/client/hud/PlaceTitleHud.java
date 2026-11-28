package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.compat.TravelersTitlesIntegration;
import net.alex.guzhenren.network.payload.PlaceTitlePayload;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 地名标题[Place Title]图层：服务端 {@code PlaceTitles} 决定弹出后，屏幕中上方显示两行字，上行小字类别、下行大字地名。
 * <ul>
 *   <li>节奏按真实时间：淡入 {@value #FADE_IN_MS} ms、停留 {@value #HOLD_MS} ms、淡出 {@value #FADE_OUT_MS} ms；新标题
 *       直接顶替正在显示或排队的。</li>
 *   <li>和 Traveler's Titles 排队：收到时它正显示群系或维度标题，就等它放完再开始，至多等 {@value #MAX_WAIT_MS} ms；本
 *       标题显示期间它新出的标题不画（{@link #isShowing}，由 {@code ClientEvents} 取消它的图层），避免两行字叠在一起。
 *       </li>
 *   <li>两行都逐字加空格拉开；类别 {@value #KIND_SCALE} 倍、暖灰，两侧各一道 {@value #RULE_W} 像素暗金线带金色端点
 *       （Alex 定），地名 {@value #NAME_SCALE} 倍、取载荷颜色，都带阴影。</li>
 *   <li>位置在屏幕中心以上 {@value #NAME_TOP} 至 {@value #KIND_TOP} 像素（GUI 坐标），高过 Traveler's Titles 默认位置
 *       （中心以上 33 像素起、约 19 像素高）。</li>
 *   <li>⚠ 字体在透明度低于 4/255 时会按不透明绘制，所以透明度过低直接不画。</li>
 *   <li>登出时（{@link #onLoggingOut}）清空正在显示与排队的标题，免得旧世界的地名在下一个世界或服务器弹出。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class PlaceTitleHud {

    public static final PlaceTitleHud INSTANCE = new PlaceTitleHud();

    private PlaceTitleHud() {}

    static final long FADE_IN_MS = 500L;
    static final long HOLD_MS = 4000L;
    static final long FADE_OUT_MS = 1000L;
    static final long MAX_WAIT_MS = 10_000L;
    private static final float KIND_SCALE = 1.2F;
    private static final float NAME_SCALE = 3.0F;
    private static final int KIND_TOP = 88;
    private static final int NAME_TOP = 74;
    private static final int KIND_COLOR = 0xD8CCB8;
    private static final int RULE_W = 30;
    private static final int RULE_GAP = 6;
    private static final int MIN_ALPHA = 4;
    private @Nullable PlaceTitlePayload waiting;
    private long waitingSince;
    private @Nullable PlaceTitlePayload shown;
    private long shownAt;

    public void show(@NotNull PlaceTitlePayload title) {
        shown = null;
        waiting = title;
        waitingSince = Util.getMillis();
    }

    public boolean isShowing() { return shown != null; }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        INSTANCE.waiting = null;
        INSTANCE.shown = null;
    }

    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        if (waiting != null && (Util.getMillis() - waitingSince >= MAX_WAIT_MS
                || !TravelersTitlesIntegration.isShowingTitle())) {
            shown = waiting;
            shownAt = Util.getMillis();
            waiting = null;
        }
        PlaceTitlePayload title = shown;
        if (title == null) return;

        long age = Util.getMillis() - shownAt;
        if (age >= FADE_IN_MS + HOLD_MS + FADE_OUT_MS) {
            shown = null;
            return;
        }
        int alpha = Mth.floor(getOpacity(age) * 255.0F);
        if (alpha < MIN_ALPHA) return;

        Font font = Minecraft.getInstance().font;
        int centreY = graphics.guiHeight() / 2;
        String kind = I18n.get(title.kindKey());
        drawRules(graphics, font, kind, centreY - KIND_TOP, alpha);
        drawLine(graphics, font, kind, centreY - KIND_TOP, KIND_SCALE, alpha << 24 | KIND_COLOR);
        drawLine(graphics, font, I18n.get(title.nameKey()), centreY - NAME_TOP, NAME_SCALE,
                alpha << 24 | (title.color() & 0xFFFFFF));
    }

    static float getOpacity(long age) {
        if (age < FADE_IN_MS) return (float) age / FADE_IN_MS;
        if (age < FADE_IN_MS + HOLD_MS) return 1.0F;
        return Mth.clamp(1.0F - (float) (age - FADE_IN_MS - HOLD_MS) / FADE_OUT_MS, 0.0F, 1.0F);
    }

    static @NotNull String spaced(@NotNull String text) {
        StringBuilder spaced = new StringBuilder();
        text.codePoints().forEach(point -> {
            if (!spaced.isEmpty()) spaced.append(' ');
            spaced.appendCodePoint(point);
        });
        return spaced.toString();
    }

    /** 类别行两侧各一道金线，外端一个 2×3 端点。 */
    private static void drawRules(GuiGraphics graphics, Font font, String kind, int top, int alpha) {
        int half = Math.round(font.width(spaced(kind)) * KIND_SCALE / 2.0F) + RULE_GAP;
        int centre = graphics.guiWidth() / 2;
        int y = top + Math.round(font.lineHeight * KIND_SCALE / 2.0F) - 1;
        int line = alpha << 24 | ModPalette.ACCENT_DARK & 0xFFFFFF;
        int cap = alpha << 24 | ModPalette.ACCENT & 0xFFFFFF;
        graphics.fill(centre - half - RULE_W, y, centre - half, y + 1, line);
        graphics.fill(centre + half, y, centre + half + RULE_W, y + 1, line);
        graphics.fill(centre - half - RULE_W - 2, y - 1, centre - half - RULE_W, y + 2, cap);
        graphics.fill(centre + half + RULE_W, y - 1, centre + half + RULE_W + 2, y + 2, cap);
    }

    private static void drawLine(GuiGraphics graphics, Font font, String text, int top, float scale, int argb) {
        String line = spaced(text);
        graphics.pose().pushPose();
        graphics.pose().translate(graphics.guiWidth() / 2.0F, top, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, line, -font.width(line) / 2, 0, argb, true);
        graphics.pose().popPose();
    }
}
