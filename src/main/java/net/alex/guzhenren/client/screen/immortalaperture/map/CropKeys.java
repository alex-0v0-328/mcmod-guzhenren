package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.function.IntPredicate;
import java.util.function.IntToLongFunction;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmRules;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 作物键网格：{@link FarmRules#crops()} 各作物的种子图标，右下角写库藏种子数（没有时灰字），每行 {@value #COLUMNS} 个，从
 * 第 {@code row} 行起画 {@code rows} 行；选定的作物用选中样式。田的对象栏的作物格用（{@link MapFieldBar}），作物再多也只是多几行。
 * <ul>
 *   <li>键下标即作物下标；{@link #MIXED} 表示所选各田作物不一，{@link #NONE} 表示光标不在键上或还没选定。</li>
 * </ul>
 */

final class CropKeys {

    static final int SIZE = 20;
    static final int PITCH = SIZE + 2;
    static final int COLUMNS = 5;
    static final int MIXED = Integer.MIN_VALUE;
    static final int NONE = Integer.MIN_VALUE + 1;

    private CropKeys() {}

    static int count() { return FarmRules.crops().size(); }

    static int rows() { return (count() + COLUMNS - 1) / COLUMNS; }

    static int width() { return COLUMNS * PITCH - 2; }

    static int height(int rows) { return rows * PITCH - 2; }

    static Component name(int crop) { return FarmRules.crops().get(crop).getName(); }

    static ItemStack seed(int crop) { return new ItemStack(FarmRules.crops().get(crop).asItem()); }

    /** 作物 {@code crop} 那个键的左上角；不在可见行里时返回 null。 */
    static int @Nullable [] corner(int x, int y, int crop, int row, int rows) {
        int at = crop / COLUMNS - row;
        if (at < 0 || at >= rows) return null;
        return new int[] {x + crop % COLUMNS * PITCH, y + at * PITCH};
    }

    /** 光标下那个键的作物下标，不在任何键上时 {@link #NONE}。 */
    static int at(int x, int y, int row, int rows, double mouseX, double mouseY) {
        for (int crop = row * COLUMNS; crop < Math.min(count(), (row + rows) * COLUMNS); crop++) {
            int[] corner = corner(x, y, crop, row, rows);
            if (corner != null && Widgets.isOver(mouseX, mouseY, corner[0], corner[1], SIZE, SIZE)) return crop;
        }
        return NONE;
    }

    static void sprites(SpriteBatch sprites, int x, int y, int row, int rows, int selected, IntPredicate live,
                        double mouseX, double mouseY) {
        for (int crop = row * COLUMNS; crop < Math.min(count(), (row + rows) * COLUMNS); crop++) {
            int[] corner = corner(x, y, crop, row, rows);
            if (corner == null) continue;
            Widgets.Look look = crop == selected ? Widgets.Look.SELECTED : Widgets.Look.PLAIN;
            Widgets.button(sprites, corner[0], corner[1], SIZE, SIZE, look, live.test(crop), mouseX, mouseY);
        }
    }

    static void texts(GuiGraphics graphics, Font font, int x, int y, int row, int rows, IntToLongFunction seeds) {
        for (int crop = row * COLUMNS; crop < Math.min(count(), (row + rows) * COLUMNS); crop++) {
            int[] corner = corner(x, y, crop, row, rows);
            if (corner != null) seed(graphics, font, crop, corner[0], corner[1], SIZE, seeds.applyAsLong(crop));
        }
    }

    /** 一枚作物键的内容：种子图标靠左上，库藏数靠右下（没有时灰字）。 */
    static void seed(GuiGraphics graphics, Font font, int crop, int left, int top, int width, long count) {
        graphics.renderItem(seed(crop), left + 2, top + 2);
        String text = PanelDrawing.count(count);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        graphics.drawString(font, text, left + width - 1 - font.width(text), top + SIZE - 9,
                count > 0 ? ModPalette.TEXT : ModPalette.TEXT_DEAD, true);
        graphics.pose().popPose();
    }
}
