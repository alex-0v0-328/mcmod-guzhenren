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
 * Draws GUI sprites ({@code textures/gui/sprites}) the way {@code GuiGraphics.blitSprite} does -- stretched, tiled or
 * nine-sliced as the sprite's {@code .mcmeta} says, or cropped from the left for a part-full bar ({@link #blitLeft}) --
 * but every quad of a block goes into one buffer and one draw call.
 *
 * <p>{@code blitSprite} draws each tile on its own, so a full-screen nine-slice panel with a 20-pixel center costs a
 * few hundred draw calls a frame. {@link #draw} collects a block's quads and draws them when the block ends, after
 * flushing what the graphics already queued: a block lands over earlier fills and under later fills and text, and
 * inside a block a later sprite covers an earlier one. A tint multiplies the sprite's pixels, which is how a
 * grayscale sprite takes a data color.
 *
 * <p>The nine-slice's right column is as wide as the sprite's right border; vanilla 1.21.1 draws it at the left
 * border's width, which cuts a sprite whose two side borders differ.
 *
 * <p>⚠ A block holds the shared {@link Tesselator} open, so nothing inside it may call a vanilla {@code blit}. Every
 * GUI sprite sits on the one GUI atlas.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuiBatch
 * @since 1.0.0
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

    /** Draws a {@code texW} x {@code texH} sprite's leftmost {@code width} columns unscaled, whatever its scaling. */
    public void blitLeft(ResourceLocation id, int texW, int texH, int x, int y, int width, int tint) {
        if (width <= 0) return;
        addRegion(sprites.getSprite(id), x, y, width, texH, 0, 0, texW, texH, tint);
    }

    //region geometry -- pure, so the slicing is testable without a render thread
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
