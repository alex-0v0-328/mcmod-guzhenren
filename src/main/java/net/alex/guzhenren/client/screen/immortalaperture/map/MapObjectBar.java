package net.alex.guzhenren.client.screen.immortalaperture.map;

import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 对象栏：选中田或牧场时升到功能栏上方的一条（{@code overlay} 底，Alex 定），田与牧场共用这份排版：表头左写对象名（金）、
 * 右写读数，可选一条进度条，下面一排 {@value #KEY} 见方的键均分整条宽，宽档键下写名字；键的弹窗（选取格、列表、数量窗、行
 * 菜单）由子类画在本条正上方，见 {@link MapPopup}。
 * <ul>
 *   <li>两档（{@link #wide}）：界面宽不到 {@value #WIDE_MIN_W} 或高不到 {@value #WIDE_MIN_H} 用紧凑档（不写键名），缩放 6 的
 *       2560×1600 即紧凑档；功能栏同档。</li>
 *   <li>键的样子由子类给 {@link Face}：图标或一个字（牧场指定的动物）、样式、能不能点、右上角数字。</li>
 *   <li>画图与写字两步：{@link #barSprites} 画底、键与图标，{@link #barFills} 画进度条，{@link #barTexts} 写表头、键上
 *       的字与键名。</li>
 * </ul>
 */

abstract class MapObjectBar {

    static final int KEY = 22;
    static final int WIDTH = 190;
    private static final int SIDE = 6;
    private static final int TITLE_Y = 4;
    private static final int BAR_Y = 14;
    private static final int KEYS_Y = 20;
    private static final int COMPACT_H = KEYS_Y + KEY + 4;
    private static final int WIDE_H = COMPACT_H + 11;
    private static final int GAP = 4;
    private static final int WIDE_MIN_W = 560;
    private static final int WIDE_MIN_H = 320;
    private static final int TRACK = 0xFF2A2328;

    /** 一枚键：{@code icon} 是精灵名（为空就写 {@code text}，两者都空即虚线空键），{@code corner} 是右上角数字（负数不画）。 */
    record Face(@Nullable String icon, @Nullable Component text, PanelDrawing.Look look, boolean live, int corner,
                Component label) {}

    private boolean wide;
    private int boxX;
    private int boxY;
    private int boxH;

    static boolean wide(int width, int height) { return width >= WIDE_MIN_W && height >= WIDE_MIN_H; }

    /** 排在 {@code above}（功能栏顶边）上方。 */
    void layout(int width, int height, int above) {
        wide = wide(width, height);
        boxH = wide ? WIDE_H : COMPACT_H;
        boxX = (width - WIDTH) / 2;
        boxY = above - GAP - boxH;
    }

    int top() { return boxY; }

    /** 对象栏的 x、y、宽、高（排版自检用）。 */
    int[] box() { return new int[] {boxX, boxY, WIDTH, boxH}; }

    int left() { return boxX; }

    /** 对象栏连同上方提示条的顶边。 */
    int hintTop() { return boxY - 5 - MapHud.CHIP_H; }

    boolean barContains(double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, boxX, boxY, WIDTH, boxH);
    }

    protected abstract int keys();

    protected abstract Face face(int key);

    protected int keyX(int key) {
        int pitch = (WIDTH - 2 * SIDE) / keys();
        return boxX + SIDE + key * pitch + (pitch - KEY) / 2;
    }

    protected int keyY() { return boxY + KEYS_Y; }

    /** 键 {@code key} 的中心 x，弹窗按它对齐。 */
    protected int keyCentre(int key) { return keyX(key) + KEY / 2; }

    protected int keyAt(double mouseX, double mouseY) {
        for (int key = 0; key < keys(); key++) {
            if (Widgets.isOver(mouseX, mouseY, keyX(key), keyY(), KEY, KEY)) return key;
        }
        return -1;
    }

    //region Drawing [绘制]
    protected void barSprites(SpriteBatch sprites, double mouseX, double mouseY) {
        sprites.blit(PanelDrawing.OVERLAY, boxX, boxY, WIDTH, boxH);
        for (int key = 0; key < keys(); key++) {
            Face face = face(key);
            boolean empty = face.icon() == null && face.text() == null;
            PanelDrawing.Look look = empty ? PanelDrawing.Look.GHOST : face.look();
            PanelDrawing.button(sprites, keyX(key), keyY(), KEY, KEY, look, face.live(), mouseX, mouseY);
            if (face.icon() == null) continue;
            int sunk = !face.live() || look == PanelDrawing.Look.SELECTED
                    || PanelDrawing.pressed(keyX(key), keyY(), KEY, KEY) ? 1 : 0;
            sprites.blit(PanelDrawing.sprite(face.icon()), keyX(key) + 3, keyY() + 3 + sunk, 16, 16,
                    face.live() ? 0xFFFFFFFF : PanelDrawing.DIM_ICON);
        }
    }

    /** 表头下的进度条，{@code percent} 为负不画。 */
    protected void barFills(GuiGraphics graphics, int percent) {
        if (percent < 0) return;
        int left = boxX + SIDE;
        int right = boxX + WIDTH - SIDE;
        graphics.fill(left, boxY + BAR_Y, right, boxY + BAR_Y + 2, TRACK);
        int filled = (right - left) * Math.clamp(percent, 0, 100) / 100;
        if (filled > 0) graphics.fill(left, boxY + BAR_Y, left + filled, boxY + BAR_Y + 2, ModPalette.PLANT);
    }

    protected void barTexts(GuiGraphics graphics, Font font, Component title, Component readout) {
        graphics.drawString(font, title, boxX + SIDE, boxY + TITLE_Y, PanelDrawing.ACCENT, false);
        PanelDrawing.right(graphics, font, readout, boxX + WIDTH - SIDE, boxY + TITLE_Y, PanelDrawing.LEGEND);
        for (int key = 0; key < keys(); key++) {
            Face face = face(key);
            if (face.icon() == null && face.text() != null) {
                PanelDrawing.label(graphics, font, face.text(), keyX(key), keyY(), KEY, KEY, face.look(), face.live());
            }
            if (face.corner() >= 0) corner(graphics, font, key, face.corner(), face.live());
            if (wide) {
                boolean on = face.look() == PanelDrawing.Look.SELECTED;
                PanelDrawing.centered(graphics, font, face.label(), keyX(key) - 8, keyX(key) + KEY + 8,
                        keyY() + KEY + 3, on ? PanelDrawing.ACCENT : face.live() ? PanelDrawing.TEXT : PanelDrawing.DEAD);
            }
        }
    }

    private void corner(GuiGraphics graphics, Font font, int key, int count, boolean live) {
        String text = PanelDrawing.count(count);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        graphics.drawString(font, text, keyX(key) + KEY - 1 - font.width(text), keyY() + KEY - 9,
                live && count > 0 ? PanelDrawing.TEXT : PanelDrawing.DEAD, true);
        graphics.pose().popPose();
    }

    protected boolean keyTip(MapPage page, GuiGraphics graphics, int key, int mouseX, int mouseY, Component text) {
        return page.tipAt(graphics, keyX(key), keyY(), KEY, KEY, mouseX, mouseY, text);
    }
    //endregion
}
