package net.alex.guzhenren.client.screen.immortalaperture;

import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

/**
 * 地灵经营面板的布局与专属图标：两列卡片的位置（{@link Columns}）、地灵面板图标精灵图（{@link #sprite}，在
 * {@code textures/gui/sprites/blissful_land/screen/}）、玩家头像与数量缩写。
 * <ul>
 *   <li>通用部件（按钮、卡片、凹槽、行、开关、键帽、滚动条与对齐文字）都在 {@link net.alex.guzhenren.client.screen.Widgets}，
 *       颜色在 {@link net.alex.guzhenren.client.ModPalette}；本类只留地灵面板自己的布局常量。</li>
 * </ul>
 */

public final class PanelDrawing {

    private PanelDrawing() {}

    public static final int DIM_ICON = 0xFFB3B3B3;
    public static final int MARGIN = 9;
    public static final int GAP = 10;
    public static final int STACK_BELOW = 370;
    public static final int MAX_WIDTH = 622;

    /**
     * 两列卡片的位置：内容宽至多 {@value #MAX_WIDTH}、居中；窗口不足 {@value #STACK_BELOW} 宽时右卡排到左卡下面，两张都占满
     * 内容宽。
     */
    public record Columns(int leftX, int leftW, int rightX, int rightY, int rightW, boolean stacked) {

        public static Columns of(int screenWidth, int top, int leftW, int leftH) {
            int width = contentWidth(screenWidth);
            int x = contentX(screenWidth);
            if (screenWidth < STACK_BELOW) return new Columns(x, width, x, top + leftH + GAP, width, true);
            return new Columns(x, leftW, x + leftW + GAP, top, width - leftW - GAP, false);
        }
    }

    public static int contentWidth(int screenWidth) { return Math.min(screenWidth - 2 * MARGIN, MAX_WIDTH); }

    public static int contentX(int screenWidth) { return (screenWidth - contentWidth(screenWidth)) / 2; }

    public static ResourceLocation sprite(String name) { return Guzhenren.id("blissful_land/screen/" + name); }

    public static void head(GuiGraphics graphics, UUID player, int x, int y) {
        Minecraft minecraft = Minecraft.getInstance();
        PlayerInfo info = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(player);
        PlayerFaceRenderer.draw(graphics, info != null ? info.getSkin() : DefaultPlayerSkin.get(player), x, y, 8);
    }

    public static String count(long value) {
        if (value < 10_000L) return Long.toString(value);
        if (value < 1_000_000L) return value / 1_000L + "k";
        return value / 1_000_000L + "m";
    }
}
