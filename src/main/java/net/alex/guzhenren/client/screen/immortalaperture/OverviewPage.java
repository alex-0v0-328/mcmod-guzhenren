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
 * 信息页「总览」小节，上下两张卡排成一列（Alex 定，宽至多 {@value InfoPage#COLUMN_W}、居中）：上「福地概况」卡排疆域、荡魂山、
 * 养殖、种植四个可点数据块（点了跳到地图页、秘境页、养殖或种植小节）与占两格的元泉块（点了跳库藏页，悬停写每眼每分钟产量与登记
 * 眼数），一行三块；下「在场玩家」卡（标题右侧「N 人 · 外人 M」）一人一张 {@value #TILE_W}×{@value #TILE_H} 小卡（Alex 定：
 * 原来一人一整行太长太挤）：上行头像、名字、主人金字或外人红字，下行白名单开关与幽灵「驱逐」；小卡下方危险键「驱逐外人」。
 * <ul>
 *   <li>外人 = 不在白名单的非主人（Alex 定）。主人那张不画「驱逐」；白名单开关照常可点（主人可以选择承受震荡）。</li>
 *   <li>白名单与秘境页的荡魂山白名单是同一份。</li>
 *   <li>小卡一行排得下几张就排几张，行数随窗口高（{@value #MIN_ROWS} 到 {@value #MAX_ROWS} 行），多了滚轮按行翻。</li>
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
    private static final int STAT_H = 26;
    private static final int STAT_PITCH = 30;
    private static final int STAT_GAP = 4;
    private static final int SUMMARY_H = 20 + 2 * STAT_PITCH;
    private static final int TILES_Y = 20;
    private static final int TILE_W = 130;
    private static final int TILE_H = 40;
    private static final int TILE_GAP = 8;
    private static final int MIN_ROWS = 1;
    private static final int MAX_ROWS = 4;
    private static final int EXPEL_W = 30;
    private static final int OUTSIDERS_W = 68;
    private static final int OUTSIDERS_H = 18;
    private int columnX;
    private int columnW = InfoPage.COLUMN_W;
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
        columnX = InfoPage.columnX(width);
        columnW = InfoPage.columnWidth(width);
        int fixed = TILES_Y + TILE_GAP + OUTSIDERS_H + PanelDrawing.PAD + PanelDrawing.MARGIN;
        rows = Math.clamp((height - peopleY() - fixed + TILE_GAP) / (TILE_H + TILE_GAP), MIN_ROWS, MAX_ROWS);
    }

    //region Geometry [几何]
    private int statW(int stat) {
        int single = (columnW - 12 - 2 * STAT_GAP) / 3;
        return stat == SPRING_STAT ? 2 * single + STAT_GAP : single;
    }

    private int statX(int stat) {
        int column = stat == SPRING_STAT ? 1 : stat % 3;
        return columnX + 6 + column * (statW(0) + STAT_GAP);
    }

    private int statY(int stat) { return InfoPage.CONTENT_TOP + 20 + (stat >= 3 ? STAT_PITCH : 0); }

    private boolean isOverStat(int stat, double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, statX(stat), statY(stat), statW(stat), STAT_H);
    }

    private int peopleY() { return InfoPage.CONTENT_TOP + SUMMARY_H + PanelDrawing.GAP; }

    private int perRow() { return Math.max(1, (columnW - 12 + TILE_GAP) / (TILE_W + TILE_GAP)); }

    private int totalRows() { return (players().size() + perRow() - 1) / perRow(); }

    private int tileX(int slot) { return columnX + 6 + slot % perRow() * (TILE_W + TILE_GAP); }

    private int tileY(int slot) { return peopleY() + TILES_Y + slot / perRow() * (TILE_H + TILE_GAP); }

    private int tilesH() { return rows * (TILE_H + TILE_GAP) - TILE_GAP; }

    private int switchX(int slot) { return tileX(slot) + 6; }

    private int switchY(int slot) { return tileY(slot) + 25; }

    private int expelX(int slot) { return tileX(slot) + TILE_W - 6 - EXPEL_W; }

    private int expelY(int slot) { return tileY(slot) + 23; }

    private int outsidersX() { return columnX + columnW - PanelDrawing.PAD - OUTSIDERS_W; }

    private int outsidersY() { return peopleY() + TILES_Y + tilesH() + TILE_GAP; }

    private int peopleH() { return TILES_Y + tilesH() + TILE_GAP + OUTSIDERS_H + PanelDrawing.PAD; }

    /** 第 {@code slot} 张可见小卡对应的人（按行翻过 {@link #offset} 行），没有为 -1。 */
    private int personAt(int slot) {
        int index = offset * perRow() + slot;
        return slot < rows * perRow() && index < players().size() ? index : -1;
    }
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
        PanelDrawing.card(sprites, columnX, InfoPage.CONTENT_TOP, columnW, SUMMARY_H);
        for (int i = 0; i < STAT_ICONS.length; i++) {
            PanelDrawing.stat(sprites, PanelDrawing.sprite(STAT_ICONS[i]), statX(i), statY(i), statW(i), STAT_H,
                    isOverStat(i, mouseX, mouseY));
        }
        PanelDrawing.card(sprites, columnX, peopleY(), columnW, peopleH());
        offset = PanelDrawing.clampScroll(offset, rows, totalRows());
        List<WhitelistEntry> players = players();
        for (int slot = 0; personAt(slot) >= 0; slot++) {
            WhitelistEntry person = players.get(personAt(slot));
            sprites.blit(Widgets.isOver(mouseX, mouseY, tileX(slot), tileY(slot), TILE_W, TILE_H)
                    ? PanelDrawing.STAT_HIGHLIGHTED : PanelDrawing.STAT, tileX(slot), tileY(slot), TILE_W, TILE_H);
            PanelDrawing.toggle(sprites, switchX(slot), switchY(slot), person.whitelisted(), true, mouseX, mouseY);
            if (!person.owner()) {
                PanelDrawing.button(sprites, expelX(slot), expelY(slot), EXPEL_W, PanelDrawing.GHOST_H,
                        PanelDrawing.Look.GHOST, true, mouseX, mouseY);
            }
        }
        if (totalRows() > rows) {
            int barX = columnX + columnW - PanelDrawing.PAD - 6;
            PanelDrawing.scrollbar(sprites, barX, peopleY() + TILES_Y, tilesH(), offset, rows, totalRows(), false);
        }
        PanelDrawing.button(sprites, outsidersX(), outsidersY(), OUTSIDERS_W, OUTSIDERS_H, PanelDrawing.Look.DANGER,
                true, mouseX, mouseY);
    }

    @Override
    protected void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        OverviewPart.Section data = data();
        PanelDrawing.title(graphics, font(), Component.translatable(UI + "summary"), Component.empty(), columnX,
                InfoPage.CONTENT_TOP, columnW);
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
                Component.translatable(UI + "present_count", players.size(), outsiders), columnX, peopleY(), columnW);
        Component whitelist = Component.translatable(UI + "whitelist");
        for (int slot = 0; personAt(slot) >= 0; slot++) {
            WhitelistEntry person = players.get(personAt(slot));
            graphics.drawString(font(), person.name(), tileX(slot) + 20, tileY(slot) + 7, PanelDrawing.TEXT, false);
            int right = tileX(slot) + TILE_W - 6;
            if (person.owner()) {
                PanelDrawing.right(graphics, font(), Component.translatable(UI + "owner"), right, tileY(slot) + 7,
                        PanelDrawing.ACCENT);
            } else if (outsider(person)) {
                PanelDrawing.right(graphics, font(), Component.translatable(UI + "outsider"), right, tileY(slot) + 7,
                        PanelDrawing.OUTSIDER);
            }
            graphics.drawString(font(), whitelist, switchX(slot) + PanelDrawing.SWITCH_W + 4, switchY(slot) + 1,
                    PanelDrawing.LEGEND, false);
            if (!person.owner()) {
                PanelDrawing.label(graphics, font(), Component.translatable(UI + "expel"), expelX(slot), expelY(slot),
                        EXPEL_W, PanelDrawing.GHOST_H, PanelDrawing.Look.GHOST, true);
            }
        }
        PanelDrawing.label(graphics, font(), Component.translatable(UI + "expel_outsiders"), outsidersX(),
                outsidersY(), OUTSIDERS_W, OUTSIDERS_H, PanelDrawing.Look.DANGER, true);
    }

    @Override
    protected void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        List<WhitelistEntry> players = players();
        for (int slot = 0; personAt(slot) >= 0; slot++) {
            PanelDrawing.head(graphics, players.get(personAt(slot)).id(), tileX(slot) + 6, tileY(slot) + 7);
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
        for (int slot = 0; personAt(slot) >= 0; slot++) {
            if (tip(graphics, switchX(slot), switchY(slot), PanelDrawing.SWITCH_W, PanelDrawing.SWITCH_H, mouseX,
                    mouseY, Component.translatable(TIP + "whitelist"))) {
                return;
            }
            if (!players.get(personAt(slot)).owner() && tip(graphics, expelX(slot), expelY(slot), EXPEL_W,
                    PanelDrawing.GHOST_H, mouseX, mouseY, Component.translatable(TIP + "expel"))) {
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
        for (int slot = 0; personAt(slot) >= 0; slot++) {
            WhitelistEntry person = players.get(personAt(slot));
            if (Widgets.isOver(mouseX, mouseY, switchX(slot), switchY(slot), PanelDrawing.SWITCH_W,
                    PanelDrawing.SWITCH_H)) {
                send(new OverviewPart.SetWhitelisted(person.id(), !person.whitelisted()));
                return true;
            }
            if (!person.owner() && Widgets.isOver(mouseX, mouseY, expelX(slot), expelY(slot), EXPEL_W,
                    PanelDrawing.GHOST_H)) {
                send(new OverviewPart.Expel(person.id()));
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (!Widgets.isOver(mouseX, mouseY, columnX, peopleY() + TILES_Y, columnW, tilesH())) return false;
        offset = PanelDrawing.clampScroll(offset - (int) Math.signum(delta), rows, totalRows());
        return true;
    }
}
