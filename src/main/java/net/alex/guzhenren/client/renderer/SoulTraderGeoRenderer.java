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
 * 魂体交易者的渲染器，画成半透明的 {@code human_soul}。
 *
 * <ul>
 *   <li>每个实例注入交易者的固定颜色贴图，各色共用一张眼睛发光遮罩；隐身交易者回退 GeckoLib 自带隐身与发光分支；无阴
 *       影、无受伤或死亡红色染色、无死亡倾倒，魂体改经 {@code animation.disappear} 淡出。</li>
 *   <li>身体背面剔除（{@code entityTranslucentCull}）：模型 26 个面朝向相反、彼此相贴，否则闪烁且剪影远侧透出。</li>
 *   <li>⚠ {@link EyeGlowLayer} 用 {@code RenderType.eyes} 与发光遮罩重绘模型，朝镜头推近 {@code EYE_LIFT} 格，否则内层
 *       头部的眼睛在深度测试中输给前方半像素的外层帽壳（半透明身体写深度）。</li>
 *   <li>推近沿眼睛（脚底上方 {@code EYE_HEIGHT}，模型第 28.5 像素行）到镜头的连线，发光沿视线移动、不偏离眼睛纹素；镜头
 *       在渲染姿态原点，任何旋转或缩放下逆姿态矩阵都能把它映射到模型空间；背面剔除仍从背后挡住眼睛。</li>
 * </ul>
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
                    bufferSource.getBuffer(glow), partialTick, LightTexture.FULL_BRIGHT, packedOverlay, -1);
            poseStack.popPose();
        }
    }
}
