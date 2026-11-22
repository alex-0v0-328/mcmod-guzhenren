package net.alex.guzhenren.client.screen.blissful;

import java.util.List;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.blissful.member.OverviewModule;
import net.alex.guzhenren.gameplay.blissful.member.Person;
import net.alex.guzhenren.gameplay.blissful.mountain.MountainModule;
import net.alex.guzhenren.gameplay.blissful.mountain.MountainWork;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderMountain;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 「荡魂山」页：胆石进度、「挪生物上山」「胆识蛊催动」、荡魂山白名单（按名字添加、逐行移出）。
 * <ul>
 *   <li>挪生物冷却中按钮失效，文字换成剩余秒数。</li>
 *   <li>名字输入框是 {@link EarthSpiritScreen} 持有的 {@code EditBox}（灰字补全在线玩家名、Tab 补全），本页只画它的底图并
 *       在「添加」时读走内容。</li>
 *   <li>主人那一行也能移出（主人可以选择承受震荡）。列表一屏 {@value #VISIBLE} 行，滚轮翻。</li>
 * </ul>
 */

final class MountainPage extends PanelPage {

    private static final String UI = EarthSpiritScreen.UI;
    private static final String TIP = EarthSpiritScreen.TIP;
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

    private MountainModule.Section data() { return section(MountainModule.Section.EMPTY); }

    private List<Person> listed() { return data().whitelist(); }

    @Override
    void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        MountainModule.Section data = data();
        int filled = data.gutsCap() <= 0 ? 0 : (BAR_W - 2) * Math.min(data.gutsStones(), data.gutsCap())
                / data.gutsCap();
        sprites.blit(PanelKit.BAR_BACKGROUND, x + 8, y + 31, BAR_W, 8);
        sprites.blit(PanelKit.BAR_FILL, x + 9, y + 32, filled, 6, PanelKit.ACCENT);
        PanelKit.button(sprites, x + MOVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, data.moveCooldown() == 0, mouseX,
                mouseY);
        PanelKit.button(sprites, x + DRIVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, true, mouseX, mouseY);
        sprites.blit(screen.isNameFieldFocused() ? PanelKit.TEXT_FIELD_FOCUSED : PanelKit.TEXT_FIELD, x + 8,
                y + FIELD_Y, 128, 14);
        PanelKit.button(sprites, x + ADD_X, y + FIELD_Y, ADD_W, PanelKit.SQUARE, true, mouseX, mouseY);
        sprites.blit(PanelKit.WELL, x + 8, y + LIST_Y, 160, 82);
        List<Person> listed = listed();
        offset = PanelKit.clampScroll(offset, VISIBLE, listed.size());
        for (int i = 0; i < VISIBLE && offset + i < listed.size(); i++) {
            int top = y + LIST_Y + 1 + i * ROW_H;
            PanelKit.row(sprites, x + 9, top, 158, ROW_H, Widgets.isOver(mouseX, mouseY, x + 9, top, 158, ROW_H));
            PanelKit.button(sprites, x + REMOVE_X, top + 1, PanelKit.SQUARE, PanelKit.SQUARE, true, mouseX, mouseY);
        }
    }

    @Override
    void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        MountainModule.Section data = data();
        graphics.drawString(font(), Component.translatable(UI + "guts_stones"), x + 8, y + 21, PanelKit.LEGEND, false);
        PanelKit.right(graphics, font(), data.gutsStones() + " / " + data.gutsCap(), x + 167, y + 21, PanelKit.TEXT);
        boolean moveLive = data.moveCooldown() == 0;
        Component move = moveLive ? Component.translatable(UI + "move_mobs")
                : Component.translatable(UI + "cooldown", data.moveCooldown());
        PanelKit.label(graphics, font(), move, x + MOVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, moveLive);
        PanelKit.label(graphics, font(), Component.translatable(UI + "drive_guts_gu"), x + DRIVE_X, y + BUTTON_Y,
                BUTTON_W, BUTTON_H, true);
        graphics.drawString(font(), Component.translatable(UI + "exempt"), x + 8, y + 76, PanelKit.ACCENT, false);
        PanelKit.right(graphics, font(), Component.translatable(UI + "people", listed().size()), x + 167, y + 76,
                PanelKit.LEGEND);
        PanelKit.label(graphics, font(), Component.translatable(UI + "add"), x + ADD_X, y + FIELD_Y, ADD_W,
                PanelKit.SQUARE, true);
        List<Person> listed = listed();
        for (int i = 0; i < VISIBLE && offset + i < listed.size(); i++) {
            Person person = listed.get(offset + i);
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
        List<Person> listed = listed();
        for (int i = 0; i < VISIBLE && offset + i < listed.size(); i++) {
            PanelKit.head(graphics, listed.get(offset + i).id(), x + 13, y + LIST_Y + 1 + i * ROW_H + 4);
        }
    }

    @Override
    void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        if (tip(graphics, x + MOVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "move_mobs", MountainWork.MOVE_LIMIT,
                        MountainWork.MOVE_COOLDOWN_TICKS / Ticks.SECOND))) {
            return;
        }
        if (tip(graphics, x + DRIVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "drive_guts_gu", data().gutsStones(),
                        SoulsunderMountain.GUTS_GU_SOUL_BONUS))) {
            return;
        }
        if (tip(graphics, x + ADD_X, y + FIELD_Y, ADD_W, PanelKit.SQUARE, mouseX, mouseY,
                Component.translatable(TIP + "add_whitelist"))) {
            return;
        }
        List<Person> listed = listed();
        for (int i = 0; i < VISIBLE && offset + i < listed.size(); i++) {
            int top = y + LIST_Y + 1 + i * ROW_H;
            if (tip(graphics, x + REMOVE_X, top + 1, PanelKit.SQUARE, PanelKit.SQUARE, mouseX, mouseY,
                    Component.translatable(TIP + "remove_whitelist"))) {
                return;
            }
        }
    }

    @Override
    boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        if (Widgets.isOver(mouseX, mouseY, x + MOVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H)) {
            if (data().moveCooldown() == 0) send(new MountainModule.MoveMobs());
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, x + DRIVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H)) {
            send(new MountainModule.DriveGutsGu());
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, x + ADD_X, y + FIELD_Y, ADD_W, PanelKit.SQUARE)) {
            screen.addTypedName();
            return true;
        }
        List<Person> listed = listed();
        for (int i = 0; i < VISIBLE && offset + i < listed.size(); i++) {
            int top = y + LIST_Y + 1 + i * ROW_H;
            if (Widgets.isOver(mouseX, mouseY, x + REMOVE_X, top + 1, PanelKit.SQUARE, PanelKit.SQUARE)) {
                send(new OverviewModule.SetWhitelisted(listed.get(offset + i).id(), false));
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
