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
 * The Hope Gu [希望蛊] of every awakening ritual [开窍仪式] this client can see, drawn in the world: it flies from
 * the player's main hand to the synced hover point, bobs there, and shrinks into the light before the awakening.
 *
 * <p>Static renderer called once a frame after the entities ({@code AwakeningEvents}). The Gu is the item model,
 * full bright, lit head-on and turned to face the camera, {@link #SIZE} blocks across, drawn from its own buffer.
 * The hand point sits {@link #HAND_FORWARD} ahead of the eyes, {@link #HAND_SIDE} to the main-arm side and
 * {@link #HAND_DROP} below, so in the player's own first person the Gu comes up from the lower corner of the view.
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

        ItemStack gu = new ItemStack(ModItems.HOPE_GU.get());
        Vec3 view = camera.getPosition();
        boolean drawn = false;
        for (Player player : level.players()) {
            AwakeningRitual ritual = AwakeningRitualService.get(player);
            if (ritual == null) continue;
            float elapsed = ritual.getElapsed(level.getGameTime(), partialTick);
            float scale = AwakeningTimeline.getGuScale(elapsed) * SIZE;
            if (elapsed < 0.0F || scale <= 0.0F) continue;

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
            drawn = true;
        }
        if (!drawn) return;

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
