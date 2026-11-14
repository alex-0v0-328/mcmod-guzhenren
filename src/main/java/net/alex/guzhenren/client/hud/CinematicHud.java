package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.client.cinematic.CultivationCamera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * 温养[Nourish]运镜（{@link CultivationCamera}）的上下黑边与切镜黑屏。
 * <ul>
 *   <li>注册在全部 HUD 层之下（{@code ClientEvents}），运镜期间其余层由白名单隐藏，温养条改画在下黑边正中
 *       （{@link #getBottomCenter}）。</li>
 *   <li>黑边各占屏高 {@link #BAR_FRACTION}，0.5 秒滑入滑出；切镜黑屏铺满画面、压在黑边之下。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see CultivationCamera
 * @since 1.0.0
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
