package net.alex.guzhenren.client.renderer;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * 人魂实体（魂体交易者[Soul Trader]、地灵[Earth Spirit]）共用的 {@code human_soul} GeckoLib 模型，独立于所穿贴图颜色。
 * <ul>
 *   <li>⚠ 视线转动以叠加方式写入头部骨骼而非直接覆盖：GeckoLib 默认头部转向（启用头部骨骼的
 *       {@code DefaultedEntityGeoModel}）会直接覆盖旋转角，从而抹掉手势动画在该骨骼上打关键帧的点头与摇头；由于魂
 *       体每个动画都会为头部打关键帧，每帧从关键帧重置后再做叠加即可安全保留手势动画。</li>
 * </ul>
 */

public final class HumanSoulGeoModel<T extends Mob & GeoEntity> extends DefaultedEntityGeoModel<T> {

    private static final String HEAD = "head";

    public HumanSoulGeoModel() {
        super(Guzhenren.id("human_soul"), false);
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId,
                                    AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        GeoBone head = this.getAnimationProcessor().getBone(HEAD);
        EntityModelData data = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head == null || data == null) return;

        head.setRotX(head.getRotX() + data.headPitch() * Mth.DEG_TO_RAD);
        head.setRotY(head.getRotY() + data.netHeadYaw() * Mth.DEG_TO_RAD);
    }
}
