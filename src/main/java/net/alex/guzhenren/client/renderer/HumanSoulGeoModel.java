package net.alex.guzhenren.client.renderer;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.trade.SoulTraderEntity;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * The {@code human_soul} GeckoLib model shared by every soul trader, whatever texture it wears.
 *
 * <p>⚠ The look turn is added to the head bone, not written over it. GeckoLib's own head turn
 * ({@code DefaultedEntityGeoModel} with a head bone) sets the rotation outright and would erase the nod and
 * the head shake the gestures key on that bone. Adding is safe because every animation the soul plays keys the
 * head, so each processed frame starts the bone from its keyframe again.
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
        CoreGeoBone head = this.getAnimationProcessor().getBone(HEAD);
        EntityModelData data = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head == null || data == null) return;

        head.setRotX(head.getRotX() + data.headPitch() * Mth.DEG_TO_RAD);
        head.setRotY(head.getRotY() + data.netHeadYaw() * Mth.DEG_TO_RAD);
    }
}
