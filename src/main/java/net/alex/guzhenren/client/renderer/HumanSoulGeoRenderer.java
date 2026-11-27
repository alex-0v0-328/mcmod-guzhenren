package net.alex.guzhenren.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * 人魂渲染器：以半透明形态渲染 {@code human_soul} 模型，魂体交易者[Soul Trader]与地灵[Earth Spirit]共用，每个实体类型
 * 只换贴图（蓝色交易魂、粉色地灵）。
 * <ul>
 *   <li>每个实例注入各自固定的颜色贴图并共用同一张眼部发光遮罩；隐身交易者回退至 GeckoLib 自带的隐身与发光分支；关
 *       闭地面阴影、受击或死亡染红及原版死亡倾倒，改由 {@code animation.disappear} 淡出。</li>
 *   <li>身体采用开启背面剔除的半透明类型（{@code entityTranslucentCull}）：因模型 26 个面朝向相反且彼此相贴，若不剔
 *       除背面不仅会闪烁还会透出远侧剪影。</li>
 *   <li>⚠ 两种身体与眼部发光经 {@link #registerBuffers} 登记为固定缓冲：共享缓冲一换类型就当场画出，比半透明地形
 *       （荡魂山石、水、染色玻璃）早，身体写进的深度会把身后的山壁剔掉，透过半透明身体直接看见山后；固定缓冲在非
 *       「极佳」画质下等半透明地形画完才统一画出（身体在前、眼睛在后，按登记顺序），「极佳」画质本就分目标合成。</li>
 *   <li>⚠ {@link EyeGlowLayer} 使用 {@code RenderType.eyes} 与发光遮罩重绘模型，并必须沿视线朝镜头推近
 *       {@code EYE_LIFT} 格：因为半透明身体会写入深度缓冲，若不前移，内层头部的眼睛在深度测试中会输给前方仅半像素之
 *       差的外层帽壳。</li>
 *   <li>前移向量取自眼睛位置（脚底上方 {@code EYE_HEIGHT}，对应模型第 28.5 像素行）指向相机原点，确保发光始终沿视线
 *       移动而不偏移眼睛纹素；在渲染姿态坐标系中相机位于原点，因此无论实体如何旋转或缩放，逆姿态矩阵都能将该向量准确映
 *       射回模型空间，同时背面剔除仍会从背后正确遮挡眼睛。</li>
 * </ul>
 */

public final class HumanSoulGeoRenderer<T extends Mob & GeoEntity> extends GeoEntityRenderer<T> {

    public static final ResourceLocation BLUE_TEXTURE = Guzhenren.id("textures/entity/human_soul_blue.png");
    public static final ResourceLocation PINK_TEXTURE = Guzhenren.id("textures/entity/human_soul_pink.png");
    public static final ResourceLocation GLOW_MASK = Guzhenren.id("textures/entity/human_soul_glowmask.png");
    private static final float EYE_LIFT = 0.08F;
    private static final float EYE_HEIGHT = 28.5F / 16.0F;
    private final ResourceLocation texture;

    public HumanSoulGeoRenderer(EntityRendererProvider.Context context, GeoModel<T> model,
                                 ResourceLocation texture) {
        super(context, model);
        this.shadowRadius = 0.0F;
        this.texture = texture;
        this.addRenderLayer(new EyeGlowLayer<>(this));
    }

    public static void registerBuffers(RegisterRenderBuffersEvent event) {
        event.registerRenderBuffer(RenderType.entityTranslucentCull(BLUE_TEXTURE));
        event.registerRenderBuffer(RenderType.entityTranslucentCull(PINK_TEXTURE));
        event.registerRenderBuffer(RenderType.eyes(GLOW_MASK));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T entity) { return this.texture; }

    @Override
    public @Nullable RenderType getRenderType(T animatable, ResourceLocation texture,
                                              @Nullable MultiBufferSource bufferSource, float partialTick) {
        if (animatable.isInvisible()) return super.getRenderType(animatable, texture, bufferSource, partialTick);

        return RenderType.entityTranslucentCull(texture);
    }

    @Override
    public int getPackedOverlay(T animatable, float u, float partialTick) {
        return OverlayTexture.NO_OVERLAY;
    }

    @Override
    protected float getDeathMaxRotation(@NotNull T entity) { return 0.0F; }

    private static final class EyeGlowLayer<T extends Mob & GeoEntity> extends GeoRenderLayer<T> {

        EyeGlowLayer(HumanSoulGeoRenderer<T> renderer) { super(renderer); }

        @Override
        public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel,
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
