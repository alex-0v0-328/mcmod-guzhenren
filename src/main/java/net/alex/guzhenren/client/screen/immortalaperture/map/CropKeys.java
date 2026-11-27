package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.function.IntToLongFunction;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmRules;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 一排作物图标键：「自动」+ {@link FarmRules#crops()} 各作物的种子图标，右下角写库藏种子数（没有时灰字），当前作物用选中
 * 样式；操作栏「种植」与开垦工具的新田作物共用。
 * <ul>
 *   <li>键下标 0 = 自动（{@link FarmState#AUTO_CROP}），其余依次为作物下标；{@link #MIXED} 表示所选各田作物不一，不标选中。</li>
 * </ul>
 */

final class CropKeys {

    static final int SIZE = 20;
    static final int PITCH = SIZE + 2;
    static final int MIXED = Integer.MIN_VALUE;
    static final int NONE = Integer.MIN_VALUE + 1;

    private CropKeys() {}

    static int slots() { return FarmRules.crops().size() + 1; }

    static int width() { return slots() * PITCH - 2; }

    static int cropOf(int slot) { return slot == 0 ? FarmState.AUTO_CROP : slot - 1; }

    /** 作物 {@code crop} 那个键的左边。 */
    static int left(int x, int crop) { return x + (crop == FarmState.AUTO_CROP ? 0 : crop + 1) * PITCH; }

    static Component name(int crop) {
        return crop == FarmState.AUTO_CROP ? Component.translatable(EarthSpiritScreen.UI + "crop_auto")
                : FarmRules.crops().get(crop).getName();
    }

    private static String shortCount(long value) {
        if (value < 1_000L) return Long.toString(value);
        if (value < 1_000_000L) return value / 1_000L + "k";
        return value / 1_000_000L + "m";
    }

    /** 光标下那个键的作物下标，不在任何键上时 {@link #NONE}。 */
    static int at(int x, int y, double mouseX, double mouseY) {
        for (int slot = 0; slot < slots(); slot++) {
            if (Widgets.isOver(mouseX, mouseY, x + slot * PITCH, y, SIZE, SIZE)) return cropOf(slot);
        }
        return NONE;
    }

    static void sprites(SpriteBatch sprites, int x, int y, int selected, boolean live, double mouseX, double mouseY) {
        for (int slot = 0; slot < slots(); slot++) {
            PanelDrawing.Look look = cropOf(slot) == selected ? PanelDrawing.Look.SELECTED : PanelDrawing.Look.PLAIN;
            PanelDrawing.button(sprites, x + slot * PITCH, y, SIZE, SIZE, look, live, mouseX, mouseY);
        }
    }

    static void texts(GuiGraphics graphics, Font font, int x, int y, int selected, boolean live,
                      IntToLongFunction seeds) {
        for (int slot = 0; slot < slots(); slot++) {
            int crop = cropOf(slot);
            int left = x + slot * PITCH;
            PanelDrawing.Look look = crop == selected ? PanelDrawing.Look.SELECTED : PanelDrawing.Look.PLAIN;
            if (crop == FarmState.AUTO_CROP) {
                PanelDrawing.label(graphics, font, name(crop), left, y, SIZE, SIZE, look, live);
                continue;
            }
            graphics.renderItem(new ItemStack(FarmRules.crops().get(crop).asItem()), left + 2, y + 2);
            long count = seeds.applyAsLong(crop);
            String text = shortCount(count);
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 200.0F);
            graphics.drawString(font, text, left + SIZE - 1 - font.width(text), y + SIZE - 9,
                    count > 0 ? PanelDrawing.TEXT : PanelDrawing.DEAD, true);
            graphics.pose().popPose();
        }
    }
}
