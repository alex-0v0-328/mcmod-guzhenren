package net.alex.guzhenren.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitual;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
 * 开窍[Awakening]仪式中希望蛊[Hope Gu]的世界渲染。
 *
 * <ul>
 *   <li>本客户端可见的每场仪式都画：蛊从玩家主手（眼前 {@link #HAND_FORWARD} 格、主手侧 {@link #HAND_SIDE} 格、下方
 *       {@link #HAND_DROP} 格，本人第一人称下从视野下角升起）飞到同步的悬停点，上下浮动；{@link #getGuPosition} 也给
 *       余烬与火星定位。</li>
 *   <li>{@code AwakeningEvents} 每帧在实体后调一次；画燃烬贴图 {@code hope_burnout} 的当前帧（
 *       {@link AwakeningTimeline#getBurnoutFrame}，帧 0 即物品图标），朝镜头、全亮、{@link #PIXELS} 像素宽
 *       {@code PIXELS × BLOCKS_PER_PIXEL} 格，与粒子同密度；不缩放，燃尽即不画；绕向同头顶牌匾，自用缓冲。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningTimeline
 * @since 1.0.0
 */

public final class AwakeningGuRenderer {

    private AwakeningGuRenderer() {}

    private static final ResourceLocation BURNOUT = Guzhenren.id("textures/particle/hope_burnout.png");
    private static final int PIXELS = 16;
    private static final int FRAMES = 8;
    private static final float BLOCKS_PER_PIXEL = 1.0F / 32.0F;
    private static final int WHITE = 0xFFFFFFFF;
    private static final double HAND_FORWARD = 0.4;
    private static final double HAND_SIDE = 0.35;
    private static final double HAND_DROP = 0.35;
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    private static final MultiBufferSource.BufferSource BUFFERS =
            MultiBufferSource.immediate(new ByteBufferBuilder(256));

    public static void render(@NotNull PoseStack pose, @NotNull Camera camera, float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        Vec3 view = camera.getPosition();
        boolean drawn = false;
        for (Player player : level.players()) {
            AwakeningRitual ritual = AwakeningRitualService.get(player);
            if (ritual == null) continue;
            float elapsed = ritual.getElapsed(level.getGameTime(), partialTick);
            if (elapsed < 0.0F) continue;
            int frame = AwakeningTimeline.getBurnoutFrame((int) elapsed);
            if (frame < 0) continue;

            Vec3 at = getGuPosition(player, ritual, elapsed, partialTick);
            pose.pushPose();
            pose.translate(at.x - view.x, at.y - view.y, at.z - view.z);
            pose.mulPose(camera.rotation());
            pose.scale(BLOCKS_PER_PIXEL, -BLOCKS_PER_PIXEL, BLOCKS_PER_PIXEL);
            drawFrame(pose.last().pose(), frame);
            pose.popPose();
            drawn = true;
        }
        if (drawn) BUFFERS.endBatch();
    }

    public static @NotNull Vec3 getGuPosition(@NotNull Player player, @NotNull AwakeningRitual ritual, float elapsed,
                                              float partialTick) {
        return getHand(player, partialTick).lerp(ritual.anchor(), AwakeningTimeline.getFlight(elapsed))
                .add(0.0, AwakeningTimeline.getBob(elapsed), 0.0);
    }

    private static void drawFrame(Matrix4f matrix, int frame) {
        VertexConsumer buffer = BUFFERS.getBuffer(RenderType.text(BURNOUT));
        float half = PIXELS / 2.0F;
        float u0 = (float) frame / FRAMES;
        float u1 = (float) (frame + 1) / FRAMES;
        buffer.addVertex(matrix, -half, -half, 0.0F).setColor(WHITE).setUv(u0, 0.0F).setLight(LightTexture.FULL_BRIGHT);
        buffer.addVertex(matrix, -half, half, 0.0F).setColor(WHITE).setUv(u0, 1.0F).setLight(LightTexture.FULL_BRIGHT);
        buffer.addVertex(matrix, half, half, 0.0F).setColor(WHITE).setUv(u1, 1.0F).setLight(LightTexture.FULL_BRIGHT);
        buffer.addVertex(matrix, half, -half, 0.0F).setColor(WHITE).setUv(u1, 0.0F).setLight(LightTexture.FULL_BRIGHT);
    }

    private static Vec3 getHand(Player player, float partialTick) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick));
        Vec3 side = forward.cross(UP).scale(player.getMainArm() == HumanoidArm.RIGHT ? HAND_SIDE : -HAND_SIDE);
        return player.getEyePosition(partialTick).add(forward.scale(HAND_FORWARD)).add(side)
                .subtract(0.0, HAND_DROP, 0.0);
    }
}
