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
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ClientHooks;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
 * 头顶资质[Aptitude]牌匾的世界渲染。
 *
 * <ul>
 *   <li>开窍[Awakening]揭晓资质后显示在该玩家头顶，给旁人与本人第三人称镜头看；本人第一人称改看屏幕牌匾
 *       （{@code AwakeningHud}），两者同步淡出；隐藏规则同名牌：隐藏 HUD、超出名牌距离、观察者看不见其主人时。</li>
 *   <li>静态渲染器，{@code AwakeningEvents} 每帧在实体后调用一次；牌匾即其 GUI sprite（{@link AwakeningPlaque}），贴在
 *       朝镜头的四边形上，每像素 {@link #BLOCKS_PER_PIXEL} 格，全亮；底边在头顶上方 {@link #LIFT} 格，避开名牌。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningTimeline
 * @since 1.0.0
 */

public final class AwakeningPlaqueRenderer {

    private AwakeningPlaqueRenderer() {}

    private static final float BLOCKS_PER_PIXEL = 0.025F;
    private static final double LIFT = 0.65;
    private static final MultiBufferSource.BufferSource BUFFERS =
            MultiBufferSource.immediate(new ByteBufferBuilder(256));

    public static void render(@NotNull PoseStack pose, @NotNull Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.options.hideGui) return;

        Vec3 view = camera.getPosition();
        boolean drawn = false;
        for (Player player : level.players()) {
            AwakeningRitual ritual = AwakeningRitualService.get(player);
            if (ritual == null || player == camera.getEntity() && !camera.isDetached()) continue;
            float alpha = AwakeningTimeline.getPlaqueAlpha(ritual.getElapsed(level.getGameTime(), partialTick));
            AwakeningPlaque plaque = AwakeningPlaque.of(ritual);
            if (alpha <= 0.0F || plaque == null || player.isInvisibleTo(minecraft.player)) continue;
            Vec3 at = player.getPosition(partialTick).add(0.0, player.getBbHeight() + LIFT, 0.0);
            if (!ClientHooks.isNameplateInRenderDistance(player, at.distanceToSqr(view))) continue;

            pose.pushPose();
            pose.translate(at.x - view.x, at.y - view.y, at.z - view.z);
            pose.mulPose(camera.rotation());
            pose.scale(BLOCKS_PER_PIXEL, -BLOCKS_PER_PIXEL, BLOCKS_PER_PIXEL);
            drawQuad(pose.last().pose(), minecraft.getGuiSprites().getSprite(plaque.sprite()), plaque.height(),
                    FastColor.ARGB32.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F));
            pose.popPose();
            drawn = true;
        }
        if (drawn) BUFFERS.endBatch();
    }

    // Wound like a name tag's glyphs, in the same flipped frame, so the same face survives culling.
    private static void drawQuad(Matrix4f matrix, TextureAtlasSprite sprite, int height, int color) {
        VertexConsumer buffer = BUFFERS.getBuffer(RenderType.text(sprite.atlasLocation()));
        float right = AwakeningPlaque.WIDTH / 2.0F;
        float left = -right;
        float top = -height;
        buffer.addVertex(matrix, left, top, 0.0F).setColor(color).setUv(sprite.getU0(), sprite.getV0())
                .setLight(LightTexture.FULL_BRIGHT);
        buffer.addVertex(matrix, left, 0.0F, 0.0F).setColor(color).setUv(sprite.getU0(), sprite.getV1())
                .setLight(LightTexture.FULL_BRIGHT);
        buffer.addVertex(matrix, right, 0.0F, 0.0F).setColor(color).setUv(sprite.getU1(), sprite.getV1())
                .setLight(LightTexture.FULL_BRIGHT);
        buffer.addVertex(matrix, right, top, 0.0F).setColor(color).setUv(sprite.getU1(), sprite.getV0())
                .setLight(LightTexture.FULL_BRIGHT);
    }
}
