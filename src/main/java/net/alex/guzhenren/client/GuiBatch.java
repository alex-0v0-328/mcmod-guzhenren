package net.alex.guzhenren.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 模组批量绘制 GUI 的唯一入口。
 * <ul>
 *   <li>每次调用 {@code GuiGraphics.fill} 或 {@code drawString} 都会自行 flush，五十次填充就会产生五十次绘制调用；
 *       {@link #draw} 让代码块走原版 HUD 同款的 {@code drawManaged}（锁定版 NeoForge 已将其弃用，此处是唯一压制该警告
 *       的位置），将 flush 统一攒到代码块末尾执行。</li>
 *   <li>一旦渲染类型切换，当前批次仍会被迫结束，因此代码块内须先绘制全部填充、再绘制全部文字；仅适用于文字原本就叠在填
 *       充之上的场景。</li>
 *   <li>⚠ 严禁嵌套调用：内层 {@link #draw} 返回时会清空托管标记，导致外层余下的绘制重新退化为逐个 flush。</li>
 * </ul>
 */

public final class GuiBatch {

    private GuiBatch() {}

    @SuppressWarnings("deprecation")
    public static void draw(GuiGraphics graphics, Runnable block) { graphics.drawManaged(block); }
}
