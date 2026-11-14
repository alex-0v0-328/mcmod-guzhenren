package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.SpriteBatch;
import net.minecraft.resources.ResourceLocation;

/**
 * 由两张 GUI 精灵图 {@code hud/<name>_bar_background}、{@code hud/<name>_bar_fill} 画成的 HUD 进度条。
 *
 * <ul>
 *   <li>边框四边各比填充大 1 像素；灰度填充按条的数据色着色。</li>
 *   <li>两图都不缩放；部分填满只显示填充最左 {@code filled} 列，不压缩整图，填充上的刻度（开窍[Awakening]条五等分、
 *       温养[Nourish]条十等分）因此始终对齐边框。</li>
 *   <li>⚠ {@link #width}、{@link #height} 是填充精灵图尺寸；{@code HudBarTexturesTest} 将 PNG 钉在生成布局上。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see HotbarHud
 * @see PlayerStatsHud
 * @see net.alex.guzhenren.client.slider.HoldSlider
 * @since 1.0.0
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
