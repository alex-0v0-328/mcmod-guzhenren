package net.alex.guzhenren.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 模组批量绘制 GUI 的唯一入口。
 *
 * <ul>
 *   <li>每次 {@code GuiGraphics.fill}、{@code drawString} 自带 flush，五十个填充即五十次绘制调用；{@link #draw} 让块走
 *       原版 HUD 同款 {@code drawManaged}（锁定版 NeoForge 已弃用，此处唯一压制该警告），flush 攒到块尾。</li>
 *   <li>渲染类型一变批次照样结束，故块内先画全部填充、后画全部文字；只用于文字本就压在填充上的地方。</li>
 *   <li>⚠ 禁止嵌套：内层返回即清托管标记，外层余下绘制又逐个 flush。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class GuiBatch {

    private GuiBatch() {}

    @SuppressWarnings("deprecation")
    public static void draw(GuiGraphics graphics, Runnable block) { graphics.drawManaged(block); }
}
