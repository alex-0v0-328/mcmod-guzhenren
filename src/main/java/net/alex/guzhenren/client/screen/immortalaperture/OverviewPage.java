package net.alex.guzhenren.client.screen.immortalaperture;

import java.util.List;
import java.util.Locale;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.OverviewPart;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.Page;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.WhitelistEntry;
import net.alex.guzhenren.gameplay.immortalaperture.spring.SpringRules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 信息页「总览」小节：左「福地概况」卡排疆域、荡魂山、养殖、种植四个可点数据块（点了跳到地图页、秘境页、养殖或种植小节）
 * 与通栏的元泉块（点了跳库藏页，悬停写每眼每分钟产量与登记眼数）；右「在场玩家」卡（标题右侧「N 人 · 外人 M」）每人一行：
 * 头像、名字、主人金标签或外人红标签、白名单开关、幽灵「驱逐」，卡底危险键「驱逐外人」。
 * <ul>
 *   <li>外人 = 不在白名单的非主人（Alex 定，原「外人」卡并进本卡）。主人那一行不画「驱逐」；白名单开关照常可点（主人可以
 *       选择承受震荡）。</li>
 *   <li>白名单与秘境页的荡魂山白名单是同一份。</li>
 *   <li>列表行数随窗口高（{@value #MIN_ROWS} 到 {@value #MAX_ROWS} 行），多了滚轮翻、右侧滚动条。</li>
 * </ul>
 */

final class OverviewPage extends PanelPage {

    private static final String UI = EarthSpiritScreen.UI;
    private static final String TIP = EarthSpiritScreen.TIP;
    private static final String[] STAT_ICONS = {"icon_map", "icon_mountain", "icon_husbandry", "tool_farm",
            "icon_storage"};
    private static final int STORAGE_STAT = -3;
    private static final int MAP_STAT = -2;
    private static final int REALM_STAT = -1;
    private static final int[] STAT_PARTS = {MAP_STAT, REALM_STAT, InfoPage.HERD, InfoPage.FARM, STORAGE_STAT};
    private static final String[] STAT_TABS = {"territory", "", "husbandry", "farming", ""};
    private static final String[] STAT_TARGETS = {"map", "", "husbandry", "farming", "storage"};
    private static final int SPRING_STAT = 4;
    private static final int SUMMARY_W = 218;
    private static final int STAT_H = 26;
    private static final int STAT_PITCH = 32;
    private static final int SUMMARY_H = 20 + 3 * STAT_PITCH;
    private static final int LIST_Y = 18;
    private static final int MIN_ROWS = 3;
    private static final int MAX_ROWS = 8;
    private static final int EXPEL_W = 30;
    private static final int OUTSIDERS_W = 68;
    private static final int OUTSIDERS_H = 18;
    private PanelDrawing.Columns columns = PanelDrawing.Columns.of(0, 0, SUMMARY_W, SUMMARY_H);
    private int rows = MIN_ROWS;
    private int offset;

    OverviewPage(EarthSpiritScreen screen) { super(screen); }

    @Override
    protected Component name() { return Component.translatable(UI + "tab.overview"); }

    private OverviewPart.Section data() { return section(OverviewPart.INSTANCE, OverviewPart.Section.EMPTY); }

    private List<WhitelistEntry> players() { return data().players(); }

    private static boolean outsider(WhitelistEntry person) { return !person.whitelisted() && !person.owner(); }

    @Override
    protected void resized(int width, int height) {
        int top = InfoPage.CONTENT_TOP;
        columns = PanelDrawing.Columns.of(width, top, SUMMARY_W, SUMMARY_H);
        int fixed = LIST_Y + 2 + 6 + OUTSIDERS_H + PanelDrawing.PAD + PanelDrawing.MARGIN;
        rows = Math.clamp((height - columns.rightY() - fixed) / PanelDrawing.ROW_H, MIN_ROWS, MAX_ROWS);
    }

    //region Geometry [几何]
    private int summaryW() { return columns.leftW(); }

    private int statW(int stat) { return stat == SPRING_STAT ? summaryW() - 12 : (summaryW() - 18) / 2; }

    private int statX(int stat) { return columns.leftX() + 6 + (stat == SPRING_STAT ? 0 : stat % 2 * (statW(0) + 6)); }

    private int statY(int stat) { return InfoPage.CONTENT_TOP + 20 + stat / 2 * STAT_PITCH; }

    private boolean isOverStat(int stat, double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, statX(stat), statY(stat), statW(stat), STAT_H);
    }

    private int listX() { return columns.rightX() + 6; }

    private int listY() { return columns.rightY() + LIST_Y; }

    private int listW() { return columns.rightW() - 12; }

    private int listH() { return rows * PanelDrawing.ROW_H + 2; }

    private int rowW() { return listW() - 2 - (players().size() > rows ? 8 : 0); }

    private int rowRight() { return listX() + 1 + rowW(); }

    private int rowTop(int index) { return listY() + 1 + index * PanelDrawing.ROW_H; }

    private int expelX() { return rowRight() - 2 - EXPEL_W; }

    private int switchX() { return expelX() - 8 - PanelDrawing.SWITCH_W; }

    private int outsidersX() { return columns.rightX() + columns.rightW() - PanelDrawing.PAD - OUTSIDERS_W; }

    private int outsidersY() { return listY() + listH() + 6; }

    private int peopleH() { return LIST_Y + listH() + 6 + OUTSIDERS_H + PanelDrawing.PAD; }
    //endregion

    private Component statLabel(int stat) {
        if (stat == SPRING_STAT) return Component.translatable(UI + "springs");
        return STAT_PARTS[stat] == REALM_STAT ? screen.realmName()
                : Component.translatable(UI + "tab." + STAT_TABS[stat]);
    }

    private Component statTarget(int stat) {
        return STAT_PARTS[stat] == REALM_STAT ? screen.realmName()
                : Component.translatable(UI + "tab." + STAT_TARGETS[stat]);
    }

    private Component statTip(int stat) {
        if (stat != SPRING_STAT) return Component.translatable(TIP + "open_page", statTarget(stat));

        return Component.translatable(TIP + "springs", SpringRules.stonesPerMinute(1), data().springs(),
                statTarget(stat));
    }

    @Override
    protected void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        PanelDrawing.card(sprites, columns.leftX(), InfoPage.CONTENT_TOP, summaryW(), SUMMARY_H);
        for (int i = 0; i < STAT_ICONS.length; i++) {
            PanelDrawing.stat(sprites, PanelDrawing.sprite(STAT_ICONS[i]), statX(i), statY(i), statW(i), STAT_H,
                    isOverStat(i, mouseX, mouseY));
        }
        PanelDrawing.card(sprites, columns.rightX(), columns.rightY(), columns.rightW(), peopleH());
        sprites.blit(PanelDrawing.INSET, listX(), listY(), listW(), listH());
        List<WhitelistEntry> players = players();
        offset = PanelDrawing.clampScroll(offset, rows, players.size());
        for (int i = 0; i < rows && offset + i < players.size(); i++) {
            WhitelistEntry person = players.get(offset + i);
            int top = rowTop(i);
            PanelDrawing.row(sprites, listX() + 1, top, rowW(), PanelDrawing.ROW_H,
                    Widgets.isOver(mouseX, mouseY, listX() + 1, top, rowW(), PanelDrawing.ROW_H));
            PanelDrawing.toggle(sprites, switchX(), top + 3, person.whitelisted(), true, mouseX, mouseY);
            if (!person.owner()) {
                PanelDrawing.button(sprites, expelX(), top + 1, EXPEL_W, PanelDrawing.GHOST_H,
                        PanelDrawing.Look.GHOST, true, mouseX, mouseY);
            }
        }
        if (players.size() > rows) {
            PanelDrawing.scrollbar(sprites, rowRight() + 1, listY() + 1, listH() - 2, offset, rows, players.size(),
                    Widgets.isOver(mouseX, mouseY, rowRight() + 1, listY() + 1, 6, listH() - 2));
        }
        PanelDrawing.button(sprites, outsidersX(), outsidersY(), OUTSIDERS_W, OUTSIDERS_H, PanelDrawing.Look.DANGER,
                true, mouseX, mouseY);
    }

    @Override
    protected void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        OverviewPart.Section data = data();
        PanelDrawing.title(graphics, font(), Component.translatable(UI + "summary"), Component.empty(),
                columns.leftX(), InfoPage.CONTENT_TOP, summaryW());
        String[] values = {
                Component.translatable(UI + "acres_value", String.format(Locale.ROOT, "%,d", data.acres()))
                        .getString(),
                Component.translatable(UI + "guts_value", data.gutsStones(), data.gutsCap()).getString(),
                Component.translatable(UI + "animals_value", data.animals()).getString(),
                Component.translatable(UI + "crops_value", data.crops()).getString(),
                Component.translatable(UI + "springs_value", data.springs(),
                        SpringRules.stonesPerMinute(data.springs())).getString()};
        for (int i = 0; i < values.length; i++) {
            PanelDrawing.statText(graphics, font(), statLabel(i), values[i], statX(i), statY(i), STAT_H);
        }

        List<WhitelistEntry> players = players();
        long outsiders = players.stream().filter(OverviewPage::outsider).count();
        PanelDrawing.title(graphics, font(), Component.translatable(UI + "present"),
                Component.translatable(UI + "present_count", players.size(), outsiders), columns.rightX(),
                columns.rightY(), columns.rightW());
        Component whitelist = Component.translatable(UI + "whitelist");
        for (int i = 0; i < rows && offset + i < players.size(); i++) {
            WhitelistEntry person = players.get(offset + i);
            int top = rowTop(i);
            int nameX = listX() + 18;
            graphics.drawString(font(), person.name(), nameX, top + 4, PanelDrawing.TEXT, false);
            int tagX = nameX + font().width(person.name()) + 8;
            if (person.owner()) {
                graphics.drawString(font(), Component.translatable(UI + "owner"), tagX, top + 4, PanelDrawing.ACCENT,
                        false);
            } else if (outsider(person)) {
                graphics.drawString(font(), Component.translatable(UI + "outsider"), tagX, top + 4,
                        PanelDrawing.OUTSIDER, false);
            }
            PanelDrawing.right(graphics, font(), whitelist, switchX() - 5, top + 4, PanelDrawing.LEGEND);
            if (!person.owner()) {
                PanelDrawing.label(graphics, font(), Component.translatable(UI + "expel"), expelX(), top + 1, EXPEL_W,
                        PanelDrawing.GHOST_H, PanelDrawing.Look.GHOST, true);
            }
        }
        PanelDrawing.label(graphics, font(), Component.translatable(UI + "expel_outsiders"), outsidersX(),
                outsidersY(), OUTSIDERS_W, OUTSIDERS_H, PanelDrawing.Look.DANGER, true);
    }

    @Override
    protected void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        List<WhitelistEntry> players = players();
        for (int i = 0; i < rows && offset + i < players.size(); i++) {
            PanelDrawing.head(graphics, players.get(offset + i).id(), listX() + 6, rowTop(i) + 4);
        }
    }

    @Override
    protected void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        for (int i = 0; i < STAT_ICONS.length; i++) {
            if (tip(graphics, statX(i), statY(i), statW(i), STAT_H, mouseX, mouseY, statTip(i))) return;
        }
        long outsiders = players().stream().filter(OverviewPage::outsider).count();
        if (tip(graphics, outsidersX(), outsidersY(), OUTSIDERS_W, OUTSIDERS_H, mouseX, mouseY,
                Component.translatable(TIP + "expel_outsiders", outsiders))) {
            return;
        }
        List<WhitelistEntry> players = players();
        for (int i = 0; i < rows && offset + i < players.size(); i++) {
            int top = rowTop(i);
            if (tip(graphics, switchX(), top + 3, PanelDrawing.SWITCH_W, PanelDrawing.SWITCH_H, mouseX, mouseY,
                    Component.translatable(TIP + "whitelist"))) {
                return;
            }
            if (!players.get(offset + i).owner() && tip(graphics, expelX(), top + 1, EXPEL_W, PanelDrawing.GHOST_H,
                    mouseX, mouseY, Component.translatable(TIP + "expel"))) {
                return;
            }
        }
    }

    @Override
    protected boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        for (int i = 0; i < STAT_ICONS.length; i++) {
            if (!isOverStat(i, mouseX, mouseY)) continue;
            switch (STAT_PARTS[i]) {
                case REALM_STAT -> screen.openPage(Page.SECRET_REALM);
                case MAP_STAT -> screen.openPage(Page.MAP);
                case STORAGE_STAT -> screen.openPage(Page.STORAGE);
                default -> screen.openInfo(STAT_PARTS[i]);
            }
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, outsidersX(), outsidersY(), OUTSIDERS_W, OUTSIDERS_H)) {
            send(new OverviewPart.ExpelOutsiders());
            return true;
        }
        List<WhitelistEntry> players = players();
        for (int i = 0; i < rows && offset + i < players.size(); i++) {
            WhitelistEntry person = players.get(offset + i);
            int top = rowTop(i);
            if (Widgets.isOver(mouseX, mouseY, switchX(), top + 3, PanelDrawing.SWITCH_W, PanelDrawing.SWITCH_H)) {
                send(new OverviewPart.SetWhitelisted(person.id(), !person.whitelisted()));
                return true;
            }
            if (!person.owner() && Widgets.isOver(mouseX, mouseY, expelX(), top + 1, EXPEL_W, PanelDrawing.GHOST_H)) {
                send(new OverviewPart.Expel(person.id()));
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (!Widgets.isOver(mouseX, mouseY, listX(), listY(), listW(), listH())) return false;
        offset = PanelDrawing.clampScroll(offset - (int) Math.signum(delta), rows, players().size());
        return true;
    }
}
