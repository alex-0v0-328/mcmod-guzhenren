package net.alex.guzhenren.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.GuiSpriteManager;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * GUI 精灵图[Sprite]（{@code textures/gui/sprites}）批量绘制器。
 * <ul>
 *   <li>支持按 {@code .mcmeta} 拉伸、平铺或九宫格绘制（与 {@code GuiGraphics.blitSprite} 行为一致），支持从左侧裁切部分
 *       填充的进度条（{@link #blitLeft}）以及按区域切分的帧图与分层图（{@link #blitRegion}）；整批四边形写入同一顶点缓
 *       冲并在末尾一次提交绘制，同时支持颜色乘法着色以驱动灰度精灵图显示数据颜色。</li>
 *   <li>原版 {@code blitSprite} 对每块切片都会单独提交一次绘制，中心平铺 20 像素块的全屏九宫格面板每帧会产生数百次绘制
 *       调用。</li>
 *   <li>{@link #draw} 开始时会先 flush 当前图形队列，并在代码块结束时统一绘制收集到的四边形，使其遮盖此前的填充并位于后
 *       续填充与文字之下，批内后画的内容覆盖先画的内容。</li>
 *   <li>九宫格右列宽度正确读取右边框配置（原版 1.21.1 错误按左边框宽度绘制，导致左右边框不等的精灵图被截断）。</li>
 *   <li>⚠ 批处理块运行期间持续占用共享 {@link Tesselator}，块内严禁调用原版 {@code blit} 或其他触发绘制的方法。</li>
 *   <li>所有 GUI 精灵图共用同一张图集；切片几何逻辑均为纯函数，无需启动渲染线程即可执行单元测试。</li>
 * </ul>
 */

public final class SpriteBatch {

    private static final int WHITE = 0xFFFFFFFF;
    private final GuiSpriteManager sprites = Minecraft.getInstance().getGuiSprites();
    private final Matrix4f pose;
    private @Nullable BufferBuilder buffer;
    private @Nullable ResourceLocation atlas;

    private SpriteBatch(Matrix4f pose) { this.pose = pose; }

    public static void draw(GuiGraphics graphics, Consumer<SpriteBatch> block) {
        SpriteBatch batch = new SpriteBatch(graphics.pose().last().pose());
        block.accept(batch);
        batch.end(graphics);
    }

    public void blit(ResourceLocation sprite, int x, int y, int width, int height) {
        blit(sprite, x, y, width, height, WHITE);
    }

    public void blit(ResourceLocation id, int x, int y, int width, int height, int tint) {
        if (width <= 0 || height <= 0) return;

        TextureAtlasSprite sprite = sprites.getSprite(id);
        switch (sprites.getSpriteScaling(sprite)) {
            case GuiSpriteScaling.NineSlice nine -> {
                GuiSpriteScaling.NineSlice.Border border = nine.border();
                slice(width, height, nine.width(), nine.height(), border.left(), border.top(), border.right(),
                        border.bottom(), (rx, ry, rw, rh, u, v) ->
                                addRegion(sprite, x + rx, y + ry, rw, rh, u, v, nine.width(), nine.height(), tint));
            }
            case GuiSpriteScaling.Tile tile -> tile(0, 0, width, height, 0, 0, tile.width(), tile.height(),
                    (rx, ry, rw, rh, u, v) ->
                            addRegion(sprite, x + rx, y + ry, rw, rh, u, v, tile.width(), tile.height(), tint));
            default -> addQuad(sprite, x, y, width, height, 0.0F, 0.0F, 1.0F, 1.0F, tint);
        }
    }

    /** 不缩放地画 {@code texW} × {@code texH} 精灵图最左边 {@code width} 列，无视其缩放方式。 */
    public void blitLeft(ResourceLocation id, int texW, int texH, int x, int y, int width, int tint) {
        if (width <= 0) return;
        addRegion(sprites.getSprite(id), x, y, width, texH, 0, 0, texW, texH, tint);
    }

    /** 把精灵图中 ({@code u}, {@code v}) 起的一块拉伸画到目标框，无视其缩放方式。 */
    public void blitRegion(ResourceLocation id, int texW, int texH, int u, int v, int regionW, int regionH, int x, int y,
                           int width, int height, int tint) {
        if (width <= 0 || height <= 0) return;
        addQuad(sprites.getSprite(id), x, y, width, height, (float) u / texW, (float) v / texH,
                (float) (u + regionW) / texW, (float) (v + regionH) / texH, tint);
    }

    //region geometry [几何]
    static void slice(int width, int height, int texW, int texH, int left, int top, int right, int bottom,
                      Region out) {
        if (width == texW && height == texH) {
            out.add(0, 0, width, height, 0, 0);
            return;
        }
        int l = Math.min(left, width / 2);
        int r = Math.min(right, width / 2);
        int t = Math.min(top, height / 2);
        int b = Math.min(bottom, height / 2);
        int[] dstX = { 0, l, width - r };
        int[] dstW = { l, width - l - r, r };
        int[] srcU = { 0, l, texW - r };
        int[] srcW = { l, texW - l - r, r };
        int[] dstY = { 0, t, height - b };
        int[] dstH = { t, height - t - b, b };
        int[] srcV = { 0, t, texH - b };
        int[] srcH = { t, texH - t - b, b };
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                tile(dstX[col], dstY[row], dstW[col], dstH[row], srcU[col], srcV[row], srcW[col], srcH[row], out);
            }
        }
    }

    static void tile(int x, int y, int width, int height, int u, int v, int tileW, int tileH, Region out) {
        if (width <= 0 || height <= 0) return;
        if (tileW <= 0 || tileH <= 0) {
            throw new IllegalArgumentException("Tile size must be positive, got " + tileW + "x" + tileH);
        }
        for (int dx = 0; dx < width; dx += tileW) {
            for (int dy = 0; dy < height; dy += tileH) {
                out.add(x + dx, y + dy, Math.min(tileW, width - dx), Math.min(tileH, height - dy), u, v);
            }
        }
    }
    //endregion

    private void addRegion(TextureAtlasSprite sprite, int x, int y, int width, int height, int u, int v, int texW,
                           int texH, int tint) {
        addQuad(sprite, x, y, width, height, (float) u / texW, (float) v / texH, (float) (u + width) / texW,
                (float) (v + height) / texH, tint);
    }

    private void addQuad(TextureAtlasSprite sprite, int x, int y, int width, int height, float u0, float v0, float u1,
                         float v1, int tint) {
        if (buffer == null) {
            atlas = sprite.atlasLocation();
            buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        }
        float minU = sprite.getU(u0);
        float maxU = sprite.getU(u1);
        float minV = sprite.getV(v0);
        float maxV = sprite.getV(v1);
        buffer.addVertex(pose, x, y, 0.0F).setUv(minU, minV).setColor(tint);
        buffer.addVertex(pose, x, y + height, 0.0F).setUv(minU, maxV).setColor(tint);
        buffer.addVertex(pose, x + width, y + height, 0.0F).setUv(maxU, maxV).setColor(tint);
        buffer.addVertex(pose, x + width, y, 0.0F).setUv(maxU, minV).setColor(tint);
    }

    private void end(GuiGraphics graphics) {
        if (buffer == null || atlas == null) return;

        graphics.flush();
        RenderSystem.setShaderTexture(0, atlas);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    @FunctionalInterface
    interface Region {

        void add(int x, int y, int width, int height, int u, int v);
    }
}
