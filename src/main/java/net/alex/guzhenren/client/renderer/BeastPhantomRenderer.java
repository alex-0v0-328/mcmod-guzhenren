package net.alex.guzhenren.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.BeastPhantomEntity;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

/**
 * Draws a Beast Phantom [兽力虚影] with its family's own model and animations -- the wild boar's for the boar, the
 * bear's and the tiger's -- in the translucent gold {@code <model>_phantom} texture at full brightness, and redraws
 * it with {@code <model>_phantom_glowmask} as an emissive layer for the eyes and the faint body glow.
 *
 * <p>Back-face culled like the soul traders, so the far side of the silhouette does not show through. It fades in
 * over {@link #FADE_IN_TICKS} and out over the last {@link #FADE_OUT_TICKS} of its life; the glow layer is additive
 * and ignores alpha, so it fades by darkening instead. No shadow, no hurt tint. The ox never manifests and falls back
 * to the boar.
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastPhantomEntity
 * @since 1.0.0
 */

public final class BeastPhantomRenderer extends GeoEntityRenderer<BeastPhantomEntity> {

    private static final int FADE_IN_TICKS = 3;
    private static final int FADE_OUT_TICKS = 10;

    public BeastPhantomRenderer(EntityRendererProvider.Context context) {
        super(context, new PhantomModel());
        this.shadowRadius = 0.0F;
        this.addRenderLayer(new GlowLayer(this));
    }

    @Override
    public void render(@NotNull BeastPhantomEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public @Nullable RenderType getRenderType(BeastPhantomEntity animatable, ResourceLocation texture,
                                              @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucentCull(texture);
    }

    @Override
    public Color getRenderColor(BeastPhantomEntity animatable, float partialTick, int packedLight) {
        return Color.ofARGB(getFade(animatable, partialTick), 1.0F, 1.0F, 1.0F);
    }

    @Override
    public int getPackedOverlay(BeastPhantomEntity animatable, float u, float partialTick) {
        return OverlayTexture.NO_OVERLAY;
    }

    private static float getFade(BeastPhantomEntity phantom, float partialTick) {
        float age = phantom.tickCount + partialTick;
        float in = age / FADE_IN_TICKS;
        float out = (BeastPhantomEntity.LIFETIME_TICKS - age) / FADE_OUT_TICKS;
        return Mth.clamp(Math.min(in, out), 0.0F, 1.0F);
    }

    private static String getModelName(BeastStrengthFamily family) {
        return switch (family) {
            case BOAR, OX -> "wild_boar";
            case BEAR -> "bear";
            case TIGER -> "tiger";
        };
    }

    private static final class PhantomModel extends GeoModel<BeastPhantomEntity> {

        @Override
        public ResourceLocation getModelResource(BeastPhantomEntity animatable) {
            return Guzhenren.id("geo/entity/" + getModelName(animatable.getFamily()) + ".geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(BeastPhantomEntity animatable) {
            return Guzhenren.id("textures/entity/" + getModelName(animatable.getFamily()) + "_phantom.png");
        }

        @Override
        public ResourceLocation getAnimationResource(BeastPhantomEntity animatable) {
            return Guzhenren.id("animations/entity/" + getModelName(animatable.getFamily()) + ".animation.json");
        }
    }

    private static final class GlowLayer extends GeoRenderLayer<BeastPhantomEntity> {

        GlowLayer(BeastPhantomRenderer renderer) { super(renderer); }

        @Override
        public void render(PoseStack poseStack, BeastPhantomEntity animatable, BakedGeoModel bakedModel,
                           @Nullable RenderType renderType, MultiBufferSource bufferSource,
                           @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            ResourceLocation mask = Guzhenren.id("textures/entity/" + getModelName(animatable.getFamily())
                    + "_phantom_glowmask.png");
            RenderType glow = RenderType.eyes(mask);
            float fade = getFade(animatable, partialTick);
            int color = Color.ofARGB(1.0F, fade, fade, fade).argbInt();
            this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow,
                    bufferSource.getBuffer(glow), partialTick, LightTexture.FULL_BRIGHT, packedOverlay, color);
        }
    }
}
