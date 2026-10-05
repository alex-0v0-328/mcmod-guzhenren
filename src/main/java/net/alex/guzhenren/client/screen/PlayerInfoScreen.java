package net.alex.guzhenren.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.client.ModKeyMappings;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.screen.InfoRowRenderer.ScreenRow;
import net.alex.guzhenren.display.InfoModel;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.network.payload.OpenRefinementPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The B panel: every tab of what a player is, read straight off the synced attachments.
 *
 * <p>Extends {@link net.minecraft.client.gui.screens.Screen} (no menu behind it). Six tabs: 空窍,
 * 肉身, 魂魄, 流派造诣, 脑海, 炼蛊. The aperture tab draws one column per aperture -- a lone aperture
 * keeps the single-column layout -- and every column carries its own nourish [温养空窍] /
 * flush [冲刷窍壁] / storage [空窍存储] buttons; the storage button opens that aperture's container.
 * The refinement tab opens its container via a client-intent payload instead of drawing rows. Row
 * content comes from {@link InfoModel}, shared with {@code /gzr
 * info}, so the two surfaces cannot diverge; {@link InfoRowRenderer} turns each entry into the panel's
 * label and value, {@link ApertureButtons} are the buttons under each aperture, and {@link PathPicker} is
 * the secondary-path grid that opens over the panel.
 *
 * <p>⚠ A plain screen with no menu behind it: no container channel to send an intent over.
 *
 * @author Alex
 * @version 1.0.0
 * @see InfoModel
 * @since 1.0.0
 */

public final class PlayerInfoScreen extends Screen {

    private static final float SCREEN_FRACTION = 0.80F;
    private static final int PAD = 12;
    static final int HEADER_H = 22;
    private static final int CONTENT_TOP = HEADER_H + 8;
    private static final int LINE_H = 12;
    private static final int CONTENT_INSET_DIVISOR = 12;
    private static final int TAB_W = 76;
    private static final int TAB_H = 20;
    private static final int TAB_GAP = 4;
    static final int DIVIDER = 0x33FFFFFF;
    static final int ROW_HOVER = 0x14FFFFFF;
    private static final int TAB_IDLE = 0x26FFFFFF;
    private static final int TAB_TEXT_IDLE = 0xFFBBBBBB;
    private static final int TAB_TEXT_DEAD = 0xFF6A6A6A;
    private static final int[] ACCENT = { ModPalette.APERTURE, ModPalette.BODY, ModPalette.SOUL,
            ModPalette.PATH, ModPalette.MIND, ModPalette.REFINEMENT };
    private static final String[] TAB_KEYS = {
            "guzhenren.screen.tab.aperture",
            "guzhenren.screen.tab.body",
            "guzhenren.screen.tab.soul",
            "guzhenren.screen.tab.path",
            "guzhenren.screen.tab.mind",
            "guzhenren.screen.tab.refinement",
    };
    static final int BTN_H = 20;
    static final int BTN_GAP = 4;
    private static final int TAB_APERTURE = 0;
    private static final int TAB_BODY = 1;
    private static final int TAB_SOUL = 2;
    private static final int TAB_PATH = 3;
    private static final int TAB_MIND = 4;
    private static final int TAB_REFINEMENT = 5;
    private static final int SCROLL_W = 2;
    private static final int SCROLL_GAP = 5;
    private static final int COL_GAP = 16;
    private int leftPos;
    private int topPos;
    private int panelW;
    private int panelH;
    private int activeTab;
    private @Nullable InfoRowRenderer.Click hoverClick;
    private int scrollRow;
    private final PathPicker picker = new PathPicker();

    public PlayerInfoScreen() { super(Component.translatable("guzhenren.screen.info.title")); }

    @Override
    protected void init() {
        panelW = Math.round(width * SCREEN_FRACTION);
        panelH = Math.round(height * SCREEN_FRACTION);
        leftPos = (width - panelW) / 2;
        topPos = (height - panelH) / 2;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int right = leftPos + panelW;
        int accent = ACCENT[activeTab];

        graphics.fill(leftPos, topPos, right, topPos + panelH, ModPalette.PANEL_FILL);
        graphics.renderOutline(leftPos, topPos, panelW, panelH, ModPalette.BORDER);

        graphics.drawString(font, Component.translatable(TAB_KEYS[activeTab]),
                leftPos + PAD, topPos + (HEADER_H - font.lineHeight) / 2, accent, false);
        graphics.fill(leftPos + PAD, topPos + HEADER_H, right - PAD, topPos + HEADER_H + 1, DIVIDER);

        renderTabs(graphics, mouseX, mouseY);

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if (activeTab == TAB_APERTURE && twoApertures(player)) {
            renderApertureColumns(graphics, player, apertureGroups(player), mouseX, mouseY, accent);
        } else {
            renderRows(graphics, rows(player), mouseX, mouseY, accent);
            renderBottomButtons(graphics, mouseX, mouseY);
        }
        if (picker.isOpen()) picker.render(graphics, font, leftPos, topPos, panelW, panelH, mouseX, mouseY, accent);
    }

    private void renderRows(GuiGraphics graphics, List<ScreenRow> rows, int mouseX, int mouseY, int accent) {
        int visible = visibleRows();
        int hidden = Math.max(0, rows.size() - visible);
        scrollRow = Mth.clamp(scrollRow, 0, hidden);

        int valueRight = valueRight();
        int rowLeft = contentLeft();
        int y = contentTop();
        hoverClick = null;
        for (int i = scrollRow; i < Math.min(rows.size(), scrollRow + visible); i++) {
            drawRow(graphics, rows.get(i), rowLeft, valueRight, y, mouseX, mouseY, accent);
            y += LINE_H;
        }
        if (hidden > 0) renderScrollBar(graphics, rows.size(), visible, accent);
    }

    private void drawRow(GuiGraphics graphics, ScreenRow row, int x0, int x1, int y, int mouseX, int mouseY,
                         int accent) {
        if (mouseY >= y - 1 && mouseY < y + LINE_H - 1 && mouseX >= x0 - 2 && mouseX < x1 + 2) {
            graphics.fill(x0 - 2, y - 1, x1 + 2, y + LINE_H - 1, ROW_HOVER);
            if (row.click() != null) hoverClick = row.click();
        }
        int labelColor = row.value() == null ? accent : ModPalette.TEXT;
        graphics.drawString(font, row.label(), x0 + row.indent(), y, labelColor, false);
        if (row.value() != null) {
            graphics.drawString(font, row.value(), x1 - font.width(row.value()), y, ModPalette.TEXT, false);
        }
    }

    //region aperture columns -- two apertures render side by side, left first right second
    private static boolean twoApertures(LocalPlayer player) {
        return ApertureService.get(player).count() == 2;
    }

    private List<List<ScreenRow>> apertureGroups(LocalPlayer player) {
        List<List<ScreenRow>> groups = new ArrayList<>();
        List<ScreenRow> current = null;
        for (InfoModel.Row modelRow : InfoModel.aperture(player)) {
            if (modelRow.entry() instanceof InfoModel.Blank) continue;
            ScreenRow drawn = InfoRowRenderer.draw(modelRow.indent(), modelRow.entry());
            if (drawn == null) continue;
            if (modelRow.entry() instanceof InfoModel.ApertureIndex || current == null) {
                current = new ArrayList<>();
                groups.add(current);
            }
            current.add(drawn);
        }
        return groups;
    }

    private void renderApertureColumns(GuiGraphics graphics, LocalPlayer player, List<List<ScreenRow>> groups,
                                       int mouseX, int mouseY, int accent) {
        int colW = (valueRight() - contentLeft() - COL_GAP) / 2;
        int divider = contentLeft() + colW + COL_GAP / 2;
        graphics.fill(divider, contentTop(), divider + 1, contentBottom(), DIVIDER);
        hoverClick = null;
        for (int c = 0; c < groups.size(); c++) {
            int x0 = contentLeft() + c * (colW + COL_GAP);
            int x1 = x0 + colW;
            int y = contentTop();
            for (ScreenRow row : groups.get(c)) {
                drawRow(graphics, row, x0, x1, y, mouseX, mouseY, accent);
                y += LINE_H;
            }
        }
        for (ApertureButtons.ColumnButton b : columnButtons(player, groups)) {
            ApertureButtons.draw(graphics, font, player, b, mouseX, mouseY, accent);
        }
    }
    //endregion

    //region per-aperture buttons -- where each stack sits; what a button is and does is ApertureButtons
    private List<ApertureButtons.ColumnButton> bottomButtons(LocalPlayer player) {
        List<ApertureButtons.ColumnButton> buttons = new ArrayList<>();
        if (!ApertureService.hasAperture(player)) return buttons;
        List<ApertureButtons.StackButton> stack = ApertureButtons.stack(player, ApertureData.PRIMARY);
        int base = contentBottom() - stack.size() * (BTN_H + BTN_GAP) + BTN_GAP;
        for (ApertureButtons.StackButton b : stack) {
            buttons.add(new ApertureButtons.ColumnButton(b.aperture(), b.kind(), b.key(), contentLeft(),
                    valueRight(), base + b.top()));
        }
        return buttons;
    }

    private List<ApertureButtons.ColumnButton> columnButtons(LocalPlayer player, List<List<ScreenRow>> groups) {
        List<ApertureButtons.ColumnButton> buttons = new ArrayList<>();
        if (!ApertureService.hasAperture(player)) return buttons;
        int colW = (valueRight() - contentLeft() - COL_GAP) / 2;
        for (int c = 0; c < groups.size(); c++) {
            int x0 = contentLeft() + c * (colW + COL_GAP);
            int base = contentTop() + (groups.get(c).size() + 1) * LINE_H;
            for (ApertureButtons.StackButton b : ApertureButtons.stack(player, c)) {
                buttons.add(new ApertureButtons.ColumnButton(b.aperture(), b.kind(), b.key(), x0, x0 + colW,
                        base + b.top()));
            }
        }
        return buttons;
    }

    private void renderBottomButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || activeTab != TAB_APERTURE || twoApertures(player)
                || !ApertureService.hasAperture(player)) return;
        for (ApertureButtons.ColumnButton b : bottomButtons(player)) {
            ApertureButtons.draw(graphics, font, player, b, mouseX, mouseY, ACCENT[activeTab]);
        }
    }

    private boolean clickApertureButtons(double mouseX, double mouseY) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || activeTab != TAB_APERTURE || !ApertureService.hasAperture(player)) return false;
        List<ApertureButtons.ColumnButton> buttons = twoApertures(player)
                ? columnButtons(player, apertureGroups(player)) : bottomButtons(player);
        for (ApertureButtons.ColumnButton b : buttons) {
            if (!ApertureButtons.inBox(mouseX, mouseY, b.x0(), b.x1(), b.top())) continue;
            if (ApertureButtons.click(player, b)) onClose();
            return true;
        }
        return false;
    }
    //endregion

    //region scrolling
    private int contentTop() { return topPos + CONTENT_TOP; }

    private int contentBottom() { return topPos + panelH - PAD; }

    private int contentLeft() { return edgeLeft() + inset(); }

    private int valueRight() { return edgeRight() - inset(); }

    private int inset() { return (edgeRight() - edgeLeft()) / CONTENT_INSET_DIVISOR; }

    private int edgeLeft() { return leftPos + PAD; }

    private int edgeRight() { return tabLeft() - PAD; }

    private int bottomButtonCount() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || activeTab != TAB_APERTURE || twoApertures(player)
                || !ApertureService.hasAperture(player)) return 0;
        return ApertureButtons.stack(player, ApertureData.PRIMARY).size();
    }

    private int rowsBottom() { return contentBottom() - bottomButtonCount() * (BTN_H + BTN_GAP); }

    private int visibleRows() { return Math.max(0, (rowsBottom() - contentTop()) / LINE_H); }

    private void renderScrollBar(GuiGraphics graphics, int total, int visible, int accent) {
        int x0 = tabLeft() - SCROLL_GAP;
        int top = contentTop();
        int track = visible * LINE_H;

        graphics.fill(x0, top, x0 + SCROLL_W, top + track, DIVIDER);
        int thumb = Math.max(LINE_H, track * visible / total);
        int offset = (track - thumb) * scrollRow / Math.max(1, total - visible);
        graphics.fill(x0, top + offset, x0 + SCROLL_W, top + offset + thumb, accent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        if (picker.isOpen()) return true;
        if (dy == 0.0) return super.mouseScrolled(mouseX, mouseY, dx, dy);

        scrollRow = Math.max(0, scrollRow - (int) Math.signum(dy));
        return true;
    }
    //endregion

    private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int i = 0; i < TAB_KEYS.length; i++) {
            boolean active = i == activeTab;
            boolean live = tabLive(i);
            int x0 = tabLeft();
            int y0 = tabTop(i);
            boolean hover = live && !active && inTab(mouseX, mouseY, i);

            graphics.fill(x0, y0, x0 + TAB_W, y0 + TAB_H, active ? ACCENT[i] : TAB_IDLE);
            if (hover) graphics.fill(x0, y0, x0 + TAB_W, y0 + TAB_H, ROW_HOVER);
            if (active) graphics.fill(x0 - 2, y0, x0, y0 + TAB_H, ACCENT[i]);

            Component label = Component.translatable(TAB_KEYS[i]);
            int color = active ? 0xFF101010 : live ? TAB_TEXT_IDLE : TAB_TEXT_DEAD;
            graphics.drawString(font, label, x0 + (TAB_W - font.width(label)) / 2,
                    y0 + (TAB_H - font.lineHeight) / 2 + 1, color, false);
        }
    }

    private boolean tabLive(int tab) {
        if (tab != TAB_REFINEMENT) return true;

        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && ApertureService.isAwakened(player);
    }

    private int tabLeft() { return leftPos + panelW - TAB_W - PAD; }

    private int tabTop(int i) { return topPos + CONTENT_TOP + i * (TAB_H + TAB_GAP); }

    private boolean inTab(double mouseX, double mouseY, int i) {
        return mouseX >= tabLeft() && mouseX < tabLeft() + TAB_W && mouseY >= tabTop(i) && mouseY < tabTop(i) + TAB_H;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (picker.isOpen()) {
            if (button == 0) picker.click(mouseX, mouseY, leftPos, topPos, panelW, panelH);
            return true;
        }
        if (button == 0) {
            if (clickApertureButtons(mouseX, mouseY)) return true;
            if (hoverClick != null) {
                picker.open(hoverClick.aperture());
                return true;
            }
            for (int i = 0; i < TAB_KEYS.length; i++) {
                if (!inTab(mouseX, mouseY, i) || !tabLive(i)) continue;
                if (i == TAB_REFINEMENT) {
                    PacketDistributor.sendToServer(OpenRefinementPayload.INSTANCE);
                } else {
                    activeTab = i;
                    scrollRow = 0;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (picker.isOpen() && (keyCode == InputConstants.KEY_ESCAPE
                || ModKeyMappings.OPEN_INFO.matches(keyCode, scanCode))) {
            picker.close();
            return true;
        }
        if (ModKeyMappings.OPEN_INFO.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private List<ScreenRow> rows(LocalPlayer player) {
        List<InfoModel.Row> model = switch (activeTab) {
            case TAB_BODY -> InfoModel.body(player);
            case TAB_SOUL -> InfoModel.soul(player);
            case TAB_PATH -> InfoModel.pathAchieve(player);
            case TAB_MIND -> InfoModel.mind(player);
            default -> InfoModel.aperture(player);
        };

        List<ScreenRow> rows = new ArrayList<>(model.size());
        for (int i = 0; i < model.size(); i++) {
            InfoModel.Row row = model.get(i);
            if (activeTab == TAB_PATH && row.entry() instanceof InfoModel.PathRow first) {
                ScreenRow left = InfoRowRenderer.draw(row.indent(), first);
                if (left == null) continue;
                ScreenRow right = i + 1 < model.size()
                        && model.get(i + 1).entry() instanceof InfoModel.PathRow second
                        ? InfoRowRenderer.draw(model.get(i + 1).indent(), second) : null;
                if (right != null) {
                    rows.add(new ScreenRow(row.indent(), left.label(), right.label()));
                    i++;
                } else {
                    rows.add(left);
                }
                continue;
            }
            ScreenRow drawn = InfoRowRenderer.draw(row.indent(), row.entry());
            if (drawn != null) rows.add(drawn);
        }
        return rows;
    }
}
