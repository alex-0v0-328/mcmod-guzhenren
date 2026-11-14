package net.alex.guzhenren.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 共享模型配单一固定贴图的 GeckoLib 渲染器，同族每实体类型各一实例；关闭原版死亡倾倒，模型自带死亡动画原样显示；慢速族用
 * {@link #withMotionAnimThreshold} 压低 GeckoLib 判定行走的速度阈值（默认 0.015）。
 *
 * @author Alex
 * @version 1.0.0
 * @see ModEntityRenderers
 * @since 1.0.0
 */

public final class FixedTextureGeoRenderer<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T> {

    private final ResourceLocation texture;
    private final boolean deathTilt;
    private float motionAnimThreshold = Float.NaN;

    public FixedTextureGeoRenderer(EntityRendererProvider.Context context, GeoModel<T> model,
                                   ResourceLocation texture, float shadowRadius, boolean deathTilt) {
        super(context, model);
        this.shadowRadius = shadowRadius;
        this.texture = texture;
        this.deathTilt = deathTilt;
    }

    public FixedTextureGeoRenderer<T> withMotionAnimThreshold(float threshold) {
        this.motionAnimThreshold = threshold;
        return this;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T entity) {
        return this.texture;
    }

    @Override
    protected float getDeathMaxRotation(@NotNull T entity) {
        return this.deathTilt ? super.getDeathMaxRotation(entity) : 0.0F;
    }

    @Override
    public float getMotionAnimThreshold(T entity) {
        return Float.isNaN(this.motionAnimThreshold) ? super.getMotionAnimThreshold(entity) : this.motionAnimThreshold;
    }
}
