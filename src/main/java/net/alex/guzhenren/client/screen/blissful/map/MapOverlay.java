package net.alex.guzhenren.client.screen.blissful.map;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;

/**
 * 地图叠加层[Map Overlay]：盖在地图贴图上的占地轮廓（合格绿 / 不合格红）、选中框、标记点、玩家头像与等高线数字；坐标走
 * {@link MapProjection}，与贴图逐像素对齐。
 * <ul>
 *   <li>{@link #draw} 把位姿缩放到 1 地图像素 = 2 真实像素、原点移到视口左上角，块内一律用视口地图像素作画；轮廓线
 *       1 地图像素粗、下垫 1 像素暗影，等距视角的斜边按 2:1 像素台阶画（与菱形边重合）。</li>
 *   <li>占地与选中框按列的外框画在平面 {@code y + 1}（y 为地面方块高）；标记点与头像按实体脚下坐标投影。</li>
 *   <li>全部经 {@link GuiGraphics#fill} 与原版文字、头像绘制；调用方可包一层 {@code GuiBatch}，块内先画填充再写字
 *       （{@link #label} 自带底衬，宜放在最后）。</li>
 * </ul>
 */

public final class MapOverlay {

    public static final int FIT = 0xFF66E07A;
    public static final int UNFIT = 0xFFFF5468;
    public static final int SELECTED = 0xFFFFFFFF;
    public static final int PLANNED = 0xFFFFE080;
    public static final int SHADOW = 0xC0101018;
    public static final int LABEL_BACK = 0xA0101018;
    private static final int HEAD = 8;
    private final GuiGraphics graphics;
    private final MapCamera camera;
    private final MapProjection projection;
    private final int originX;
    private final int originY;

    private MapOverlay(GuiGraphics graphics, MapCamera camera) {
        this.graphics = graphics;
        this.camera = camera;
        this.projection = camera.projection();
        this.originX = camera.originX();
        this.originY = camera.originY();
    }

    public static void draw(GuiGraphics graphics, MapCamera camera, int guiX, int guiY, double guiScale,
                            Consumer<MapOverlay> block) {
        double scale = MapCamera.guiPerMapPixel(guiScale);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(guiX, guiY, 0.0F);
        pose.scale((float) scale, (float) scale, 1.0F);
        block.accept(new MapOverlay(graphics, camera));
        pose.popPose();
    }

    public MapCamera camera() { return camera; }

    //region Outlines [轮廓]
    public void footprint(int minX, int minZ, int maxX, int maxZ, int y, boolean fit) {
        outline(minX, minZ, maxX, maxZ, y, fit ? FIT : UNFIT);
    }

    /** 任意颜色的占地轮廓：列含两端，{@code y} 为地面方块高。 */
    public void outline(int minX, int minZ, int maxX, int maxZ, int y, int colour) {
        edges(minX, minZ, maxX + 1, maxZ + 1, y + 1, colour);
    }

    public void select(int x, int z, int y) { edges(x, z, x + 1, z + 1, y + 1, SELECTED); }

    private void edges(int minX, int minZ, int maxX, int maxZ, int plane, int colour) {
        int[] xs = { minX, maxX, maxX, minX };
        int[] zs = { minZ, minZ, maxZ, maxZ };
        int[] screenX = new int[4];
        int[] screenY = new int[4];
        for (int i = 0; i < 4; i++) {
            screenX[i] = (int) Math.round(projection.screenX(xs[i], plane, zs[i])) - originX;
            screenY[i] = (int) Math.round(projection.screenY(xs[i], plane, zs[i])) - originY;
        }
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 4; i++) {
                int next = (i + 1) & 3;
                edge(screenX[i], screenY[i] + 1 - pass, screenX[next], screenY[next] + 1 - pass,
                        pass == 0 ? SHADOW : colour);
            }
        }
    }

    private void edge(int fromX, int fromY, int toX, int toY, int colour) {
        if (fromY == toY) {
            graphics.fill(Math.min(fromX, toX), fromY, Math.max(fromX, toX) + 1, fromY + 1, colour);
            return;
        }
        if (fromX == toX) {
            graphics.fill(fromX, Math.min(fromY, toY), fromX + 1, Math.max(fromY, toY) + 1, colour);
            return;
        }
        int startX = fromY < toY ? fromX : toX;
        int startY = Math.min(fromY, toY);
        int endX = fromY < toY ? toX : fromX;
        int steps = Math.abs(toY - fromY);
        int direction = endX > startX ? 1 : -1;
        for (int step = 0; step < steps; step++) {
            int x = direction > 0 ? startX + 2 * step : startX - 2 * step - 2;
            graphics.fill(x, startY + step, x + 2, startY + step + 1, colour);
        }
    }
    //endregion

    //region Marks [标记]
    public void mark(double x, double y, double z, int colour) {
        int centerX = viewX(x, y, z);
        int centerY = viewY(x, y, z);
        graphics.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, SHADOW);
        graphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, colour);
    }

    public void head(UUID player, double x, double y, double z) {
        int left = viewX(x, y, z) - HEAD / 2;
        int top = viewY(x, y, z) - HEAD;
        graphics.fill(left - 1, top - 1, left + HEAD + 1, top + HEAD + 1, SHADOW);
        Minecraft minecraft = Minecraft.getInstance();
        PlayerInfo info = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(player);
        PlayerFaceRenderer.draw(graphics, info != null ? info.getSkin() : DefaultPlayerSkin.get(player), left, top,
                HEAD);
    }

    /** 在全局地图像素 ({@code globalX}, {@code globalY}) 居中写一段带底衬的文字（等高线数字用）。 */
    public void label(Font font, String text, int globalX, int globalY, int colour) {
        int width = font.width(text);
        int left = globalX - originX - width / 2;
        int top = globalY - originY - font.lineHeight / 2;
        graphics.fill(left - 1, top - 1, left + width + 1, top + font.lineHeight, LABEL_BACK);
        graphics.drawString(font, text, left, top, colour, false);
    }

    public int viewX(double x, double y, double z) {
        return (int) Math.floor(projection.screenX(x, y, z)) - originX;
    }

    public int viewY(double x, double y, double z) {
        return (int) Math.floor(projection.screenY(x, y, z)) - originY;
    }
    //endregion
}
