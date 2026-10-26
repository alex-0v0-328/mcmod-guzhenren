package net.alex.guzhenren.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;

/**
 * Draws the skin flash [皮肤闪现] over a player: a translucent, single-color copy of the player model,
 * inflated a little past the skin so it never fights the skin's own pixels, fading with the flash's alpha.
 *
 * <p>One layer per player model (wide and slim), added to both player renderers at
 * {@code EntityRenderersEvent.AddLayers}. The copy takes the parent's pose every frame, the way vanilla's
 * energy swirl does, and the parent's outer-layer visibility (hat, jacket, sleeves, pants), so a player who hid
 * a layer gets no inflated ghost of it; the texture is a flat white square, so the tint IS the color. A flash is
 * the only thing drawn: the worn skin itself changes nothing on the model.
 *
 * @author Alex
 * @version 1.0.0
 * @see SkinFlashes
 * @since 1.0.0
 */

public final class SkinFlashLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation TEXTURE = Guzhenren.id("textures/entity/skin_flash.png");
    private static final float INFLATE = 0.25F;
    private static final int TEXTURE_SIZE = 64;
    private final PlayerModel<AbstractClientPlayer> model;

    public SkinFlashLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                          boolean slim) {
        super(parent);
        this.model = new PlayerModel<>(LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(INFLATE),
                slim), TEXTURE_SIZE, TEXTURE_SIZE).bakeRoot(), slim);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight,
                       @NotNull AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;

        SkinFlashes.Flash flash = SkinFlashes.get(player.getId(), player.level().getGameTime());
        if (flash == null) return;

        float alpha = flash.getAlpha(player.level().getGameTime(), partialTick);
        if (alpha <= 0.0F) return;

        PlayerModel<AbstractClientPlayer> parent = getParentModel();
        parent.copyPropertiesTo(model);
        model.hat.visible = parent.hat.visible;
        model.jacket.visible = parent.jacket.visible;
        model.leftSleeve.visible = parent.leftSleeve.visible;
        model.rightSleeve.visible = parent.rightSleeve.visible;
        model.leftPants.visible = parent.leftPants.visible;
        model.rightPants.visible = parent.rightPants.visible;
        model.prepareMobModel(player, limbSwing, limbSwingAmount, partialTick);
        model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        int tint = flash.getTint();
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                FastColor.ARGB32.color(Math.round(alpha * 255.0F), tint));
    }
}
