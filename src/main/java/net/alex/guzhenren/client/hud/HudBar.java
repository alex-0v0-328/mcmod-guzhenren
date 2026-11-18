package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.SpriteBatch;
import net.minecraft.resources.ResourceLocation;

/**
 * 基于两张 GUI 精灵图 {@code hud/<name>_bar_background} 与 {@code hud/<name>_bar_fill} 构成的 HUD 进度条。
 * <ul>
 *   <li>背景边框四边各比填充区域宽 1 像素；灰度填充贴图按进度条的数据色进行颜色乘法着色。</li>
 *   <li>背景与填充均不做拉伸缩放；部分填充时仅裁切显示填充贴图最左侧 {@code filled} 列而不压缩整图，因此填充图上的刻度
 *       线（如开窍[Awakening]条五等分、温养[Nourish]条十等分）始终与边框保持对齐。</li>
 *   <li>⚠ {@link #width} 与 {@link #height} 表示填充精灵图的像素尺寸；{@code HudBarTexturesTest} 会将 PNG 尺寸锁定
 *       在生成布局规格上。</li>
 * </ul>
 */

public record HudBar(ResourceLocation background, ResourceLocation fill, int width, int height) {

    public static HudBar of(String name, int width, int height) {
        return new HudBar(Guzhenren.id("hud/" + name + "_bar_background"), Guzhenren.id("hud/" + name + "_bar_fill"),
                width, height);
    }

    /** 画条，填充左上角位于（{@code x}，{@code y}）；{@code filled} 取 0 到 {@code width}。 */
    public void draw(SpriteBatch sprites, int x, int y, int filled, int tint) {
        sprites.blit(background, x - 1, y - 1, width + 2, height + 2);
        sprites.blitLeft(fill, width, height, x, y, Math.min(filled, width), tint);
    }
}
