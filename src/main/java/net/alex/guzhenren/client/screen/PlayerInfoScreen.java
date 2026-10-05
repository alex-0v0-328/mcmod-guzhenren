package net.alex.guzhenren.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.client.ModKeyMappings;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.display.InfoModel;
import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.network.payload.ImpactApertureWallPayload;
import net.alex.guzhenren.network.payload.NourishAperturePayload;
import net.alex.guzhenren.network.payload.OpenApertureStoragePayload;
import net.alex.guzhenren.network.payload.OpenRefinementPayload;
import net.alex.guzhenren.network.payload.SetSecondaryPathPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
 * info}, so the two surfaces cannot diverge.
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
    private static final int HEADER_H = 22;
    private static final int CONTENT_TOP = HEADER_H + 8;
    private static final int LINE_H = 12;
    private static final int CONTENT_INSET_DIVISOR = 12;
    private static final int TAB_W = 76;
    private static final int TAB_H = 20;
    private static final int TAB_GAP = 4;
    private static final int DIVIDER = 0x33FFFFFF;
    private static final int ROW_HOVER = 0x14FFFFFF;
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
    private static final int BTN_H = 20;
    private static final int BTN_GAP = 4;
    private static final int BTN_PROGRESS = 0x804FC3F7;
    private static final String KEY_NOURISH = "guzhenren.screen.nourish";
    private static final String KEY_NOURISH_STOP = "guzhenren.screen.nourish_stop";
    private static final String KEY_NOURISH_SECOND = "guzhenren.screen.nourish_second";
    private static final String KEY_IMPACT = "guzhenren.screen.impact";
    private static final String KEY_STORAGE = "guzhenren.screen.button.storage";
    private static final int TAB_APERTURE = 0;
    private static final int TAB_BODY = 1;
    private static final int TAB_SOUL = 2;
    private static final int TAB_PATH = 3;
    private static final int TAB_MIND = 4;
    private static final int TAB_REFINEMENT = 5;
    private static final int PICK_COLS = 4;
    private static final int PICK_CELL_W = 84;
    private static final int PICK_CELL_H = 14;
    private static final int PICK_PAD = 8;
    private static final int SCROLL_W = 2;
    private static final int SCROLL_GAP = 5;
    private static final int COL_GAP = 16;
    private static final int BTN_NOURISH = 0;
    private static final int BTN_IMPACT = 1;
    private static final int BTN_STORAGE = 2;
    private int leftPos;
    private int topPos;
    private int panelW;
    private int panelH;
    private int activeTab;
    private @Nullable Click hoverClick;
    private boolean picking;
    private int pickerAperture = ApertureData.PRIMARY;
    private int scrollRow;

    public PlayerInfoScreen() { super(Component.translatable("guzhenren.screen.info.title")); }

    @Override
    protected void init() {
        panelW = Math.round(width * SCREEN_FRACTION);
        panelH = Math.round(height * SCREEN_FRACTION);
        leftPos = (width - panelW) / 2;
        topPos = (height - panelH) / 2;
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        int right = leftPos + panelW;
        int accent = ACCENT[activeTab];

        g.fill(leftPos, topPos, right, topPos + panelH, ModPalette.PANEL_FILL);
        g.renderOutline(leftPos, topPos, panelW, panelH, ModPalette.BORDER);

        g.drawString(font, Component.translatable(TAB_KEYS[activeTab]),
                leftPos + PAD, topPos + (HEADER_H - font.lineHeight) / 2, accent, false);
        g.fill(leftPos + PAD, topPos + HEADER_H, right - PAD, topPos + HEADER_H + 1, DIVIDER);

        renderTabs(g, mouseX, mouseY);

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if (activeTab == TAB_APERTURE && twoApertures(player)) {
            renderApertureColumns(g, player, apertureGroups(player), mouseX, mouseY, accent);
        } else {
            renderRows(g, rows(player), mouseX, mouseY, accent);
            renderBottomButtons(g, mouseX, mouseY);
        }
        if (picking) renderPicker(g, mouseX, mouseY, accent);
    }

    private void renderRows(GuiGraphics g, List<Row> rows, int mouseX, int mouseY, int accent) {
        int visible = visibleRows();
        int hidden = Math.max(0, rows.size() - visible);
        scrollRow = Mth.clamp(scrollRow, 0, hidden);

        int valueRight = valueRight();
        int rowLeft = contentLeft();
        int y = contentTop();
        hoverClick = null;
        for (int i = scrollRow; i < Math.min(rows.size(), scrollRow + visible); i++) {
            Row row = rows.get(i);
            if (mouseY >= y - 1 && mouseY < y + LINE_H - 1 && mouseX >= rowLeft - 2 && mouseX < valueRight + 2) {
                g.fill(rowLeft - 2, y - 1, valueRight + 2, y + LINE_H - 1, ROW_HOVER);
                if (row.click() != null) hoverClick = row.click();
            }
            int labelColor = row.value() == null ? accent : ModPalette.TEXT;
            g.drawString(font, row.label(), rowLeft + row.indent(), y, labelColor, false);
            if (row.value() != null) {
                g.drawString(font, row.value(), valueRight - font.width(row.value()), y, ModPalette.TEXT, false);
            }
            y += LINE_H;
        }
        if (hidden > 0) renderScrollBar(g, rows.size(), visible, accent);
    }

    //region aperture columns -- two apertures render side by side, left first right second
    private static boolean twoApertures(LocalPlayer player) {
        return ApertureService.get(player).count() == 2;
    }

    private List<List<Row>> apertureGroups(LocalPlayer player) {
        List<List<Row>> groups = new ArrayList<>();
        List<Row> current = null;
        for (InfoModel.Row modelRow : InfoModel.aperture(player)) {
            if (modelRow.entry() instanceof InfoModel.Blank) continue;
            Row drawn = draw(modelRow.indent(), modelRow.entry());
            if (drawn == null) continue;
            if (modelRow.entry() instanceof InfoModel.ApertureIndex || current == null) {
                current = new ArrayList<>();
                groups.add(current);
            }
            current.add(drawn);
        }
        return groups;
    }

    private void renderApertureColumns(GuiGraphics g, LocalPlayer player, List<List<Row>> groups,
                                       int mouseX, int mouseY, int accent) {
        int colW = (valueRight() - contentLeft() - COL_GAP) / 2;
        int divider = contentLeft() + colW + COL_GAP / 2;
        g.fill(divider, contentTop(), divider + 1, contentBottom(), DIVIDER);
        hoverClick = null;
        for (int c = 0; c < groups.size(); c++) {
            int x0 = contentLeft() + c * (colW + COL_GAP);
            int x1 = x0 + colW;
            int y = contentTop();
            for (Row row : groups.get(c)) {
                if (mouseY >= y - 1 && mouseY < y + LINE_H - 1 && mouseX >= x0 - 2 && mouseX < x1 + 2) {
                    g.fill(x0 - 2, y - 1, x1 + 2, y + LINE_H - 1, ROW_HOVER);
                    if (row.click() != null) hoverClick = row.click();
                }
                int labelColor = row.value() == null ? accent : ModPalette.TEXT;
                g.drawString(font, row.label(), x0 + row.indent(), y, labelColor, false);
                if (row.value() != null) {
                    g.drawString(font, row.value(), x1 - font.width(row.value()), y, ModPalette.TEXT, false);
                }
                y += LINE_H;
            }
        }
        for (ColumnButton b : columnButtons(player, groups)) {
            drawApButton(g, player, b, mouseX, mouseY);
        }
    }
    //endregion

    //region per-aperture buttons -- nourish [温养空窍] / flush [冲刷窍壁] / storage [空窍存储], one stack per aperture
    private record ApButton(int aperture, int kind, String key, int top) {}

    private record ColumnButton(int aperture, int kind, String key, int x0, int x1, int top) {}

    private List<ApButton> buttonStack(LocalPlayer player, int aperture) {
        List<ApButton> buttons = new ArrayList<>();
        if (aperture == ApertureData.PRIMARY && ApertureNourishService.canImpact(player)) {
            buttons.add(new ApButton(aperture, BTN_IMPACT, KEY_IMPACT, 0));
            return withStorage(aperture, buttons, BTN_H + BTN_GAP);
        }
        if (!ApertureNourishService.atCeiling(player, aperture)) {
            boolean running = ApertureNourishService.isCultivating(player)
                    && ApertureNourishService.targetIndex(player) == aperture;
            String key = running ? KEY_NOURISH_STOP
                    : ApertureService.aperture(player, aperture).second() ? KEY_NOURISH_SECOND : KEY_NOURISH;
            buttons.add(new ApButton(aperture, BTN_NOURISH, key, 0));
            return withStorage(aperture, buttons, BTN_H + BTN_GAP);
        }
        return withStorage(aperture, buttons, 0);
    }

    private static List<ApButton> withStorage(int aperture, List<ApButton> buttons, int top) {
        buttons.add(new ApButton(aperture, BTN_STORAGE, KEY_STORAGE, top));
        return buttons;
    }

    private List<ColumnButton> bottomButtons(LocalPlayer player) {
        List<ColumnButton> buttons = new ArrayList<>();
        if (!ApertureService.hasAperture(player)) return buttons;
        List<ApButton> stack = buttonStack(player, ApertureData.PRIMARY);
        int base = contentBottom() - stack.size() * (BTN_H + BTN_GAP) + BTN_GAP;
        for (ApButton b : stack) {
            buttons.add(new ColumnButton(b.aperture(), b.kind(), b.key(), contentLeft(), valueRight(),
                    base + b.top()));
        }
        return buttons;
    }

    private List<ColumnButton> columnButtons(LocalPlayer player, List<List<Row>> groups) {
        List<ColumnButton> buttons = new ArrayList<>();
        if (!ApertureService.hasAperture(player)) return buttons;
        int colW = (valueRight() - contentLeft() - COL_GAP) / 2;
        for (int c = 0; c < groups.size(); c++) {
            int x0 = contentLeft() + c * (colW + COL_GAP);
            int base = contentTop() + (groups.get(c).size() + 1) * LINE_H;
            for (ApButton b : buttonStack(player, c)) {
                buttons.add(new ColumnButton(b.aperture(), b.kind(), b.key(), x0, x0 + colW, base + b.top()));
            }
        }
        return buttons;
    }

    private void renderBottomButtons(GuiGraphics g, int mouseX, int mouseY) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || activeTab != TAB_APERTURE || twoApertures(player)
                || !ApertureService.hasAperture(player)) return;
        for (ColumnButton b : bottomButtons(player)) {
            drawApButton(g, player, b, mouseX, mouseY);
        }
    }

    private void drawApButton(GuiGraphics g, LocalPlayer player, ColumnButton b, int mouseX, int mouseY) {
        boolean hover = inBox(mouseX, mouseY, b.x0(), b.x1(), b.top());
        switch (b.kind()) {
            case BTN_IMPACT -> {
                int strike = !ApertureNourishService.canAffordImpact(player) ? ModPalette.BUTTON_DEAD
                        : hover ? ModPalette.BUTTON_HOVER : ModPalette.BUTTON_IDLE;
                g.fill(b.x0(), b.top(), b.x1(), b.top() + BTN_H, strike);
                g.renderOutline(b.x0(), b.top(), b.x1() - b.x0(), BTN_H, ACCENT[activeTab]);
                label(g, Component.translatable(b.key()), b.x0(), b.x1(), b.top());
            }
            case BTN_NOURISH -> {
                boolean running = ApertureNourishService.isCultivating(player)
                        && ApertureNourishService.targetIndex(player) == b.aperture();
                int fill = running ? ModPalette.BUTTON_HOVER
                        : ApertureNourishService.canNourish(player, b.aperture()) ? ModPalette.BUTTON_IDLE
                        : ModPalette.BUTTON_DEAD;
                g.fill(b.x0(), b.top(), b.x1(), b.top() + BTN_H, fill);
                if (running) {
                    int done = b.x0() + Math.round((b.x1() - b.x0())
                            * ApertureNourishService.fraction(player, b.aperture()));
                    g.fill(b.x0(), b.top(), done, b.top() + BTN_H, BTN_PROGRESS);
                }
                g.renderOutline(b.x0(), b.top(), b.x1() - b.x0(), BTN_H,
                        running ? ACCENT[activeTab] : ModPalette.BORDER);
                label(g, Component.translatable(b.key()), b.x0(), b.x1(), b.top());
            }
            default -> {
                g.fill(b.x0(), b.top(), b.x1(), b.top() + BTN_H,
                        hover ? ModPalette.BUTTON_HOVER : ModPalette.BUTTON_IDLE);
                g.renderOutline(b.x0(), b.top(), b.x1() - b.x0(), BTN_H, ModPalette.BORDER);
                label(g, Component.translatable(b.key()), b.x0(), b.x1(), b.top());
            }
        }
    }

    private boolean clickApertureButtons(double mx, double my) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || activeTab != TAB_APERTURE || !ApertureService.hasAperture(player)) return false;
        List<ColumnButton> buttons = twoApertures(player)
                ? columnButtons(player, apertureGroups(player)) : bottomButtons(player);
        for (ColumnButton b : buttons) {
            if (!inBox(mx, my, b.x0(), b.x1(), b.top())) continue;
            switch (b.kind()) {
                case BTN_IMPACT -> PacketDistributor.sendToServer(ImpactApertureWallPayload.INSTANCE);
                case BTN_NOURISH -> {
                    if (ApertureNourishService.isCultivating(player)
                            && ApertureNourishService.targetIndex(player) == b.aperture()) {
                        PacketDistributor.sendToServer(new NourishAperturePayload(
                                NourishAperturePayload.Action.CANCEL, ApertureNourishService.targetIndex(player)));
                    } else if (ApertureNourishService.canNourish(player, b.aperture())) {
                        PacketDistributor.sendToServer(new NourishAperturePayload(
                                NourishAperturePayload.Action.START, b.aperture()));
                        onClose();
                    }
                }
                default -> PacketDistributor.sendToServer(new OpenApertureStoragePayload(b.aperture()));
            }
            return true;
        }
        return false;
    }

    private void label(GuiGraphics g, Component text, int x0, int x1, int top) {
        g.drawString(font, text, x0 + (x1 - x0 - font.width(text)) / 2,
                top + (BTN_H - font.lineHeight) / 2, ModPalette.TEXT, false);
    }

    private boolean inBox(double mx, double my, int x0, int x1, int top) {
        return mx >= x0 && mx < x1 && my >= top && my < top + BTN_H;
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
        return buttonStack(player, ApertureData.PRIMARY).size();
    }

    private int rowsBottom() { return contentBottom() - bottomButtonCount() * (BTN_H + BTN_GAP); }

    private int visibleRows() { return Math.max(0, (rowsBottom() - contentTop()) / LINE_H); }

    private void renderScrollBar(GuiGraphics g, int total, int visible, int accent) {
        int x0 = tabLeft() - SCROLL_GAP;
        int top = contentTop();
        int track = visible * LINE_H;

        g.fill(x0, top, x0 + SCROLL_W, top + track, DIVIDER);
        int thumb = Math.max(LINE_H, track * visible / total);
        int offset = (track - thumb) * scrollRow / Math.max(1, total - visible);
        g.fill(x0, top + offset, x0 + SCROLL_W, top + offset + thumb, accent);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (picking) return true;
        if (dy == 0.0) return super.mouseScrolled(mx, my, dx, dy);

        scrollRow = Math.max(0, scrollRow - (int) Math.signum(dy));
        return true;
    }
    //endregion

    //region secondary-path picker
    private void renderPicker(GuiGraphics g, int mouseX, int mouseY, int accent) {
        int x0 = pickLeft();
        int y0 = pickTop();
        int w = PICK_COLS * PICK_CELL_W + PICK_PAD * 2;
        int h = pickRows() * PICK_CELL_H + PICK_PAD * 2 + HEADER_H;

        g.fill(x0, y0, x0 + w, y0 + h, 0xF0000000);
        g.renderOutline(x0, y0, w, h, ModPalette.BORDER);
        g.drawString(font, Component.translatable("guzhenren.screen.pick.title"),
                x0 + PICK_PAD, y0 + (HEADER_H - font.lineHeight) / 2, accent, false);
        g.fill(x0 + PICK_PAD, y0 + HEADER_H, x0 + w - PICK_PAD, y0 + HEADER_H + 1, DIVIDER);

        for (int i = 0; i < pickCount(); i++) {
            int cx = x0 + PICK_PAD + (i % PICK_COLS) * PICK_CELL_W;
            int cy = y0 + PICK_PAD + HEADER_H + (i / PICK_COLS) * PICK_CELL_H;
            boolean hover = mouseX >= cx && mouseX < cx + PICK_CELL_W
                    && mouseY >= cy && mouseY < cy + PICK_CELL_H;
            if (hover) g.fill(cx, cy, cx + PICK_CELL_W, cy + PICK_CELL_H, ROW_HOVER);
            g.drawString(font, ModDisplayText.path(pickPath(i)), cx + 3,
                    cy + (PICK_CELL_H - font.lineHeight) / 2, ModPalette.TEXT, false);
        }
    }

    private static int pickCount() { return GuPath.values().length + 1; }

    private static @Nullable GuPath pickPath(int i) { return i == 0 ? null : GuPath.values()[i - 1]; }

    private static int pickRows() { return (pickCount() + PICK_COLS - 1) / PICK_COLS; }

    private int pickWidth() { return PICK_COLS * PICK_CELL_W + PICK_PAD * 2; }

    private int pickHeight() { return pickRows() * PICK_CELL_H + PICK_PAD * 2 + HEADER_H; }

    private int pickLeft() { return leftPos + (panelW - pickWidth()) / 2; }

    private int pickTop() { return topPos + (panelH - pickHeight()) / 2; }

    private static MutableComponent pickHint() { return Component.translatable("guzhenren.screen.pick.hint"); }

    private void clickPicker(double mx, double my) {
        int x0 = pickLeft() + PICK_PAD;
        int y0 = pickTop() + PICK_PAD + HEADER_H;
        for (int i = 0; i < pickCount(); i++) {
            int cx = x0 + (i % PICK_COLS) * PICK_CELL_W;
            int cy = y0 + (i / PICK_COLS) * PICK_CELL_H;
            if (mx < cx || mx >= cx + PICK_CELL_W || my < cy || my >= cy + PICK_CELL_H) continue;

            PacketDistributor.sendToServer(new SetSecondaryPathPayload(pickerAperture, pickPath(i)));
            picking = false;
            return;
        }
        picking = false;
    }
    //endregion

    private void renderTabs(GuiGraphics g, int mouseX, int mouseY) {
        for (int i = 0; i < TAB_KEYS.length; i++) {
            boolean active = i == activeTab;
            boolean live = tabLive(i);
            int x0 = tabLeft();
            int y0 = tabTop(i);
            boolean hover = live && !active && inTab(mouseX, mouseY, i);

            g.fill(x0, y0, x0 + TAB_W, y0 + TAB_H, active ? ACCENT[i] : TAB_IDLE);
            if (hover) g.fill(x0, y0, x0 + TAB_W, y0 + TAB_H, ROW_HOVER);
            if (active) g.fill(x0 - 2, y0, x0, y0 + TAB_H, ACCENT[i]);

            Component label = Component.translatable(TAB_KEYS[i]);
            int color = active ? 0xFF101010 : live ? TAB_TEXT_IDLE : TAB_TEXT_DEAD;
            g.drawString(font, label, x0 + (TAB_W - font.width(label)) / 2,
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

    private boolean inTab(double mx, double my, int i) {
        return mx >= tabLeft() && mx < tabLeft() + TAB_W && my >= tabTop(i) && my < tabTop(i) + TAB_H;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (picking) {
            if (button == 0) clickPicker(mx, my);
            return true;
        }
        if (button == 0) {
            if (clickApertureButtons(mx, my)) return true;
            if (hoverClick != null) {
                pickerAperture = hoverClick.aperture();
                picking = true;
                return true;
            }
            for (int i = 0; i < TAB_KEYS.length; i++) {
                if (!inTab(mx, my, i) || !tabLive(i)) continue;
                if (i == TAB_REFINEMENT) {
                    PacketDistributor.sendToServer(OpenRefinementPayload.INSTANCE);
                } else {
                    activeTab = i;
                    scrollRow = 0;
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (picking && (keyCode == InputConstants.KEY_ESCAPE
                || ModKeyMappings.OPEN_INFO.matches(keyCode, scanCode))) {
            picking = false;
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

    private List<Row> rows(LocalPlayer player) {
        List<InfoModel.Row> model = switch (activeTab) {
            case TAB_BODY -> InfoModel.body(player);
            case TAB_SOUL -> InfoModel.soul(player);
            case TAB_PATH -> InfoModel.pathAchieve(player);
            case TAB_MIND -> InfoModel.mind(player);
            default -> InfoModel.aperture(player);
        };

        List<Row> rows = new ArrayList<>(model.size());
        for (int i = 0; i < model.size(); i++) {
            InfoModel.Row row = model.get(i);
            if (activeTab == TAB_PATH && row.entry() instanceof InfoModel.PathRow first) {
                Row left = draw(row.indent(), first);
                if (left == null) continue;
                Row right = i + 1 < model.size()
                        && model.get(i + 1).entry() instanceof InfoModel.PathRow second
                        ? draw(model.get(i + 1).indent(), second) : null;
                if (right != null) {
                    rows.add(new Row(row.indent(), left.label(), right.label()));
                    i++;
                } else {
                    rows.add(left);
                }
                continue;
            }
            Row drawn = draw(row.indent(), row.entry());
            if (drawn != null) rows.add(drawn);
        }
        return rows;
    }

    private static @Nullable Row draw(int indent, InfoModel.Entry entry) {
        return switch (entry) {
            case InfoModel.ApertureIndex e -> new Row(indent, ModDisplayText.apertureName(e.number()), null);
            case InfoModel.Blank ignored -> new Row(indent, Component.empty(), null);
            case InfoModel.Realm e -> new Row(indent, label("realm"), ModDisplayText.realmTitle(e.aperture()));
            case InfoModel.Status e -> new Row(indent, label("aperture_status"),
                    name(e.status().getTranslationKey()));
            case InfoModel.Talent e -> new Row(indent, label("talent"), talent(e));
            case InfoModel.Essence e -> new Row(indent, label("essence"), Component.literal(
                    ModDisplayText.pool(e.aperture().currentEssence(), e.aperture().maxEssence())));
            case InfoModel.Distilled e -> new Row(indent, label("distilled"), Component.literal(
                    ModDisplayText.pool(e.aperture().distilledEssence(), e.aperture().maxEssence())));
            case InfoModel.Pressure e -> new Row(indent,
                    Component.translatable("guzhenren.screen.label.aperture_pressure"), Component.literal(
                    e.aperture().pressure() + "%"));

            case InfoModel.PathChoice e -> e.primary()
                    ? new Row(indent, label("primary_path"), ModDisplayText.path(e.path()))
                    : new Row(indent, label("secondary_path"),
                    detail(pickHint()).copy().append(e.path() == null
                            ? none().withStyle(ChatFormatting.DARK_GRAY)
                            : detail(ModDisplayText.path(e.path()))),
                    new Click(e.aperture()));

            case InfoModel.PhysiqueRow e -> new Row(indent, label("physique"),
                    ModDisplayText.physiqueValue(e.physique(), e.extremePhysique()));
            case InfoModel.RaceRow e -> new Row(indent, label("race"), name(e.race().getTranslationKey()));
            case InfoModel.Soul e -> new Row(indent, label("soul"),
                    Component.literal(ModDisplayText.pool(e.soul().currentSoul(), e.soul().maxSoul()))
                            .append(detail(name(e.soul().tier().getTranslationKey()))));
            case InfoModel.Lifespan e -> new Row(indent, label("lifespan"),
                    ModDisplayText.lifespan(e.lifespan(), e.age()));
            case InfoModel.PathsHeader e -> new Row(indent, label("paths"), e.empty() ? none() : null);
            case InfoModel.PathRow e ->
                    new Row(indent, ModDisplayText.pathLine(e.path(), e.entry()), Component.empty());
            case InfoModel.QiPathAchieveHeader ignored -> new Row(indent, label("qi_path_achieve"), null);
            case InfoModel.QiKindRow e -> new Row(indent, name(e.kind().getTranslationKey()),
                    Component.literal(String.valueOf(e.amount())));
            case InfoModel.TimePathAchieveHeader ignored -> new Row(indent, label("time_path_achieve"), null);
            case InfoModel.TimeRateUpRow e -> new Row(indent, label("time_rate_up"),
                    ModDisplayText.timeRateUp(e.rate()));
            case InfoModel.StrengthPathAchieveHeader ignored -> new Row(indent, label("strength_path_achieve"), null);
            case InfoModel.StrengthPathBranchRow e -> new Row(indent,
                    ModDisplayText.strengthLabel(name(e.branch().getTranslationKey()), e.totalJin()), e.reading());
            case InfoModel.CapacityRow e -> new Row(indent,
                    Component.translatable("guzhenren.screen.label.body_capacity"),
                    Component.translatable("guzhenren.screen.capacity", e.usable(), e.total()));
            case InfoModel.AttackRow e -> new Row(indent, label("attack"),
                    Component.literal(ModDisplayText.attackBonus(e.bonus())));
            case InfoModel.WisdomPathAchieveHeader ignored -> new Row(indent, label("wisdom_path_achieve"), null);
            case InfoModel.ThoughtTagRow e -> new Row(indent, name(e.tag().getTranslationKey()),
                    Component.literal(String.valueOf(e.amount())));

            case InfoModel.BrillianceRow e -> new Row(indent, label("brilliance"),
                    name(e.brilliance().getTranslationKey()).append(detail(Component.translatable(
                            "guzhenren.command.info.brilliance_rate", e.brilliance().getThoughtsPerSecond()))));
            case InfoModel.MindHeader ignored -> null;
            case InfoModel.MindRow e -> new Row(indent, name(e.type().getTranslationKey()),
                    Component.literal(ModDisplayText.pool(e.pool().current(), e.pool().max())));
        };
    }

    private static MutableComponent talent(InfoModel.Talent e) {
        MutableComponent talent = ModDisplayText.talent(e.aperture());
        if (e.awakened()) talent.append(detail(ModDisplayText.baseFraction(e.aperture().baseEssence())));
        return talent;
    }

    private static MutableComponent name(String key) { return Component.translatable(key); }

    private static Component label(String name) { return Component.translatable("guzhenren.screen.label." + name); }

    private static MutableComponent none() { return Component.translatable("guzhenren.display.none"); }

    private static Component detail(Component v) {
        return Component.translatable("guzhenren.command.info.detail", v).withStyle(ChatFormatting.DARK_GRAY);
    }

    private record Row(int indent, Component label, @Nullable Component value, @Nullable Click click) {

        Row(int indent, Component label, @Nullable Component value) { this(indent, label, value, null); }
    }

    private record Click(int aperture) {}
}
