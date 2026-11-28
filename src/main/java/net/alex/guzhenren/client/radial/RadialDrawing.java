package net.alex.guzhenren.client.radial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.alex.guzhenren.client.ModPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

/**
 * 扇形选择器的单帧渲染器。
 * <ul>
 *   <li>读取 {@link RadialMenu} 提供的实时状态并依次绘制扇区背景、条目图标与中心悬停条目名称；悬停权重驱动扇区外推
 *       {@link #POP}、外缘加宽 {@link #GROW} 与图标放大 {@link #ICON_GROW}，且由于物品图标无法半透明淡出而改为随环不透
 *       明度等比缩放。</li>
 *   <li>暗炭金样式（Alex 定）：扇区平涂暗炭、内外缘各一圈 1 像素墨线；悬停扇区提亮一档，并随悬停权重在外缘画
 *       {@link #RIM} 像素金线（金只表焦点）；选中扇区随闪光权重插值至米金；中心悬停名称用金色。</li>
 *   <li>四边形顶点绕序与 {@code GuiGraphics.fill} 等 GUI 填充保持一致；禁用遮罩覆盖在物品图标之上，呈现与原版物品冷却一
 *       致的视觉效果。</li>
 * </ul>
 */

final class RadialDrawing {

    private static final double POP = 4.0D;
    private static final double GROW = 6.0D;
    private static final float ICON_GROW = 0.25F;
    private static final int ARC_STEPS = 64;
    private static final double GAP = 0.025D;
    private static final int SECTOR_FILL = 0xE0120E10;
    private static final int HOVER_FILL = 0xF0524042;
    private static final int FLASH_FILL = 0xE6FFF1C8;
    private static final int EDGE = 0xCC0C090A;
    private static final double RIM = 2.0D;
    private static final int DISABLED_SHADE = 0xA0120E10;
    private static final float LABEL_RISE = 3.0F;

    private RadialDrawing() {}

    static void draw(GuiGraphics graphics, List<RadialMenu.Entry> shown, float[] weights, long sinceOpen, float scale,
                     float alpha, int flashIndex, float flash) {
        drawSectors(graphics, shown.size(), weights, sinceOpen, scale, alpha, flashIndex, flash);
        graphics.flush();
        drawIcons(graphics, shown, weights, sinceOpen, scale, alpha);
    }

    private static void drawSectors(GuiGraphics graphics, int count, float[] weights, long sinceOpen, float scale,
                                    float alpha, int flashIndex, float flash) {
        double centerX = graphics.guiWidth() / 2.0D;
        double centerY = graphics.guiHeight() / 2.0D;
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f matrix = graphics.pose().last().pose();
        for (int i = 0; i < count; i++) {
            float appear = RadialMotion.getAppear(sinceOpen, i);
            if (appear <= 0.0F) continue;

            double angle = RadialLayout.centerAngle(i, count);
            double half = count == 1 ? Math.PI : Math.PI / count - GAP;
            double size = scale * appear;
            double push = POP * weights[i] * size;
            int color = RadialMotion.mix(SECTOR_FILL, HOVER_FILL, weights[i]);
            if (i == flashIndex) color = RadialMotion.mix(color, FLASH_FILL, flash);
            double x = centerX + RadialLayout.x(push, angle);
            double y = centerY + RadialLayout.y(push, angle);
            double inner = RadialLayout.INNER_RADIUS * size;
            double outer = (RadialLayout.OUTER_RADIUS + GROW * weights[i]) * size;
            fillArc(buffer, matrix, x, y, inner, outer, angle - half, angle + half,
                    RadialMotion.fade(color, alpha * appear));
            fillArc(buffer, matrix, x, y, inner, inner + size, angle - half, angle + half,
                    RadialMotion.fade(EDGE, alpha * appear));
            fillArc(buffer, matrix, x, y, outer - size, outer, angle - half, angle + half,
                    RadialMotion.fade(EDGE, alpha * appear));
            if (weights[i] > 0.0F) {
                fillArc(buffer, matrix, x, y, outer - RIM * size, outer, angle - half, angle + half,
                        RadialMotion.fade(ModPalette.ACCENT, alpha * appear * weights[i]));
            }
        }
    }

    private static void drawIcons(GuiGraphics graphics, List<RadialMenu.Entry> shown, float[] weights,
                                  long sinceOpen, float scale, float alpha) {
        double centerX = graphics.guiWidth() / 2.0D;
        double centerY = graphics.guiHeight() / 2.0D;
        int count = shown.size();
        PoseStack pose = graphics.pose();
        for (int i = 0; i < count; i++) {
            float appear = RadialMotion.getAppear(sinceOpen, i);
            if (appear <= 0.0F) continue;

            double angle = RadialLayout.centerAngle(i, count);
            double radius = (RadialLayout.ICON_RADIUS + GROW / 2.0D * weights[i] + POP * weights[i]) * scale * appear;
            float iconScale = (1.0F + ICON_GROW * weights[i]) * scale * appear * alpha;
            pose.pushPose();
            pose.translate(centerX + RadialLayout.x(radius, angle), centerY + RadialLayout.y(radius, angle), 0.0D);
            pose.scale(iconScale, iconScale, 1.0F);
            graphics.renderItem(shown.get(i).icon(), -8, -8);
            if (!shown.get(i).enabled()) {
                pose.translate(0.0F, 0.0F, 200.0F);
                graphics.fill(RenderType.guiOverlay(), -8, -8, 8, 8, DISABLED_SHADE);
            }
            pose.popPose();
        }
    }

    static void drawLabel(GuiGraphics graphics, Component label, float alpha, float rise) {
        int opacity = Math.round(alpha * 255.0F);
        if (opacity < 4) return;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, (1.0F - rise) * LABEL_RISE, 0.0F);
        graphics.drawCenteredString(Minecraft.getInstance().font, label, graphics.guiWidth() / 2,
                graphics.guiHeight() / 2 - 4, opacity << 24 | ModPalette.ACCENT & 0xFFFFFF);
        pose.popPose();
    }

    private static void fillArc(VertexConsumer buffer, Matrix4f matrix, double centerX, double centerY, double inner,
                                double outer, double from, double to, int color) {
        int steps = Math.max(2, (int) Math.ceil((to - from) / (2.0D * Math.PI) * ARC_STEPS));
        for (int s = 0; s < steps; s++) {
            double a0 = from + (to - from) * s / steps;
            double a1 = from + (to - from) * (s + 1) / steps;
            vertex(buffer, matrix, centerX, centerY, inner, a0, color);
            vertex(buffer, matrix, centerX, centerY, inner, a1, color);
            vertex(buffer, matrix, centerX, centerY, outer, a1, color);
            vertex(buffer, matrix, centerX, centerY, outer, a0, color);
        }
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, double centerX, double centerY, double radius,
                               double angle, int color) {
        buffer.addVertex(matrix, (float) (centerX + RadialLayout.x(radius, angle)),
                (float) (centerY + RadialLayout.y(radius, angle)), 0.0F).setColor(color);
    }
}
