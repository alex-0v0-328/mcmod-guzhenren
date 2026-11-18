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
 * 快捷栏上方各进度条的共用布局基类：封装几何尺寸、随护甲与坐骑自适应抬升的基线、精灵图条绘制与居中标题渲染。
 * <ul>
 *   <li>为 {@link ChargeHud}、{@link NourishHud} 与 {@link ShieldHud} 共用的包内抽象基类。</li>
 *   <li>{@code barTop} 跟随原版手持物品名称基线动态计算，确保护甲值与坐骑血量会把进度条向上推开而非互相遮挡。</li>
 *   <li>每条对应一个 {@link HudBar}，在同一个 {@link SpriteBatch} 中完成条体绘制后再写标题文字；蓄力与护盾条使用
 *       {@link #HOTBAR} 样式，开窍[Awakening]与温养[Nourish]条使用各自专用样式。</li>
 *   <li>各子类通过 {@link #render} 绘制当前帧，并由客户端 HUD 图层注册（{@code ClientEvents}）驱动调用。</li>
 *   <li>当多个进度条同时出现（例如护盾生效、温养中且手持蓄力蛊）时，{@link #getStackShift} 将后绘制的条抬高至下方标题
 *       之上，使各条整齐叠放而不重叠。</li>
 * </ul>
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
