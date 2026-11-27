package net.alex.guzhenren.client.renderer.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.item.armor.SchoolUniformItem;
import net.alex.guzhenren.item.armor.UniformColor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

/**
 * 学堂制服[School Uniform]双层 GeckoLib 护甲渲染器。
 * <ul>
 *   <li>主渲染通道绘制 {@code school_uniform_layer.png} 并按物品堆的 {@link UniformColor} 乘色；固定细节层
 *       （{@code school_uniform_base.png}：白领缘、素白腰带、滚边、绷带与黑面白底布鞋）通过 {@link BaseDetailLayer}
 *       以纯白 {@code 0xFFFFFFFF} 叠绘其上，不受染色影响并保留附魔光效。</li>
 *   <li>袖子（外袖、内袖、腕绷带）按手臂宽度分两份骨骼：{@code *_arm_wide} 配 4 像素宽手臂，{@code *_arm_slim} 配纤细
 *       皮肤的 3 像素宽手臂；内袖与腕绷带贴手臂，外袖都比手臂外偏 1 像素保持宽大。{@link #preRender} 按穿戴者皮肤
 *       （{@link #isSlimArmed}）隐藏不匹配的一份。</li>
 *   <li>Epic Fight 下不走本渲染器：EF 读 {@code animmodels/armor/<物品 id>.json} 三份蒙皮网格（几何与 UV 同
 *       {@code school_uniform.geo.json}，下摆随大腿，袖子同样分 {@code leftArm} / {@code leftArmSlim} 等部件），改 geo
 *       几何或 UV 须用 {@code tools/school_uniform_ef_mesh.py} 重新生成。</li>
 * </ul>
 */

public final class SchoolUniformArmorRenderer extends GeoArmorRenderer<SchoolUniformItem> {

    private static final ResourceLocation BASE_TEXTURE = Guzhenren.id("textures/armor/school_uniform_base.png");
    private static final List<String> WIDE_BONES = List.of("left_arm_wide", "right_arm_wide");
    private static final List<String> SLIM_BONES = List.of("left_arm_slim", "right_arm_slim");

    public SchoolUniformArmorRenderer() {
        super(new SchoolUniformArmorModel());
        addRenderLayer(new BaseDetailLayer(this));
    }

    public static boolean isSlimArmed(@Nullable Entity wearer) {
        return wearer instanceof AbstractClientPlayer player && player.getSkin().model() == PlayerSkin.Model.SLIM;
    }

    @Override
    public void preRender(PoseStack poseStack, SchoolUniformItem animatable, BakedGeoModel model,
                          @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);
        for (String name : isSlimArmed(this.currentEntity) ? WIDE_BONES : SLIM_BONES) {
            model.getBone(name).ifPresent(bone -> bone.setHidden(true));
        }
    }

    @Override
    public Color getRenderColor(SchoolUniformItem animatable, float partialTick, int packedLight) {
        if (this.currentStack != null && animatable.isDyeable()) {
            return Color.ofOpaque(UniformColor.rgb(UniformColor.getColor(this.currentStack)));
        }
        return Color.WHITE;
    }

    private final class BaseDetailLayer extends GeoRenderLayer<SchoolUniformItem> {

        private BaseDetailLayer(SchoolUniformArmorRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, SchoolUniformItem animatable, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                           float partialTick, int packedLight, int packedOverlay) {
            RenderType detailType = RenderType.armorCutoutNoCull(BASE_TEXTURE);
            boolean foil = SchoolUniformArmorRenderer.this.currentStack != null
                    && SchoolUniformArmorRenderer.this.currentStack.hasFoil();
            VertexConsumer detailBuffer = ItemRenderer.getArmorFoilBuffer(bufferSource, detailType, foil);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, detailType, detailBuffer,
                    partialTick, packedLight, packedOverlay, 0xFFFFFFFF);
        }
    }
}
