package net.alex.guzhenren.client.renderer.armor;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.item.armor.SchoolUniformItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 学堂制服[School Uniform]的 GeckoLib 护甲模型定义：主通道绑定灰度主布料贴图（供染色乘色），固定细节层由
 * {@link SchoolUniformArmorRenderer} 叠加绘制。
 */

public final class SchoolUniformArmorModel extends GeoModel<SchoolUniformItem> {

    private static final ResourceLocation MODEL = Guzhenren.id("geo/armor/school_uniform.geo.json");
    private static final ResourceLocation LAYER_TEXTURE = Guzhenren.id("textures/armor/school_uniform_layer.png");
    private static final ResourceLocation ANIMATION = Guzhenren.id("animations/armor/school_uniform.animation.json");

    @Override
    public ResourceLocation getModelResource(SchoolUniformItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SchoolUniformItem animatable) {
        return LAYER_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SchoolUniformItem animatable) {
        return ANIMATION;
    }
}
