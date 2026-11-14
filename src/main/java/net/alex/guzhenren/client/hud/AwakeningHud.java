package net.alex.guzhenren.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitual;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 本地屏幕上的开窍[Awakening]仪式：缓慢闪白与资质[Aptitude]牌匾。
 *
 * <ul>
 *   <li>注册在全部其他层之上（{@code ClientEvents}），全白盖住整个 HUD；不持状态，每帧由同步的仪式与时钟画出，仪式中断
 *       （死亡、换维度）即停画。</li>
 *   <li>{@link #getBarBase} 给 {@link ChargeHud} 供资质条；读仪式，因为蛊点击时已耗掉。</li>
 *   <li>全屏闪白，十绝天资由白转 {@link #TEN_EXTREMES_FLASH} 淡鹅黄（{@link AwakeningTimeline#getFlashTint}）；HUD 隐藏
 *       仍覆盖。</li>
 *   <li>牌匾（{@link AwakeningPlaque}）随 HUD 隐藏，头顶那块也一样；只在第一人称显示，因脱离的镜头看得到头顶那块；按不
 *       超过 {@link #MAX_PLAQUE_SCALE} 的最大整数倍画，高不超过屏高的 {@link #PLAQUE_SCREEN_FRACTION}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningTimeline
 * @since 1.0.0
 */

public final class AwakeningHud {

    public static final AwakeningHud INSTANCE = new AwakeningHud();

    private AwakeningHud() {}

    private static final int WHITE = 0xFFFFFF;
    private static final int TEN_EXTREMES_FLASH = 0xFFF4C2;
    private static final int MAX_PLAQUE_SCALE = 3;
    private static final float PLAQUE_SCREEN_FRACTION = 0.6F;

    /**
     * 资质条本帧显示的资质，不显示时为 -1；是否显示按整 tick 判断，使 {@link ChargeHud#isShowing} 与所画的帧一致。
     */
    int getBarBase(@NotNull Player player, float partialTick) {
        AwakeningRitual ritual = AwakeningRitualService.get(player);
        long gameTime = player.level().getGameTime();
        if (ritual == null || !AwakeningTimeline.showsBar(ritual.getElapsed(gameTime, 0.0F))) return -1;
        return AwakeningTimeline.getBarBase(ritual.getElapsed(gameTime, partialTick), ritual.base());
    }

    boolean isBarFull(@NotNull Player player, float partialTick) {
        AwakeningRitual ritual = AwakeningRitualService.get(player);
        return ritual != null
                && AwakeningTimeline.isBarFull(ritual.getElapsed(player.level().getGameTime(), partialTick));
    }

    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        AwakeningRitual ritual = AwakeningRitualService.get(player);
        if (ritual == null) return;

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        float elapsed = ritual.getElapsed(player.level().getGameTime(), partialTick);
        if (elapsed < 0.0F || AwakeningTimeline.isOver(elapsed)) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        renderFlash(graphics, ritual, elapsed);
        if (!minecraft.options.hideGui && !minecraft.gameRenderer.getMainCamera().isDetached()) {
            renderPlaque(graphics, ritual, elapsed);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void renderFlash(GuiGraphics graphics, AwakeningRitual ritual, float elapsed) {
        int alpha = Mth.clamp(Math.round(AwakeningTimeline.getFlashAlpha(elapsed) * 255.0F), 0, 255);
        if (alpha == 0) return;

        int rgb = Talent.fromPercent(ritual.base()) == Talent.TEN_EXTREMES
                ? FastColor.ARGB32.lerp(AwakeningTimeline.getFlashTint(elapsed), WHITE, TEN_EXTREMES_FLASH) : WHITE;
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), FastColor.ARGB32.color(alpha, rgb));
    }

    private static void renderPlaque(GuiGraphics graphics, AwakeningRitual ritual, float elapsed) {
        float alpha = AwakeningTimeline.getPlaqueAlpha(elapsed);
        AwakeningPlaque plaque = AwakeningPlaque.of(ritual);
        if (alpha <= 0.0F || plaque == null) return;

        int scale = getPlaqueScale(graphics.guiHeight(), plaque.height());
        int width = AwakeningPlaque.WIDTH * scale;
        int height = plaque.height() * scale;
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blitSprite(plaque.sprite(), (graphics.guiWidth() - width) / 2, (graphics.guiHeight() - height) / 2,
                width, height);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    static int getPlaqueScale(int screenHeight, int plaqueHeight) {
        return Mth.clamp((int) (screenHeight * PLAQUE_SCREEN_FRACTION / plaqueHeight), 1, MAX_PLAQUE_SCALE);
    }
}
