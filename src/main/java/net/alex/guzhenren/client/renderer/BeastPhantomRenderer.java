package net.alex.guzhenren.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.EnumMap;
import java.util.Map;
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
 * 兽力虚影渲染器。
 * <ul>
 *   <li>沿用所属野兽族的模型与动画：猪族用野猪，熊与虎用各自模型，龟族用陆龟，牛族用水牛；不可显形的族（
 *       {@code BeastStrengthFamily.canManifest}）直接跳过渲染。</li>
 *   <li>先全亮绘制半透明的 {@code <model>_phantom} 贴图，再叠绘 {@code <model>_phantom_glowmask} 作为自发光层（身体
 *       以自身颜色微光，不透明眼部最亮）；贴图选定在 {@link #getTextureLocation} 完成，不混用野兽原版贴图。各族的模型、
 *       贴图、遮罩与动画路径在类加载时建好（{@code RESOURCES}），每帧只查表。</li>
 *   <li>马族没有 GeckoLib 模型，由 {@link #render} 委托原版马模型（{@link HorsePhantomModel}）按相同双层管线绘制
 *       （偏航、俯仰与淡出一致），实现主人身旁扬蹄或兽力冲击奔跑。</li>
 *   <li>主人身旁召唤虚影与兽力冲击均统一读取 {@link BeastPhantomVisual}：按 {@link BeastPhantomVisual#getFade} 淡
 *       出、叠加 {@link BeastPhantomVisual#getDrift} 偏移、以脚底为原点按 {@link BeastPhantomVisual#getScale} 缩
 *       放、发光强度乘以 {@link BeastPhantomVisual#getGlow}，启用背面剔除（同魂体交易者）以免远侧剪影透出，且不产生阴
 *       影或受击染红。</li>
 *   <li>自发光层仅保留遮罩自身 Alpha 的 {@link #GLOW_ALPHA} 比例，避免半透明身体被全强度覆盖成完全不透明。</li>
 *   <li>⚠ 本体层与发光层均使用自建的背面剔除且不写深度的渲染类型（{@link #BODY} 与 {@link #GLOW}）：原版半透明实
 *       体类型会写入深度，而在 Iris 光影下玩家皮肤渲染晚于虚影，会导致第三人称时身后虚影直接把玩家本体遮没；该折中的
 *       代价是虚影后方的半透明方块（水、染色玻璃）会盖在虚影之上，且同一虚影前后重叠部位发光会累加。</li>
 *   <li>⚠ 不可改用 {@code RenderType.eyes}：该类型忽略顶点 Alpha，会把覆盖全身的发光遮罩绘成满强度不透明。</li>
 *   <li>⚠ 绕脚底上方 {@link #PITCH_PIVOT} 格（约模型半高）应用 {@link BeastPhantomVisual#getPitch} 俯仰角；因
 *       GeckoLib 仅对活体应用身体偏航，本渲染器须先手动应用实体自身偏航旋转。</li>
 * </ul>
 */

public final class BeastPhantomRenderer<T extends Entity & GeoEntity & BeastPhantomVisual> extends GeoEntityRenderer<T> {

    /** 虚影相对野兽的尺寸倍数。 */
    public static final float PHANTOM_SCALE = 1.0F;
    /** 发光层保留的遮罩自身 alpha 比例；遮罩覆盖全身。 */
    public static final float GLOW_ALPHA = 0.25F;
    public static final float PITCH_PIVOT = 0.7F;
    private static final Function<ResourceLocation, RenderType> BODY = Util.memoize(texture -> create("body",
            RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER, texture, true));
    private static final Function<ResourceLocation, RenderType> GLOW = Util.memoize(texture -> create("glow",
            RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER, texture, false));
    private static final Map<BeastStrengthFamily, Resources> RESOURCES = Util.make(
            new EnumMap<>(BeastStrengthFamily.class), map -> {
                for (BeastStrengthFamily family : BeastStrengthFamily.values()) {
                    map.put(family, Resources.of(getModelName(family)));
                }
            });

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
        Resources horseResources = RESOURCES.get(BeastStrengthFamily.HORSE);
        RenderType body = BODY.apply(horseResources.texture());
        horse.render(poseStack, bufferSource.getBuffer(body), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                Color.ofARGB(fade, 1.0F, 1.0F, 1.0F).argbInt());
        RenderType glow = GLOW.apply(horseResources.glowmask());
        horse.render(poseStack, bufferSource.getBuffer(glow), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                getGlowColor(entity, partialTick));
        poseStack.popPose();
    }
    //endregion

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T animatable) {
        return RESOURCES.get(animatable.getFamily()).texture();
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

    private record Resources(ResourceLocation model, ResourceLocation texture, ResourceLocation glowmask,
                             ResourceLocation animation) {

        static Resources of(String name) {
            return new Resources(Guzhenren.id("geo/entity/" + name + ".geo.json"),
                    Guzhenren.id("textures/entity/" + name + "_phantom.png"),
                    Guzhenren.id("textures/entity/" + name + "_phantom_glowmask.png"),
                    Guzhenren.id("animations/entity/" + name + ".animation.json"));
        }
    }

    private static final class PhantomModel<T extends Entity & GeoEntity & BeastPhantomVisual> extends GeoModel<T> {

        @Override
        public ResourceLocation getModelResource(T animatable) {
            return RESOURCES.get(animatable.getFamily()).model();
        }

        @Override
        public ResourceLocation getTextureResource(T animatable) {
            return RESOURCES.get(animatable.getFamily()).texture();
        }

        @Override
        public ResourceLocation getAnimationResource(T animatable) {
            return RESOURCES.get(animatable.getFamily()).animation();
        }
    }

    private static final class GlowLayer<T extends Entity & GeoEntity & BeastPhantomVisual>
            extends GeoRenderLayer<T> {

        GlowLayer(BeastPhantomRenderer<T> renderer) { super(renderer); }

        @Override
        public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType,
                           MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick,
                           int packedLight, int packedOverlay) {
            RenderType glow = GLOW.apply(RESOURCES.get(animatable.getFamily()).glowmask());
            this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow,
                    bufferSource.getBuffer(glow), partialTick, LightTexture.FULL_BRIGHT, packedOverlay,
                    getGlowColor(animatable, partialTick));
        }
    }
}
