package net.alex.guzhenren.client.screen.blissful;

import java.util.List;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.gameplay.blissful.PanelAction;
import net.alex.guzhenren.gameplay.blissful.PanelSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

/**
 * 「养殖」页：每种动物一行（刷怪蛋图标、名字、数量、托管开关、保持数 −/+），底部「喂养繁殖」「宰杀收取」。
 * <ul>
 *   <li>托管按种类开关；关着时 −/+ 失效、保持数画失效色，但保持数照存，一键宰杀也照它算。</li>
 *   <li>−/+ 每次改 1，按住 Shift 改 {@value #SHIFT_STEP}；保持数不小于 0。</li>
 *   <li>一屏 {@value #VISIBLE} 行，滚轮翻，右侧滚动条。没有刷怪蛋的种类不画图标。</li>
 * </ul>
 */

final class HerdPage extends PanelPage {

    private static final String UI = EarthSpiritScreen.UI;
    private static final int SHIFT_STEP = 10;
    private static final int LIST_Y = 31;
    private static final int ROW_H = 20;
    private static final int ROW_W = 150;
    private static final int VISIBLE = 7;
    private static final int SWITCH_X = 92;
    private static final int MINUS_X = 114;
    private static final int PLUS_X = 144;
    private static final int BUTTON_Y = 192;
    private static final int BUTTON_W = 78;
    private static final int BUTTON_H = 20;
    private static final int BREED_X = 8;
    private static final int SLAUGHTER_X = 90;
    private static final Component MINUS = Component.literal("-");
    private static final Component PLUS = Component.literal("+");
    private int offset;

    HerdPage(EarthSpiritScreen screen) { super(screen); }

    @Override
    Component name() { return Component.translatable(UI + "tab.husbandry"); }

    private List<PanelSnapshot.Herd> herds() { return snapshot().herds(); }

    @Override
    void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        sprites.blit(PanelKit.WELL, x + 8, y + LIST_Y, 160, 142);
        List<PanelSnapshot.Herd> herds = herds();
        offset = PanelKit.clampScroll(offset, VISIBLE, herds.size());
        for (int i = 0; i < VISIBLE && offset + i < herds.size(); i++) {
            PanelSnapshot.Herd herd = herds.get(offset + i);
            int top = y + LIST_Y + 1 + i * ROW_H;
            PanelKit.row(sprites, x + 9, top, ROW_W, ROW_H, Widgets.isOver(mouseX, mouseY, x + 9, top, ROW_W, ROW_H));
            PanelKit.toggle(sprites, x + SWITCH_X, top + 5, herd.hosted(), true, mouseX, mouseY);
            PanelKit.button(sprites, x + MINUS_X, top + 3, PanelKit.SQUARE, PanelKit.SQUARE, herd.hosted(), mouseX,
                    mouseY);
            PanelKit.button(sprites, x + PLUS_X, top + 3, PanelKit.SQUARE, PanelKit.SQUARE, herd.hosted(), mouseX,
                    mouseY);
        }
        boolean over = Widgets.isOver(mouseX, mouseY, x + 160, y + 32, 6, 140);
        PanelKit.scrollbar(sprites, x + 160, y + 32, 140, offset, VISIBLE, herds.size(), over);
        PanelKit.button(sprites, x + BREED_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, true, mouseX, mouseY);
        PanelKit.button(sprites, x + SLAUGHTER_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, true, mouseX, mouseY);
    }

    @Override
    void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        graphics.drawString(font(), Component.translatable(UI + "animal"), x + 12, y + 21, PanelKit.LEGEND, false);
        PanelKit.right(graphics, font(), Component.translatable(UI + "count"), x + 84, y + 21, PanelKit.LEGEND);
        graphics.drawString(font(), Component.translatable(UI + "hosting"), x + 92, y + 21, PanelKit.LEGEND, false);
        PanelKit.centered(graphics, font(), Component.translatable(UI + "keep"), x + 114, x + 157, y + 21,
                PanelKit.LEGEND);
        List<PanelSnapshot.Herd> herds = herds();
        for (int i = 0; i < VISIBLE && offset + i < herds.size(); i++) {
            PanelSnapshot.Herd herd = herds.get(offset + i);
            int top = y + LIST_Y + 1 + i * ROW_H;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(herd.type());
            graphics.drawString(font(), type.getDescription(), x + 30, top + 6, PanelKit.TEXT, false);
            PanelKit.right(graphics, font(), Integer.toString(herd.count()), x + 84, top + 6, PanelKit.TEXT);
            PanelKit.label(graphics, font(), MINUS, x + MINUS_X, top + 3, PanelKit.SQUARE, PanelKit.SQUARE,
                    herd.hosted());
            PanelKit.label(graphics, font(), PLUS, x + PLUS_X, top + 3, PanelKit.SQUARE, PanelKit.SQUARE,
                    herd.hosted());
            PanelKit.centered(graphics, font(), Component.literal(Integer.toString(herd.keep())), x + 128, x + 143,
                    top + 6, herd.hosted() ? PanelKit.TEXT : PanelKit.DEAD);
        }
        graphics.drawString(font(), Component.translatable(UI + "herd_note"), x + 8, y + 178, PanelKit.LEGEND, false);
        PanelKit.label(graphics, font(), Component.translatable(UI + "breed"), x + BREED_X, y + BUTTON_Y, BUTTON_W,
                BUTTON_H, true);
        PanelKit.label(graphics, font(), Component.translatable(UI + "slaughter"), x + SLAUGHTER_X, y + BUTTON_Y,
                BUTTON_W, BUTTON_H, true);
    }

    @Override
    void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        List<PanelSnapshot.Herd> herds = herds();
        for (int i = 0; i < VISIBLE && offset + i < herds.size(); i++) {
            SpawnEggItem egg = SpawnEggItem.byId(BuiltInRegistries.ENTITY_TYPE.get(herds.get(offset + i).type()));
            if (egg != null) graphics.renderItem(new ItemStack(egg), x + 11, y + LIST_Y + 1 + i * ROW_H + 2);
        }
    }

    @Override
    boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        if (Widgets.isOver(mouseX, mouseY, x + BREED_X, y + BUTTON_Y, BUTTON_W, BUTTON_H)) {
            screen.send(PanelAction.BREED, 0, "");
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, x + SLAUGHTER_X, y + BUTTON_Y, BUTTON_W, BUTTON_H)) {
            screen.send(PanelAction.SLAUGHTER, 0, "");
            return true;
        }
        List<PanelSnapshot.Herd> herds = herds();
        int step = shift ? SHIFT_STEP : 1;
        for (int i = 0; i < VISIBLE && offset + i < herds.size(); i++) {
            PanelSnapshot.Herd herd = herds.get(offset + i);
            int top = y + LIST_Y + 1 + i * ROW_H;
            String type = herd.type().toString();
            if (Widgets.isOver(mouseX, mouseY, x + SWITCH_X, top + 5, PanelKit.SWITCH_W, PanelKit.SWITCH_H)) {
                screen.send(PanelAction.HERD_HOSTING, herd.hosted() ? 0 : 1, type);
                return true;
            }
            if (!herd.hosted()) continue;
            if (Widgets.isOver(mouseX, mouseY, x + MINUS_X, top + 3, PanelKit.SQUARE, PanelKit.SQUARE)) {
                screen.send(PanelAction.HERD_KEEP, Math.max(0, herd.keep() - step), type);
                return true;
            }
            if (Widgets.isOver(mouseX, mouseY, x + PLUS_X, top + 3, PanelKit.SQUARE, PanelKit.SQUARE)) {
                screen.send(PanelAction.HERD_KEEP, herd.keep() + step, type);
                return true;
            }
        }
        return false;
    }

    @Override
    boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (!Widgets.isOver(mouseX, mouseY, x + 8, y + LIST_Y, 160, 142)) return false;
        offset = PanelKit.clampScroll(offset - (int) Math.signum(delta), VISIBLE, herds().size());
        return true;
    }
}
