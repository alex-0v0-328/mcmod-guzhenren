package net.alex.guzhenren.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The horse's phantom body [马的兽力虚影]: vanilla's adult horse geometry ({@link ModelLayers#HORSE}), since the
 * phantom texture is painted on vanilla's horse layout, posed here without a horse entity, drawn {@link #SCALE} times
 * the model's size as vanilla draws its horses. The saddle, reins and the
 * foal's legs are hidden for good. {@link #pose} follows vanilla's {@code HorseModel.prepareMobModel} with the head
 * straight ahead, no grazing, no mouth and no tail swish: {@code stand} rears it up as a horse does, the limb swing
 * gallops it.
 *
 * <p>{@link #getRear} is the phantom's one-off attack beside its owner: up over {@link #REAR_RISE_TICKS}, held until
 * {@link #REAR_HOLD_TICKS}, down by {@link #REAR_END_TICKS}, then it stands. A Beast Charge [兽力冲击] gallops at
 * {@link #GALLOP_SWING_PER_TICK} limb swing a tick, at full stride. ⚠ The timings and the gallop rate are
 * provisional.
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastPhantomRenderer
 * @since 1.0.0
 */

public final class HorsePhantomModel {

    public static final float SCALE = 1.1F;
    public static final float REAR_RISE_TICKS = 4.0F;
    public static final float REAR_HOLD_TICKS = 12.0F;
    public static final float REAR_END_TICKS = 18.0F;
    public static final float GALLOP_SWING_PER_TICK = 1.2F;
    private static final float THIRD_PI = (float) (Math.PI / 3.0D);
    private static final float SIXTH_PI = (float) (Math.PI / 6.0D);
    private static final float TWELFTH_PI = (float) (Math.PI / 12.0D);
    private static final float QUARTER_PI = (float) (Math.PI / 4.0D);
    private static final String[] HIDDEN_ROOT_PARTS = {
            "left_hind_baby_leg", "right_hind_baby_leg", "left_front_baby_leg", "right_front_baby_leg" };
    private static final String[] HIDDEN_HEAD_PARTS = {
            "left_saddle_mouth", "right_saddle_mouth", "left_saddle_line", "right_saddle_line", "head_saddle",
            "mouth_saddle_wrap" };
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart headParts;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart tail;

    public HorsePhantomModel(EntityRendererProvider.Context context) {
        this.root = context.bakeLayer(ModelLayers.HORSE);
        this.body = root.getChild("body");
        this.headParts = root.getChild("head_parts");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.tail = body.getChild("tail");
        body.getChild("saddle").visible = false;
        for (String name : HIDDEN_ROOT_PARTS) root.getChild(name).visible = false;
        for (String name : HIDDEN_HEAD_PARTS) headParts.getChild(name).visible = false;
    }

    public static float getRear(float age) {
        if (age <= 0.0F || age >= REAR_END_TICKS) return 0.0F;
        if (age < REAR_RISE_TICKS) return age / REAR_RISE_TICKS;
        if (age <= REAR_HOLD_TICKS) return 1.0F;
        return 1.0F - (age - REAR_HOLD_TICKS) / (REAR_END_TICKS - REAR_HOLD_TICKS);
    }

    public void pose(float stand, float limbSwing, float limbSwingAmount, float ageInTicks) {
        float down = 1.0F - stand;
        float nod = limbSwingAmount > 0.2F ? Mth.cos(limbSwing * 0.8F) * 0.15F * limbSwingAmount : 0.0F;
        headParts.xRot = stand * (TWELFTH_PI + nod) + down * (SIXTH_PI + nod);
        headParts.yRot = 0.0F;
        headParts.y = stand * -4.0F + down * 4.0F;
        headParts.z = stand * -4.0F + down * -12.0F;
        body.xRot = stand * -QUARTER_PI;
        body.y = 11.0F;

        float swing = Mth.cos(limbSwing * 0.6662F + (float) Math.PI);
        float stride = swing * 0.8F * limbSwingAmount;
        float paw = Mth.cos(ageInTicks * 0.6F + (float) Math.PI);
        float hindLift = TWELFTH_PI * stand;
        leftFrontLeg.y = 2.0F * stand + 14.0F * down;
        leftFrontLeg.z = -6.0F * stand - 10.0F * down;
        rightFrontLeg.y = leftFrontLeg.y;
        rightFrontLeg.z = leftFrontLeg.z;
        leftFrontLeg.xRot = (-THIRD_PI + paw) * stand + stride * down;
        rightFrontLeg.xRot = (-THIRD_PI - paw) * stand - stride * down;
        leftHindLeg.xRot = hindLift - swing * 0.5F * limbSwingAmount * down;
        rightHindLeg.xRot = hindLift + swing * 0.5F * limbSwingAmount * down;

        tail.xRot = SIXTH_PI + limbSwingAmount * 0.75F;
        tail.yRot = 0.0F;
        tail.y = -5.0F + limbSwingAmount;
        tail.z = 2.0F + limbSwingAmount * 2.0F;
    }

    public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
