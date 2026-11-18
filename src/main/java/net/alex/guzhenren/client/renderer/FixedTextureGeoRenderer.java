package net.alex.guzhenren.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 共享模型配合单一固定贴图的 GeckoLib 实体渲染器：同族每个实体类型各实例化一次，默认关闭原版死亡倾倒以原样播放模型
 * 自带死亡动画；对于移动缓慢的实体族，可通过 {@link #withMotionAnimThreshold} 压低 GeckoLib 判定行走动画的速度阈
 * 值（默认 0.015）。
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
