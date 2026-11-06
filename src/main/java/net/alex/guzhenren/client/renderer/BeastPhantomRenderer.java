package net.alex.guzhenren.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.BeastPhantomVisual;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

/**
 * Draws a Beast Phantom [兽力虚影] with its family's own model and animations -- the wild boar's for the boar, the
 * bear's and the tiger's -- in the translucent gold {@code <model>_phantom} texture at full brightness, and redraws
 * it with {@code <model>_phantom_glowmask} as an emissive layer for the eyes and the faint body glow. The texture is
 * picked here ({@link #getTextureLocation}), not left to the model, so the wild beasts' own textures never leak in.
 *
 * <p>Draws every {@link BeastPhantomVisual}: the phantom beside its owner and the Beast Charge [兽力冲击] alike, at
 * {@link #PHANTOM_SCALE} times the wild beast's size.
 * Back-face culled like the soul traders, so the far side of the silhouette does not show through. Each fades as its
 * own {@link BeastPhantomVisual#getFade} says; the glow layer is translucent emissive at {@link #GLOW_ALPHA} of the
 * mask's own alpha, since the mask paints the whole body and at full alpha it closed the gold up to opaque. ⚠ Not {@code RenderType.eyes}: that one ignores alpha and
 * paints the whole masked body at full strength, which buried the gold under the mask's brown. GeckoLib turns only a living entity by its body yaw, so this turns these by their own yaw, then
 * tips them by {@link BeastPhantomVisual#getPitch} about a point {@link #PITCH_PIVOT} blocks above the feet, about
 * half the beasts' model height. No shadow, no hurt tint. The ox never manifests and falls back to the boar.
 * ⚠ {@link #PITCH_PIVOT} is provisional.
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastPhantomVisual
 * @since 1.0.0
 */

public final class BeastPhantomRenderer<T extends Entity & GeoEntity & BeastPhantomVisual> extends GeoEntityRenderer<T> {

    /** The phantom beasts are drawn this many times the wild beasts' size. */
    public static final float PHANTOM_SCALE = 1.0F;
    /** How much of the glow mask's own alpha the glow layer keeps; the mask covers the whole body. Provisional. */
    public static final float GLOW_ALPHA = 0.25F;
    public static final float PITCH_PIVOT = 0.7F;

    public BeastPhantomRenderer(EntityRendererProvider.Context context) {
        super(context, new PhantomModel<>());
        this.shadowRadius = 0.0F;
        this.withScale(PHANTOM_SCALE);
        this.addRenderLayer(new GlowLayer<>(this));
    }

    @Override
    public void render(@NotNull T entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks, float rotationYaw,
                                  float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks,
                Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot()), partialTick, nativeScale);
        float pitch = animatable.getPitch(partialTick);
        if (pitch == 0.0F) return;

        poseStack.translate(0.0F, PITCH_PIVOT, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.translate(0.0F, -PITCH_PIVOT, 0.0F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T animatable) {
        return Guzhenren.id("textures/entity/" + getModelName(animatable.getFamily()) + "_phantom.png");
    }

    @Override
    public @Nullable RenderType getRenderType(T animatable, ResourceLocation texture,
                                              @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucentCull(texture);
    }

    @Override
    public Color getRenderColor(T animatable, float partialTick, int packedLight) {
        return Color.ofARGB(animatable.getFade(partialTick), 1.0F, 1.0F, 1.0F);
    }

    @Override
    public int getPackedOverlay(T animatable, float u, float partialTick) { return OverlayTexture.NO_OVERLAY; }

    private static String getModelName(BeastStrengthFamily family) {
        return switch (family) {
            case BOAR, OX -> "wild_boar";
            case BEAR -> "bear";
            case TIGER -> "tiger";
        };
    }

    private static final class PhantomModel<T extends Entity & GeoEntity & BeastPhantomVisual> extends GeoModel<T> {

        @Override
        public ResourceLocation getModelResource(T animatable) {
            return Guzhenren.id("geo/entity/" + getModelName(animatable.getFamily()) + ".geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(T animatable) {
            return Guzhenren.id("textures/entity/" + getModelName(animatable.getFamily()) + "_phantom.png");
        }

        @Override
        public ResourceLocation getAnimationResource(T animatable) {
            return Guzhenren.id("animations/entity/" + getModelName(animatable.getFamily()) + ".animation.json");
        }
    }

    private static final class GlowLayer<T extends Entity & GeoEntity & BeastPhantomVisual>
            extends GeoRenderLayer<T> {

        GlowLayer(BeastPhantomRenderer<T> renderer) { super(renderer); }

        @Override
        public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType,
                           MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick,
                           int packedLight, int packedOverlay) {
            ResourceLocation mask = Guzhenren.id("textures/entity/" + getModelName(animatable.getFamily())
                    + "_phantom_glowmask.png");
            RenderType glow = RenderType.entityTranslucentEmissive(mask);
            int color = Color.ofARGB(animatable.getFade(partialTick) * GLOW_ALPHA, 1.0F, 1.0F, 1.0F).argbInt();
            this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow,
                    bufferSource.getBuffer(glow), partialTick, LightTexture.FULL_BRIGHT, packedOverlay, color);
        }
    }
}
