package net.alex.guzhenren.client.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 全屏辉光渲染状态辅助类：启用按 Alpha 加法混合（如同世界环般提亮下方画面）并关闭深度测试以避免被前景遮挡。
 * <ul>
 *   <li>{@link ShieldGlowHud} 与 {@link SonicBoomHud} 在 {@link #begin} 与 {@link #end} 之间执行绘制，并共用
 *       {@link #getEffectScale} 读取原版「屏幕扭曲效果」滑条缩放系数。</li>
 * </ul>
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
