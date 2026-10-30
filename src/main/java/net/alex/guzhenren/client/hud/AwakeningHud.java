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
 * The awakening ritual [开窍仪式] on the local player's own screen: the slow white flash [闪白] and the aptitude plaque
 * [资质牌匾]. Registered above every other layer ({@code ClientEvents}), so the full white covers the whole HUD.
 *
 * <p>Holds no state: every frame is drawn from the synced ritual and the clock, so a ritual cut short (a death, a
 * dimension change) simply stops drawing. {@link #getBarBase} feeds {@link ChargeHud} the aptitude bar, which reads
 * the ritual because the Gu was spent at the click.
 *
 * <p>The flash fills the screen white, or for Ten Extremes white turning {@link #TEN_EXTREMES_FLASH} pale goose yellow
 * [淡鹅黄] ({@link AwakeningTimeline#getFlashTint}); it covers a hidden HUD too. The plaque ({@link AwakeningPlaque})
 * hides with the HUD, as the one over the head does, and shows in first person only, since a detached camera sees the
 * one over the head; it is drawn at the largest whole scale up to {@link #MAX_PLAQUE_SCALE} that keeps it within
 * {@link #PLAQUE_SCREEN_FRACTION} of the screen height.
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
     * The aptitude the bar shows this frame, or -1 when no bar shows. Whether it shows goes by the whole tick, so
     * {@link ChargeHud#isShowing} and the frame it draws agree.
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
