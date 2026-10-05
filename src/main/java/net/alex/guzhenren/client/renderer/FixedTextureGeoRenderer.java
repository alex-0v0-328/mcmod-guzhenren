package net.alex.guzhenren.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * A GeckoLib renderer that draws a shared model with one fixed texture, so each entity type of a family gets
 * its own instance. Without the vanilla death tilt the model's own death animation shows as authored.
 *
 * @author Alex
 * @version 1.0.0
 * @see ModEntityRenderers
 * @since 1.0.0
 */

public final class FixedTextureGeoRenderer<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T> {

    private final ResourceLocation texture;
    private final boolean deathTilt;

    public FixedTextureGeoRenderer(EntityRendererProvider.Context context, GeoModel<T> model,
                                   ResourceLocation texture, float shadowRadius, boolean deathTilt) {
        super(context, model);
        this.shadowRadius = shadowRadius;
        this.texture = texture;
        this.deathTilt = deathTilt;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T entity) {
        return this.texture;
    }

    @Override
    protected float getDeathMaxRotation(@NotNull T entity) {
        return this.deathTilt ? super.getDeathMaxRotation(entity) : 0.0F;
    }
}
