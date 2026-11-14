package net.alex.guzhenren.client.renderer;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitual;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * 开窍[Awakening]仪式中希望蛊[Hope Gu]的世界渲染。
 *
 * <ul>
 *   <li>本客户端可见的每场仪式都画：蛊从玩家主手（眼前 {@link #HAND_FORWARD} 格、主手侧 {@link #HAND_SIDE} 格、下方
 *       {@link #HAND_DROP} 格，本人第一人称下从视野下角升起）飞到同步的悬停点，上下浮动，开窍前缩入光中。</li>
 *   <li>{@code AwakeningEvents} 每帧在实体后调一次；物品模型全亮、正面受光、朝镜头、宽 {@link #SIZE} 格，自用缓
 *       冲。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningTimeline
 * @since 1.0.0
 */

public final class AwakeningGuRenderer {

    private AwakeningGuRenderer() {}

    private static final float SIZE = 0.5F;
    private static final double HAND_FORWARD = 0.4;
    private static final double HAND_SIDE = 0.35;
    private static final double HAND_DROP = 0.35;
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    private static final MultiBufferSource.BufferSource BUFFERS =
            MultiBufferSource.immediate(new ByteBufferBuilder(1536));

    public static void render(@NotNull PoseStack pose, @NotNull Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;

        Vec3 view = camera.getPosition();
        ItemStack gu = null;
        for (Player player : level.players()) {
            AwakeningRitual ritual = AwakeningRitualService.get(player);
            if (ritual == null) continue;
            float elapsed = ritual.getElapsed(level.getGameTime(), partialTick);
            float scale = AwakeningTimeline.getGuScale(elapsed) * SIZE;
            if (elapsed < 0.0F || scale <= 0.0F) continue;

            if (gu == null) gu = new ItemStack(ModItems.HOPE_GU.get());
            Vec3 at = getHand(player, partialTick).lerp(ritual.anchor(), AwakeningTimeline.getFlight(elapsed))
                    .add(0.0, AwakeningTimeline.getBob(elapsed), 0.0);
            pose.pushPose();
            pose.translate(at.x - view.x, at.y - view.y, at.z - view.z);
            pose.mulPose(camera.rotation());
            // The FIXED transform turns an item's front away (item frames face the wall); turn it back.
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            pose.scale(scale, scale, scale);
            minecraft.getItemRenderer().renderStatic(gu, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, pose, BUFFERS, level, player.getId());
            pose.popPose();
        }
        if (gu == null) return;

        // The level's diffuse light shades a camera-facing quad by where the camera looks (about 75% looking level);
        // both lights on the view axis keep the Gu as bright as its icon.
        Vector3f toCamera = camera.getLookVector().negate(new Vector3f());
        RenderSystem.setShaderLights(toCamera, toCamera);
        BUFFERS.endBatch();
        if (level.effects().constantAmbientLight()) {
            Lighting.setupNetherLevel();
        } else {
            Lighting.setupLevel();
        }
    }

    private static Vec3 getHand(Player player, float partialTick) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick));
        Vec3 side = forward.cross(UP).scale(player.getMainArm() == HumanoidArm.RIGHT ? HAND_SIDE : -HAND_SIDE);
        return player.getEyePosition(partialTick).add(forward.scale(HAND_FORWARD)).add(side)
                .subtract(0.0, HAND_DROP, 0.0);
    }
}
