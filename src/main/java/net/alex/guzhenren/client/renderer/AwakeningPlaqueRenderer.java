package net.alex.guzhenren.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.alex.guzhenren.client.hud.AwakeningPlaque;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitual;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ClientHooks;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 头顶资质[Aptitude]牌匾的世界内渲染器。
 * <ul>
 *   <li>开窍仪式揭晓资质后在目标玩家头顶渲染，供旁人与本人第三人称视角观看；本人第一人称改为观看屏幕牌匾（
 *       {@code AwakeningHud}），两者严格按同一 tick 展开、扫光并碎裂；可见性规则与原版名牌一致（
 *       {@link #isVisible}）：在隐藏 HUD、超出名牌距离或观察者看不见主体时隐藏。</li>
 *   <li>为无状态静态渲染器，由 {@code AwakeningEvents} 每帧在实体渲染之后调用一次；直接复用 GUI 精灵图（
 *       {@link AwakeningPlaque}）贴在朝向镜头的全亮四边形上，每像素固定占 {@link #BLOCKS_PER_PIXEL} 格（与希望蛊及
 *       粒子密度一致），底边置于头顶上方 {@link #LIFT} 格以避开名牌，四边形绕序与名牌字形在翻转坐标系中一致以确保正
 *       面留存。</li>
 *   <li>扫光像素作为白色小方块沿视线朝镜头方向前移 {@link #SHEEN_LIFT} 格以避免深度冲突；{@link #getPixelPosition}
 *       为火星粒子换算牌匾像素的世界坐标并同样略朝镜头前移。</li>
 * </ul>
 */

public final class AwakeningPlaqueRenderer {

    private AwakeningPlaqueRenderer() {}

    private static final float BLOCKS_PER_PIXEL = 1.0F / 32.0F;
    private static final double LIFT = 0.65;
    private static final double SHEEN_LIFT = 0.004;
    private static final double SPARK_LIFT = 0.0625;
    private static final int SHEEN_PIXELS = 2;
    private static final int WHITE = 0xFFFFFFFF;
    private static final MultiBufferSource.BufferSource BUFFERS =
            MultiBufferSource.immediate(new ByteBufferBuilder(1536));

    public static void render(@NotNull PoseStack pose, @NotNull Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;

        Vec3 view = camera.getPosition();
        boolean drawn = false;
        for (Player player : level.players()) {
            AwakeningRitual ritual = AwakeningRitualService.get(player);
            if (ritual == null || player == camera.getEntity() && !camera.isDetached()) continue;
            AwakeningPlaque plaque = AwakeningPlaque.of(ritual);
            int tick = (int) (level.getGameTime() - ritual.startTick());
            if (plaque == null || !AwakeningTimeline.showsPlaque(tick, plaque.height())) continue;
            Vec3 at = getAnchor(player, partialTick);
            if (!isVisible(minecraft, player, at, view)) continue;

            int height = plaque.height();
            int rows = AwakeningTimeline.getUnfoldedRows(tick, height);
            int top = (height - rows) / 2;
            int bottom = Math.min(top + rows,
                    height - AwakeningTimeline.getShatteredRows(tick) * AwakeningTimeline.SHATTER_BLOCK);
            if (bottom <= top) continue;

            TextureAtlasSprite sprite = minecraft.getGuiSprites().getSprite(plaque.sprite());
            pose.pushPose();
            pose.translate(at.x - view.x, at.y - view.y, at.z - view.z);
            pose.mulPose(camera.rotation());
            pose.scale(BLOCKS_PER_PIXEL, -BLOCKS_PER_PIXEL, BLOCKS_PER_PIXEL);
            drawPlaque(pose.last().pose(), sprite, height, top, bottom);
            pose.popPose();

            int offset = AwakeningTimeline.getSheenOffset(tick, AwakeningPlaque.WIDTH + height - 1);
            if (offset >= 0) {
                Vec3 lifted = at.subtract(view).subtract(new Vec3(camera.getLookVector()).scale(SHEEN_LIFT));
                pose.pushPose();
                pose.translate(lifted.x, lifted.y, lifted.z);
                pose.mulPose(camera.rotation());
                pose.scale(BLOCKS_PER_PIXEL, -BLOCKS_PER_PIXEL, BLOCKS_PER_PIXEL);
                drawSheen(pose.last().pose(), AwakeningPlaque.getHighlight(sprite), height, top, bottom, offset);
                pose.popPose();
            }
            drawn = true;
        }
        if (drawn) BUFFERS.endBatch();
    }

    public static boolean isVisible(@NotNull Minecraft minecraft, @NotNull Player player, @NotNull Vec3 at,
                                    @NotNull Vec3 view) {
        return !minecraft.options.hideGui && minecraft.player != null && !player.isInvisibleTo(minecraft.player)
                && ClientHooks.isNameplateInRenderDistance(player, at.distanceToSqr(view));
    }

    public static @NotNull Vec3 getAnchor(@NotNull Player player, float partialTick) {
        return player.getPosition(partialTick).add(0.0, player.getBbHeight() + LIFT, 0.0);
    }

    public static @NotNull Vec3 getPixelPosition(@NotNull Player player, @NotNull Camera camera, int x, int y,
                                                 int height) {
        Vector3f offset = new Vector3f((x - AwakeningPlaque.WIDTH / 2.0F) * BLOCKS_PER_PIXEL,
                (height - y) * BLOCKS_PER_PIXEL, 0.0F).rotate(camera.rotation());
        return getAnchor(player, 1.0F).add(offset.x, offset.y, offset.z)
                .subtract(new Vec3(camera.getLookVector()).scale(SPARK_LIFT));
    }

    private static void drawPlaque(Matrix4f matrix, TextureAtlasSprite sprite, int height, int top, int bottom) {
        VertexConsumer buffer = BUFFERS.getBuffer(RenderType.text(sprite.atlasLocation()));
        float right = AwakeningPlaque.WIDTH / 2.0F;
        float left = -right;
        float v0 = sprite.getV((float) top / height);
        float v1 = sprite.getV((float) bottom / height);
        buffer.addVertex(matrix, left, top - height, 0.0F).setColor(WHITE).setUv(sprite.getU0(), v0)
                .setLight(LightTexture.FULL_BRIGHT);
        buffer.addVertex(matrix, left, bottom - height, 0.0F).setColor(WHITE).setUv(sprite.getU0(), v1)
                .setLight(LightTexture.FULL_BRIGHT);
        buffer.addVertex(matrix, right, bottom - height, 0.0F).setColor(WHITE).setUv(sprite.getU1(), v1)
                .setLight(LightTexture.FULL_BRIGHT);
        buffer.addVertex(matrix, right, top - height, 0.0F).setColor(WHITE).setUv(sprite.getU1(), v0)
                .setLight(LightTexture.FULL_BRIGHT);
    }

    private static void drawSheen(Matrix4f matrix, boolean[] lit, int height, int top, int bottom, int offset) {
        VertexConsumer buffer = BUFFERS.getBuffer(RenderType.textBackground());
        float half = AwakeningPlaque.WIDTH / 2.0F;
        for (int y = top; y < bottom; y++) {
            int end = Math.min(AwakeningPlaque.WIDTH, offset + SHEEN_PIXELS - y);
            for (int x = Math.max(0, offset - y); x < end; x++) {
                if (!lit[y * AwakeningPlaque.WIDTH + x]) continue;
                float left = x - half;
                float up = y - height;
                buffer.addVertex(matrix, left, up, 0.0F).setColor(WHITE).setLight(LightTexture.FULL_BRIGHT);
                buffer.addVertex(matrix, left, up + 1.0F, 0.0F).setColor(WHITE).setLight(LightTexture.FULL_BRIGHT);
                buffer.addVertex(matrix, left + 1.0F, up + 1.0F, 0.0F).setColor(WHITE)
                        .setLight(LightTexture.FULL_BRIGHT);
                buffer.addVertex(matrix, left + 1.0F, up, 0.0F).setColor(WHITE).setLight(LightTexture.FULL_BRIGHT);
            }
        }
    }
}
