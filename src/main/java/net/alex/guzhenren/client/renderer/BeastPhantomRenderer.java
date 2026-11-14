package net.alex.guzhenren.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.function.Function;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.BeastPhantomVisual;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

/**
 * 兽力虚影的渲染器。
 *
 * <ul>
 *   <li>用所属族的模型与动画：猪用野猪，熊、虎用自己的，龟用陆龟，牛用水牛；不能显形的族
 *       （{@code BeastStrengthFamily.canManifest}）不画。</li>
 *   <li>半透明 {@code <model>_phantom} 贴图全亮绘制，再以副本 {@code <model>_phantom_glowmask} 作自发光层重绘：身体以自
 *       身颜色微光，不透明的眼睛最亮；贴图在此选定（{@link #getTextureLocation}），不混入野兽自身贴图。</li>
 *   <li>马无 GeckoLib 模型：{@link #render} 交给原版马几何（{@link HorsePhantomModel}），画法相同（偏航、俯仰、淡出、两
 *       层），在主人身旁扬蹄一次，冲击时奔跑。</li>
 *   <li>主人身旁虚影与兽力冲击的 {@link BeastPhantomVisual} 一视同仁：按 {@link BeastPhantomVisual#getFade} 淡出，偏移
 *       {@link BeastPhantomVisual#getDrift}，以脚底为中心缩放 {@link BeastPhantomVisual#getScale}，发光乘
 *       {@link BeastPhantomVisual#getGlow}；背面剔除（同魂体交易者），剪影远侧不透出；无阴影、无受伤染色。</li>
 *   <li>发光层半透明自发光，只取遮罩 alpha 的 {@link #GLOW_ALPHA}，否则半透明身体被封成不透明。</li>
 *   <li>⚠ 两层都用自建类型（{@link #BODY}、{@link #GLOW}），背面剔除且不写深度：原版半透明实体类型写深度，而光影
 *       （Iris）下玩家皮肤晚于虚影绘制，第三人称时身后扇形会把主人整个挡掉。代价是虚影后方的半透明方块（水、染色玻璃）
 *       盖在虚影上，同一虚影前后重叠的部位发光叠加。</li>
 *   <li>⚠ 不用 {@code RenderType.eyes}：它忽略 alpha，遮罩下全身画成满强度。</li>
 *   <li>⚠ 绕脚底上方 {@link #PITCH_PIVOT} 格（约模型高度一半，暂定值）按 {@link BeastPhantomVisual#getPitch} 俯仰；
 *       GeckoLib 只按身体偏航转活体，故先按实体自身偏航转动。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastPhantomVisual
 * @since 1.0.0
 */

public final class BeastPhantomRenderer<T extends Entity & GeoEntity & BeastPhantomVisual> extends GeoEntityRenderer<T> {

    /** 虚影相对野兽的尺寸倍数。 */
    public static final float PHANTOM_SCALE = 1.0F;
    /** 发光层保留的遮罩自身 alpha 比例；遮罩覆盖全身。暂定值。 */
    public static final float GLOW_ALPHA = 0.25F;
    public static final float PITCH_PIVOT = 0.7F;
    private static final Function<ResourceLocation, RenderType> BODY = Util.memoize(texture -> create("body",
            RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER, texture, true));
    private static final Function<ResourceLocation, RenderType> GLOW = Util.memoize(texture -> create("glow",
            RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER, texture, false));

    private final HorsePhantomModel horse;

    public BeastPhantomRenderer(EntityRendererProvider.Context context) {
        super(context, new PhantomModel<>());
        this.shadowRadius = 0.0F;
        this.withScale(PHANTOM_SCALE);
        this.addRenderLayer(new GlowLayer<>(this));
        this.horse = new HorsePhantomModel(context);
    }

    private static RenderType create(String layer, RenderStateShard.ShaderStateShard shader, ResourceLocation texture,
                                     boolean lit) {
        RenderType.CompositeState.CompositeStateBuilder state = RenderType.CompositeState.builder()
                .setShaderState(shader)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE);
        if (lit) state.setLightmapState(RenderStateShard.LIGHTMAP);
        return RenderType.create(Guzhenren.MOD_ID + "_beast_phantom_" + layer, DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, RenderType.TRANSIENT_BUFFER_SIZE, true, true,
                state.createCompositeState(true));
    }

    @Override
    public void render(@NotNull T entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight) {
        BeastStrengthFamily family = entity.getFamily();
        if (!family.canManifest()) return;

        poseStack.pushPose();
        applyDrift(entity, poseStack, partialTick);
        if (family == BeastStrengthFamily.HORSE) {
            renderHorse(entity, partialTick, poseStack, bufferSource);
        } else {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
        }
        poseStack.popPose();
    }

    private static void applyDrift(Entity entity, PoseStack poseStack, float partialTick) {
        Vec3 drift = ((BeastPhantomVisual) entity).getDrift(partialTick);
        if (drift.equals(Vec3.ZERO)) return;

        float radians = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()) * Mth.DEG_TO_RAD;
        float sin = Mth.sin(radians);
        float cos = Mth.cos(radians);
        poseStack.translate(-sin * drift.z - cos * drift.x, drift.y, cos * drift.z - sin * drift.x);
    }

    private static int getGlowColor(BeastPhantomVisual visual, float partialTick) {
        float alpha = Mth.clamp(visual.getGlow(partialTick) * GLOW_ALPHA, 0.0F, 1.0F);
        return Color.ofARGB(alpha, 1.0F, 1.0F, 1.0F).argbInt();
    }

    @Override
    protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks, float rotationYaw,
                                  float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks,
                Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot()), partialTick, nativeScale);
        float scale = animatable.getScale(partialTick);
        poseStack.scale(scale, scale, scale);
        applyPitch(animatable, poseStack, partialTick);
    }

    private static void applyPitch(Entity entity, PoseStack poseStack, float partialTick) {
        float pitch = ((BeastPhantomVisual) entity).getPitch(partialTick);
        if (pitch == 0.0F) return;

        poseStack.translate(0.0F, PITCH_PIVOT, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.translate(0.0F, -PITCH_PIVOT, 0.0F);
    }

    //region Horse [马]
    private void renderHorse(T entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        float age = entity.tickCount + partialTick;
        if (entity.isCharging()) {
            horse.pose(0.0F, age * HorsePhantomModel.GALLOP_SWING_PER_TICK, 1.0F, age);
        } else {
            horse.pose(HorsePhantomModel.getRear(age), 0.0F, 0.0F, age);
        }

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        applyPitch(entity, poseStack, partialTick);
        float scale = PHANTOM_SCALE * entity.getScale(partialTick);
        poseStack.scale(-scale, -scale, scale);
        poseStack.scale(HorsePhantomModel.SCALE, HorsePhantomModel.SCALE, HorsePhantomModel.SCALE);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        float fade = entity.getFade(partialTick);
        String model = getModelName(BeastStrengthFamily.HORSE);
        RenderType body = BODY.apply(Guzhenren.id("textures/entity/" + model + "_phantom.png"));
        horse.render(poseStack, bufferSource.getBuffer(body), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                Color.ofARGB(fade, 1.0F, 1.0F, 1.0F).argbInt());
        RenderType glow = GLOW.apply(Guzhenren.id("textures/entity/" + model + "_phantom_glowmask.png"));
        horse.render(poseStack, bufferSource.getBuffer(glow), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                getGlowColor(entity, partialTick));
        poseStack.popPose();
    }
    //endregion

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T animatable) {
        return Guzhenren.id("textures/entity/" + getModelName(animatable.getFamily()) + "_phantom.png");
    }

    @Override
    public @Nullable RenderType getRenderType(T animatable, ResourceLocation texture,
                                              @Nullable MultiBufferSource bufferSource, float partialTick) {
        return BODY.apply(texture);
    }

    @Override
    public Color getRenderColor(T animatable, float partialTick, int packedLight) {
        return Color.ofARGB(animatable.getFade(partialTick), 1.0F, 1.0F, 1.0F);
    }

    @Override
    public int getPackedOverlay(T animatable, float u, float partialTick) { return OverlayTexture.NO_OVERLAY; }

    private static String getModelName(BeastStrengthFamily family) {
        return switch (family) {
            case BOAR -> "wild_boar";
            case OX -> "buffalo";
            case BEAR -> "bear";
            case TIGER -> "tiger";
            case TURTLE -> "tortoise";
            case HORSE -> "horse";
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
            RenderType glow = GLOW.apply(mask);
            this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow,
                    bufferSource.getBuffer(glow), partialTick, LightTexture.FULL_BRIGHT, packedOverlay,
                    getGlowColor(animatable, partialTick));
        }
    }
}
