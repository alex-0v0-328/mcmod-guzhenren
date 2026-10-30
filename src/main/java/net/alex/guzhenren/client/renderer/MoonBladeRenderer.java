package net.alex.guzhenren.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.projectile.MoonBladeEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * Draws a {@link MoonBladeEntity} as one full-bright sprite a block wide, facing the camera and turned so the
 * crescent's bulge points where the blade flies on screen. Seen head-on, flying straight away from the eye or at it,
 * there is no on-screen direction and it stays upright.
 *
 * <p>The texture is a vertical strip of {@link #FRAMES} 16-pixel frames read by age, not a vanilla animation: the
 * crescent grows over the first three ticks, holds the full frame while it flies, and fades over the last three
 * before {@link MoonBladeEntity#LIFETIME_TICKS}. A blade that hits something vanishes on its full frame.
 *
 * @author Alex
 * @version 1.0.0
 * @see MoonBladeEntity
 * @since 1.0.0
 */

public final class MoonBladeRenderer extends EntityRenderer<MoonBladeEntity> {

    private static final ResourceLocation MOON = Guzhenren.id("textures/entity/moon_blade.png");
    private static final ResourceLocation BLOOD = Guzhenren.id("textures/entity/blood_moon_blade.png");
    private static final int FRAMES = 7;
    private static final int FULL_FRAME = 3;
    private static final float SIZE = 1.0F;
    private static final double HEAD_ON = 1.0E-4D;

    public MoonBladeRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override
    public void render(@NotNull MoonBladeEntity blade, float entityYaw, float partialTicks, PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight) {
        int frame = getFrame(blade.tickCount);
        float top = frame / (float) FRAMES;
        float bottom = (frame + 1) / (float) FRAMES;

        poseStack.pushPose();
        poseStack.translate(0.0F, blade.getBbHeight() / 2.0F, 0.0F);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotation(getScreenAngle(blade.getDeltaMovement())));
        poseStack.scale(SIZE, SIZE, SIZE);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(getTextureLocation(blade)));
        vertex(consumer, pose, -0.5F, -0.5F, 0.0F, bottom);
        vertex(consumer, pose, 0.5F, -0.5F, 1.0F, bottom);
        vertex(consumer, pose, 0.5F, 0.5F, 1.0F, top);
        vertex(consumer, pose, -0.5F, 0.5F, 0.0F, top);
        poseStack.popPose();
        super.render(blade, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    static int getFrame(int age) {
        int fading = age - (MoonBladeEntity.LIFETIME_TICKS - 2);
        if (fading >= 0) return Math.min(FULL_FRAME + 1 + fading, FRAMES - 1);
        return Math.min(age, FULL_FRAME);
    }

    private float getScreenAngle(Vec3 motion) {
        Camera camera = entityRenderDispatcher.camera;
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        double right = -(motion.x * left.x() + motion.y * left.y() + motion.z * left.z());
        double upward = motion.x * up.x() + motion.y * up.y() + motion.z * up.z();
        if (right * right + upward * upward < HEAD_ON) return 0.0F;
        return (float) Mth.atan2(upward, right);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v) {
        consumer.addVertex(pose, x, y, 0.0F)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull MoonBladeEntity blade) {
        return blade.getVariant() == MoonBladeEntity.Variant.BLOOD ? BLOOD : MOON;
    }
}
