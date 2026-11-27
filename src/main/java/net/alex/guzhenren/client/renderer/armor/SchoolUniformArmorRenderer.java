package net.alex.guzhenren.client.renderer.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.item.armor.SchoolUniformItem;
import net.alex.guzhenren.item.armor.UniformColor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
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
 *   <li>Epic Fight 下不走本渲染器：EF 读 {@code animmodels/armor/<物品 id>.json} 三份蒙皮网格（几何与 UV 同
 *       {@code school_uniform.geo.json}，下摆随大腿），改 geo 几何或 UV 须从 Blockbench 工程重新生成。</li>
 * </ul>
 */

public final class SchoolUniformArmorRenderer extends GeoArmorRenderer<SchoolUniformItem> {

    private static final ResourceLocation BASE_TEXTURE = Guzhenren.id("textures/armor/school_uniform_base.png");

    public SchoolUniformArmorRenderer() {
        super(new SchoolUniformArmorModel());
        addRenderLayer(new BaseDetailLayer(this));
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
