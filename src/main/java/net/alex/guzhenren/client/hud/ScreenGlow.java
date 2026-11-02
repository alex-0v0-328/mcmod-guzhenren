package net.alex.guzhenren.client.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The render state of a full-screen glow: drawn additively at an alpha, so it brightens what lies under it like the
 * world rings, with depth off so nothing hides it. {@link ShieldGlowHud} and {@link SonicBoomHud} blit between
 * {@link #begin} and {@link #end}; {@link #getEffectScale} is the vanilla Distortion Effects slider both scale by.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

final class ScreenGlow {

    private ScreenGlow() {}

    static float getEffectScale(Minecraft minecraft) {
        return minecraft.options.screenEffectScale().get().floatValue();
    }

    static void begin(GuiGraphics graphics, float alpha) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
    }

    static void end(GuiGraphics graphics) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
