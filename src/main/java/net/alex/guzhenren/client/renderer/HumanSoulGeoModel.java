package net.alex.guzhenren.client.renderer;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.trade.SoulTraderEntity;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * 所有魂体交易者共用的 {@code human_soul} GeckoLib 模型，与所穿贴图无关。
 * ⚠ 视线转动叠加到头部骨骼、不覆盖：GeckoLib 自带头部转动（带头部骨骼的 {@code DefaultedEntityGeoModel}）直接设旋转，会
 * 抹掉手势在该骨骼上打关键帧的点头与摇头；魂体每个动画都给头部打关键帧，每个处理帧从关键帧重来，叠加安全。
 *
 * @author Alex
 * @version 1.0.0
 * @see SoulTraderGeoRenderer
 * @since 1.0.0
 */

public final class HumanSoulGeoModel extends DefaultedEntityGeoModel<SoulTraderEntity> {

    private static final String HEAD = "head";

    public HumanSoulGeoModel() {
        super(Guzhenren.id("human_soul"), false);
    }

    @Override
    public void setCustomAnimations(SoulTraderEntity animatable, long instanceId,
                                    AnimationState<SoulTraderEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        GeoBone head = this.getAnimationProcessor().getBone(HEAD);
        EntityModelData data = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head == null || data == null) return;

        head.setRotX(head.getRotX() + data.headPitch() * Mth.DEG_TO_RAD);
        head.setRotY(head.getRotY() + data.netHeadYaw() * Mth.DEG_TO_RAD);
    }
}
