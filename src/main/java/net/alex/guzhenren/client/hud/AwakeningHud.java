package net.alex.guzhenren.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitual;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The awakening ritual [开窍仪式] in the local player's own eyes: the white halo [白色光晕] and the aptitude plaque
 * [资质牌匾]. Registered above every other layer ({@code ClientEvents}), so the full white covers the whole HUD.
 *
 * <p>The server's ritual ends at {@link AwakeningTimeline#RELEASE_TICKS}, so this class keeps its own copy of the
 * start tick and plays the rest after the synced ritual is gone. {@link #update} runs every client tick: a new
 * ritual starts the copy; a ritual gone more than {@link #CUT_SHORT_SLACK_TICKS} before the awakening was cut short
 * (a death, a dimension change) and draws nothing more. {@link #holdsStill} is the stance hold the input lock reads,
 * which ends with the synced ritual and survives a removal packet that lands a little early. {@link #getBarBase} feeds {@link ChargeHud} the aptitude bar,
 * which reads the ritual because the Gu was spent at the click.
 *
 * <p>The halo is {@link #HALO}, white and clear in the middle, stretched over a hole {@link
 * AwakeningTimeline#getHaloOpening} screens wide, with plain white filling the screen around it. The plaque is a GUI
 * sprite named after the rolled grade ({@code plaque/talent/<grade>}), or after the physique for Ten Extremes
 * ({@code plaque/extreme_physique/<physique>}), drawn at the largest whole scale up to {@link #MAX_PLAQUE_SCALE} that
 * keeps it within {@link #PLAQUE_SCREEN_FRACTION} of the screen height.
 *
 * <p>⚠ A Ten Extremes plaque waits for the physique: the server rolls it in the same awakening, and its body sync
 * may land a packet after the ritual's removal. Until it arrives the plaque draws nothing.
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningTimeline
 * @since 1.0.0
 */

public final class AwakeningHud {

    public static final AwakeningHud INSTANCE = new AwakeningHud();

    private AwakeningHud() {}

    private static final ResourceLocation HALO = Guzhenren.id("textures/gui/awakening_halo.png");
    private static final int HALO_SIZE = 256;
    private static final String TALENT_PLAQUE = "plaque/talent/";
    private static final String PHYSIQUE_PLAQUE = "plaque/extreme_physique/";
    private static final int PLAQUE_WIDTH = 16;
    private static final int TALENT_PLAQUE_HEIGHT = 52;
    private static final int PHYSIQUE_PLAQUE_HEIGHT = 64;
    private static final int MAX_PLAQUE_SCALE = 3;
    private static final float PLAQUE_SCREEN_FRACTION = 0.6F;
    static final int CUT_SHORT_SLACK_TICKS = 10;
    private static final long NOT_RUNNING = Long.MIN_VALUE;
    private long startTick = NOT_RUNNING;
    private int base;
    private boolean revealed;
    private @Nullable ResourceLocation plaque;
    private int plaqueHeight;

    public void update(@NotNull LocalPlayer player) {
        AwakeningRitual ritual = AwakeningRitualService.get(player);
        if (ritual != null) {
            if (ritual.startTick() != startTick) begin(ritual);
            return;
        }
        if (startTick == NOT_RUNNING) return;

        float elapsed = getElapsed(player, 0.0F);
        if (elapsed < AwakeningTimeline.AWAKEN_TICKS - CUT_SHORT_SLACK_TICKS || AwakeningTimeline.isOver(elapsed)) {
            reset();
            return;
        }
        if (!revealed && elapsed >= AwakeningTimeline.RELEASE_TICKS) {
            revealed = true;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0F));
        }
    }

    public void reset() {
        startTick = NOT_RUNNING;
        revealed = false;
        plaque = null;
    }

    public boolean holdsStill(@NotNull Player player) {
        return startTick != NOT_RUNNING && AwakeningTimeline.holdsStill(getElapsed(player, 0.0F));
    }

    /**
     * The aptitude the bar shows this frame, or -1 when no bar shows. Whether it shows goes by the whole tick, so
     * {@link ChargeHud#isShowing} and the frame it draws agree.
     */
    int getBarBase(@NotNull Player player, float partialTick) {
        if (startTick == NOT_RUNNING || !AwakeningTimeline.showsBar(getElapsed(player, 0.0F))) return -1;
        return AwakeningTimeline.getBarBase(getElapsed(player, partialTick), base);
    }

    boolean isBarFull(@NotNull Player player, float partialTick) {
        return AwakeningTimeline.isBarFull(getElapsed(player, partialTick));
    }

    private void begin(AwakeningRitual ritual) {
        reset();
        startTick = ritual.startTick();
        base = ritual.base();
    }

    private float getElapsed(Player player, float partialTick) {
        return (float) (player.level().getGameTime() - startTick) + partialTick;
    }

    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || startTick == NOT_RUNNING) return;

        float elapsed = getElapsed(player, deltaTracker.getGameTimeDeltaPartialTick(false));
        if (elapsed < 0.0F || AwakeningTimeline.isOver(elapsed)) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        renderHalo(graphics, elapsed);
        renderPlaque(graphics, player, elapsed);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    //region the white halo
    private static void renderHalo(GuiGraphics graphics, float elapsed) {
        float alpha = AwakeningTimeline.getHaloAlpha(elapsed);
        if (alpha <= 0.0F) return;

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float opening = AwakeningTimeline.getHaloOpening(elapsed);
        int holeWidth = Math.round(width * opening);
        int holeHeight = Math.round(height * opening);
        int left = (width - holeWidth) / 2;
        int top = (height - holeHeight) / 2;
        if (holeWidth > 0 && holeHeight > 0) {
            graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
            graphics.blit(HALO, left, top, holeWidth, holeHeight, 0.0F, 0.0F, HALO_SIZE, HALO_SIZE, HALO_SIZE,
                    HALO_SIZE);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        int white = Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24 | 0xFFFFFF;
        int right = left + holeWidth;
        int bottom = top + holeHeight;
        if (top > 0) graphics.fill(0, 0, width, top, white);
        if (bottom < height) graphics.fill(0, Math.max(bottom, 0), width, height, white);
        if (left > 0) graphics.fill(0, Math.max(top, 0), left, Math.min(bottom, height), white);
        if (right < width) graphics.fill(right, Math.max(top, 0), width, Math.min(bottom, height), white);
    }
    //endregion

    //region the aptitude plaque
    private void renderPlaque(GuiGraphics graphics, Player player, float elapsed) {
        float alpha = AwakeningTimeline.getPlaqueAlpha(elapsed);
        if (alpha <= 0.0F || !resolvePlaque(player)) return;

        int scale = getPlaqueScale(graphics.guiHeight(), plaqueHeight);
        int width = PLAQUE_WIDTH * scale;
        int height = plaqueHeight * scale;
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blitSprite(plaque, (graphics.guiWidth() - width) / 2, (graphics.guiHeight() - height) / 2,
                width, height);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private boolean resolvePlaque(Player player) {
        if (plaque != null) return true;
        Talent talent = Talent.fromPercent(base);
        if (talent == Talent.NONE) return false;
        if (talent != Talent.TEN_EXTREMES) {
            plaque = Guzhenren.id(TALENT_PLAQUE + talent.getSerializedName());
            plaqueHeight = TALENT_PLAQUE_HEIGHT;
            return true;
        }
        ExtremePhysique physique = BodyService.getExtremePhysique(player);
        if (physique == ExtremePhysique.NONE) return false;
        plaque = Guzhenren.id(PHYSIQUE_PLAQUE + physique.getSerializedName());
        plaqueHeight = PHYSIQUE_PLAQUE_HEIGHT;
        return true;
    }

    static int getPlaqueScale(int screenHeight, int plaqueHeight) {
        return Mth.clamp((int) (screenHeight * PLAQUE_SCREEN_FRACTION / plaqueHeight), 1, MAX_PLAQUE_SCALE);
    }
    //endregion
}
