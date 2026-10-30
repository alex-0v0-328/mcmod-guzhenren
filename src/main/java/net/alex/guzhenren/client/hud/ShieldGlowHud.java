package net.alex.guzhenren.client.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ShieldData;
import net.alex.guzhenren.gameplay.body.ShieldLayer;
import net.alex.guzhenren.gameplay.body.ShieldService;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The shield glow [护盾光晕]: white light over the screen edges while a shield [护盾] is up on the local player.
 *
 * <p>Raising a shield over none fades the glow in over {@link #FADE_IN_TICKS}; it then holds at {@link #PEAK_ALPHA}
 * times the points left over the points raised, summed over the layers up. A hit that spends a layer and leaves
 * some shield flashes it {@link #FLASH_ALPHA} brighter, fading back over {@link #FLASH_TICKS}; a layer whose time
 * runs out only lowers it, and the end of the last layer fades it out over {@link #FADE_OUT_TICKS} from where it
 * stood. {@link #tick} reads the synced {@code ShieldData} once a client tick and keeps those moments in game time; a
 * new local player entity (a respawn, a dimension change) starts it over, so the glow fades back in once its shield
 * syncs. {@link #render} draws vanilla's vignette texture
 * additively in white, so the edges brighten instead of darkening, and the vanilla Distortion Effects slider scales
 * it as it does {@link SonicBoomHud}. Registered above the vanilla camera overlays ({@code ClientEvents}).
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldService
 * @since 1.0.0
 */

public final class ShieldGlowHud {

    public static final ShieldGlowHud INSTANCE = new ShieldGlowHud();

    private ShieldGlowHud() {}

    private static final ResourceLocation VIGNETTE =
            ResourceLocation.withDefaultNamespace("textures/misc/vignette.png");
    static final int FADE_IN_TICKS = 10;
    static final int FLASH_TICKS = 5;
    static final int FADE_OUT_TICKS = 6;
    static final float PEAK_ALPHA = 0.35F;
    static final float FLASH_ALPHA = 0.3F;
    private @Nullable LocalPlayer owner;
    private ShieldData shield = ShieldData.DEFAULT;
    private float points;
    private float max;
    private long raisedAt = Long.MIN_VALUE;
    private long flashedAt = Long.MIN_VALUE;
    private long droppedAt = Long.MIN_VALUE;
    private float droppedFrom;

    public void tick(@NotNull LocalPlayer player) {
        if (player != owner) {
            reset();
            owner = player;
        }
        ShieldData next = ShieldService.get(player);
        long now = BodyService.getGameTime(player);
        float nextPoints = next.getPoints(now);
        if (nextPoints > 0.0F && points <= 0.0F) {
            raisedAt = now;
        } else if (nextPoints > 0.0F && isSpent(shield, next)) {
            flashedAt = now;
        } else if (nextPoints <= 0.0F && points > 0.0F) {
            droppedFrom = getAlpha(now, 0.0F);
            droppedAt = now;
        }
        shield = next;
        points = nextPoints;
        max = next.getMax(now);
    }

    public void reset() {
        owner = null;
        shield = ShieldData.DEFAULT;
        points = 0.0F;
        max = 0.0F;
        raisedAt = Long.MIN_VALUE;
        flashedAt = Long.MIN_VALUE;
        droppedAt = Long.MIN_VALUE;
        droppedFrom = 0.0F;
    }

    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isSpectator()) return;

        float alpha = getAlpha(BodyService.getGameTime(player), deltaTracker.getGameTimeDeltaPartialTick(false))
                * minecraft.options.screenEffectScale().get().floatValue();
        if (alpha <= 0.0F) return;

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(VIGNETTE, 0, 0, -90, 0.0F, 0.0F, width, height, width, height);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private float getAlpha(long now, float partialTick) {
        if (points <= 0.0F || max <= 0.0F) {
            float out = getAge(now, droppedAt, partialTick) / FADE_OUT_TICKS;
            return out < 1.0F ? droppedFrom * (1.0F - out) : 0.0F;
        }
        float in = Math.min(getAge(now, raisedAt, partialTick) / FADE_IN_TICKS, 1.0F);
        float flash = getAge(now, flashedAt, partialTick) / FLASH_TICKS;
        return PEAK_ALPHA * Math.min(points / max, 1.0F) * in + (flash < 1.0F ? FLASH_ALPHA * (1.0F - flash) : 0.0F);
    }

    /** Whether a layer still up from the same raising (its end tick unchanged) holds fewer points than before. */
    private static boolean isSpent(@NotNull ShieldData before, @NotNull ShieldData after) {
        for (ShieldLayer layer : before.layers()) {
            ShieldLayer next = after.find(layer.type());
            if (next != null && next.endTick() == layer.endTick() && next.points() < layer.points()) return true;
        }
        return false;
    }

    private static float getAge(long now, long at, float partialTick) {
        return at == Long.MIN_VALUE || now < at ? Float.MAX_VALUE : now - at + partialTick;
    }
}
