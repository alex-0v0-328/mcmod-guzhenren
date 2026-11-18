package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.client.cinematic.CultivationCamera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * 温养[Nourish]运镜（{@link CultivationCamera}）的上下黑边与切镜黑屏绘制器。
 * <ul>
 *   <li>注册在全部 HUD 图层之下（{@code ClientEvents}），运镜期间其余图层由白名单隐藏，温养条则改画在下黑边正中（
 *       {@link #getBottomCenter}）。</li>
 *   <li>上下黑边各占屏幕高度的 {@link #BAR_FRACTION} 并在 0.5 秒内滑入滑出；切镜黑屏铺满全屏且压在黑边之下。</li>
 * </ul>
 */

public final class CinematicHud {

    public static final CinematicHud INSTANCE = new CinematicHud();

    private CinematicHud() {}

    static final float BAR_FRACTION = 0.1F;
    private static final int BLACK = 0xFF000000;

    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        if (!CultivationCamera.isShowing()) return;
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        float fade = CultivationCamera.getFade(partialTick);
        if (fade > 0.0F) graphics.fill(0, 0, width, height, FastColor.ARGB32.color(Mth.ceil(fade * 255.0F), BLACK));
        int bar = Math.round(getBarHeight(height) * CultivationCamera.getBars(partialTick));
        if (bar <= 0) return;
        graphics.fill(0, 0, width, bar, BLACK);
        graphics.fill(0, height - bar, width, height, BLACK);
    }

    static int getBarHeight(int height) { return Math.round(height * BAR_FRACTION); }

    static int getBottomCenter(int height) { return height - getBarHeight(height) / 2; }
}
