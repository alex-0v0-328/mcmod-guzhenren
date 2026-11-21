package net.alex.guzhenren.client.screen.blissful;

import java.util.List;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.gameplay.blissful.PanelAction;
import net.alex.guzhenren.gameplay.blissful.PanelSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 「荡魂山」页：胆石进度、「挪生物上山」「胆识蛊催动」、免震荡白名单（按名字添加、逐行移出）。
 * <ul>
 *   <li>挪生物冷却中按钮失效，文字换成剩余秒数。</li>
 *   <li>名字输入框是 {@link EarthSpiritScreen} 持有的 {@code EditBox}，本页只画它的底图并在「添加」时读走内容。</li>
 *   <li>主人那一行也能移出（主人可以选择承受震荡）。列表一屏 {@value #VISIBLE} 行，滚轮翻。</li>
 * </ul>
 */

final class MountainPage extends PanelPage {

    private static final String UI = EarthSpiritScreen.UI;
    private static final int BAR_W = 160;
    private static final int BUTTON_Y = 45;
    private static final int BUTTON_W = 78;
    private static final int BUTTON_H = 20;
    private static final int MOVE_X = 8;
    private static final int DRIVE_X = 90;
    private static final int FIELD_Y = 87;
    private static final int ADD_X = 138;
    private static final int ADD_W = 30;
    private static final int LIST_Y = 105;
    private static final int ROW_H = 16;
    private static final int VISIBLE = 5;
    private static final int REMOVE_X = 151;
    private static final Component REMOVE = Component.literal("x");
    private int offset;

    MountainPage(EarthSpiritScreen screen) { super(screen); }

    @Override
    Component name() { return Component.translatable(UI + "tab.mountain"); }

    private List<PanelSnapshot.Person> listed() { return snapshot().whitelist(); }

    @Override
    void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        PanelSnapshot snapshot = snapshot();
        int filled = snapshot.gutsCap() <= 0 ? 0 : (BAR_W - 2) * Math.min(snapshot.gutsStones(), snapshot.gutsCap())
                / snapshot.gutsCap();
        sprites.blit(PanelKit.BAR_BACKGROUND, x + 8, y + 31, BAR_W, 8);
        sprites.blit(PanelKit.BAR_FILL, x + 9, y + 32, filled, 6, PanelKit.ACCENT);
        PanelKit.button(sprites, x + MOVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, snapshot.moveCooldown() == 0, mouseX,
                mouseY);
        PanelKit.button(sprites, x + DRIVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, true, mouseX, mouseY);
        sprites.blit(screen.isNameFieldFocused() ? PanelKit.TEXT_FIELD_FOCUSED : PanelKit.TEXT_FIELD, x + 8,
                y + FIELD_Y, 128, 14);
        PanelKit.button(sprites, x + ADD_X, y + FIELD_Y, ADD_W, PanelKit.SQUARE, true, mouseX, mouseY);
        sprites.blit(PanelKit.WELL, x + 8, y + LIST_Y, 160, 82);
        List<PanelSnapshot.Person> listed = listed();
        offset = PanelKit.clampScroll(offset, VISIBLE, listed.size());
        for (int i = 0; i < VISIBLE && offset + i < listed.size(); i++) {
            int top = y + LIST_Y + 1 + i * ROW_H;
            PanelKit.row(sprites, x + 9, top, 158, ROW_H, Widgets.isOver(mouseX, mouseY, x + 9, top, 158, ROW_H));
            PanelKit.button(sprites, x + REMOVE_X, top + 1, PanelKit.SQUARE, PanelKit.SQUARE, true, mouseX, mouseY);
        }
    }

    @Override
    void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        PanelSnapshot snapshot = snapshot();
        graphics.drawString(font(), Component.translatable(UI + "guts_stones"), x + 8, y + 21, PanelKit.LEGEND, false);
        PanelKit.right(graphics, font(), snapshot.gutsStones() + " / " + snapshot.gutsCap(), x + 167, y + 21,
                PanelKit.TEXT);
        boolean moveLive = snapshot.moveCooldown() == 0;
        Component move = moveLive ? Component.translatable(UI + "move_mobs")
                : Component.translatable(UI + "cooldown", snapshot.moveCooldown());
        PanelKit.label(graphics, font(), move, x + MOVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, moveLive);
        PanelKit.label(graphics, font(), Component.translatable(UI + "drive_guts_gu"), x + DRIVE_X, y + BUTTON_Y,
                BUTTON_W, BUTTON_H, true);
        graphics.drawString(font(), Component.translatable(UI + "exempt"), x + 8, y + 76, PanelKit.ACCENT, false);
        PanelKit.right(graphics, font(), Component.translatable(UI + "people", listed().size()), x + 167, y + 76,
                PanelKit.LEGEND);
        PanelKit.label(graphics, font(), Component.translatable(UI + "add"), x + ADD_X, y + FIELD_Y, ADD_W,
                PanelKit.SQUARE, true);
        List<PanelSnapshot.Person> listed = listed();
        for (int i = 0; i < VISIBLE && offset + i < listed.size(); i++) {
            PanelSnapshot.Person person = listed.get(offset + i);
            int top = y + LIST_Y + 1 + i * ROW_H;
            graphics.drawString(font(), person.name(), x + 25, top + 4, PanelKit.TEXT, false);
            if (person.owner()) {
                PanelKit.right(graphics, font(), Component.translatable(UI + "owner"), x + 146, top + 4,
                        PanelKit.ACCENT);
            }
            PanelKit.label(graphics, font(), REMOVE, x + REMOVE_X, top + 1, PanelKit.SQUARE, PanelKit.SQUARE, true);
        }
        graphics.drawString(font(), Component.translatable(UI + "exempt_note"), x + 8, y + 198, PanelKit.LEGEND,
                false);
    }

    @Override
    void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        List<PanelSnapshot.Person> listed = listed();
        for (int i = 0; i < VISIBLE && offset + i < listed.size(); i++) {
            PanelKit.head(graphics, listed.get(offset + i).id(), x + 13, y + LIST_Y + 1 + i * ROW_H + 4);
        }
    }

    @Override
    boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        if (Widgets.isOver(mouseX, mouseY, x + MOVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H)) {
            if (snapshot().moveCooldown() == 0) screen.send(PanelAction.MOVE_MOBS, 0, "");
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, x + DRIVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H)) {
            screen.send(PanelAction.DRIVE_GUTS_GU, 0, "");
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, x + ADD_X, y + FIELD_Y, ADD_W, PanelKit.SQUARE)) {
            String name = screen.takeNameField();
            if (!name.isBlank()) screen.send(PanelAction.WHITELIST_ADD, 0, name.strip());
            return true;
        }
        List<PanelSnapshot.Person> listed = listed();
        for (int i = 0; i < VISIBLE && offset + i < listed.size(); i++) {
            int top = y + LIST_Y + 1 + i * ROW_H;
            if (Widgets.isOver(mouseX, mouseY, x + REMOVE_X, top + 1, PanelKit.SQUARE, PanelKit.SQUARE)) {
                screen.send(PanelAction.WHITELIST_SET, 0, listed.get(offset + i).id().toString());
                return true;
            }
        }
        return false;
    }

    @Override
    boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (!Widgets.isOver(mouseX, mouseY, x + 8, y + LIST_Y, 160, 82)) return false;
        offset = PanelKit.clampScroll(offset - (int) Math.signum(delta), VISIBLE, listed().size());
        return true;
    }
}
