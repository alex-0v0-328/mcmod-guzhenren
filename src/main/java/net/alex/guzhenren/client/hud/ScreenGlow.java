package net.alex.guzhenren.client.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 全屏辉光渲染状态：按 alpha 加法混合（像世界环一样提亮下方），关深度测试（不被遮挡）。
 * <ul>
 *   <li>{@link ShieldGlowHud}、{@link SonicBoomHud} 在 {@link #begin}、{@link #end} 间绘制，{@link #getEffectScale}
 *       为二者共用的原版「屏幕扭曲效果」滑条。</li>
 * </ul>
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
