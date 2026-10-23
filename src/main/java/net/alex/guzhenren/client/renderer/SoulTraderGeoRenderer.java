package net.alex.guzhenren.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.trade.SoulTraderEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Renders a soul trader as the translucent {@code human_soul}: each trader's renderer instance injects its fixed
 * color texture, and every color shares one eye glow mask.
 *
 * <p>The body draws back-face culled ({@code entityTranslucentCull}): the model has 26 opposite-facing touching
 * faces that would flicker, and the far side of the silhouette would show through, without culling. An
 * invisible trader falls back to GeckoLib's own invisibility and glowing branches. There is no shadow, no
 * red hurt or death tint, and no death tilt -- the soul fades through {@code animation.disappear} instead.
 *
 * <p>⚠ {@link EyeGlowLayer} redraws the model with {@code RenderType.eyes} and the glow mask, nudged
 * {@code EYE_LIFT} blocks toward the camera. Without the nudge the eyes, which sit on the inner head, lose the
 * depth test to the outer hat shell half a pixel in front of them -- the translucent body writes depth. The
 * camera sits at the origin of the render pose, so the inverted pose matrix maps it into model space whatever
 * rotation or scale the pose carries; the nudge runs along the line from the eyes ({@code EYE_HEIGHT} above the
 * feet, pixel row 28.5 of the model) to the camera, so it moves the glow along the view ray instead of off the eye
 * texels. Back-face culling still hides the eyes from behind.
 *
 * @author Alex
 * @version 1.0.0
 * @see HumanSoulGeoModel
 * @since 1.0.0
 */

public final class SoulTraderGeoRenderer extends GeoEntityRenderer<SoulTraderEntity> {

    public static final ResourceLocation BLUE_TEXTURE = Guzhenren.id("textures/entity/human_soul_blue.png");
    public static final ResourceLocation GLOW_MASK = Guzhenren.id("textures/entity/human_soul_glowmask.png");
    private static final float EYE_LIFT = 0.08F;
    private static final float EYE_HEIGHT = 28.5F / 16.0F;
    private final ResourceLocation texture;

    public SoulTraderGeoRenderer(EntityRendererProvider.Context context, GeoModel<SoulTraderEntity> model,
                                 ResourceLocation texture) {
        super(context, model);
        this.shadowRadius = 0.0F;
        this.texture = texture;
        this.addRenderLayer(new EyeGlowLayer(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SoulTraderEntity entity) { return this.texture; }

    @Override
    public @Nullable RenderType getRenderType(SoulTraderEntity animatable, ResourceLocation texture,
                                              @Nullable MultiBufferSource bufferSource, float partialTick) {
        if (animatable.isInvisible()) return super.getRenderType(animatable, texture, bufferSource, partialTick);

        return RenderType.entityTranslucentCull(texture);
    }

    @Override
    public int getPackedOverlay(SoulTraderEntity animatable, float u, float partialTick) {
        return OverlayTexture.NO_OVERLAY;
    }

    @Override
    protected float getDeathMaxRotation(@NotNull SoulTraderEntity entity) { return 0.0F; }

    private static final class EyeGlowLayer extends GeoRenderLayer<SoulTraderEntity> {

        EyeGlowLayer(SoulTraderGeoRenderer renderer) { super(renderer); }

        @Override
        public void render(PoseStack poseStack, SoulTraderEntity animatable, BakedGeoModel bakedModel,
                           @Nullable RenderType renderType, MultiBufferSource bufferSource,
                           @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            if (animatable.isInvisible()) return;

            RenderType glow = RenderType.eyes(GLOW_MASK);
            poseStack.pushPose();
            Vector3f camera = new Matrix4f(poseStack.last().pose()).invert().transformPosition(new Vector3f())
                    .sub(0.0F, EYE_HEIGHT, 0.0F);
            if (camera.lengthSquared() > 0.0F) {
                camera.normalize(EYE_LIFT);
                poseStack.translate(camera.x(), camera.y(), camera.z());
            }
            this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow,
                    bufferSource.getBuffer(glow), partialTick, LightTexture.FULL_BRIGHT, packedOverlay,
                    1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }
    }
}
