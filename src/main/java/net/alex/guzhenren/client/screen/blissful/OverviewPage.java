package net.alex.guzhenren.client.screen.blissful;

import java.util.List;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.gameplay.blissful.PanelAction;
import net.alex.guzhenren.gameplay.blissful.PanelSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 「总览」页：亩数、胆石、动物、作物四张数据卡，在场玩家每人一行（白名单开关、「驱逐」），底部「驱逐外人」。
 * <ul>
 *   <li>主人那一行不画「驱逐」，改写「主人」；白名单开关照常可点（主人可以选择承受震荡）。</li>
 *   <li>白名单与「荡魂山」页的荡魂山白名单是同一份。</li>
 *   <li>列表一屏 {@value #VISIBLE} 行，滚轮翻。</li>
 * </ul>
 */

final class OverviewPage extends PanelPage {

    private static final String UI = EarthSpiritScreen.UI;
    private static final int[][] CARDS = {{8, 19}, {89, 19}, {8, 49}, {89, 49}};
    private static final String[] CARD_ICONS = {"icon_territory", "icon_mountain", "icon_husbandry", "icon_farming"};
    private static final String[] CARD_LABELS = {"acres", "guts_stones", "animals", "crops"};
    private static final int CARD_W = 79;
    private static final int CARD_H = 28;
    private static final int LIST_Y = 94;
    private static final int ROW_H = 16;
    private static final int VISIBLE = 5;
    private static final int SWITCH_X = 112;
    private static final int EXPEL_X = 134;
    private static final int EXPEL_W = 30;
    private static final int OUTSIDERS_X = 98;
    private static final int OUTSIDERS_Y = 192;
    private static final int OUTSIDERS_W = 70;
    private static final int OUTSIDERS_H = 20;
    private int offset;

    OverviewPage(EarthSpiritScreen screen) { super(screen); }

    @Override
    Component name() { return Component.translatable(UI + "tab.overview"); }

    private List<PanelSnapshot.Person> players() { return snapshot().players(); }

    @Override
    void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        for (int i = 0; i < CARDS.length; i++) {
            sprites.blit(PanelKit.WELL, x + CARDS[i][0], y + CARDS[i][1], CARD_W, CARD_H);
            sprites.blit(PanelKit.sprite(CARD_ICONS[i]), x + CARDS[i][0] + 5, y + CARDS[i][1] + 6, 16, 16);
        }
        sprites.blit(PanelKit.WELL, x + 8, y + LIST_Y, 160, 82);
        List<PanelSnapshot.Person> players = players();
        offset = PanelKit.clampScroll(offset, VISIBLE, players.size());
        for (int i = 0; i < VISIBLE && offset + i < players.size(); i++) {
            PanelSnapshot.Person person = players.get(offset + i);
            int top = y + LIST_Y + 1 + i * ROW_H;
            PanelKit.row(sprites, x + 9, top, 158, ROW_H, Widgets.isOver(mouseX, mouseY, x + 9, top, 158, ROW_H));
            PanelKit.toggle(sprites, x + SWITCH_X, top + 3, person.whitelisted(), true, mouseX, mouseY);
            if (!person.owner()) PanelKit.button(sprites, x + EXPEL_X, top + 1, EXPEL_W, PanelKit.SQUARE, true,
                    mouseX, mouseY);
        }
        PanelKit.button(sprites, x + OUTSIDERS_X, y + OUTSIDERS_Y, OUTSIDERS_W, OUTSIDERS_H, true, mouseX, mouseY);
    }

    @Override
    void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        PanelSnapshot snapshot = snapshot();
        int crops = snapshot.cropsMature() + snapshot.cropsGrowing();
        String[] values = {Long.toString(snapshot.acres()), snapshot.gutsStones() + " / " + snapshot.gutsCap(),
                Integer.toString(snapshot.animals()), Integer.toString(crops)};
        for (int i = 0; i < CARDS.length; i++) {
            int left = x + CARDS[i][0] + 26;
            graphics.drawString(font(), Component.translatable(UI + CARD_LABELS[i]), left, y + CARDS[i][1] + 5,
                    PanelKit.LEGEND, false);
            graphics.drawString(font(), values[i], left, y + CARDS[i][1] + 15, PanelKit.TEXT, false);
        }
        Component present = Component.translatable(UI + "present");
        graphics.drawString(font(), present, x + 8, y + 84, PanelKit.ACCENT, false);
        graphics.drawString(font(), Component.translatable(UI + "people", players().size()),
                x + 8 + font().width(present) + 5, y + 84, PanelKit.LEGEND, false);
        PanelKit.centered(graphics, font(), Component.translatable(UI + "whitelist"), x + 107, x + 134, y + 84,
                PanelKit.LEGEND);

        List<PanelSnapshot.Person> players = players();
        for (int i = 0; i < VISIBLE && offset + i < players.size(); i++) {
            PanelSnapshot.Person person = players.get(offset + i);
            int top = y + LIST_Y + 1 + i * ROW_H;
            graphics.drawString(font(), person.name(), x + 25, top + 4, PanelKit.TEXT, false);
            if (person.owner()) {
                PanelKit.right(graphics, font(), Component.translatable(UI + "owner"), x + 162, top + 4,
                        PanelKit.ACCENT);
            } else {
                PanelKit.label(graphics, font(), Component.translatable(UI + "expel"), x + EXPEL_X, top + 1,
                        EXPEL_W, PanelKit.SQUARE, true);
            }
        }
        PanelKit.label(graphics, font(), Component.translatable(UI + "expel_outsiders"), x + OUTSIDERS_X,
                y + OUTSIDERS_Y, OUTSIDERS_W, OUTSIDERS_H, true);
    }

    @Override
    void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        List<PanelSnapshot.Person> players = players();
        for (int i = 0; i < VISIBLE && offset + i < players.size(); i++) {
            PanelKit.head(graphics, players.get(offset + i).id(), x + 13, y + LIST_Y + 1 + i * ROW_H + 4);
        }
    }

    @Override
    boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        if (Widgets.isOver(mouseX, mouseY, x + OUTSIDERS_X, y + OUTSIDERS_Y, OUTSIDERS_W, OUTSIDERS_H)) {
            screen.send(PanelAction.EXPEL_OUTSIDERS, 0, "");
            return true;
        }
        List<PanelSnapshot.Person> players = players();
        for (int i = 0; i < VISIBLE && offset + i < players.size(); i++) {
            PanelSnapshot.Person person = players.get(offset + i);
            int top = y + LIST_Y + 1 + i * ROW_H;
            if (Widgets.isOver(mouseX, mouseY, x + SWITCH_X, top + 3, PanelKit.SWITCH_W, PanelKit.SWITCH_H)) {
                screen.send(PanelAction.WHITELIST_SET, person.whitelisted() ? 0 : 1, person.id().toString());
                return true;
            }
            if (!person.owner() && Widgets.isOver(mouseX, mouseY, x + EXPEL_X, top + 1, EXPEL_W, PanelKit.SQUARE)) {
                screen.send(PanelAction.EXPEL, 0, person.id().toString());
                return true;
            }
        }
        return false;
    }

    @Override
    boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (!Widgets.isOver(mouseX, mouseY, x + 8, y + LIST_Y, 160, 82)) return false;
        offset = PanelKit.clampScroll(offset - (int) Math.signum(delta), VISIBLE, players().size());
        return true;
    }
}
