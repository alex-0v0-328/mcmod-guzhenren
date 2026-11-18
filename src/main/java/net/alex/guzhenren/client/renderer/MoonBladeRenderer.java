package net.alex.guzhenren.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.projectile.MoonBladeEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * {@link MoonBladeEntity} 的实体渲染器。
 * <ul>
 *   <li>绘制一片全亮且双面可见的月牙，展开在飞行线与其水平横轴构成的平面内（平射贴地、斜射随俯仰倾斜，凸面始终朝
 *       前）。</li>
 *   <li>锁定追踪目标的月刃在转弯时按 {@link MoonBladeEntity#getBankDegrees} 绕飞行线滚转侧倾（左弯时左缘下压，最
 *       大倾角为 {@code MoonBladeArc.MAX_BANK_DEGREES}，各发独立倾斜）。</li>
 *   <li>完全平放的刃面在略高于月刃的射手与旁观者视角下会细成一条线，因此整片刃会绕同时垂直于刃面与视线方向的轴朝镜头
 *       偏转 {@link #TILT_DEGREES}（视角越正对则偏转越小，完全正对时不偏转），确保后方射手与旁观者均能看到约三分之
 *       一刃宽而凸面仍朝前。</li>
 *   <li>贴图由 {@link #FRAMES} 帧 16 像素竖条组成，直接按实体 age 索引读取而非原版动画：前 3 tick 逐帧放大，飞行中
 *       维持满帧，寿命（{@link MoonBladeEntity#getLifetimeTicks}）耗尽前 3 tick 淡出，击中物体时则停留在满帧并消
 *       失。</li>
 * </ul>
 */

public final class MoonBladeRenderer extends EntityRenderer<MoonBladeEntity> {

    private static final ResourceLocation MOON = Guzhenren.id("textures/entity/moon_blade.png");
    private static final ResourceLocation BLOOD = Guzhenren.id("textures/entity/blood_moon_blade.png");
    private static final int FRAMES = 7;
    private static final int FULL_FRAME = 3;
    private static final float HALF_SIZE = 0.75F;
    private static final int COLOR = FastColor.ARGB32.color(255, 255, 255, 255);
    private static final float TILT_DEGREES = 18.0F;
    private static final double VERTICAL = 1.0E-4D;

    public MoonBladeRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override
    public void render(@NotNull MoonBladeEntity blade, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        int frame = getFrame(blade.tickCount, blade.getLifetimeTicks());
        float top = frame / (float) FRAMES;
        float bottom = (frame + 1) / (float) FRAMES;

        Vec3 motion = blade.getDeltaMovement();
        Vector3f forward = motion.lengthSqr() < VERTICAL
                ? new Vector3f(0.0F, 0.0F, 1.0F) : motion.normalize().toVector3f();
        Vector3f side = new Vector3f(-forward.z(), 0.0F, forward.x());
        if (side.lengthSquared() < VERTICAL) side.set(1.0F, 0.0F, 0.0F);
        side.normalize();
        Vector3f normal = forward.cross(side, new Vector3f());
        bank(blade.getBankDegrees(), forward, side, normal);
        tiltTowardCamera(blade, partialTicks, forward, side, normal);

        poseStack.pushPose();
        poseStack.translate(0.0F, blade.getBbHeight() / 2.0F, 0.0F);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(getTextureLocation(blade)));
        vertex(consumer, pose, forward, side, normal, -HALF_SIZE, -HALF_SIZE, 0.0F, bottom);
        vertex(consumer, pose, forward, side, normal, HALF_SIZE, -HALF_SIZE, 1.0F, bottom);
        vertex(consumer, pose, forward, side, normal, HALF_SIZE, HALF_SIZE, 1.0F, top);
        vertex(consumer, pose, forward, side, normal, -HALF_SIZE, HALF_SIZE, 0.0F, top);
        poseStack.popPose();
        super.render(blade, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** 绕飞行线滚转刃面：正值（左倾）抬起 {@code side}，即右缘。 */
    private static void bank(float degrees, Vector3f forward, Vector3f side, Vector3f normal) {
        if (degrees == 0.0F) return;
        Quaternionf roll = new Quaternionf().fromAxisAngleRad(forward, (float) Math.toRadians(-degrees));
        side.rotate(roll);
        normal.rotate(roll);
    }

    private void tiltTowardCamera(MoonBladeEntity blade, float partialTicks, Vector3f forward, Vector3f side,
                                  Vector3f normal) {
        Vec3 toCamera = entityRenderDispatcher.camera.getPosition()
                .subtract(blade.getPosition(partialTicks).add(0.0D, blade.getBbHeight() / 2.0D, 0.0D));
        if (toCamera.lengthSqr() < VERTICAL) return;

        Vector3f view = toCamera.normalize().toVector3f();
        if (normal.dot(view) < 0.0F) normal.negate();
        Vector3f axis = normal.cross(view, new Vector3f());
        if (axis.lengthSquared() < VERTICAL) return;

        float angle = Math.min((float) Math.acos(Math.min(normal.dot(view), 1.0F)),
                (float) Math.toRadians(TILT_DEGREES));
        Quaternionf turn = new Quaternionf().fromAxisAngleRad(axis.normalize(), angle);
        forward.rotate(turn);
        side.rotate(turn);
        normal.rotate(turn);
    }

    static int getFrame(int age, int lifetime) {
        int fading = age - (lifetime - 2);
        if (fading >= 0) return Math.min(FULL_FRAME + 1 + fading, FRAMES - 1);
        return Math.min(age, FULL_FRAME);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vector3f forward, Vector3f side,
                               Vector3f normal, float along, float across, float u, float v) {
        consumer.addVertex(pose, forward.x() * along + side.x() * across, forward.y() * along + side.y() * across,
                        forward.z() * along + side.z() * across)
                .setColor(COLOR)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, normal.x(), normal.y(), normal.z());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull MoonBladeEntity blade) {
        return blade.getVariant() == MoonBladeEntity.Variant.BLOOD ? BLOOD : MOON;
    }
}
