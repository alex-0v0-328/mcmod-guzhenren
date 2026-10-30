package net.alex.guzhenren.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.ModKeyMappings;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The B panel: every tab of what a player is, read straight off the synced attachments.
 *
 * <p>Extends {@link net.minecraft.client.gui.screens.Screen} (no menu behind it). Seven tabs: 空窍,
 * 肉身, 魂魄, 流派, 流派造诣, 脑海, 炼蛊. The aperture tab draws one column per aperture -- a lone aperture
 * keeps the single-column layout -- and every column carries its own nourish [温养空窍] /
 * flush [冲刷窍壁] / storage [空窍存储] buttons; the storage button opens that aperture's container.
 * The refinement tab opens its container via a client-intent payload instead of drawing rows. Row
 * content comes from {@link InfoModel}; {@link InfoRowRenderer} turns each entry into the panel's
 * label and value, {@link ApertureButtons} are the buttons under each aperture, and {@link PathPicker} is
 * the secondary-path grid that opens over the panel. A frame is drawn in one {@link GuiBatch}: every sprite in one
 * {@link SpriteBatch}, then the fills over them (rule, hovers, nourish progress), then every text; {@link InfoRowCache}
 * keeps the rows of the last frame while the model is unchanged. The selected tab is the idle tab sprite with the
 * greyscale selection border over it, tinted the tab's accent; its title takes the same accent.
 *
 * <p>⚠ A plain screen with no menu behind it: no container channel to send an intent over.
 *
 * <p>Past 300 lines on purpose: the tabs, the rows, the scroll bar and the aperture columns are one layout that
 * reads the same edges ({@code contentLeft}, {@code valueRight}, {@code tabLeft}) as the panel resizes.
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
    private static final ResourceLocation TAB = Guzhenren.id("tab");
    private static final ResourceLocation TAB_HIGHLIGHTED = Guzhenren.id("tab_highlighted");
    private static final ResourceLocation TAB_SELECTED = Guzhenren.id("tab_selected");
    private static final ResourceLocation TAB_DISABLED = Guzhenren.id("tab_disabled");
    private static final ResourceLocation SCROLLER = Guzhenren.id("scroller");
    private static final ResourceLocation SCROLLER_BACKGROUND = Guzhenren.id("scroller_background");
    private static final int[] ACCENT = { ModPalette.APERTURE, ModPalette.BODY, ModPalette.SOUL,
            ModPalette.PATH, ModPalette.PATH, ModPalette.MIND, ModPalette.REFINEMENT };
    private static final Component[] TAB_TITLES = {
            Component.translatable("guzhenren.screen.tab.aperture"),
            Component.translatable("guzhenren.screen.tab.body"),
            Component.translatable("guzhenren.screen.tab.soul"),
            Component.translatable("guzhenren.screen.tab.paths"),
            Component.translatable("guzhenren.screen.tab.path"),
            Component.translatable("guzhenren.screen.tab.mind"),
            Component.translatable("guzhenren.screen.tab.refinement"),
    };
    static final int BTN_H = 20;
    static final int BTN_GAP = 4;
    private static final int TAB_APERTURE = 0;
    private static final int TAB_BODY = 1;
    private static final int TAB_SOUL = 2;
    private static final int TAB_PATHS = 3;
    private static final int TAB_PATH = 4;
    private static final int TAB_MIND = 5;
    private static final int TAB_REFINEMENT = 6;
    private static final int SCROLL_W = 6;
    private static final int SCROLL_GAP = 9;
    private static final int COL_GAP = 16;
    private int leftPos;
    private int topPos;
    private int panelW;
    private int panelH;
    private int activeTab;
    private @Nullable InfoRowRenderer.Click hoverClick;
    private int scrollRow;
    private final PathPicker picker = new PathPicker();
    private final InfoRowCache rowCache = new InfoRowCache();

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
        int accent = ACCENT[activeTab];
        LocalPlayer player = Minecraft.getInstance().player;
        Content content = player == null ? Content.EMPTY : getContent(player);
        hoverClick = null;

        GuiBatch.draw(graphics, () -> {
            SpriteBatch.draw(graphics, sprites -> drawSprites(sprites, content, player, mouseX, mouseY));
            Widgets.drawRule(graphics, leftPos + PAD, topPos + HEADER_H, leftPos + panelW - PAD, DIVIDER);
            if (player != null) fillContent(graphics, content, player, mouseX, mouseY);
            drawFrameTexts(graphics, accent);
            if (player != null) drawContentTexts(graphics, content, accent);
        });
        if (player != null && picker.isOpen()) {
            picker.render(graphics, font, leftPos, topPos, panelW, panelH, mouseX, mouseY, accent);
        }
    }

    private Content getContent(LocalPlayer player) {
        if (activeTab == TAB_APERTURE && hasTwoApertures(player)) {
            List<List<ScreenRow>> groups = rowCache.getApertureGroups(InfoModel.buildApertureRows(player));
            return new Content(true, List.of(), groups, buildColumnButtons(player, groups), 0);
        }
        List<ScreenRow> rows = rowCache.getRows(getModel(player));
        List<ApertureButtons.ColumnButton> buttons = buildBottomButtons(player);
        int rowsBottom = getContentBottom() - buttons.size() * (BTN_H + BTN_GAP);
        int visible = Math.max(0, (rowsBottom - getContentTop()) / LINE_H);
        scrollRow = Mth.clamp(scrollRow, 0, Math.max(0, rows.size() - visible));
        return new Content(false, rows, List.of(), buttons, visible);
    }

    private List<InfoModel.Row> getModel(LocalPlayer player) {
        return switch (activeTab) {
            case TAB_BODY -> InfoModel.buildBodyRows(player);
            case TAB_SOUL -> InfoModel.buildSoulRows(player);
            case TAB_PATHS -> InfoModel.buildPathRows(player);
            case TAB_PATH -> InfoModel.buildPathAchieveRows(player);
            case TAB_MIND -> InfoModel.buildMindRows(player);
            default -> InfoModel.buildApertureRows(player);
        };
    }

    //region the three passes -- every sprite, then every fill over them, then every text
    private void drawSprites(SpriteBatch sprites, Content content, @Nullable LocalPlayer player, int mouseX,
                             int mouseY) {
        Widgets.drawPanel(sprites, leftPos, topPos, panelW, panelH);
        for (int i = 0; i < TAB_TITLES.length; i++) drawTab(sprites, i, mouseX, mouseY);
        if (player == null) return;

        if (!content.columns() && content.rows().size() > content.visible()) {
            drawScrollBar(sprites, content.rows().size(), content.visible());
        }
        for (ApertureButtons.ColumnButton b : content.buttons()) {
            ApertureButtons.draw(sprites, player, b, mouseX, mouseY);
        }
    }

    private void drawTab(SpriteBatch sprites, int i, int mouseX, int mouseY) {
        int x0 = getTabLeft();
        int y0 = getTabTop(i);
        boolean active = i == activeTab;
        ResourceLocation tab = !isTabLive(i) ? TAB_DISABLED
                : !active && isInTab(mouseX, mouseY, i) ? TAB_HIGHLIGHTED : TAB;

        sprites.blit(tab, x0, y0, TAB_W, TAB_H);
        if (active) sprites.blit(TAB_SELECTED, x0, y0, TAB_W, TAB_H, ACCENT[i]);
    }

    private void fillContent(GuiGraphics graphics, Content content, LocalPlayer player, int mouseX, int mouseY) {
        if (content.columns()) {
            int divider = getContentLeft() + getColumnWidth() + COL_GAP / 2;
            graphics.fill(divider, getContentTop(), divider + 1, getContentBottom(), DIVIDER);
        }
        forEachRow(content, (row, x0, x1, y) -> fillRowHover(graphics, row, x0, x1, y, mouseX, mouseY));
        for (ApertureButtons.ColumnButton b : content.buttons()) ApertureButtons.fillProgress(graphics, player, b);
    }

    private void drawFrameTexts(GuiGraphics graphics, int accent) {
        graphics.drawString(font, TAB_TITLES[activeTab],
                leftPos + PAD, topPos + (HEADER_H - font.lineHeight) / 2, accent, false);
        for (int i = 0; i < TAB_TITLES.length; i++) {
            int color = i == activeTab ? ACCENT[i] : isTabLive(i) ? ModPalette.TEXT : ModPalette.TEXT_DEAD;
            Widgets.drawCentered(graphics, font, TAB_TITLES[i], getTabLeft(), getTabTop(i), TAB_W, TAB_H, color);
        }
    }

    private void drawContentTexts(GuiGraphics graphics, Content content, int accent) {
        forEachRow(content, (row, x0, x1, y) -> drawRowText(graphics, row, x0, x1, y, accent));
        for (ApertureButtons.ColumnButton b : content.buttons()) ApertureButtons.drawLabel(graphics, font, b);
    }

    private void forEachRow(Content content, RowVisitor visitor) {
        if (!content.columns()) {
            int end = Math.min(content.rows().size(), scrollRow + content.visible());
            int y = getContentTop();
            for (int i = scrollRow; i < end; i++) {
                visitor.visit(content.rows().get(i), getContentLeft(), getValueRight(), y);
                y += LINE_H;
            }
            return;
        }
        int colW = getColumnWidth();
        for (int c = 0; c < content.groups().size(); c++) {
            int x0 = getContentLeft() + c * (colW + COL_GAP);
            int y = getContentTop();
            for (ScreenRow row : content.groups().get(c)) {
                visitor.visit(row, x0, x0 + colW, y);
                y += LINE_H;
            }
        }
    }

    private void fillRowHover(GuiGraphics graphics, ScreenRow row, int x0, int x1, int y, int mouseX, int mouseY) {
        if (!Widgets.isOver(mouseX, mouseY, x0 - 2, y - 1, x1 - x0 + 4, LINE_H)) return;

        graphics.fill(x0 - 2, y - 1, x1 + 2, y + LINE_H - 1, ROW_HOVER);
        if (row.click() != null) hoverClick = row.click();
    }

    private void drawRowText(GuiGraphics graphics, ScreenRow row, int x0, int x1, int y, int accent) {
        int labelColor = row.value() == null ? accent : ModPalette.TEXT;
        graphics.drawString(font, row.label(), x0 + row.indent(), y, labelColor, false);
        if (row.value() != null) {
            graphics.drawString(font, row.value(), x1 - font.width(row.value()), y, ModPalette.TEXT, false);
        }
    }
    //endregion

    //region aperture columns -- two apertures render side by side, left first right second
    private static boolean hasTwoApertures(LocalPlayer player) {
        return ApertureService.get(player).count() == 2;
    }

    private int getColumnWidth() { return (getValueRight() - getContentLeft() - COL_GAP) / 2; }
    //endregion

    //region per-aperture buttons -- where each stack sits; what a button is and does is ApertureButtons
    private boolean hasNoButtons(LocalPlayer player) {
        return activeTab != TAB_APERTURE || !ApertureService.hasAperture(player);
    }

    private List<ApertureButtons.ColumnButton> buildBottomButtons(LocalPlayer player) {
        if (hasTwoApertures(player) || hasNoButtons(player)) return List.of();

        List<ApertureButtons.StackButton> stack = ApertureButtons.buildStackButtons(player, ApertureData.PRIMARY);
        List<ApertureButtons.ColumnButton> buttons = new ArrayList<>(stack.size());
        int base = getContentBottom() - stack.size() * (BTN_H + BTN_GAP) + BTN_GAP;
        place(buttons, stack, getContentLeft(), getValueRight(), base);
        return buttons;
    }

    private List<ApertureButtons.ColumnButton> buildColumnButtons(LocalPlayer player, List<List<ScreenRow>> groups) {
        if (hasNoButtons(player)) return List.of();

        List<ApertureButtons.ColumnButton> buttons = new ArrayList<>();
        int colW = getColumnWidth();
        for (int c = 0; c < groups.size(); c++) {
            int x0 = getContentLeft() + c * (colW + COL_GAP);
            int base = getContentTop() + (groups.get(c).size() + 1) * LINE_H;
            place(buttons, ApertureButtons.buildStackButtons(player, c), x0, x0 + colW, base);
        }
        return buttons;
    }

    private static void place(List<ApertureButtons.ColumnButton> buttons, List<ApertureButtons.StackButton> stack,
                              int x0, int x1, int base) {
        for (ApertureButtons.StackButton b : stack) {
            buttons.add(new ApertureButtons.ColumnButton(b.aperture(), b.kind(), b.label(), x0, x1, base + b.top()));
        }
    }

    private boolean clickApertureButtons(double mouseX, double mouseY) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;

        for (ApertureButtons.ColumnButton b : getContent(player).buttons()) {
            if (!ApertureButtons.isInBox(mouseX, mouseY, b.x0(), b.x1(), b.top())) continue;
            if (ApertureButtons.click(player, b)) onClose();
            return true;
        }
        return false;
    }
    //endregion

    //region scrolling
    private int getContentTop() { return topPos + CONTENT_TOP; }

    private int getContentBottom() { return topPos + panelH - PAD; }

    private int getContentLeft() { return getEdgeLeft() + getInset(); }

    private int getValueRight() { return getEdgeRight() - getInset(); }

    private int getInset() { return (getEdgeRight() - getEdgeLeft()) / CONTENT_INSET_DIVISOR; }

    private int getEdgeLeft() { return leftPos + PAD; }

    private int getEdgeRight() { return getTabLeft() - PAD; }

    private void drawScrollBar(SpriteBatch sprites, int total, int visible) {
        int x0 = getTabLeft() - SCROLL_GAP;
        int top = getContentTop();
        int track = visible * LINE_H;

        sprites.blit(SCROLLER_BACKGROUND, x0, top, SCROLL_W, track);
        int thumb = Math.max(LINE_H, track * visible / total);
        int offset = (track - thumb) * scrollRow / Math.max(1, total - visible);
        sprites.blit(SCROLLER, x0, top + offset, SCROLL_W, thumb);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        if (picker.isOpen()) return true;
        if (dy == 0.0) return super.mouseScrolled(mouseX, mouseY, dx, dy);

        scrollRow = Math.max(0, scrollRow - (int) Math.signum(dy));
        return true;
    }
    //endregion

    private boolean isTabLive(int tab) {
        if (tab != TAB_REFINEMENT) return true;

        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && ApertureService.isAwakened(player);
    }

    private int getTabLeft() { return leftPos + panelW - TAB_W - PAD; }

    private int getTabTop(int i) { return topPos + CONTENT_TOP + i * (TAB_H + TAB_GAP); }

    private boolean isInTab(double mouseX, double mouseY, int i) {
        return Widgets.isOver(mouseX, mouseY, getTabLeft(), getTabTop(i), TAB_W, TAB_H);
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
            for (int i = 0; i < TAB_TITLES.length; i++) {
                if (!isInTab(mouseX, mouseY, i) || !isTabLive(i)) continue;
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

    private interface RowVisitor {

        void visit(ScreenRow row, int x0, int x1, int y);
    }

    private record Content(boolean columns, List<ScreenRow> rows, List<List<ScreenRow>> groups,
                           List<ApertureButtons.ColumnButton> buttons, int visible) {

        static final Content EMPTY = new Content(false, List.of(), List.of(), List.of(), 0);
    }
}
