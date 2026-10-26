package net.alex.guzhenren.client.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * The sonic boom ring [音爆环] in the entering player's own first person: the shockwave ring drawn on the screen,
 * opening from the crosshair past the screen edges, while the world ring
 * ({@code net.alex.guzhenren.particle.SecondStageRingEmitter}) opens behind the camera for everyone else. Registered
 * above the vanilla camera overlays ({@code ClientEvents}).
 *
 * <p>It plays only for an entry into second-stage flight [二段飞行] this client asked for: {@link #noteRequested}
 * marks a double tap that asks to enter ({@code net.alex.guzhenren.client.input.SecondStageInput}), and
 * {@link #onSecondStageStarted} plays the ring when the synced mark turns on within {@link #REQUEST_WINDOW_TICKS}
 * after it ({@code net.alex.guzhenren.client.event.SecondStageEvents}). A mark that turns on with no request, such as
 * the attachment sync after a dimension change, plays nothing. Both ticks are the local player's {@code tickCount},
 * which a new player entity resets, so every window is bounded at both ends.
 *
 * <p>The ring follows the world ring's bloom: Alex's five ring frames ({@link #RINGS}), one per tick, then
 * {@link #LIFETIME_TICKS} minus the frames of linger, fading out. Its diameter eases out from
 * {@link #START_HEIGHT_FRACTION} of the screen height to {@link #END_WIDTH_FRACTION} of the screen width, so the
 * center stays open and the stroke crosses the edges before it fades; the pixels scale with it and read coarse, the
 * same coarse look as the 4-block world ring. It draws additive like the world rings, and the vanilla Distortion
 * Effects slider scales its opacity, so a player who turned screen effects off sees none.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.particle.RingParticle
 * @since 1.0.0
 */

public final class SonicBoomHud {

    public static final SonicBoomHud INSTANCE = new SonicBoomHud();

    private SonicBoomHud() {}

    private static final List<Integer> RING_CANVASES = List.of(3, 8, 13, 18, 23);
    private static final List<ResourceLocation> RINGS = RING_CANVASES.stream()
            .map(size -> Guzhenren.id("textures/particle/ring_" + size + "x" + size + ".png")).toList();
    static final int LIFETIME_TICKS = 8;
    static final float START_HEIGHT_FRACTION = 0.25F;
    static final float END_WIDTH_FRACTION = 1.7F;
    private static final int REQUEST_WINDOW_TICKS = 20;
    private int requestedAt = Integer.MIN_VALUE;
    private int startedAt = Integer.MIN_VALUE;

    public void noteRequested(int tickCount) { requestedAt = tickCount; }

    public void onSecondStageStarted(int tickCount) {
        long sinceRequest = (long) tickCount - requestedAt;
        if (sinceRequest < 0L || sinceRequest > REQUEST_WINDOW_TICKS) return;
        startedAt = tickCount;
        requestedAt = Integer.MIN_VALUE;
    }

    public void reset() {
        requestedAt = Integer.MIN_VALUE;
        startedAt = Integer.MIN_VALUE;
    }

    /** Draws the ring for the frame; the loader's HUD layer calls it, see {@code ClientEvents}. */
    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !minecraft.options.getCameraType().isFirstPerson()) return;

        float age = getAge(player.tickCount, deltaTracker.getGameTimeDeltaPartialTick(false));
        if (age < 0.0F || age >= LIFETIME_TICKS) return;
        float alpha = getAlpha(age) * minecraft.options.screenEffectScale().get().floatValue();
        if (alpha <= 0.0F) return;

        int frame = getFrame(age);
        int canvas = RING_CANVASES.get(frame);
        int diameter = getDiameter(age, graphics.guiWidth(), graphics.guiHeight());
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(RINGS.get(frame), (graphics.guiWidth() - diameter) / 2, (graphics.guiHeight() - diameter) / 2,
                diameter, diameter, 0.0F, 0.0F, canvas, canvas, canvas, canvas);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    /** Ticks since the ring started, partial tick included; the ring plays only inside [0, LIFETIME_TICKS). */
    float getAge(int tickCount, float partialTick) {
        return (float) ((long) tickCount - startedAt) + partialTick;
    }

    static int getFrame(float age) {
        return Mth.clamp((int) age, 0, RING_CANVASES.size() - 1);
    }

    static float getAlpha(float age) {
        int frames = RING_CANVASES.size();
        if (age < frames) return 1.0F;
        return Mth.clamp(1.0F - (age - frames) / (LIFETIME_TICKS - frames), 0.0F, 1.0F);
    }

    static int getDiameter(float age, int screenWidth, int screenHeight) {
        float progress = Mth.clamp(age / LIFETIME_TICKS, 0.0F, 1.0F);
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        return Math.round(Mth.lerp(eased, START_HEIGHT_FRACTION * screenHeight, END_WIDTH_FRACTION * screenWidth));
    }
}
