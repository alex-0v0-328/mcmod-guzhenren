package net.alex.guzhenren.client.screen.immortalaperture;

import java.util.List;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.OverviewPart;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.SecretRealmPart;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.WhitelistEntry;
import net.alex.guzhenren.gameplay.immortalaperture.soulsundermountain.SoulsunderMountainRules;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderMountain;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 秘境页（页签写秘境地名，即荡魂山）：左卡（标题 = 地名，右侧胆石数）一条金色胆石进度条与「挪生物上山」「胆识蛊催动」；
 * 右「荡魂山白名单」卡（右侧人数）按名字添加、凹槽里逐行移出。
 * <ul>
 *   <li>挪生物冷却中按钮失效，文字换成剩余秒数。</li>
 *   <li>名字输入框是 {@link EarthSpiritScreen} 持有的 {@code EditBox}（灰字补全在线玩家名、Tab 补全），本页只画它的底图、
 *       给出位置（{@link #fieldX} 等，界面布局时先调 {@link #resized}），并在「添加」时读走内容。</li>
 *   <li>主人那一行也能移出（主人可以选择承受震荡）；白名单的作用写在卡片标题的悬停提示里。</li>
 *   <li>列表行数随窗口高（{@value #MIN_ROWS} 到 {@value #MAX_ROWS} 行），多了滚轮翻、右侧滚动条。</li>
 * </ul>
 */

final class SecretRealmPage extends PanelPage {

    private static final String UI = EarthSpiritScreen.UI;
    private static final String TIP = EarthSpiritScreen.TIP;
    private static final int TOP = EarthSpiritScreen.BAR_H + 7;
    private static final int REALM_W = 218;
    private static final int BUTTON_Y = 38;
    private static final int BUTTON_H = 18;
    private static final int REALM_H = BUTTON_Y + BUTTON_H + PanelDrawing.PAD;
    private static final int ADD_W = 40;
    private static final int LIST_Y = 40;
    private static final int REMOVE_W = 30;
    private static final int MIN_ROWS = 3;
    private static final int MAX_ROWS = 10;
    private PanelDrawing.Columns columns = PanelDrawing.Columns.of(0, TOP, REALM_W, REALM_H);
    private int rows = MIN_ROWS;
    private int offset;

    SecretRealmPage(EarthSpiritScreen screen) { super(screen); }

    @Override
    protected Component name() { return screen.realmName(); }

    private SecretRealmPart.Section data() { return section(SecretRealmPart.INSTANCE, SecretRealmPart.Section.EMPTY); }

    private List<WhitelistEntry> listed() { return data().whitelist(); }

    @Override
    protected void resized(int width, int height) {
        columns = PanelDrawing.Columns.of(width, TOP, REALM_W, REALM_H);
        int fixed = LIST_Y + 2 + PanelDrawing.PAD + PanelDrawing.MARGIN;
        rows = Math.clamp((height - columns.rightY() - fixed) / PanelDrawing.ROW_H, MIN_ROWS, MAX_ROWS);
    }

    //region Geometry [几何]
    private int realmX() { return columns.leftX(); }

    private int realmW() { return columns.leftW(); }

    private int buttonW() { return (realmW() - 2 * PanelDrawing.PAD - 6) / 2; }

    private int moveX() { return realmX() + PanelDrawing.PAD; }

    private int driveX() { return moveX() + buttonW() + 6; }

    private int buttonY() { return TOP + BUTTON_Y; }

    int fieldX() { return columns.rightX() + PanelDrawing.PAD; }

    int fieldY() { return columns.rightY() + 18; }

    int fieldW() { return columns.rightW() - 2 * PanelDrawing.PAD - ADD_W - 4; }

    private int addX() { return fieldX() + fieldW() + 4; }

    private int listX() { return columns.rightX() + PanelDrawing.PAD; }

    private int listY() { return columns.rightY() + LIST_Y; }

    private int listW() { return columns.rightW() - 2 * PanelDrawing.PAD; }

    private int listH() { return rows * PanelDrawing.ROW_H + 2; }

    private int rowW() { return listW() - 2 - (listed().size() > rows ? 8 : 0); }

    private int rowTop(int index) { return listY() + 1 + index * PanelDrawing.ROW_H; }

    private int removeX() { return listX() + 1 + rowW() - 2 - REMOVE_W; }

    private int exemptH() { return LIST_Y + listH() + PanelDrawing.PAD; }
    //endregion

    @Override
    protected void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        SecretRealmPart.Section data = data();
        PanelDrawing.card(sprites, realmX(), TOP, realmW(), REALM_H);
        int barW = realmW() - 2 * PanelDrawing.PAD;
        int filled = data.gutsCap() <= 0 ? 0
                : (barW - 2) * Math.min(data.gutsStones(), data.gutsCap()) / data.gutsCap();
        sprites.blit(PanelDrawing.BAR_BACKGROUND, realmX() + PanelDrawing.PAD, TOP + 22, barW, 8);
        sprites.blit(PanelDrawing.BAR_FILL, realmX() + PanelDrawing.PAD + 1, TOP + 23, filled, 6, PanelDrawing.ACCENT);
        PanelDrawing.button(sprites, moveX(), buttonY(), buttonW(), BUTTON_H, data.moveCooldown() == 0, mouseX, mouseY);
        PanelDrawing.button(sprites, driveX(), buttonY(), buttonW(), BUTTON_H, true, mouseX, mouseY);

        PanelDrawing.card(sprites, columns.rightX(), columns.rightY(), columns.rightW(), exemptH());
        sprites.blit(screen.isNameFieldFocused() ? PanelDrawing.TEXT_FIELD_FOCUSED : PanelDrawing.TEXT_FIELD, fieldX(),
                fieldY(), fieldW(), PanelDrawing.BUTTON_H);
        PanelDrawing.button(sprites, addX(), fieldY(), ADD_W, PanelDrawing.BUTTON_H, true, mouseX, mouseY);
        sprites.blit(PanelDrawing.INSET, listX(), listY(), listW(), listH());
        List<WhitelistEntry> listed = listed();
        offset = PanelDrawing.clampScroll(offset, rows, listed.size());
        for (int i = 0; i < rows && offset + i < listed.size(); i++) {
            int top = rowTop(i);
            PanelDrawing.row(sprites, listX() + 1, top, rowW(), PanelDrawing.ROW_H,
                    Widgets.isOver(mouseX, mouseY, listX() + 1, top, rowW(), PanelDrawing.ROW_H));
            PanelDrawing.button(sprites, removeX(), top + 1, REMOVE_W, PanelDrawing.GHOST_H, PanelDrawing.Look.GHOST,
                    true, mouseX, mouseY);
        }
        if (listed.size() > rows) {
            int barX = listX() + 1 + rowW() + 1;
            PanelDrawing.scrollbar(sprites, barX, listY() + 1, listH() - 2, offset, rows, listed.size(),
                    Widgets.isOver(mouseX, mouseY, barX, listY() + 1, 6, listH() - 2));
        }
    }

    @Override
    protected void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        SecretRealmPart.Section data = data();
        PanelDrawing.title(graphics, font(), name(), Component.translatable(UI + "guts_value", data.gutsStones(),
                data.gutsCap()), realmX(), TOP, realmW());
        boolean moveLive = data.moveCooldown() == 0;
        Component move = moveLive ? Component.translatable(UI + "move_mobs")
                : Component.translatable(UI + "cooldown", data.moveCooldown());
        PanelDrawing.label(graphics, font(), move, moveX(), buttonY(), buttonW(), BUTTON_H, moveLive);
        PanelDrawing.label(graphics, font(), Component.translatable(UI + "drive_guts_gu"), driveX(), buttonY(),
                buttonW(), BUTTON_H, true);

        PanelDrawing.title(graphics, font(), Component.translatable(UI + "exempt"),
                Component.translatable(UI + "people", listed().size()), columns.rightX(), columns.rightY(),
                columns.rightW());
        PanelDrawing.label(graphics, font(), Component.translatable(UI + "add"), addX(), fieldY(), ADD_W,
                PanelDrawing.BUTTON_H, true);
        List<WhitelistEntry> listed = listed();
        for (int i = 0; i < rows && offset + i < listed.size(); i++) {
            WhitelistEntry person = listed.get(offset + i);
            int top = rowTop(i);
            int nameX = listX() + 18;
            graphics.drawString(font(), person.name(), nameX, top + 4, PanelDrawing.TEXT, false);
            if (person.owner()) {
                int tagX = nameX + font().width(person.name()) + 8;
                graphics.drawString(font(), Component.translatable(UI + "owner"), tagX, top + 4, PanelDrawing.ACCENT,
                        false);
            }
            PanelDrawing.label(graphics, font(), Component.translatable(UI + "remove"), removeX(), top + 1, REMOVE_W,
                    PanelDrawing.GHOST_H, PanelDrawing.Look.GHOST, true);
        }
    }

    @Override
    protected void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        List<WhitelistEntry> listed = listed();
        for (int i = 0; i < rows && offset + i < listed.size(); i++) {
            PanelDrawing.head(graphics, listed.get(offset + i).id(), listX() + 6, rowTop(i) + 4);
        }
    }

    @Override
    protected void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        if (tip(graphics, moveX(), buttonY(), buttonW(), BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "move_mobs", SoulsunderMountainRules.MOVE_LIMIT,
                        SoulsunderMountainRules.MOVE_COOLDOWN_TICKS / Ticks.SECOND))) {
            return;
        }
        if (tip(graphics, driveX(), buttonY(), buttonW(), BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "drive_guts_gu", data().gutsStones(),
                        SoulsunderMountain.GUTS_GU_SOUL_BONUS))) {
            return;
        }
        if (tip(graphics, addX(), fieldY(), ADD_W, PanelDrawing.BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "add_whitelist"))) {
            return;
        }
        if (tip(graphics, columns.rightX(), columns.rightY(), columns.rightW(), PanelDrawing.TITLE_H, mouseX, mouseY,
                Component.translatable(UI + "exempt_note"))) {
            return;
        }
        List<WhitelistEntry> listed = listed();
        for (int i = 0; i < rows && offset + i < listed.size(); i++) {
            if (tip(graphics, removeX(), rowTop(i) + 1, REMOVE_W, PanelDrawing.GHOST_H, mouseX, mouseY,
                    Component.translatable(TIP + "remove_whitelist"))) {
                return;
            }
        }
    }

    @Override
    protected boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        if (Widgets.isOver(mouseX, mouseY, moveX(), buttonY(), buttonW(), BUTTON_H)) {
            if (data().moveCooldown() == 0) send(new SecretRealmPart.MoveMobs());
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, driveX(), buttonY(), buttonW(), BUTTON_H)) {
            send(new SecretRealmPart.DriveGutsGu());
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, addX(), fieldY(), ADD_W, PanelDrawing.BUTTON_H)) {
            screen.addTypedName();
            return true;
        }
        List<WhitelistEntry> listed = listed();
        for (int i = 0; i < rows && offset + i < listed.size(); i++) {
            if (Widgets.isOver(mouseX, mouseY, removeX(), rowTop(i) + 1, REMOVE_W, PanelDrawing.GHOST_H)) {
                send(new OverviewPart.SetWhitelisted(listed.get(offset + i).id(), false));
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (!Widgets.isOver(mouseX, mouseY, listX(), listY(), listW(), listH())) return false;
        offset = PanelDrawing.clampScroll(offset - (int) Math.signum(delta), rows, listed().size());
        return true;
    }
}
