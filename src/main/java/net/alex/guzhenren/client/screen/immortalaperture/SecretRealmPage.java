package net.alex.guzhenren.client.screen.immortalaperture;

import java.util.List;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.immortalaperture.member.OverviewModule;
import net.alex.guzhenren.gameplay.immortalaperture.member.WhitelistEntry;
import net.alex.guzhenren.gameplay.immortalaperture.soulsundermountain.MountainModule;
import net.alex.guzhenren.gameplay.immortalaperture.soulsundermountain.SoulsunderMountainWork;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderMountain;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 秘境页（标签写秘境地名，即荡魂山）：胆石进度、「挪生物上山」「胆识蛊催动」、荡魂山白名单（按名字添加、逐行移出）。
 * <ul>
 *   <li>挪生物冷却中按钮失效，文字换成剩余秒数。</li>
 *   <li>名字输入框是 {@link EarthSpiritScreen} 持有的 {@code EditBox}（灰字补全在线玩家名、Tab 补全），本页只画它的底图
 *       并在「添加」时读走内容。</li>
 *   <li>主人那一行也能移出（主人可以选择承受震荡）。列表一屏 {@value #VISIBLE} 行，滚轮翻。</li>
 * </ul>
 */

final class SecretRealmPage extends PanelPage {

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

    SecretRealmPage(EarthSpiritScreen screen) { super(screen); }

    @Override
    protected Component name() { return screen.realmName(); }

    private MountainModule.Section data() { return section(MountainModule.INSTANCE, MountainModule.Section.EMPTY); }

    private List<WhitelistEntry> listed() { return data().whitelist(); }

    @Override
    protected void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        MountainModule.Section data = data();
        int filled = data.gutsCap() <= 0 ? 0 : (BAR_W - 2) * Math.min(data.gutsStones(), data.gutsCap())
                / data.gutsCap();
        sprites.blit(PanelDrawing.BAR_BACKGROUND, x + 8, y + 31, BAR_W, 8);
        sprites.blit(PanelDrawing.BAR_FILL, x + 9, y + 32, filled, 6, PanelDrawing.ACCENT);
        PanelDrawing.button(sprites, x + MOVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, data.moveCooldown() == 0, mouseX,
                mouseY);
        PanelDrawing.button(sprites, x + DRIVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, true, mouseX, mouseY);
        sprites.blit(screen.isNameFieldFocused() ? PanelDrawing.TEXT_FIELD_FOCUSED : PanelDrawing.TEXT_FIELD, x + 8,
                y + FIELD_Y, 128, 14);
        PanelDrawing.button(sprites, x + ADD_X, y + FIELD_Y, ADD_W, PanelDrawing.SQUARE, true, mouseX, mouseY);
        sprites.blit(PanelDrawing.WELL, x + 8, y + LIST_Y, 160, 82);
        List<WhitelistEntry> listed = listed();
        offset = PanelDrawing.clampScroll(offset, VISIBLE, listed.size());
        for (int i = 0; i < VISIBLE && offset + i < listed.size(); i++) {
            int top = y + LIST_Y + 1 + i * ROW_H;
            PanelDrawing.row(sprites, x + 9, top, 158, ROW_H, Widgets.isOver(mouseX, mouseY, x + 9, top, 158, ROW_H));
            PanelDrawing.button(sprites, x + REMOVE_X, top + 1, PanelDrawing.SQUARE, PanelDrawing.SQUARE, true, mouseX, mouseY);
        }
    }

    @Override
    protected void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        MountainModule.Section data = data();
        graphics.drawString(font(), Component.translatable(UI + "guts_stones"), x + 8, y + 21, PanelDrawing.LEGEND, false);
        PanelDrawing.right(graphics, font(), data.gutsStones() + " / " + data.gutsCap(), x + 167, y + 21, PanelDrawing.TEXT);
        boolean moveLive = data.moveCooldown() == 0;
        Component move = moveLive ? Component.translatable(UI + "move_mobs")
                : Component.translatable(UI + "cooldown", data.moveCooldown());
        PanelDrawing.label(graphics, font(), move, x + MOVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, moveLive);
        PanelDrawing.label(graphics, font(), Component.translatable(UI + "drive_guts_gu"), x + DRIVE_X, y + BUTTON_Y,
                BUTTON_W, BUTTON_H, true);
        graphics.drawString(font(), Component.translatable(UI + "exempt"), x + 8, y + 76, PanelDrawing.ACCENT, false);
        PanelDrawing.right(graphics, font(), Component.translatable(UI + "people", listed().size()), x + 167, y + 76,
                PanelDrawing.LEGEND);
        PanelDrawing.label(graphics, font(), Component.translatable(UI + "add"), x + ADD_X, y + FIELD_Y, ADD_W,
                PanelDrawing.SQUARE, true);
        List<WhitelistEntry> listed = listed();
        for (int i = 0; i < VISIBLE && offset + i < listed.size(); i++) {
            WhitelistEntry person = listed.get(offset + i);
            int top = y + LIST_Y + 1 + i * ROW_H;
            graphics.drawString(font(), person.name(), x + 25, top + 4, PanelDrawing.TEXT, false);
            if (person.owner()) {
                PanelDrawing.right(graphics, font(), Component.translatable(UI + "owner"), x + 146, top + 4,
                        PanelDrawing.ACCENT);
            }
            PanelDrawing.label(graphics, font(), REMOVE, x + REMOVE_X, top + 1, PanelDrawing.SQUARE, PanelDrawing.SQUARE, true);
        }
        graphics.drawString(font(), Component.translatable(UI + "exempt_note"), x + 8, y + 198, PanelDrawing.LEGEND,
                false);
    }

    @Override
    protected void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        List<WhitelistEntry> listed = listed();
        for (int i = 0; i < VISIBLE && offset + i < listed.size(); i++) {
            PanelDrawing.head(graphics, listed.get(offset + i).id(), x + 13, y + LIST_Y + 1 + i * ROW_H + 4);
        }
    }

    @Override
    protected void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        if (tip(graphics, x + MOVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "move_mobs", SoulsunderMountainWork.MOVE_LIMIT,
                        SoulsunderMountainWork.MOVE_COOLDOWN_TICKS / Ticks.SECOND))) {
            return;
        }
        if (tip(graphics, x + DRIVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "drive_guts_gu", data().gutsStones(),
                        SoulsunderMountain.GUTS_GU_SOUL_BONUS))) {
            return;
        }
        if (tip(graphics, x + ADD_X, y + FIELD_Y, ADD_W, PanelDrawing.SQUARE, mouseX, mouseY,
                Component.translatable(TIP + "add_whitelist"))) {
            return;
        }
        List<WhitelistEntry> listed = listed();
        for (int i = 0; i < VISIBLE && offset + i < listed.size(); i++) {
            int top = y + LIST_Y + 1 + i * ROW_H;
            if (tip(graphics, x + REMOVE_X, top + 1, PanelDrawing.SQUARE, PanelDrawing.SQUARE, mouseX, mouseY,
                    Component.translatable(TIP + "remove_whitelist"))) {
                return;
            }
        }
    }

    @Override
    protected boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        if (Widgets.isOver(mouseX, mouseY, x + MOVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H)) {
            if (data().moveCooldown() == 0) send(new MountainModule.MoveMobs());
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, x + DRIVE_X, y + BUTTON_Y, BUTTON_W, BUTTON_H)) {
            send(new MountainModule.DriveGutsGu());
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, x + ADD_X, y + FIELD_Y, ADD_W, PanelDrawing.SQUARE)) {
            screen.addTypedName();
            return true;
        }
        List<WhitelistEntry> listed = listed();
        for (int i = 0; i < VISIBLE && offset + i < listed.size(); i++) {
            int top = y + LIST_Y + 1 + i * ROW_H;
            if (Widgets.isOver(mouseX, mouseY, x + REMOVE_X, top + 1, PanelDrawing.SQUARE, PanelDrawing.SQUARE)) {
                send(new OverviewModule.SetWhitelisted(listed.get(offset + i).id(), false));
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (!Widgets.isOver(mouseX, mouseY, x + 8, y + LIST_Y, 160, 82)) return false;
        offset = PanelDrawing.clampScroll(offset - (int) Math.signum(delta), VISIBLE, listed().size());
        return true;
    }
}
