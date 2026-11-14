package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 快捷栏上方各进度条的共用布局：几何尺寸、随护甲与坐骑移动的基线、每条复用的精灵图绘制与标题。
 *
 * <ul>
 *   <li>{@link net.alex.guzhenren.client.hud.ChargeHud}、{@link net.alex.guzhenren.client.hud.NourishHud}、
 *       {@link net.alex.guzhenren.client.hud.ShieldHud} 的包内抽象基类。</li>
 *   <li>{@code barTop} 跟随原版手持物品名基线，护甲与坐骑血量把条往上推而非遮住。</li>
 *   <li>每条是一个 {@link HudBar}，在同一 {@link SpriteBatch} 里画完再画标题；蓄力与护盾用 {@link #HOTBAR}，
 *       开窍[Awakening]、温养[Nourish]条用各自的。</li>
 *   <li>各条 {@link #render} 画本帧，由加载器 HUD 层（{@code ClientEvents}）调用。</li>
 *   <li>多条可同时出现（护盾下温养时持蓄力蛊）：{@link #getStackShift} 把后出现的条抬过下方标题，叠放不重叠。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.hud.ChargeHud
 * @see net.alex.guzhenren.client.hud.NourishHud
 * @since 1.0.0
 */

abstract class HotbarHud {

    static final int BAR_WIDTH = 182;
    static final int BAR_HEIGHT = 5;
    static final int TEXT_GAP = 3;
    static final int NAME_GAP = 4;
    static final int MIN_SHIFT = 59;
    static final int CREATIVE_LIFT = 14;
    static final int STACK_GAP = 3;
    static final HudBar HOTBAR = HudBar.of("hotbar", BAR_WIDTH, BAR_HEIGHT);

    public abstract void render(@NotNull GuiGraphics graphics);

    static int getBarLeft(Minecraft minecraft) { return (minecraft.getWindow().getGuiScaledWidth() - BAR_WIDTH) / 2; }

    static int getBarTop(Minecraft minecraft) {
        Gui gui = minecraft.gui;
        int shift = Math.max(Math.max(gui.leftHeight, gui.rightHeight), MIN_SHIFT);
        int baseline = minecraft.getWindow().getGuiScaledHeight() - shift;
        if (minecraft.gameMode != null && !minecraft.gameMode.canHurtPlayer()) baseline += CREATIVE_LIFT;
        return baseline - NAME_GAP - BAR_HEIGHT;
    }

    static int getStackShift(Minecraft minecraft) {
        return minecraft.font.lineHeight + TEXT_GAP + BAR_HEIGHT + STACK_GAP;
    }

    static void draw(GuiGraphics g, Minecraft mc, HudBar bar, int x, int y, float fraction, int fill,
                     @Nullable Component label) {
        int filled = Math.round(BAR_WIDTH * Mth.clamp(fraction, 0.0F, 1.0F));
        SpriteBatch.draw(g, sprites -> bar.draw(sprites, x, y, filled, fill));
        if (label != null) {
            g.drawString(mc.font, label, x + (BAR_WIDTH - mc.font.width(label)) / 2, y - TEXT_GAP - mc.font.lineHeight,
                    ModPalette.TEXT, true);
        }
    }
}
