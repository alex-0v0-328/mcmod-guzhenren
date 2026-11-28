package net.alex.guzhenren.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.ModKeyMappings;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.InfoRowRenderer.RowBlock;
import net.alex.guzhenren.client.screen.InfoRowRenderer.ScreenRow;
import net.alex.guzhenren.display.InfoModel;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.body.BodyPart;
import net.alex.guzhenren.gameplay.body.BodyService;
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
 * B 面板玩家信息总界面：直接读取客户端同步附件并以六个页签展示玩家全部状态。
 * <ul>
 *   <li>包含空窍、肉身、魂魄、流派、脑海、炼蛊六个页签；页签平铺无框，悬停提亮一档，选中页签左侧一道 2 像素金边（金只
 *       表状态，标题与分组标题同用金色，Alex 定）；面板宽度取屏幕宽度的 0.80，不足 {@code PANEL_MIN_W} 时补齐至最小
 *       值（GUI 6 缩放下由 342 补齐至 400）。</li>
 *   <li>肉身页与空窍页采用四栏布局（图 | 文字 | 按钮 | 页签，由 Alex 选定）：左侧图栏留白 {@code FIGURE_MARGIN}
 *       且与文字栏相隔 {@code COLUMN_GAP}，图栏宽度等于图原宽乘以整数倍率（在留足按钮栏与 {@code TEXT_MIN_W} 后取
 *       可用最大值，上下限见 {@link BodyFigure} 与 {@link ApertureFigure}；空窍图保底 2 倍，窄屏下允许两页图栏不
 *       同宽且空窍页文字栏在 GUI 6 下缩至 122 低于常规下限）；肉身页无按钮栏，文字栏直接延伸至页签前。</li>
 *   <li>空窍页为每只空窍划分独立段落（空窍图、文字与 {@link ApertureButtons} 顶对齐），拥有双空窍时上下两段整体滚
 *       动；段高与段距均为行高整倍数以保证文本整行进出视口，插图与按钮由裁剪区域截边；{@link PathPicker} 作为叠加
 *       网格弹窗展示在面板之上。</li>
 *   <li>⚠ 普通 {@code Screen} 背后不绑定菜单容器，没有容器按钮通道可用于发送打开容器意图：因此炼蛊页不绘制内容行，
 *       而是经由客户端意图 payload 请求服务端打开炼蛊容器。</li>
 *   <li>信息行内容取自 {@link InfoModel} 并由 {@link InfoRowRenderer} 排版绘制，右侧值在 {@link RowBlock} 量算的
 *       列宽内左对齐（仅超出行右缘时贴右），模型未变时由 {@link InfoRowCache} 跨帧复用。</li>
 *   <li>整帧复用单个 {@link GuiBatch}：先画外框（面板、页签、滚动条与标题），再在内容区裁剪框内绘制精灵图、填色与
 *       文字；因开关裁剪区域会主动冲刷批次，外框与内容各自独立成批。</li>
 *   <li>空窍存储与炼蛊界面作为本界面的子界面，按 Esc 会退回本面板；以这种方式返回打开的面板会吞掉 Esc 直到松开，确
 *       保连续按住或重复按键一次只退一级。</li>
 *   <li>本文件有意超过 300 行：页签、信息行、滚动条与分栏计算属于同一套响应式布局，缩放窗口时读取同一组边界变量
 *       （{@code edgeLeft}、{@code edgeRight}、{@code tabLeft}）。</li>
 * </ul>
 */

public final class PlayerInfoScreen extends Screen {

    private static final float SCREEN_FRACTION = 0.80F;
    private static final int PANEL_MIN_W = 400;
    private static final int PAD = 12;
    static final int HEADER_H = 22;
    private static final int CONTENT_TOP = HEADER_H + 8;
    private static final int LINE_H = 12;
    private static final int CONTENT_INSET_DIVISOR = 12;
    private static final int TAB_W = 76;
    private static final int TAB_H = 20;
    private static final int TAB_GAP = 4;
    static final int DIVIDER = ModPalette.SURFACE_2;
    static final int ROW_HOVER = ModPalette.SURFACE_2;
    private static final ResourceLocation TAB = Guzhenren.id("tab");
    private static final ResourceLocation TAB_HIGHLIGHTED = Guzhenren.id("tab_highlighted");
    private static final ResourceLocation TAB_SELECTED = Guzhenren.id("tab_selected");
    private static final ResourceLocation TAB_DISABLED = Guzhenren.id("tab_disabled");
    private static final ResourceLocation SCROLLER = Guzhenren.id("blissful_land/screen/scroller");
    private static final ResourceLocation SCROLLER_BACKGROUND = Guzhenren.id("blissful_land/screen/scroller_track");
    private static final Component[] TAB_TITLES = {
            Component.translatable("guzhenren.screen.tab.aperture"),
            Component.translatable("guzhenren.screen.tab.body"),
            Component.translatable("guzhenren.screen.tab.soul"),
            Component.translatable("guzhenren.screen.tab.paths"),
            Component.translatable("guzhenren.screen.tab.mind"),
            Component.translatable("guzhenren.screen.tab.refinement"),
    };
    static final int BTN_H = 20;
    static final int BTN_GAP = 4;
    private static final int TAB_APERTURE = 0;
    private static final int TAB_BODY = 1;
    private static final int TAB_PATHS = 3;
    private static final int TAB_MIND = 4;
    private static final int TAB_REFINEMENT = 5;
    private static final int SCROLL_W = 6;
    private static final int SCROLL_GAP = 9;
    private static final int FIGURE_MARGIN = 8;
    private static final int COLUMN_GAP = 10;
    private static final int TEXT_MIN_W = 150;
    private static final int BUTTON_PAD = 6;
    private static final int BUTTON_MIN_W = 40;
    private int leftPos;
    private int topPos;
    private int panelW;
    private int panelH;
    private int activeTab;
    private @Nullable InfoRowRenderer.Click hoverClick;
    private @Nullable Component hoverValue;
    private int scrollRow;
    private final PathPicker picker = new PathPicker();
    private final InfoRowCache rowCache = new InfoRowCache();
    private boolean escapeHeld;

    public PlayerInfoScreen() { this(false); }

    PlayerInfoScreen(boolean escapeHeld) {
        super(Component.translatable("guzhenren.screen.info.title"));
        this.escapeHeld = escapeHeld;
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 2 * PAD, Math.max(Math.round(width * SCREEN_FRACTION), PANEL_MIN_W));
        panelH = Math.round(height * SCREEN_FRACTION);
        leftPos = (width - panelW) / 2;
        topPos = (height - panelH) / 2;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int accent = ModPalette.ACCENT;
        LocalPlayer player = Minecraft.getInstance().player;
        Content content = player == null ? Content.EMPTY : getContent(player);
        hoverClick = null;
        hoverValue = null;

        GuiBatch.draw(graphics, () -> {
            SpriteBatch.draw(graphics, sprites -> drawFrameSprites(sprites, content, player, mouseX, mouseY));
            Widgets.drawRule(graphics, leftPos + PAD, topPos + HEADER_H, leftPos + panelW - PAD, DIVIDER);
            drawFrameTexts(graphics, accent);
            if (player == null) return;

            graphics.enableScissor(getEdgeLeft(), getContentTop() - 1, getEdgeRight(), getContentBottom());
            SpriteBatch.draw(graphics, sprites -> drawContentSprites(sprites, content, player, mouseX, mouseY));
            fillContent(graphics, content, player, mouseX, mouseY);
            drawContentTexts(graphics, content, accent);
            graphics.disableScissor();
        });
        if (player != null && !picker.isOpen()) drawTooltips(graphics, content, player, mouseX, mouseY);
        if (player != null && picker.isOpen()) {
            picker.render(graphics, font, leftPos, topPos, panelW, panelH, mouseX, mouseY, accent);
        }
    }

    private Content getContent(LocalPlayer player) {
        int visible = (getContentBottom() - getContentTop()) / LINE_H;
        if (activeTab != TAB_APERTURE && activeTab != TAB_BODY) {
            RowBlock block = rowCache.getRows(font, getListModel(player));
            scrollRow = Mth.clamp(scrollRow, 0, Math.max(0, block.rows().size() - visible));
            return new Content(block, block.rows().size(), visible, null, List.of(), List.of());
        }
        boolean aperturePage = activeTab == TAB_APERTURE;
        boolean hasButtons = aperturePage && ApertureService.hasAperture(player);
        Columns columns = getColumns(aperturePage, hasButtons);
        List<RowBlock> blocks = aperturePage
                ? rowCache.getApertureGroups(font, InfoModel.buildApertureRows(player))
                : List.of(rowCache.getRows(font, InfoModel.buildBodyRows(player)));
        List<Band> bands = getBands(player, blocks, hasButtons, columns.figureH());
        int total = bands.isEmpty() ? 0 : bands.getLast().bottom() / LINE_H;
        scrollRow = Mth.clamp(scrollRow, 0, Math.max(0, total - visible));
        return new Content(RowBlock.EMPTY, total, visible, columns, bands, placeButtons(bands, columns));
    }

    private List<InfoModel.Row> getListModel(LocalPlayer player) {
        return switch (activeTab) {
            case TAB_PATHS -> InfoModel.buildPathRows(player);
            case TAB_MIND -> InfoModel.buildMindRows(player);
            default -> InfoModel.buildSoulRows(player);
        };
    }

    //region the three passes [三道绘制]
    private void drawFrameSprites(SpriteBatch sprites, Content content, @Nullable LocalPlayer player, int mouseX,
                                  int mouseY) {
        Widgets.drawPanel(sprites, leftPos, topPos, panelW, panelH);
        for (int i = 0; i < TAB_TITLES.length; i++) drawTab(sprites, i, mouseX, mouseY);
        if (player != null && content.total() > content.visible()) {
            drawScrollBar(sprites, content.total(), content.visible());
        }
    }

    private void drawTab(SpriteBatch sprites, int i, int mouseX, int mouseY) {
        int x0 = getTabLeft();
        int y0 = getTabTop(i);
        boolean active = i == activeTab;
        ResourceLocation tab = !isTabLive(i) ? TAB_DISABLED
                : !active && isInTab(mouseX, mouseY, i) ? TAB_HIGHLIGHTED : TAB;

        sprites.blit(tab, x0, y0, TAB_W, TAB_H);
        if (active) sprites.blit(TAB_SELECTED, x0, y0, TAB_W, TAB_H);
    }

    private void drawFrameTexts(GuiGraphics graphics, int accent) {
        graphics.drawString(font, TAB_TITLES[activeTab],
                leftPos + PAD, topPos + (HEADER_H - font.lineHeight) / 2, accent, false);
        for (int i = 0; i < TAB_TITLES.length; i++) {
            int color = i == activeTab ? ModPalette.TEXT : isTabLive(i) ? ModPalette.LEGEND_TEXT : ModPalette.TEXT_DEAD;
            Widgets.drawCentered(graphics, font, TAB_TITLES[i], getTabLeft(), getTabTop(i), TAB_W, TAB_H, color);
        }
    }

    private void drawContentSprites(SpriteBatch sprites, Content content, LocalPlayer player, int mouseX,
                                    int mouseY) {
        for (ApertureButtons.ColumnButton b : content.buttons()) {
            ApertureButtons.draw(sprites, player, b, isOverButton(mouseX, mouseY, b));
        }
        Columns columns = content.columns();
        if (columns == null) return;
        if (activeTab == TAB_BODY) {
            BodyFigure.draw(sprites, BodyService.getAnatomy(player), columns.figureLeft(),
                    getBandTop(content.bands().getFirst()), columns.scale());
            return;
        }
        for (int i = 0; i < content.bands().size(); i++) {
            ApertureFigure.draw(sprites, ApertureService.getAperture(player, i), columns.figureLeft(),
                    getBandTop(content.bands().get(i)), columns.scale());
        }
    }

    private void fillContent(GuiGraphics graphics, Content content, LocalPlayer player, int mouseX, int mouseY) {
        Columns columns = content.columns();
        if (columns != null) {
            for (int i = 1; i < content.bands().size(); i++) {
                int y = getBandTop(content.bands().get(i)) - LINE_H / 2;
                graphics.fill(columns.textLeft(), y, getEdgeRight(), y + 1, DIVIDER);
            }
        }
        forEachRow(content, (block, row, x0, x1, y) -> fillRowHover(graphics, block, row, x0, x1, y, mouseX,
                mouseY));
        for (ApertureButtons.ColumnButton b : content.buttons()) ApertureButtons.fillProgress(graphics, player, b);
    }

    private void drawContentTexts(GuiGraphics graphics, Content content, int accent) {
        forEachRow(content, (block, row, x0, x1, y) -> drawRowText(graphics, block, row, x0, x1, y, accent));
        for (ApertureButtons.ColumnButton b : content.buttons()) ApertureButtons.drawLabel(graphics, font, b);
    }

    private void forEachRow(Content content, RowVisitor visitor) {
        Columns columns = content.columns();
        if (columns == null) {
            RowBlock block = content.block();
            int end = Math.min(block.rows().size(), scrollRow + content.visible());
            int y = getContentTop();
            for (int i = scrollRow; i < end; i++) {
                visitor.visit(block, block.rows().get(i), getContentLeft(), getValueRight(), y);
                y += LINE_H;
            }
            return;
        }
        for (Band band : content.bands()) {
            int y = getBandTop(band);
            for (ScreenRow row : band.block().rows()) {
                if (y >= getContentTop() && y + LINE_H <= getContentBottom()) {
                    visitor.visit(band.block(), row, columns.textLeft(), columns.textRight(), y);
                }
                y += LINE_H;
            }
        }
    }

    private void fillRowHover(GuiGraphics graphics, RowBlock block, ScreenRow row, int x0, int x1, int y, int mouseX,
                              int mouseY) {
        if (!Widgets.isOver(mouseX, mouseY, x0 - 2, y - 1, x1 - x0 + 4, LINE_H)) return;

        graphics.fill(x0 - 2, y - 1, x1 + 2, y + LINE_H - 1, ROW_HOVER);
        if (row.click() != null) hoverClick = row.click();
        if (InfoRowRenderer.isValueClipped(font, block, row, x0, x1)) hoverValue = row.value();
    }

    private void drawRowText(GuiGraphics graphics, RowBlock block, ScreenRow row, int x0, int x1, int y, int accent) {
        int labelColor = row.value() == null ? accent : ModPalette.TEXT;
        graphics.drawString(font, row.label(), x0 + row.indent(), y, labelColor, false);
        Component value = row.value();
        if (value != null) {
            int left = InfoRowRenderer.getValueLeft(font, block, row, value, x0, x1);
            graphics.drawString(font, InfoRowRenderer.clip(font, value, x1 - left), left, y, ModPalette.TEXT, false);
        }
        int cellW = Math.min(block.cellWidth(), (x1 - x0 - row.indent()) / InfoModel.READINGS_PER_ROW);
        for (int i = 0; i < row.cells().size(); i++) {
            graphics.drawString(font, row.cells().get(i), x0 + row.indent() + i * cellW, y, ModPalette.TEXT, false);
        }
    }

    private void drawTooltips(GuiGraphics graphics, Content content, LocalPlayer player, int mouseX, int mouseY) {
        if (hoverValue != null) {
            graphics.renderTooltip(font, hoverValue, mouseX, mouseY);
            return;
        }
        for (ApertureButtons.ColumnButton b : content.buttons()) {
            if (isOverButton(mouseX, mouseY, b) && ApertureButtons.isClipped(font, b)) {
                graphics.renderTooltip(font, b.label(), mouseX, mouseY);
                return;
            }
        }
        Columns columns = content.columns();
        if (columns == null || activeTab != TAB_BODY || content.bands().isEmpty()) return;
        if (!isInContent(mouseX, mouseY)) return;

        BodyPart part = BodyFigure.getHovered(mouseX, mouseY, columns.figureLeft(),
                getBandTop(content.bands().getFirst()), columns.scale());
        if (part == null) return;

        graphics.renderComponentTooltip(font, BodyFigure.getTooltip(BodyService.getAnatomy(player), part), mouseX,
                mouseY);
    }
    //endregion

    //region figure columns [分栏]
    private Columns getColumns(boolean aperturePage, boolean hasButtons) {
        int span = getEdgeRight() - getEdgeLeft();
        int height = getContentBottom() - getContentTop();
        int buttonW = Math.max(BUTTON_MIN_W, Math.min(ApertureButtons.getLabelWidth(font) + 2 * BUTTON_PAD,
                span - 2 * FIGURE_MARGIN - BodyFigure.WIDTH - 2 * COLUMN_GAP - TEXT_MIN_W));
        int figureBudget = span - 2 * FIGURE_MARGIN - 2 * COLUMN_GAP - buttonW - TEXT_MIN_W;
        int scale = aperturePage ? ApertureFigure.getScale(figureBudget, height)
                : Math.max(1, BodyFigure.getScale(figureBudget, height));
        int figureW = (aperturePage ? ApertureFigure.SIZE : BodyFigure.WIDTH) * scale;
        int figureH = (aperturePage ? ApertureFigure.SIZE : BodyFigure.HEIGHT) * scale;
        int figureLeft = getEdgeLeft() + FIGURE_MARGIN;
        int textLeft = figureLeft + figureW + FIGURE_MARGIN + COLUMN_GAP;
        int buttonLeft = getEdgeRight() - buttonW;
        return new Columns(scale, figureLeft, figureH, textLeft,
                hasButtons ? buttonLeft - COLUMN_GAP : getEdgeRight(), buttonLeft);
    }

    private List<Band> getBands(LocalPlayer player, List<RowBlock> blocks, boolean hasButtons, int figureH) {
        List<Band> bands = new ArrayList<>(blocks.size());
        int top = 0;
        for (int i = 0; i < blocks.size(); i++) {
            RowBlock block = blocks.get(i);
            List<ApertureButtons.StackButton> stack = hasButtons ? ApertureButtons.buildStackButtons(player, i)
                    : List.of();
            int stackH = stack.size() * (BTN_H + BTN_GAP) - BTN_GAP;
            int height = Math.max(block.rows().size() * LINE_H, Math.max(figureH, stackH));
            height = Mth.roundToward(height, LINE_H);
            bands.add(new Band(block, stack, top, height));
            top += height + LINE_H;
        }
        return bands;
    }

    private List<ApertureButtons.ColumnButton> placeButtons(List<Band> bands, Columns columns) {
        List<ApertureButtons.ColumnButton> buttons = new ArrayList<>();
        for (Band band : bands) {
            int top = getBandTop(band);
            for (ApertureButtons.StackButton b : band.stack()) {
                buttons.add(new ApertureButtons.ColumnButton(b.aperture(), b.kind(), b.label(), columns.buttonLeft(),
                        getEdgeRight(), top + b.top()));
            }
        }
        return buttons;
    }

    private int getBandTop(Band band) { return getContentTop() + band.top() - scrollRow * LINE_H; }

    private boolean isOverButton(double mouseX, double mouseY, ApertureButtons.ColumnButton b) {
        return isInContent(mouseX, mouseY) && ApertureButtons.isInBox(mouseX, mouseY, b.x0(), b.x1(), b.top());
    }

    private boolean isInContent(double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, getEdgeLeft(), getContentTop(), getEdgeRight() - getEdgeLeft(),
                getContentBottom() - getContentTop());
    }

    private boolean clickApertureButtons(double mouseX, double mouseY) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;

        for (ApertureButtons.ColumnButton b : getContent(player).buttons()) {
            if (!isOverButton(mouseX, mouseY, b)) continue;
            if (ApertureButtons.click(player, b)) onClose();
            return true;
        }
        return false;
    }
    //endregion

    //region scrolling [滚动]
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
        if (escapeHeld && keyCode == InputConstants.KEY_ESCAPE) return true;
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
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) escapeHeld = false;
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private interface RowVisitor {

        void visit(RowBlock block, ScreenRow row, int x0, int x1, int y);
    }

    private record Columns(int scale, int figureLeft, int figureH, int textLeft, int textRight, int buttonLeft) {}

    private record Band(RowBlock block, List<ApertureButtons.StackButton> stack, int top, int height) {

        int bottom() { return top + height; }
    }

    private record Content(RowBlock block, int total, int visible, @Nullable Columns columns, List<Band> bands,
                           List<ApertureButtons.ColumnButton> buttons) {

        static final Content EMPTY = new Content(RowBlock.EMPTY, 0, 0, null, List.of(), List.of());
    }
}
